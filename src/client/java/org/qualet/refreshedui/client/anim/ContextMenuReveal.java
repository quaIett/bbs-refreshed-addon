package org.qualet.refreshedui.client.anim;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.utils.Area;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Context menus open out of the point that was clicked — a small scale-up plus a fade — and fade back
 * into it when they close.
 *
 * <p><b>Open</b> is armed when {@code UIContext} puts a menu on screen ({@code setContextMenu},
 * {@code replaceContextMenu}, and a step back to a parked menu) and played by wrapping the menu's render
 * in its container's children loop ({@code UISectionBodyRevealMixin}). The scale origin is the menu
 * corner nearest the cursor, which is where BBS anchors the menu.</p>
 *
 * <p><b>Close</b> must not hold the menu on screen: the action a row runs has already happened, and a
 * menu kept in the tree would still take clicks. So the menu leaves the tree as stock, and a detached
 * "ghost" of it is drawn for the few frames of the fade by the overlay container after its children
 * ({@code UIElementRenderMixin}), with the pointer moved out of its way so nothing in it lights up.</p>
 *
 * <p>Both directions composite the menu as one picture ({@link OverlaySnapshot#fade}). Gated on
 * {@link Animations#enabled()}; render thread only.</p>
 */
public final class ContextMenuReveal
{
    public static final long OPEN_MS = 150L;
    public static final long CLOSE_MS = 110L;
    /** Scale a menu starts from (open) and shrinks toward (close), around its origin corner. */
    public static final float SCALE_FROM = 0.94F;

    private static final Map<UIContextMenu, Pop> opening = new IdentityHashMap<>();
    private static final List<Ghost> ghosts = new ArrayList<>();

    private ContextMenuReveal()
    {}

    /** A menu was just put on screen by {@code context}: play its opening. */
    public static void armOpen(UIContextMenu menu, UIContext context)
    {
        if (menu == null || !menu.hasParent() || !Animations.enabled())
        {
            return;
        }

        opening.keySet().removeIf(m -> !m.hasParent());

        Area area = menu.area;
        float ox = clamp(context.mouseX(), area.x, area.ex());
        float oy = clamp(context.mouseY(), area.y, area.ey());

        opening.put(menu, new Pop(Tween.now(), ox, oy));
    }

    /**
     * Render {@code menu} with its opening applied, if it is opening. Returns false when it is not
     * (the caller renders it as usual).
     */
    public static boolean renderOpening(UIContextMenu menu, UIContext context)
    {
        if (opening.isEmpty())
        {
            return false;
        }

        Pop pop = opening.get(menu);

        if (pop == null)
        {
            return false;
        }

        float vis = pop.visibility(Tween.now());

        if (vis >= 1F || !Animations.enabled())
        {
            opening.remove(menu);

            return false;
        }

        render(menu, context, pop.ox, pop.oy, vis);

        return true;
    }

    /** {@code menu} (the one on screen) is leaving the tree: keep drawing it while it fades out. */
    public static void armClose(UIContextMenu menu)
    {
        UIElement container = menu.getParent();
        Pop pop = opening.remove(menu);

        if (container == null || !menu.isVisible() || !Animations.enabled())
        {
            return;
        }

        long now = Tween.now();
        float from = pop == null ? 1F : pop.visibility(now);
        float ox = pop == null ? menu.area.mx() : pop.ox;
        float oy = pop == null ? menu.area.y : pop.oy;

        ghosts.add(new Ghost(menu, container, now, ox, oy, from));
    }

    /** Draw the fading copies of menus that closed over {@code container}. After its children. */
    public static void renderGhosts(UIElement container, UIContext context)
    {
        if (ghosts.isEmpty())
        {
            return;
        }

        long now = Tween.now();
        Iterator<Ghost> it = ghosts.iterator();

        while (it.hasNext())
        {
            Ghost ghost = it.next();
            float t = (now - ghost.start) / (float) Animations.ms(CLOSE_MS);

            if (t >= 1F || !Animations.enabled())
            {
                it.remove();

                continue;
            }

            if (ghost.container != container)
            {
                continue;
            }

            float vis = ghost.from * (1F - Easings.outCubic(t));
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;

            /* Nothing in a closed menu answers to the pointer: no hovered row, no tooltip */
            context.mouseX = context.mouseY = -100000;

            try
            {
                render(ghost.menu, context, ghost.ox, ghost.oy, vis);
            }
            finally
            {
                context.mouseX = mouseX;
                context.mouseY = mouseY;
            }
        }
    }

    private static void render(UIContextMenu menu, UIContext context, float ox, float oy, float vis)
    {
        float scale = SCALE_FROM + (1F - SCALE_FROM) * vis;

        OverlaySnapshot.fade(context, vis, () ->
        {
            MatrixStack matrices = context.batcher.getContext().getMatrices();

            matrices.push();
            matrices.translate(ox, oy, 0F);
            matrices.scale(scale, scale, 1F);
            matrices.translate(-ox, -oy, 0F);
            menu.render(context);
            matrices.pop();
        });
    }

    private static float clamp(float v, float min, float max)
    {
        return v < min ? min : (v > max ? max : v);
    }

    private static final class Pop
    {
        final long start;
        final float ox;
        final float oy;

        Pop(long start, float ox, float oy)
        {
            this.start = start;
            this.ox = ox;
            this.oy = oy;
        }

        float visibility(long now)
        {
            float t = (now - this.start) / (float) Animations.ms(OPEN_MS);

            return t >= 1F ? 1F : Easings.outCubic(Math.max(0F, t));
        }
    }

    private static final class Ghost
    {
        final UIContextMenu menu;
        final UIElement container;
        final long start;
        final float ox;
        final float oy;
        final float from;

        Ghost(UIContextMenu menu, UIElement container, long start, float ox, float oy, float from)
        {
            this.menu = menu;
            this.container = container;
            this.start = start;
            this.ox = ox;
            this.oy = oy;
            this.from = from;
        }
    }
}
