package org.qualet.refreshedui.client.anim;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.ui.IListMotionHost;
import org.qualet.refreshedui.client.ui.RoundedAreas;
import org.qualet.refreshedui.client.ui.UICornerRadii;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rows of a {@code UIList} move instead of jumping. One of these lives on every list
 * ({@code UIListMixin}) and covers four things:
 *
 * <ul>
 *     <li><b>The pick slides.</b> Picking another row sends the selection fill over from the row picked
 *     before ({@link #hidesPick} keeps the new row from drawing its own fill until it arrives).</li>
 *     <li><b>Folders unfold.</b> When a branch opens, the rows below slide down and the new rows are
 *     uncovered behind them, fading in; when it closes, the rows below slide up over the leaving rows,
 *     which fade out. The fold arrow turns ({@link #arrowAngle}).</li>
 *     <li><b>Dragged rows make room.</b> While rows are carried over their own list the rest part to open a
 *     gap where they would land, and on release the carried rows settle into it from the cursor.</li>
 *     <li>Everything else stays as stock: filtering, a list rebuilt with new contents, rows added or
 *     removed by commands — the fold and drop animations only follow a fold that flipped or a drop.</li>
 * </ul>
 *
 * <p>Rows are followed across frames by identity, and across a rebuild (lists that re-wrap their data on
 * every change, like the replay list) by the list's own notion of "the same row" — its selection's
 * predicate. A row's position is kept in content pixels, so scrolling and row resizing mid-motion are
 * harmless. When nothing moves the list renders exactly as stock; while something does,
 * {@link #render} replaces the stock row loop (whose culling would drop a row sliding into view).</p>
 *
 * <p>Gated on {@link Animations#enabled()}; render thread only.</p>
 */
public final class ListMotion
{
    public static final long MOVE_MS = 180L;
    public static final long PILL_MS = 170L;
    public static final long ARROW_MS = 170L;

    /** A list that skipped more frames than this is starting afresh (it was hidden, or its screen closed). */
    private static final long CONTINUITY_MS = 250L;
    /** Matching a rebuilt list row against row is quadratic; lists this big just change without motion. */
    private static final long MATCH_BUDGET = 250_000L;

    /** Lists whose rows draw through {@code UIList.renderListElement} — where the pick fill can be held back. */
    private static final ClassValue<Boolean> PLAIN_ROWS = new ClassValue<>()
    {
        @Override
        protected Boolean computeValue(Class<?> type)
        {
            try
            {
                return type.getMethod("renderListElement", UIContext.class, Object.class, int.class, int.class, int.class, boolean.class, boolean.class).getDeclaringClass() == UIList.class;
            }
            catch (NoSuchMethodException e)
            {
                return false;
            }
        }
    };

    private List<Object> last;
    private boolean lastFiltering;
    private long lastFrame = Long.MIN_VALUE / 2L;

    private final Map<Object, Move> moves = new IdentityHashMap<>();
    private final Map<Object, Block> reveals = new IdentityHashMap<>();
    private final List<Ghost> ghosts = new ArrayList<>();
    private final Map<Object, Tween> arrows = new IdentityHashMap<>();

    private boolean dragging;
    private final List<Object> dragged = new ArrayList<>();
    private float ghostY;
    private float ghostX;
    private int gapSlot = -1;
    private int gapRows;

    private Object picked;
    private final Tween pill = new Tween(0F);
    private boolean pillMoving;

    /**
     * Called before the list paints its rows. Brings the motion up to date, draws the travelling pick,
     * and says whether rows are moving — in which case the caller hands the row loop to {@link #render}.
     */
    public boolean begin(UIList<?> list, IListMotionHost host, UIContext context)
    {
        long now = Tween.now();
        List<?> visible = host.refreshedui$visible();
        int s = list.rowHeight();
        boolean filtering = list.isFiltering();
        boolean continuity = this.last != null && now - this.lastFrame <= CONTINUITY_MS && filtering == this.lastFiltering;

        this.lastFrame = now;
        this.lastFiltering = filtering;

        if (!Animations.enabled() || filtering)
        {
            this.clear();
            this.picked = null;
            this.dragging = false;

            if (!sameRows(visible, this.last))
            {
                this.snapshot(visible);
            }

            return false;
        }

        boolean dragNow = list.drag.isActive();
        boolean dragEnded = this.dragging && !dragNow;

        if (!sameRows(visible, this.last))
        {
            if (continuity)
            {
                this.onChange(list, host, visible, s, now, dragEnded);
            }
            else
            {
                this.clear();
                this.pruneArrows(visible);
            }

            this.snapshot(visible);
        }
        else if (!continuity)
        {
            this.clear();
        }

        if (dragNow)
        {
            this.followDrag(list, visible, s, context, now);
        }
        else
        {
            if (dragEnded)
            {
                this.settleDragged(list, visible, s, now);
            }

            this.rest(visible, s, now);
        }

        this.dragging = dragNow;
        this.expire(now);
        this.updatePill(list, visible, s, context, now);

        return dragNow || !this.moves.isEmpty() || !this.reveals.isEmpty() || !this.ghosts.isEmpty();
    }

    /** The stock row loop, with every row at its moving position, new rows uncovered, leaving rows fading. */
    public void render(UIList<?> list, IListMotionHost host, UIContext context)
    {
        long now = Tween.now();
        List<?> visible = host.refreshedui$visible();
        int s = list.rowHeight();
        int originY = list.area.y - (int) list.scroll.getScroll();
        int low = list.area.y;
        int high = list.area.ey();
        int w = list.area.w;
        UIList<Object> rows = (UIList<Object>) list;

        for (int j = 0; j < visible.size(); j++)
        {
            Object row = visible.get(j);

            /* Carried rows are lifted out of the list — the drag ghost draws them by the cursor */
            if (this.dragging && indexOfIdentity(this.dragged, row) >= 0)
            {
                continue;
            }

            Move move = this.moves.get(row);
            int y = originY + Math.round(move == null ? j * s : move.y.value(now));
            int x = list.area.x + Math.round(move == null ? 0F : move.x.value(now));

            if (y + s < low || y >= high)
            {
                continue;
            }

            boolean hover = context.mouseX >= x && context.mouseY >= y && context.mouseX < x + w && context.mouseY < y + s;
            boolean selected = list.current.contains(j);
            Block reveal = this.reveals.get(row);

            if (reveal == null)
            {
                rows.renderListElement(context, row, j, x, y, hover, selected);

                continue;
            }

            float t = reveal.progress(now);
            /* The rows below slide down from the top of the new block; what they have uncovered shows */
            int front = originY + Math.round(reveal.top + reveal.height * t);
            int index = j;

            drawPartly(context, x, y, w, Math.min(s, front - y), t, () -> rows.renderListElement(context, row, index, x, y, hover, selected));
        }

        for (Ghost ghost : this.ghosts)
        {
            float t = ghost.block.progress(now);
            int y = originY + Math.round(ghost.y);
            int x = list.area.x + Math.round(ghost.x);

            if (y + s < low || y >= high)
            {
                continue;
            }

            /* The rows below slide up over the leaving block, covering it from the bottom as it fades */
            int front = originY + Math.round(ghost.block.top + ghost.block.height * (1F - t));

            drawPartly(context, x, y, w, Math.min(s, front - y), 1F - t, () -> rows.renderListElement(context, ghost.row, ghost.index, x, y, false, false));
        }
    }

    /** While the pick travels, the row it travels to leaves its own fill to the travelling one. */
    public boolean hidesPick(Object row)
    {
        return this.pillMoving && row == this.picked;
    }

    /** Whether the list is showing a gap for rows being dragged over it. */
    public boolean hasGap()
    {
        return this.dragging && this.gapSlot >= 0;
    }

    /** Content y of the middle of the gap. */
    public float gapMiddle(int rowHeight)
    {
        return (this.gapSlot + this.gapRows / 2F) * rowHeight;
    }

    /** Rotation, in degrees, to draw a branch row's fold arrow at: it turns when the branch flips. */
    public float arrowAngle(Object row, boolean expanded)
    {
        float target = expanded ? 90F : 0F;
        Tween arrow = this.arrows.get(row);

        if (arrow == null)
        {
            this.arrows.put(row, new Tween(target));

            return target;
        }

        long now = Tween.now();

        arrow.animateTo(target, ARROW_MS, Easings.OUT_CUBIC, now);

        return arrow.value(now);
    }

    /* The list changed */

    private void onChange(UIList<?> list, IListMotionHost host, List<?> visible, int s, long now, boolean dragEnded)
    {
        List<Object> old = this.last;
        int n = visible.size();
        int m = old.size();

        if ((long) n * m > MATCH_BUDGET)
        {
            this.clear();
            this.pruneArrows(visible);

            return;
        }

        Map<Object, Integer> oldIndex = new IdentityHashMap<>(m * 2);

        for (int i = 0; i < m; i++)
        {
            oldIndex.put(old.get(i), i);
        }

        int[] match = new int[n];
        boolean[] kept = new boolean[m];
        int matched = 0;
        int previous = -1;
        boolean ordered = true;
        boolean folded = false;

        for (int j = 0; j < n; j++)
        {
            Object row = visible.get(j);
            Integer found = oldIndex.get(row);
            int i = found != null ? found : ((UIList<Object>) list).selection.indexOf(old, row);

            if (i >= 0 && kept[i])
            {
                i = -1;
            }

            match[j] = i;

            if (i < 0)
            {
                continue;
            }

            kept[i] = true;
            matched += 1;
            ordered &= i > previous;
            previous = i;

            Object was = old.get(i);

            /* A rebuilt row carries on the old one's arrow and hover */
            if (was != row)
            {
                Tween arrow = this.arrows.remove(was);

                if (arrow != null)
                {
                    this.arrows.put(row, arrow);
                }

                HoverFade.transfer(list, was, row);
            }

            Tween arrow = this.arrows.get(row);
            Boolean expanded = host.refreshedui$branch(row);

            if (arrow != null && expanded != null && (arrow.target() > 45F) != expanded)
            {
                folded = true;
            }
        }

        this.pruneArrows(visible);

        if (matched == 0 || !(folded || dragEnded) || (!ordered && !dragEnded))
        {
            this.moves.clear();
            this.reveals.clear();
            this.ghosts.clear();

            return;
        }

        /* Where every old row is on screen right now */
        float[] oldY = new float[m];
        float[] oldX = new float[m];

        for (int i = 0; i < m; i++)
        {
            Object row = old.get(i);
            Move move = this.moves.get(row);
            int carried = dragEnded ? indexOfIdentity(this.dragged, row) : -1;

            if (carried >= 0)
            {
                oldY[i] = this.ghostY + carried * s;
                oldX[i] = this.ghostX;
            }
            else if (move != null)
            {
                oldY[i] = move.y.value(now);
                oldX[i] = move.x.value(now);
            }
            else
            {
                oldY[i] = i * s;
            }
        }

        Map<Object, Move> moves = new IdentityHashMap<>();

        this.reveals.clear();
        this.ghosts.clear();

        for (int j = 0; j < n; j++)
        {
            Object row = visible.get(j);
            int i = match[j];

            if (i >= 0)
            {
                if (oldY[i] != j * s || oldX[i] != 0F)
                {
                    moves.put(row, new Move(oldY[i], oldX[i], j * s, now));
                }

                continue;
            }

            /* A run of new rows is uncovered as one block */
            int end = j;

            while (end + 1 < n && match[end + 1] < 0)
            {
                end += 1;
            }

            Block block = new Block(now, j * s, (end - j + 1) * s);

            for (int k = j; k <= end; k++)
            {
                this.reveals.put(visible.get(k), block);
            }

            j = end;
        }

        for (int i = 0; i < m; i++)
        {
            if (kept[i])
            {
                continue;
            }

            int end = i;

            while (end + 1 < m && !kept[end + 1])
            {
                end += 1;
            }

            Block block = new Block(now, oldY[i], (end - i + 1) * s);

            for (int k = i; k <= end; k++)
            {
                this.ghosts.add(new Ghost(old.get(k), k, oldY[k], oldX[k], block));
            }

            i = end;
        }

        this.moves.clear();
        this.moves.putAll(moves);
    }

    /* Dragging */

    private void followDrag(UIList<?> list, List<?> visible, int s, UIContext context, long now)
    {
        UIList<Object> rows = (UIList<Object>) list;

        this.dragged.clear();

        for (Object row : visible)
        {
            if (rows.drag.isDragging(row))
            {
                this.dragged.add(row);
            }
        }

        int carried = this.dragged.size();
        int insertion = list.drag.getTarget() == list ? list.drag.getInsertion() : -1;
        boolean gap = insertion >= 0 && carried > 0;
        int kept = 0;

        this.gapSlot = -1;
        this.gapRows = carried;

        for (int j = 0; j < visible.size(); j++)
        {
            Object row = visible.get(j);

            if (gap && j == insertion)
            {
                this.gapSlot = kept;
            }

            if (indexOfIdentity(this.dragged, row) >= 0)
            {
                continue;
            }

            /* Rows past the caret step down by the carried rows; without a caret everyone keeps their place */
            int slot = gap ? kept + (j >= insertion ? carried : 0) : j;

            this.target(row, j, slot * s, s, now);
            kept += 1;
        }

        if (gap && this.gapSlot < 0)
        {
            this.gapSlot = kept;
        }

        /* Where the drag ghost is (UIList.renderDragGhost), for the settle on release */
        int originY = list.area.y - (int) list.scroll.getScroll();

        this.ghostY = context.mouseY - s / 2 - originY;
        this.ghostX = context.mouseX + 6 - list.area.x;
    }

    /** Carried rows land: from the cursor into their slot. */
    private void settleDragged(UIList<?> list, List<?> visible, int s, long now)
    {
        UIList<Object> rows = (UIList<Object>) list;

        for (Object row : visible)
        {
            if (this.moves.containsKey(row))
            {
                continue;
            }

            int carried = indexOfIdentity(this.dragged, row);

            if (carried < 0)
            {
                carried = rows.selection.indexOf(this.dragged, row);
            }

            if (carried >= 0)
            {
                Move move = new Move(this.ghostY + carried * s, this.ghostX);

                this.moves.put(row, move);
            }
        }

        this.dragged.clear();
        this.gapSlot = -1;
    }

    private void target(Object row, int index, float y, int s, long now)
    {
        Move move = this.moves.get(row);

        if (move == null)
        {
            if (y == index * s)
            {
                return;
            }

            move = new Move(index * s, 0F);
            this.moves.put(row, move);
        }

        move.y.animateTo(y, MOVE_MS, Easings.OUT_CUBIC, now);
        move.x.animateTo(0F, MOVE_MS, Easings.OUT_CUBIC, now);
    }

    /** Send every moving row home and forget the ones that got there. */
    private void rest(List<?> visible, int s, long now)
    {
        if (this.moves.isEmpty())
        {
            return;
        }

        Map<Object, Integer> index = indexMap(visible);
        Iterator<Map.Entry<Object, Move>> it = this.moves.entrySet().iterator();

        while (it.hasNext())
        {
            Map.Entry<Object, Move> entry = it.next();
            Integer j = index.get(entry.getKey());

            if (j == null)
            {
                it.remove();

                continue;
            }

            Move move = entry.getValue();

            move.y.animateTo(j * s, MOVE_MS, Easings.OUT_CUBIC, now);
            move.x.animateTo(0F, MOVE_MS, Easings.OUT_CUBIC, now);

            if (!move.y.isAnimating(now) && !move.x.isAnimating(now))
            {
                it.remove();
            }
        }
    }

    private void expire(long now)
    {
        if (!this.reveals.isEmpty())
        {
            this.reveals.values().removeIf(block -> block.done(now));
        }

        if (!this.ghosts.isEmpty())
        {
            this.ghosts.removeIf(ghost -> ghost.block.done(now));
        }
    }

    /* The travelling pick */

    private void updatePill(UIList<?> list, List<?> visible, int s, UIContext context, long now)
    {
        Object pick = list.selection.size() == 1 ? list.selection.getFirst() : null;
        int j = pick == null ? -1 : indexOfIdentity(visible, pick);

        if (j < 0 || this.dragging || !PLAIN_ROWS.get(list.getClass()))
        {
            this.pillMoving = false;
            this.picked = j < 0 ? null : pick;

            return;
        }

        float to = this.displayY(pick, j, s, now);

        if (this.picked != null && pick != this.picked && !same(list, this.picked, pick))
        {
            int was = indexOfIdentity(visible, this.picked);
            float from = this.pillMoving ? this.pill.value(now) : (was >= 0 ? this.displayY(this.picked, was, s, now) : Float.NaN);
            float top = (float) list.scroll.getScroll() - s;
            float bottom = top + list.area.h + 2 * s;

            /* Only from a row that is on screen; a pick made far away just appears */
            this.pillMoving = from >= top && from <= bottom;

            if (this.pillMoving)
            {
                this.pill.snap(from);
                this.pill.animateTo(to, PILL_MS, Easings.OUT_CUBIC, now);
            }
        }

        this.picked = pick;

        if (!this.pillMoving)
        {
            return;
        }

        this.pill.animateTo(to, PILL_MS, Easings.OUT_CUBIC, now);

        if (!this.pill.isAnimating(now))
        {
            this.pillMoving = false;

            return;
        }

        int originY = list.area.y - (int) list.scroll.getScroll();
        int y = originY + Math.round(to);
        boolean hover = context.mouseX >= list.area.x && context.mouseY >= y && context.mouseX < list.area.ex() && context.mouseY < y + s;
        int accent = BBSSettings.primaryColor.get() & Colors.RGB;

        RoundedAreas.roundedBox(context.batcher, list.area.x, originY + this.pill.value(now), list.area.w, s,
            UICornerRadii.buttonsAndTrackpads(), Colors.setA(accent, hover ? 0.75F : 0.5F));
    }

    private float displayY(Object row, int index, int s, long now)
    {
        Move move = this.moves.get(row);

        return move == null ? index * s : move.y.value(now);
    }

    /* Bookkeeping */

    private void snapshot(List<?> visible)
    {
        if (this.last == null)
        {
            this.last = new ArrayList<>(visible);
        }
        else
        {
            this.last.clear();
            this.last.addAll(visible);
        }
    }

    private void clear()
    {
        this.moves.clear();
        this.reveals.clear();
        this.ghosts.clear();
        this.dragged.clear();
        this.gapSlot = -1;
        this.pillMoving = false;
    }

    private void pruneArrows(List<?> visible)
    {
        if (this.arrows.isEmpty())
        {
            return;
        }

        Map<Object, Integer> index = indexMap(visible);

        this.arrows.keySet().removeIf(row -> !index.containsKey(row));
    }

    private static boolean same(UIList<?> list, Object a, Object b)
    {
        return ((UIList<Object>) list).selection.indexOf(Collections.singletonList(a), b) == 0;
    }

    private static boolean sameRows(List<?> a, List<?> b)
    {
        if (b == null || a.size() != b.size())
        {
            return false;
        }

        for (int i = 0, c = a.size(); i < c; i++)
        {
            if (a.get(i) != b.get(i))
            {
                return false;
            }
        }

        return true;
    }

    private static int indexOfIdentity(List<?> list, Object item)
    {
        for (int i = 0, c = list.size(); i < c; i++)
        {
            if (list.get(i) == item)
            {
                return i;
            }
        }

        return -1;
    }

    private static Map<Object, Integer> indexMap(List<?> list)
    {
        Map<Object, Integer> index = new IdentityHashMap<>(list.size() * 2);

        for (int i = 0, c = list.size(); i < c; i++)
        {
            index.put(list.get(i), i);
        }

        return index;
    }

    /** Draw a row clipped to its top {@code h} pixels at {@code alpha}. */
    private static void drawPartly(UIContext context, int x, int y, int w, int h, float alpha, Runnable row)
    {
        if (h <= 0 || alpha <= 0F)
        {
            return;
        }

        context.batcher.clip(x, y, w, h, context);
        context.batcher.flush();
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);

        row.run();

        context.batcher.flush();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        context.batcher.unclip(context);
    }

    /** A row on its way somewhere: content y and x offset. */
    private static final class Move
    {
        final Tween y;
        final Tween x;

        Move(float y, float x)
        {
            this.y = new Tween(y);
            this.x = new Tween(x);
        }

        Move(float fromY, float fromX, float toY, long now)
        {
            this(fromY, fromX);

            this.y.animateTo(toY, MOVE_MS, Easings.OUT_CUBIC, now);
            this.x.animateTo(0F, MOVE_MS, Easings.OUT_CUBIC, now);
        }
    }

    /** A run of rows appearing or leaving together, and the content span it covers. */
    private static final class Block
    {
        final long start;
        final float top;
        final float height;

        Block(long start, float top, float height)
        {
            this.start = start;
            this.top = top;
            this.height = height;
        }

        float progress(long now)
        {
            float t = (now - this.start) / (float) MOVE_MS;

            return t >= 1F ? 1F : Easings.outCubic(Math.max(0F, t));
        }

        boolean done(long now)
        {
            return now - this.start >= MOVE_MS;
        }
    }

    /** A row that left the list, drawn where it was until the rows below have closed over it. */
    private static final class Ghost
    {
        final Object row;
        final int index;
        final float y;
        final float x;
        final Block block;

        Ghost(Object row, int index, float y, float x, Block block)
        {
            this.row = row;
            this.index = index;
            this.y = y;
            this.x = x;
            this.block = block;
        }
    }
}
