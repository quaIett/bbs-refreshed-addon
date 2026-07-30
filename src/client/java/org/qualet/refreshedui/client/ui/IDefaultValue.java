package org.qualet.refreshedui.client.ui;

/**
 * The value a numeric field was last handed by its owner — i.e. what it read when the panel opened
 * or when the selection changed, as opposed to anything the user has dragged or typed since.
 *
 * <p>Implemented on {@code UINumericInput} by {@code UINumericInputMixin}, so every trackpad and
 * slider carries one. Sliders use it for the right-click reset; reach it with a cast through
 * {@code Object} ({@code ((IDefaultValue) (Object) field)}) since the interface is only mixed in at
 * runtime.</p>
 */
public interface IDefaultValue
{
    boolean refreshedui$hasDefault();

    double refreshedui$getDefault();
}
