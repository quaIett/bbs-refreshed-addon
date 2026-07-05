package org.qualet.refreshedui.replays;

import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValueString;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered {@code List<String>} persistence value (unlike {@code ValueStringKeys} which is an unordered
 * set). Ported from the fork's {@code ValueStringList}; named distinctly so it never clashes with a
 * same-named class BBS might add later. Used by {@code FilmMixin} to persist the folder display order
 * ({@code replay_category_order}) on the {@code Film}.
 */
public class ValueStringListAddon extends ValueList<ValueString>
{
    public ValueStringListAddon(String id)
    {
        super(id);
    }

    @Override
    protected ValueString create(String id)
    {
        return new ValueString(id, "");
    }

    public List<String> get()
    {
        ArrayList<String> out = new ArrayList<>();

        for (ValueString s : this.getAllTyped())
        {
            out.add(s.get());
        }

        return out;
    }

    public void set(List<String> values)
    {
        /* Wrap the clear+rebuild+sync so folder-order changes are captured by the film undo handler,
         * mirroring upstream Replays.addReplay()/remove(). */
        this.preNotify();

        this.getAllTyped().clear();

        if (values != null)
        {
            for (String v : values)
            {
                ValueString s = this.create("0");
                s.set(v == null ? "" : v);
                this.add(s);
            }
        }

        this.sync();

        this.postNotify();
    }
}
