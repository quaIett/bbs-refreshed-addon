package org.qualet.refreshedui.client.batcher;

import mchorse.bbs_mod.ui.utils.icons.Icon;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix3x2fc;

/**
 * Geometry for an icon tiled inside a rounded rectangle (the colour picker's checkerboard swatch).
 *
 * <p>The body is cut along tile boundaries so each quad samples one tile with a linear UV span. The corners
 * cannot use the rounded-rect mask on 1.21.11 (the old way multiplied the icon into a stamped alpha with
 * custom blend functions, which the GUI pipelines do not offer), so each corner is the same tile-cut region
 * clipped against a polygonal quarter disc and emitted as triangles.</p>
 */
public final class CheckerMesh
{
    /** Arc segments per corner; the swatch corners are a few pixels, so this is smooth. */
    private static final int ARC_SEGMENTS = 8;

    private CheckerMesh()
    {}

    /** Tile-cut quads filling the rectangle, the tiling phase locked to {@code originX/originY}. */
    public static void region(VertexConsumer b, Matrix3x2fc m, Icon icon, int color, float rx, float ry, float rw, float rh, float originX, float originY)
    {
        forEachTile(icon, rx, ry, rw, rh, originX, originY, (xx, yy, xw, yh, u1, v1) ->
        {
            float tw = icon.textureW;
            float th = icon.textureH;

            b.vertex(m, xx, yy).texture(u1 / tw, v1 / th).color(color);
            b.vertex(m, xx, yy + yh).texture(u1 / tw, (v1 + yh) / th).color(color);
            b.vertex(m, xx + xw, yy + yh).texture((u1 + xw) / tw, (v1 + yh) / th).color(color);
            b.vertex(m, xx + xw, yy).texture((u1 + xw) / tw, v1 / th).color(color);
        });
    }

    /**
     * The quarter disc of radius {@code r} centred at {@code (cx, cy)} spanning angles {@code a0..a0+90°}
     * (screen space, y down), filled with the tiled icon.
     */
    public static void corner(VertexConsumer b, Matrix3x2fc m, Icon icon, int color, float cx, float cy, float r, double a0, float originX, float originY)
    {
        float[] disc = new float[(ARC_SEGMENTS + 2) * 2];

        disc[0] = cx;
        disc[1] = cy;

        for (int i = 0; i <= ARC_SEGMENTS; i++)
        {
            double a = a0 + Math.PI / 2 * i / ARC_SEGMENTS;

            disc[(i + 1) * 2] = cx + (float) Math.cos(a) * r;
            disc[(i + 1) * 2 + 1] = cy + (float) Math.sin(a) * r;
        }

        int discCount = ARC_SEGMENTS + 2;

        orient(disc, discCount);

        float minX = Math.min(disc[0], Math.min(disc[2], disc[discCount * 2 - 2]));
        float minY = Math.min(disc[1], Math.min(disc[3], disc[discCount * 2 - 1]));

        forEachTile(icon, minX, minY, r, r, originX, originY, (xx, yy, xw, yh, u1, v1) ->
        {
            float[] rect = {xx, yy, xx, yy + yh, xx + xw, yy + yh, xx + xw, yy};
            float[] poly = clip(rect, 4, disc, discCount);
            int n = poly.length / 2;

            if (n < 3)
            {
                return;
            }

            float tw = icon.textureW;
            float th = icon.textureH;

            for (int i = 1; i < n - 1; i++)
            {
                emit(b, m, poly, 0, xx, yy, u1, v1, tw, th, color);
                emit(b, m, poly, i, xx, yy, u1, v1, tw, th, color);
                emit(b, m, poly, i + 1, xx, yy, u1, v1, tw, th, color);
                emit(b, m, poly, i + 1, xx, yy, u1, v1, tw, th, color);
            }
        });
    }

    private static void emit(VertexConsumer b, Matrix3x2fc m, float[] poly, int i, float xx, float yy, float u1, float v1, float tw, float th, int color)
    {
        float px = poly[i * 2];
        float py = poly[i * 2 + 1];

        b.vertex(m, px, py).texture((u1 + px - xx) / tw, (v1 + py - yy) / th).color(color);
    }

