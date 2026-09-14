package org.qualet.refreshedui.client.ui;

/**
 * Mixed into BBS's {@code Textbox} so a trackpad can drive the SAME {@link MaterialField.State} as its
 * inner text box — focusing a trackpad hands rendering over to the text box, and a shared state keeps the
 * indicator/hover animation continuous across that switch.
 */
public interface IMaterialFieldHost
{
    MaterialField.State refreshedui$fieldState();
}
