package org.qualet.refreshedui.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ik.ModelIKConfig;
import mchorse.bbs_mod.cubic.ik.ModelIKDebug;
import mchorse.bbs_mod.cubic.ik.ModelIKIO;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.render.CubicRenderer.PivotFrame;
import mchorse.bbs_mod.cubic.render.ModelPivotFrames;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;
import org.qualet.refreshedui.RefreshedUiAddon;
import org.qualet.refreshedui.client.ui.IKControllerMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reworks the IK debug overlay: instead of drawing the whole solved chain (wires,
 * joints, effector, pole) plus the goal, it draws ONLY the goal marker for chains
 * whose target bone is named {@code controller_*}. The skeleton and every other
 * marker are suppressed.
 *
 * <p>The marker keeps the stock goal look — a green {@link Draw#sphere} sized to
 * the chain's bone span — and stays pickable, so clicking it still selects the
 * controller bone exactly as before.
 *
 * <p>Gated by the "refreshed &gt; IK controller-only overlay" setting
 * ({@link RefreshedUiAddon#ikControllerOverlay}, default on): when off, the inject
 * returns WITHOUT cancelling, so BBS's stock full overlay renders unchanged.
 *
 * <p>When on, both {@code render} and {@code renderStencil} are cancelled at HEAD
 * and fully replaced. The package-private {@code ModelIKCache} is intentionally
 * bypassed: chains are rebuilt from the public {@link ModelIKIO#fromData} config and
 * the hierarchy walk is mirrored locally, so the mixin never touches non-exported
 * API. Buffer handling uses the 1.20.x {@link Tessellator#getBuffer()} path (the
 * addon's master build targets MC 1.20.1 / 1.20.4).
 */
@Mixin(ModelIKDebug.class)
public abstract class ModelIKDebugMixin
{
    @Unique
    private static final String refreshedui$CONTROLLER_PREFIX = "controller_";

    @Unique
    private static final float[] refreshedui$GOAL = {0.22F, 0.84F, 0.55F};

    /** Whether the reworked controller-only overlay is active. Default on until the setting registers. */
    @Unique
    private static boolean refreshedui$controllerOnly()
    {
        return RefreshedUiAddon.ikControllerOverlay == null || RefreshedUiAddon.ikControllerOverlay.get();
    }

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lmchorse/bbs_mod/cubic/IModel;Lmchorse/bbs_mod/data/types/MapType;Ljava/lang/String;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void refreshedui$renderControllers(MatrixStack stack, IModel model, MapType ikData, String selectedTip, CallbackInfo ci)
    {
        if (!refreshedui$controllerOnly())
        {
            return;
        }

        ci.cancel();

        if (!ModelIKDebug.enabled || model == null || ikData == null)
        {
            return;
        }

        List<IKControllerMarker> markers = refreshedui$gather(model, ikData);

        if (markers.isEmpty())
        {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();

        if (model instanceof BOBJModel)
        {
            stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
        }

        BufferBuilder dots = Tessellator.getInstance().getBuffer();
        dots.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (IKControllerMarker marker : markers)
        {
            boolean selected = selectedTip == null || selectedTip.isEmpty() || marker.tip.equals(selectedTip);
            float alpha = selected ? 1F : 0.4F;

            stack.push();
            stack.translate(marker.position.x, marker.position.y, marker.position.z);
            Draw.sphere(dots, stack, marker.radius, 9, 9, refreshedui$GOAL[0], refreshedui$GOAL[1], refreshedui$GOAL[2], alpha);
            stack.pop();
        }

        BufferRenderer.drawWithGlobalProgram(dots.end());

        stack.pop();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    @Inject(
        method = "renderStencil(Lnet/minecraft/client/util/math/MatrixStack;Lmchorse/bbs_mod/cubic/IModel;Lmchorse/bbs_mod/data/types/MapType;Lmchorse/bbs_mod/ui/framework/elements/utils/StencilMap;Lmchorse/bbs_mod/forms/forms/Form;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void refreshedui$renderStencilControllers(MatrixStack stack, IModel model, MapType ikData, StencilMap stencilMap, Form form, CallbackInfo ci)
    {
        if (!refreshedui$controllerOnly())
        {
            return;
        }

        ci.cancel();

        if (!ModelIKDebug.enabled || model == null || ikData == null || stencilMap == null)
        {
            return;
        }

        List<IKControllerMarker> markers = refreshedui$gather(model, ikData);

        if (markers.isEmpty())
        {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        stack.push();

        if (model instanceof BOBJModel)
        {
            stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
        }

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (IKControllerMarker marker : markers)
        {
            int id = stencilMap.objectIndex;
            float s = marker.pickHalf;
            Vector3f p = marker.position;

            Draw.fillBox(builder, stack, p.x - s, p.y - s, p.z - s, p.x + s, p.y + s, p.z + s, (id & 0xFF) / 255F, (id >> 8 & 0xFF) / 255F, (id >> 16 & 0xFF) / 255F, 1F);

            stencilMap.addPicking(form, marker.bone);
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        stack.pop();

        RenderSystem.enableDepthTest();
    }

    /**
     * Rebuilds the controller markers from the public IK config: for each enabled
     * chain whose {@code target} is a real bone named {@code controller_*}, resolves
     * the target position and sizes the marker to the chain's bone span (matching the
     * goal sphere the stock overlay drew). Chains whose target is not a controller,
     * or whose bones cannot be resolved, are skipped.
     */
    @Unique
    private static List<IKControllerMarker> refreshedui$gather(IModel model, MapType ikData)
    {
        ModelIKConfig config = ModelIKIO.fromData(ikData);

        if (config == null || config.chains() == null || config.chains().isEmpty())
        {
            return Collections.emptyList();
        }

        Collection<String> keys = model.getAllGroupKeys();

        if (keys == null)
        {
            return Collections.emptyList();
        }

        List<IKControllerMarker> out = new ArrayList<>();

        for (ModelIKConfig.Chain chain : config.chains())
        {
            if (chain == null || !chain.enabled())
            {
                continue;
            }

            String target = chain.target();

            if (target == null || !target.startsWith(refreshedui$CONTROLLER_PREFIX))
            {
                continue;
            }

            if (!keys.contains(target) || !keys.contains(chain.tip()))
            {
                continue;
            }

            List<String> ids = refreshedui$buildChain(model, chain.tip(), chain.chainLength());

            if (ids.size() < 2)
            {
                continue;
            }

            Set<String> wanted = new HashSet<>(ids);
            wanted.add(target);

            Map<String, PivotFrame> frames = new HashMap<>(wanted.size() * 2);
            ModelPivotFrames.collect(model, wanted, frames);

            PivotFrame targetFrame = frames.get(target);

            if (targetFrame == null)
            {
                continue;
            }

            List<Vector3f> pts = new ArrayList<>(ids.size());
            boolean complete = true;

            for (String id : ids)
            {
                PivotFrame frame = frames.get(id);

                if (frame == null)
                {
                    complete = false;
                    break;
                }

                pts.add(new Vector3f(frame.position()));
            }

            if (!complete)
            {
                continue;
            }

            int n = pts.size();
            float total = 0F;

            for (int i = 0; i < n - 1; i++)
            {
                total += pts.get(i).distance(pts.get(i + 1));
            }

            float unit = total / (n - 1);
            float span = pts.get(0).distance(pts.get(n - 1));
            float pickHalf = span / Math.max(1, n - 1) * 0.2F;

            out.add(new IKControllerMarker(target, chain.tip(), new Vector3f(targetFrame.position()), unit * 0.12F, pickHalf));
        }

        return out;
    }

    /**
     * Walks up the hierarchy from {@code tip}, collecting up to {@code chainLength}
     * bones ({@code 0} = all the way to the root), ordered root-to-tip. Mirrors the
     * stock {@code ModelIKCache.buildChainIds} so the marker scale matches.
     */
    @Unique
    private static List<String> refreshedui$buildChain(IModel model, String tip, int chainLength)
    {
        List<String> list = new ArrayList<>();
        String group = tip;

        while (group != null && !group.isEmpty())
        {
            list.add(group);

            if (chainLength > 0 && list.size() >= chainLength)
            {
                break;
            }

            String parent = model.getParentGroupKey(group);

            if (parent == null || parent.equals(group))
            {
                break;
            }

            group = parent;
        }

        Collections.reverse(list);

        return list;
    }
}
