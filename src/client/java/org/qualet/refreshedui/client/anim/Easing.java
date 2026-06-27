package org.qualet.refreshedui.client.anim;

/**
 * A normalized easing function: maps linear time {@code t} in [0, 1] to an eased value in [0, 1].
 *
 * <p>Part of the addon's small animation core ({@code org.qualet.refreshedui.client.anim}). Easings
 * are stateless and reusable across any animated object — see {@link Animator} for the time source
 * and {@link Easings} for the built-in curves.</p>
 */
@FunctionalInterface
public interface Easing
{
    float ease(float t);
}
