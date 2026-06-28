package org.qualet.refreshedui.client.ui;

import org.joml.Vector3f;

/**
 * One resolved IK controller marker for the reworked debug overlay: the target
 * bone's name and world-local position, the marker's visual sphere radius and the
 * picking cube half-size (both scaled to the chain's bone span, matching the stock
 * goal marker), plus the chain's tip so the overlay can dim non-selected chains.
 *
 * @see org.qualet.refreshedui.mixin.client.ModelIKDebugMixin
 */
public final class IKControllerMarker
{
    public final String bone;
    public final String tip;
    public final Vector3f position;
    public final float radius;
    public final float pickHalf;

    public IKControllerMarker(String bone, String tip, Vector3f position, float radius, float pickHalf)
    {
        this.bone = bone;
        this.tip = tip;
        this.position = position;
        this.radius = radius;
        this.pickHalf = pickHalf;
    }
}
