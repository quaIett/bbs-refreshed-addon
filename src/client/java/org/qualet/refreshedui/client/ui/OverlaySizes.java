package org.qualet.refreshedui.client.ui;

import mchorse.bbs_mod.ui.dashboard.textures.UIResizeTextureOverlayPanel;
import mchorse.bbs_mod.ui.film.UIFilmDetailsOverlayPanel;
import mchorse.bbs_mod.ui.film.UIFilmMoveOverlayPanel;
import mchorse.bbs_mod.ui.film.UIFilmPlayerSettingsOverlayPanel;
import mchorse.bbs_mod.ui.film.replays.UIRecordOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIFolderOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UILabelListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UILabelOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UISoundOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UITextareaOverlayPanel;

import java.util.HashMap;
import java.util.Map;

/**
 * Fixed preferred pixel sizes for popup overlay panels (3.7).
 *
 * <p>Upstream bbs-fs adds {@code getDefaultOverlayWidth/Height()} overrides on 16 overlay panels
 * (values in {@code UIConstants}). The addon keeps the base mod clean by mapping panel class ->
 * size here instead; {@code UIOverlayMixin} consults this map when {@code addOverlay} sizes a panel.
 * Exact-class lookup mirrors per-class overrides (an unmapped panel keeps the half-screen fallback).</p>
 */
public final class OverlaySizes
{
    private static final Map<Class<?>, int[]> SIZES = new HashMap<>();

    static
    {
        SIZES.put(UIMessageOverlayPanel.class, new int[] {320, 116});
        SIZES.put(UIPromptOverlayPanel.class, new int[] {280, 120});
        SIZES.put(UINumberOverlayPanel.class, new int[] {280, 120});
        SIZES.put(UIConfirmOverlayPanel.class, new int[] {280, 100});
        SIZES.put(UIFolderOverlayPanel.class, new int[] {320, 344});
        SIZES.put(UIRecordOverlayPanel.class, new int[] {320, 104});
        SIZES.put(UITextareaOverlayPanel.class, new int[] {300, 204});
        SIZES.put(UIFilmDetailsOverlayPanel.class, new int[] {300, 228});
        SIZES.put(UIResizeTextureOverlayPanel.class, new int[] {260, 120});
        SIZES.put(UIListOverlayPanel.class, new int[] {320, 280});
        SIZES.put(UILabelOverlayPanel.class, new int[] {320, 280});
        SIZES.put(UILabelListOverlayPanel.class, new int[] {320, 280});
        SIZES.put(UIStringOverlayPanel.class, new int[] {320, 280});
        SIZES.put(UISoundOverlayPanel.class, new int[] {420, 320});
        SIZES.put(UIFilmPlayerSettingsOverlayPanel.class, new int[] {280, 188});
        SIZES.put(UIFilmMoveOverlayPanel.class, new int[] {272, 112});
    }

    /** Returns {@code {w, h}} px for the panel's exact class, or {@code null} for the half-screen fallback. */
    public static int[] sizeFor(UIOverlayPanel panel)
    {
        return SIZES.get(panel.getClass());
    }

    private OverlaySizes()
    {}
}
