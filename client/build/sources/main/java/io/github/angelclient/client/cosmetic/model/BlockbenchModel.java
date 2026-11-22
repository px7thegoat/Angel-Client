package io.github.angelclient.client.cosmetic.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class BlockbenchModel {

    private final List<Element> elements = new ArrayList<>();

    public void addElement(Element e) {
        elements.add(e);
    }

    public boolean hasElements() {
        return !elements.isEmpty();
    }

    public int getElementCount() {
        return elements.size();
    }

    public void render() {
        if (elements.isEmpty()) return;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();

        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);

        for (Element elem : elements) {
            float fx = elem.from[0], fy = elem.from[1], fz = elem.from[2];
            float tx = elem.to[0], ty = elem.to[1], tz = elem.to[2];

            for (Face face : elem.faces) {
                float[][] verts = getFaceVertices(fx, fy, fz, tx, ty, tz, face.direction);
                float u1 = face.u1, v1 = face.v1, u2 = face.u2, v2 = face.v2;

                for (int i = 0; i < 4; i++) {
                    float vx = verts[i][0], vy = verts[i][1], vz = verts[i][2];
                    if (elem.rotationAxis != null) {
                        float[] rotated = rotatePoint(vx, vy, vz,
                                elem.rotationOrigin[0], elem.rotationOrigin[1], elem.rotationOrigin[2],
                                elem.rotationAxis, elem.rotationAngle);
                        vx = rotated[0]; vy = rotated[1]; vz = rotated[2];
                    }
                    float u = (i == 0 || i == 3) ? u1 : u2;
                    float v = (i == 0 || i == 1) ? v1 : v2;
                    float[] normal = faceNormal(face.direction);
                    wr.pos(vx, vy, vz).tex(u, v).normal(normal[0], normal[1], normal[2]).endVertex();
                }
            }
        }

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    private float[][] getFaceVertices(float fx, float fy, float fz, float tx, float ty, float tz, String dir) {
        switch (dir) {
            case "north": return new float[][]{
                    {tx, ty, fz}, {tx, fy, fz}, {fx, fy, fz}, {fx, ty, fz}};
            case "south": return new float[][]{
                    {fx, ty, tz}, {fx, fy, tz}, {tx, fy, tz}, {tx, ty, tz}};
            case "east": return new float[][]{
                    {tx, ty, tz}, {tx, fy, tz}, {tx, fy, fz}, {tx, ty, fz}};
            case "west": return new float[][]{
                    {fx, ty, fz}, {fx, fy, fz}, {fx, fy, tz}, {fx, ty, tz}};
            case "up": return new float[][]{
                    {fx, ty, tz}, {fx, ty, fz}, {tx, ty, fz}, {tx, ty, tz}};
            case "down": return new float[][]{
                    {fx, fy, fz}, {fx, fy, tz}, {tx, fy, tz}, {tx, fy, fz}};
        }
        return new float[4][3];
    }

    private float[] rotatePoint(float x, float y, float z, float ox, float oy, float oz, String axis, float angle) {
        float dx = x - ox, dy = y - oy, dz = z - oz;
        float rad = (float) Math.toRadians(angle);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        float rx, ry, rz;
        switch (axis) {
            case "x":
                ry = dy * c - dz * s;
                rz = dy * s + dz * c;
                rx = dx;
                break;
            case "y":
                rx = dx * c + dz * s;
                rz = -dx * s + dz * c;
                ry = dy;
                break;
            case "z":
                rx = dx * c - dy * s;
                ry = dx * s + dy * c;
                rz = dz;
                break;
            default:
                return new float[]{x, y, z};
        }
        return new float[]{rx + ox, ry + oy, rz + oz};
    }

    private float[] faceNormal(String dir) {
        switch (dir) {
            case "north": return new float[]{0, 0, -1};
            case "south": return new float[]{0, 0, 1};
            case "east":  return new float[]{1, 0, 0};
            case "west":  return new float[]{-1, 0, 0};
            case "up":    return new float[]{0, 1, 0};
            case "down":  return new float[]{0, -1, 0};
        }
        return new float[]{0, 0, 0};
    }

    public static class Element {
        public float[] from;
        public float[] to;
        public float[] rotationOrigin;
        public String rotationAxis;
        public float rotationAngle;
        public List<Face> faces = new ArrayList<>();
    }

    public static class Face {
        public String direction;
        public float u1, v1, u2, v2;
        public int uvRotation;
    }
}
