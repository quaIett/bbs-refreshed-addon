package org.qualet.refreshedui.client.ui;

import org.qualet.refreshedui.client.anim.ListMotion;

import java.util.List;

/**
 * What {@code UIListMixin} exposes of a {@code UIList} to {@link ListMotion}: the list's motion state and
 * the protected parts of the list it reads (the rows as shown, which rows are folders, how deep a level
 * indents).
 */
public interface IListMotionHost
{
    ListMotion refreshedui$motion();

    List<?> refreshedui$visible();

    /** Null for a leaf, otherwise whether the branch is unfolded. */
    Boolean refreshedui$branch(Object row);

    int refreshedui$indentStep();
}
