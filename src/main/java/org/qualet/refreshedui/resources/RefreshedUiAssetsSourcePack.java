package org.qualet.refreshedui.resources;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Overrides two of BBS's <b>own</b> {@code bbs} namespace assets — the icon atlas and the menu banner —
 * with the refreshed-theme versions bundled in this addon's JAR.
 *
 * <p>BBS serves its own assets through {@link mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack}
 * (prefix {@link Link#ASSETS}, internal prefix {@code assets/bbs/assets}). {@link mchorse.bbs_mod.resources.AssetProvider}
 * returns the FIRST pack whose {@link #hasAsset} matches, so to win we must be registered ahead of BBS's
 * internal pack via {@code provider.registerFirst(...)} (see {@code RefreshedUiAddon}). The new icon atlas
 * is mandatory: the addon's mixins reference atlas indices that don't exist in the clean atlas.</p>
 *
 * <p>Our copies live under a UNIQUE internal prefix ({@code assets/refreshedui/bbs_override/...}) rather than
 * mirroring BBS's {@code assets/bbs/assets/...} path — sharing the exact path across two JARs would make
 * {@code getResource} resolution between BBS's copy and ours non-deterministic.</p>
 */
public class RefreshedUiAssetsSourcePack implements ISourcePack
{
    private static final Class<?> ANCHOR = RefreshedUiAssetsSourcePack.class;
    private static final String INTERNAL = "assets/refreshedui/bbs_override";

    /** Asset paths (under the {@code assets} source) we override; everything else falls through to BBS. */
    private static final Set<String> OVERRIDES = Set.of(
        "textures/icons.png",
        "textures/banners/bg.png"
    );

    @Override
    public String getPrefix()
    {
        return Link.ASSETS;
    }

    @Override
    public boolean hasAsset(Link link)
    {
        if (!Link.ASSETS.equals(link.source) || !OVERRIDES.contains(link.path))
        {
            return false;
        }

        return ANCHOR.getResource("/" + INTERNAL + "/" + link.path) != null;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        InputStream stream = ANCHOR.getResourceAsStream("/" + INTERNAL + "/" + link.path);

        if (stream == null)
        {
            throw new FileNotFoundException("Asset " + link + " couldn't be found!");
        }

        return stream;
    }

    @Override
    public File getFile(Link link)
    {
        return null;
    }

    @Override
    public Link getLink(File file)
    {
        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
    }
}
