package org.qualet.refreshedui.client.ui;

/**
 * Bridge so {@code UIPropTransform}'s gizmo hotkeys (G / S / R → {@code enableMode(0|1|2)}) can drive
 * the refreshed mode selector that lives on the {@code UITransform} base. {@code UITransformMixin}
 * implements this on every transform editor; {@code UIPropTransformMixin} casts to it and forwards the
 * activated mode so the selected tab follows the keyboard.
 */
public interface ITransformModes
{
    /** Select the given transform mode (0 translate, 1 scale, 2 rotate) and relayout. No-op for an
     *  out-of-range mode or when it's already selected. */
    void refreshedui$setMode(int mode);
}
