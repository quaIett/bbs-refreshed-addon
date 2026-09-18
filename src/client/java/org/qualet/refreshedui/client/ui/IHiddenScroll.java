package org.qualet.refreshedui.client.ui;

/**
 * Mixed into BBS's {@code Scroll} by {@code ScrollMixin}. A hidden scroll draws nothing over its area:
 * no scrollbar, and none of the primary-colored edge shades BBS draws instead when the scrollbar is
 * off. The wheel still scrolls it. Call sites cast: {@code ((IHiddenScroll) view.scroll).refreshedui$hide()}.
 */
public interface IHiddenScroll
{
    void refreshedui$hide();
}