    private interface TileConsumer
    {
        void accept(float xx, float yy, float xw, float yh, float u1, float v1);
    }

    private static void forEachTile(Icon icon, float rx, float ry, float rw, float rh, float originX, float originY, TileConsumer consumer)
    {
        if (rw <= 0F || rh <= 0F || icon.w <= 0 || icon.h <= 0)
        {
            return;
        }

        float x2 = rx + rw;
        float y2 = ry + rh;
        float tileW = icon.w;
        float tileH = icon.h;

        for (float yy = ry; yy < y2; )
        {
            double relY = yy - originY;
            float dv = (float) (relY - Math.floor(relY / tileH) * tileH);
            float yh = Math.min(tileH - dv, y2 - yy);

            if (yh <= 0F)
            {
                break;
            }

            for (float xx = rx; xx < x2; )
            {
                double relX = xx - originX;
                float du = (float) (relX - Math.floor(relX / tileW) * tileW);
                float xw = Math.min(tileW - du, x2 - xx);

                if (xw <= 0F)
                {
                    break;
                }

                consumer.accept(xx, yy, xw, yh, icon.x + du, icon.y + dv);
                xx += xw;
            }

            yy += yh;
        }
    }

    /** Twice the signed area in screen space; vanilla's quad winding (TL, BL, BR, TR) is negative. */
    private static float area(float[] p, int n)
    {
        float sum = 0F;

        for (int i = 0; i < n; i++)
        {
            int j = (i + 1) % n;

            sum += p[i * 2] * p[j * 2 + 1] - p[j * 2] * p[i * 2 + 1];
        }

        return sum;
    }

    /** Reverse the polygon in place if it does not wind like vanilla's quads (so culling never drops it). */
    private static void orient(float[] p, int n)
    {
        if (area(p, n) <= 0F)
        {
            return;
        }

        for (int i = 0, j = n - 1; i < j; i++, j--)
        {
            float x = p[i * 2];
            float y = p[i * 2 + 1];

            p[i * 2] = p[j * 2];
            p[i * 2 + 1] = p[j * 2 + 1];
            p[j * 2] = x;
            p[j * 2 + 1] = y;
        }
    }

    /** Sutherland–Hodgman: {@code subject} clipped by the convex {@code clip}, both wound like vanilla's quads. */
    private static float[] clip(float[] subject, int subjectCount, float[] clip, int clipCount)
    {
        float[] out = java.util.Arrays.copyOf(subject, subjectCount * 2);
        int outCount = subjectCount;

        for (int e = 0; e < clipCount && outCount > 0; e++)
        {
            float ax = clip[e * 2];
            float ay = clip[e * 2 + 1];
            float bx = clip[((e + 1) % clipCount) * 2];
            float by = clip[((e + 1) % clipCount) * 2 + 1];

            float[] in = out;
            int inCount = outCount;

            out = new float[(inCount + 1) * 2 * 2];
            outCount = 0;

            for (int i = 0; i < inCount; i++)
            {
                float px = in[i * 2];
                float py = in[i * 2 + 1];
                float qx = in[((i + 1) % inCount) * 2];
                float qy = in[((i + 1) % inCount) * 2 + 1];
                float dp = (bx - ax) * (py - ay) - (by - ay) * (px - ax);
                float dq = (bx - ax) * (qy - ay) - (by - ay) * (qx - ax);
                boolean pIn = dp <= 0F;
                boolean qIn = dq <= 0F;

                if (pIn)
                {
                    out[outCount * 2] = px;
                    out[outCount * 2 + 1] = py;
                    outCount++;
                }

                if (pIn != qIn)
                {
                    float t = dp / (dp - dq);

                    out[outCount * 2] = px + (qx - px) * t;
                    out[outCount * 2 + 1] = py + (qy - py) * t;
                    outCount++;
                }
            }
        }

        return java.util.Arrays.copyOf(out, outCount * 2);
    }
}
