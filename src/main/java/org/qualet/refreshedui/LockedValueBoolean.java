package org.qualet.refreshedui;

import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

/**
 * A boolean setting pinned to {@code false}. Whatever the config file loads or the UI tries to set,
 * every read returns off. {@code UIValueFactoryMixin} disables its toggle so the settings screen
 * shows it locked.
 */
public class LockedValueBoolean extends ValueBoolean
{
    public LockedValueBoolean(String id)
    {
        super(id, false);
    }

    @Override
    public Boolean get()
    {
        return Boolean.FALSE;
    }

    @Override
    public Boolean getOriginalValue()
    {
        return Boolean.FALSE;
    }

    @Override
    public void set(Boolean value, int flag)
    {
        super.set(Boolean.FALSE, flag);
    }

    @Override
    public void setRuntimeValue(Boolean value)
    {}
}
