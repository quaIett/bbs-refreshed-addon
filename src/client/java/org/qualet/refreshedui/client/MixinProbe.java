package org.qualet.refreshedui.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Boot-time diagnostic: force-loads every class targeted by the addon's mixins so that Mixin's
 * apply phase runs on all of them in a single client start. Any failed injection (missing target
 * method, moved injection point, bad descriptor) throws during class transform and is captured
 * here instead of only surfacing when the user happens to open that specific BBS screen.
 *
 * <p>Enabled by the {@code refreshedui.probe} system property (wired to {@code -Pprobe} in
 * {@code build.gradle}). Prints a pass/fail line per target and writes {@code run/refreshedui-probe.txt}.
 * {@code Class.forName(name, false, loader)} loads + transforms the class WITHOUT running its static
 * initializer, so no GL/render context is required.</p>
 *
 * <p>The {@link #TARGETS} list is extracted from every {@code @Mixin(...)} target across the addon's
 * main + client mixin packages (regenerate if the mixin set changes).</p>
 */
public final class MixinProbe
{
    private static final Logger LOG = LoggerFactory.getLogger("refreshedui-probe");

    /** FQNs of the classes each kept mixin targets (extracted from @Mixin annotations). */
    private static final String[] TARGETS = {
        "mchorse.bbs_mod.BBSSettings",
        "mchorse.bbs_mod.ui.dashboard.panels.landing.UILandingScreen",
        "mchorse.bbs_mod.ui.framework.elements.utils.RowStyle",
        "mchorse.bbs_mod.ui.onboarding.Onboarding",
        "mchorse.bbs_mod.cubic.ik.ModelIKDebug",
        "mchorse.bbs_mod.settings.ui.UISettingsOverlayPanel",
        "mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels",
        "mchorse.bbs_mod.ui.dashboard.textures.UITexturePainter",
        "mchorse.bbs_mod.ui.film.UIFilmPanel",
        "mchorse.bbs_mod.ui.film.UIFilmPlayerSettingsOverlayPanel",
        "mchorse.bbs_mod.ui.film.UIFilmPreview",
        "mchorse.bbs_mod.ui.film.clips.UIClip",
        "mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderer",
        "mchorse.bbs_mod.ui.film.replays.UIReplayPropertiesPanel",
        "mchorse.bbs_mod.ui.film.replays.UIReplaysEditor",
        "mchorse.bbs_mod.ui.film.replays.UIReplaysListPanel",
        "mchorse.bbs_mod.ui.forms.editors.forms.UIForm",
        "mchorse.bbs_mod.ui.forms.editors.panels.UIFramebufferFormPanel",
        "mchorse.bbs_mod.ui.forms.editors.panels.UIGeneralFormPanel",
        "mchorse.bbs_mod.ui.forms.editors.panels.UILabelFormPanel",
        "mchorse.bbs_mod.ui.forms.editors.panels.UIModelIKFormPanel",
        "mchorse.bbs_mod.ui.forms.editors.panels.UIModelPhysicsFormPanel",
        "mchorse.bbs_mod.ui.framework.elements.UIElement",
        "mchorse.bbs_mod.ui.framework.elements.UISection",
        "mchorse.bbs_mod.ui.framework.elements.buttons.UIButton",
        "mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate",
        "mchorse.bbs_mod.ui.framework.elements.buttons.UIIconStrip",
        "mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle",
        "mchorse.bbs_mod.ui.framework.elements.context.UIActionList",
        "mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu",
        "mchorse.bbs_mod.ui.framework.elements.input.UIColor",
        "mchorse.bbs_mod.ui.framework.elements.input.UINumericInput",
        "mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform",
        "mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad",
        "mchorse.bbs_mod.ui.framework.elements.input.UITrackpad",
        "mchorse.bbs_mod.ui.framework.elements.input.UITransform",
        "mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker",
        "mchorse.bbs_mod.ui.framework.elements.input.items.UIItems",
        "mchorse.bbs_mod.ui.framework.elements.input.list.UIList",
        "mchorse.bbs_mod.ui.framework.elements.input.text.utils.Textbox",
        "mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea",
        "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout",
        "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockSlot",
        "mchorse.bbs_mod.ui.framework.elements.layout.UIDockLayout$UIDockStackTabs",
        "mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay",
        "mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel",
        "mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D",
        "mchorse.bbs_mod.ui.framework.elements.utils.UITabStrip",
        "mchorse.bbs_mod.ui.framework.tooltips.UITooltip",
        "mchorse.bbs_mod.ui.framework.tooltips.styles.DarkTooltipStyle",
        "mchorse.bbs_mod.ui.framework.tooltips.styles.LightTooltipStyle",
        "mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel",
        "mchorse.bbs_mod.ui.selectors.UISelectorsOverlayPanel",
        "mchorse.bbs_mod.ui.utils.InterfaceBlur",
        "mchorse.bbs_mod.ui.utils.Scroll",
        "mchorse.bbs_mod.ui.utils.context.ColorfulContextAction",
        "mchorse.bbs_mod.ui.utils.context.ContextAction",
    };

    private MixinProbe()
    {
    }

    public static boolean enabled()
    {
        return Boolean.getBoolean("refreshedui.probe");
    }

    public static void run()
    {
        ClassLoader loader = MixinProbe.class.getClassLoader();
        List<String> report = new ArrayList<>();
        int ok = 0;
        int fail = 0;

        report.add("refreshedui mixin probe — " + TARGETS.length + " targets");

        for (String target : TARGETS)
        {
            try
            {
                Class.forName(target, false, loader);
                ok++;
                report.add("PASS " + target);
                LOG.info("PASS {}", target);
            }
            catch (Throwable t)
            {
                fail++;
                report.add("FAIL " + target + " :: " + t.getClass().getName() + ": " + t.getMessage());
                LOG.error("FAIL {} — mixin apply failed", target, t);
            }
        }

        String summary = "refreshedui probe: " + ok + " passed, " + fail + " failed";
        report.add(summary);
        LOG.info(summary);

        try
        {
            // runClient's working directory IS the project's run/ folder, so write there directly.
            Files.write(Path.of("refreshedui-probe.txt"), report);
        }
        catch (IOException e)
        {
            LOG.warn("could not write probe report", e);
        }
    }
}
