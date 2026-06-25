package org.qualet.refreshedui.client;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client entry point. Registers the runtime localized labels for the addon's "refreshed"
 * personalization group (see {@link RefreshedUiStrings}).
 */
public class RefreshedUiClient implements ClientModInitializer
{
    private static final Logger LOG = LoggerFactory.getLogger("refreshedui");

    @Override
    public void onInitializeClient()
    {
        BBSMod.events.register(new RefreshedUiStrings());
        RefreshedUiStrings.apply(BBSModClient.getL10n());

        LOG.info("RefreshedUiClient.onInitializeClient — registered refreshed l10n labels");
    }
}
