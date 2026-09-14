package org.qualet.refreshedui.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.dashboard.panels.landing.LandingBackdrop;
import mchorse.bbs_mod.ui.dashboard.panels.landing.UILandingRow;
import mchorse.bbs_mod.ui.dashboard.panels.landing.UILandingScreen;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;
import org.qualet.refreshedui.client.batcher.IRoundedBatcher;
import org.qualet.refreshedui.client.ui.UICornerRadii;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Landing screen (BBS 2.6, the empty-tab screen with the banner):
 * <ul>
 *   <li>the banner caption (dark gradient, "BBS FS | version" plaque, "render by" credit) is not drawn at all;</li>
 *   <li>the animated {@code LandingBackdrop} (lights, sparks, parallax) is replaced by a flat fill: the
 *       text-input surface darkened by 35%, so it follows the palette;</li>
 *   <li>the card loses its drop shadow and square outline and gets the popup-overlay look: rounded fill and a
 *       1px rounded border in the muted primary colour ({@code UIOverlayPanelMixin}). The border is drawn after
 *       the card's children so it sits over the banner and cuts the banner's square corners to the curve.</li>
 * </ul>
 * The menu rows are styled globally by {@link RowStyleMixin}.
 */
@Mixin(UILandingScreen.class)
public abstract class UILandingScreenMixin
{
    @Shadow @Final private UIElement card;

    @Shadow @Final private UIElement menu;

    @Shadow @Final private UILandingRow folder;

    /** Drops the outbound links group (gap, Discord, Tutorials, Wiki): everything after "Open folder". */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$trimMenu(CallbackInfo ci)
    {
        List<UIElement> children = new ArrayList<>(this.menu.getChildren(UIElement.class));
        int index = children.indexOf(this.folder);

        for (int i = index + 1; index >= 0 && i < children.size(); i++)
        {
            this.menu.remove(children.get(i));
        }
    }

    @Unique
    private static int refreshedui$backdropColor()
    {
        return Colors.A100 | (Colors.mulRGB(BBSSettings.inputSurface(), 0.65F) & Colors.RGB);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void refreshedui$addCardOutline(CallbackInfo ci)
    {
        this.card.add(new UIRenderable(this::refreshedui$renderCardOutline));
    }

    @Unique
    private void refreshedui$renderCardOutline(UIContext context)
    {
        Area area = this.card.area;
        int border = Colors.mulRGB(BBSSettings.primaryColor.get() | Colors.A100, 0.7F);

        ((IRoundedBatcher) context.batcher).roundedOutlineOver(area.x, area.y, area.w, area.h, UICornerRadii.interfaceChrome(), border, refreshedui$backdropColor());
    }

    @Inject(method = "renderBannerCaption", at = @At("HEAD"), cancellable = true)
    private void refreshedui$noCaption(UIContext context, Area area, CallbackInfo ci)
    {
        ci.cancel();
    }

    @Redirect(
        method = "renderBackdrop",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/dashboard/panels/landing/LandingBackdrop;render(Lmchorse/bbs_mod/ui/framework/UIContext;Lmchorse/bbs_mod/ui/utils/Area;)V")
    )
    private void refreshedui$flatBackdrop(LandingBackdrop backdrop, UIContext context, Area area)
    {
        area.render(context.batcher, refreshedui$backdropColor());
    }

    @Redirect(
        method = "renderCard",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;dropShadow(IIIIIII)V")
    )
    private void refreshedui$noCardShadow(Batcher2D batcher, int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
    }

    @Redirect(
        method = "renderCard",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;box(FFFFI)V")
    )
    private void refreshedui$roundCardFill(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
        ((IRoundedBatcher) batcher).roundedBox(x1, y1, x2 - x1, y2 - y1, UICornerRadii.interfaceChrome(), color);
    }

    @Redirect(
        method = "renderCard",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/ui/framework/elements/utils/Batcher2D;outline(FFFFI)V")
    )
    private void refreshedui$noSquareOutline(Batcher2D batcher, float x1, float y1, float x2, float y2, int color)
    {
    }
}
