package org.qualet.refreshedui.client.batcher;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

/**
 * Accessor mixed into BBS's {@code UIColorPicker} (3.12) to expose the new rounded color-swatch
 * renderer so {@code UIColor} can call it across classes: {@code ((IColorPickerSwatch) picker).renderSwatchRounded(...)}.
 */
public interface IColorPickerSwatch
{
    /** Rounded swatch: solid fill, or checkerboard + alpha ramp when alpha is being edited. */
    void renderSwatchRounded(Batcher2D batcher, float x, float y, float w, float h, float radius);
}
