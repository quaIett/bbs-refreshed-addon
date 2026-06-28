package org.qualet.refreshedui.client.ui;

import org.joml.Vector3f;

/**
 * One resolved IK marker for the reworked debug overlay: the bone's name and
 * world-local position, the marker's visual sphere radius and the picking cube
 * half-size (both scaled to the chain's bone span, matching the stock markers),
 * the chain's tip so the overlay can dim non-selected chains, plus the sphere
 * colour (green for controller targets, orange for pole targets).
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
    public final float[] color;

    public IKControllerMarker(String bone, String tip, Vector3f position, float radius, float pickHalf, float[] color)
    {
        this.bone = bone;
        this.tip = tip;
        this.position = position;
        this.radius = radius;
        this.pickHalf = pickHalf;
        this.color = color;
    }
}
