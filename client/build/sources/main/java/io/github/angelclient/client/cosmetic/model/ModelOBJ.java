package io.github.angelclient.client.cosmetic.model;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class ModelOBJ {

    private final List<float[]> vertices = new ArrayList<>();
    private final List<float[]> texCoords = new ArrayList<>();
    private final List<float[]> normals = new ArrayList<>();
    private final List<int[][]> faces = new ArrayList<>();

    public void addVertex(float x, float y, float z) {
        vertices.add(new float[]{x, y, z});
    }

    public void addTexCoord(float u, float v) {
        texCoords.add(new float[]{u, v});
    }

    public void addNormal(float x, float y, float z) {
        normals.add(new float[]{x, y, z});
    }

    public void addFace(int[] vertexIndices, int[] texCoordIndices, int[] normalIndices) {
        faces.add(new int[][]{vertexIndices, texCoordIndices, normalIndices});
    }

    public int getFaceCount() {
        return faces.size();
    }

    public void render() {
        if (faces.isEmpty()) return;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.disableLighting();

        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_NORMAL);

        for (int[][] face : faces) {
            int[] vIndices = face[0];
            int[] tIndices = face[1];
            int[] nIndices = face[2];

            for (int i = 0; i < vIndices.length; i++) {
                int vi = vIndices[i] >= 0 ? vIndices[i] - 1 : vertices.size() + vIndices[i];
                int ti = (tIndices != null && i < tIndices.length)
                        ? (tIndices[i] >= 0 ? tIndices[i] - 1 : texCoords.size() + tIndices[i])
                        : -1;
                int ni = (nIndices != null && i < nIndices.length)
                        ? (nIndices[i] >= 0 ? nIndices[i] - 1 : normals.size() + nIndices[i])
                        : -1;

                float[] v = vertices.get(vi);
                float u = (ti >= 0 && ti < texCoords.size()) ? texCoords.get(ti)[0] : 0;
                float vt = (ti >= 0 && ti < texCoords.size()) ? texCoords.get(ti)[1] : 0;
                float nx = (ni >= 0 && ni < normals.size()) ? normals.get(ni)[0] : 0;
                float ny = (ni >= 0 && ni < normals.size()) ? normals.get(ni)[1] : 0;
                float nz = (ni >= 0 && ni < normals.size()) ? normals.get(ni)[2] : 0;

                wr.pos(v[0], v[1], v[2]).tex(u, vt).normal(nx, ny, nz).endVertex();
            }
        }

        tess.draw();

        GlStateManager.enableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }
}
