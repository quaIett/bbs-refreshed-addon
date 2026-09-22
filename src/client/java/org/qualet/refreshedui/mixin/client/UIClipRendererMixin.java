package org.qualet.refreshedui.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.colors.Oklab;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.ui.IClipHover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Timeline clip strips (camera and action clips share this renderer):
 * <ul>
 *   <li>no current-clip drop shadow (3.6) — the refreshed theme uses outlines instead of shadows;</li>
 *   <li>with the "Grey clips" setting ({@link RefreshedUiAddon#greyClips}, default on) every fill (plain box,
 *   audio/video waveform backing, disabled hatch) is one neutral grey, a few lightness steps above the surface
 *   ladder so clips read on both deep and base track rows; the clip type's colour moves to the outline, which
 *   no longer turns white — a hovered clip lifts its fill toward white a little, a selected one more
 *   ({@code UIClipsMixin} drops BBS' white hover frame).</li>
 * </ul>
 */
@Mixin(UIClipRenderer.class)
public abstract class UIClipRendererMixin
{
    /** Oklab lightness above {@link BBSSettings#baseSurface()} (surface step is 0.022, divider 0.054). */
    @Unique
    private static final float CLIP_LIGHTNESS = 0.09F;

    /** How far the fill moves toward white under the pointer. */
    @Unique
    private static final float HOVER_LIFT = 0.12F;

    @Unique
    private static final Oklab CLIP_OKLAB = new Oklab();

    @Unique
    private static int greySource = -1;

    @Unique
    private static int grey;

    @Unique
    private static boolean refreshedui$greyClips()
    {
        return RefreshedUiAddon.greyClips != null && RefreshedUiAddon.greyClips.get();
    }

    /** Derived from the base surface so a tinted secondary colour tints the clips too; rebuilt only when it moves. */
    @Unique
    private static int refreshedui$grey()
    {
        int base = BBSSettings.baseSurface();

        if (base != greySource)
        {
            CLIP_OKLAB.set(base);
            grey = Colors.A100 | CLIP_OKLAB.toRGB(CLIP_OKLAB.l + CLIP_LIGHTNESS);
            greySource = base;
        }

        return grey;
    }

    /** Same hover rule as BBS' white hover frame in {@code UIClips}: not selected, not grabbing, no marquee, pointer inside. */
    @Unique
    private static int refreshedui$fill(UIContext context, UIClips clips, Area area)
    {
        return ((IClipHover) clips).refreshedui$canHover() && area.isInside(context) ? Colors.lerp(refreshedui$grey(), Colors.WHITE, HOVER_LIFT) : refreshedui$grey();
    }

    @Redirect(
        method = "renderClip",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noCurrentShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }

    @ModifyArg(
        method = "renderClip",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/film/clips/renderer/UIClipRenderer;renderBackground(Lmchorse/bbs_mod/ui/framework/UIContext;ILmchorse/bbs_mod/utils/clips/Clip;Lmchorse/bbs_mod/ui/utils/Area;ZZ)V"),
        index = 1
    )
    private int refreshedui$greyFill(int color, @Local(argsOnly = true) UIContext context, @Local(argsOnly = true) UIClips clips, @Local(argsOnly = true) Area area, @Local(argsOnly = true, ordinal = 0) boolean selected)
    {
        return refreshedui$greyClips() && !selected ? refreshedui$fill(context, clips, area) : color;
    }

    @ModifyArg(
        method = "renderClip",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;iconArea(Lmchorse/bbs_mod/ui/utils/icons/Icon;IFFFF)V"),
        index = 1
    )
    private int refreshedui$greyDisabled(int color, @Local(argsOnly = true) UIContext context, @Local(argsOnly = true) UIClips clips, @Local(argsOnly = true) Area area, @Local(argsOnly = true, ordinal = 0) boolean selected)
    {
        return refreshedui$greyClips() && !selected ? refreshedui$fill(context, clips, area) : color;
    }

    /** Outline takes the type colour (half alpha when disabled); a selected clip keeps BBS' white frame. */
    @ModifyArg(
        method = "renderClip",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFI)V"),
        index = 4
    )
    private int refreshedui$typeOutline(int color, @Local(argsOnly = true) Clip clip, @Local ClipFactoryData data, @Local(argsOnly = true, ordinal = 0) boolean selected)
    {
        if (!refreshedui$greyClips() || selected)
        {
            return color;
        }

        return (clip.enabled.get() ? Colors.A100 : Colors.A50) | (data.color & Colors.RGB);
    }
}
