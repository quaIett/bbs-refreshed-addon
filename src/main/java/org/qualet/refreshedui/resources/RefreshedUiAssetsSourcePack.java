package org.qualet.refreshedui.resources;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Overrides some of BBS's <b>own</b> {@code bbs} namespace assets — the icon atlas and the landing banner —
 * with the refreshed-theme versions bundled in this addon's JAR.
 *
 * <p>BBS serves its own assets through {@link mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack}
 * (prefix {@link Link#ASSETS}, internal prefix {@code assets/bbs/assets}). {@link mchorse.bbs_mod.resources.AssetProvider}
 * returns the FIRST pack whose {@link #hasAsset} matches, so to win we must be registered ahead of BBS's
 * internal pack via {@code provider.registerFirst(...)} (see {@code RefreshedUiAddon}). The new icon atlas
 * is mandatory: the addon's mixins reference atlas indices that don't exist in the clean atlas.</p>
 *
 * <p>Since BBS 2.6 the landing screen crossfades four banners ({@code bg1..bg4}); all four slots serve our
 * single banner, so the landing shows it statically.</p>
 *
 * <p>Our copies live under a UNIQUE internal prefix ({@code assets/refreshedui/bbs_override/...}) rather than
 * mirroring BBS's {@code assets/bbs/assets/...} path — sharing the exact path across two JARs would make
 * {@code getResource} resolution between BBS's copy and ours non-deterministic.</p>
 */
public class RefreshedUiAssetsSourcePack implements ISourcePack
{
    private static final Class<?> ANCHOR = RefreshedUiAssetsSourcePack.class;
    private static final String INTERNAL = "assets/refreshedui/bbs_override";
    private static final String BANNER = "textures/banners/bg.png";

    /** Asset path (under the {@code assets} source) → our bundled file; everything else falls through to BBS. */
    private static final Map<String, String> OVERRIDES = Map.of(
        "textures/icons.png", "textures/icons.png",
        "textures/banners/bg1.png", BANNER,
        "textures/banners/bg2.png", BANNER,
        "textures/banners/bg3.png", BANNER,
        "textures/banners/bg4.png", BANNER
    );

    @Override
    public String getPrefix()
    {
        return Link.ASSETS;
    }

    @Override
    public boolean hasAsset(Link link)
    {
        String file = this.resolve(link);

        return file != null && ANCHOR.getResource(file) != null;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        String file = this.resolve(link);
        InputStream stream = file == null ? null : ANCHOR.getResourceAsStream(file);

        if (stream == null)
        {
            throw new FileNotFoundException("Asset " + link + " couldn't be found!");
        }

        return stream;
    }

    private String resolve(Link link)
    {
        String path = Link.ASSETS.equals(link.source) ? OVERRIDES.get(link.path) : null;

        return path == null ? null : "/" + INTERNAL + "/" + path;
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
