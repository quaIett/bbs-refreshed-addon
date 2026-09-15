package org.qualet.refreshedui.client.ui;

/**
 * Bridge so the clip renderer can mirror {@code UIClips}' own hover rule (no hover while grabbing clips or
 * dragging a marquee) without reaching its private state. {@code UIClipsMixin} implements this on every
 * timeline; {@code UIClipRendererMixin} casts the {@code UIClips} argument to it to light up the hovered clip.
 */
public interface IClipHover
{
    /** Whether the timeline currently allows a clip to show hover (pointer inside is checked by the caller). */
    boolean refreshedui$canHover();
}
