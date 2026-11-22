package io.github.angelclient.client.util.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class GLRectUtils {

    public static void setGlColor(int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        GlStateManager.color(red, green, blue, alpha);
    }

    public static void drawRoundedRect(float x1, float y1, float x2, float y2, float radius, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(r, g, b, a);

        GL11.glBegin(GL11.GL_POLYGON);

        // Quadrante superior-esquerdo (0 → 90°, step 6°)
        for (int i = 0; i <= 90; i += 6)
            GL11.glVertex2d(
                x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante inferior-esquerdo (90 → 180°, step 6°)
        for (int i = 90; i <= 180; i += 6)
            GL11.glVertex2d(
                x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante inferior-direito (180 → 270°, step 6°)
        for (int i = 180; i <= 270; i += 6)
            GL11.glVertex2d(
                x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante superior-direito (270 → 360°, step 6°)
        for (int i = 270; i <= 360; i += 6)
            GL11.glVertex2d(
                x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);

        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedOutline(float x1, float y1, float x2, float y2, float radius, float lineWidth, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(lineWidth);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        // Quadrante superior-esquerdo (0 → 90°, step 6°)
        for (int i = 0; i <= 90; i += 6)
            GL11.glVertex2d(
                x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante inferior-esquerdo (90 → 180°, step 6°)
        for (int i = 90; i <= 180; i += 6)
            GL11.glVertex2d(
                x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante inferior-direito (180 → 270°, step 6°)
        for (int i = 180; i <= 270; i += 6)
            GL11.glVertex2d(
                x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);

        // Quadrante superior-direito (270 → 360°, step 6°)
        for (int i = 270; i <= 360; i += 6)
            GL11.glVertex2d(
                x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);

        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Draws a filled rounded rectangle using (x, y, width, height) parameters.
     * This is a convenience wrapper around {@link #drawRoundedRect(float, float, float, float, float, int)}.
     *
     * @param x      left edge
     * @param y      top edge
     * @param width  rectangle width
     * @param height rectangle height
     * @param radius corner radius
     * @param color  ARGB color
     */
    public static void drawRoundedRectangle(float x, float y, float width, float height, float radius, int color) {
        drawRoundedRect(x, y, x + width, y + height, radius, color);
    }

    /**
     * Draws a rounded rectangle outline using (x, y, width, height) parameters.
     * This is a convenience wrapper around {@link #drawRoundedOutline(float, float, float, float, float, float, int)}.
     *
     * @param x         left edge
     * @param y         top edge
     * @param width     rectangle width
     * @param height    rectangle height
     * @param radius    corner radius
     * @param lineWidth stroke width
     * @param color     ARGB color
     */
    public static void drawRoundedRectangleOutline(float x, float y, float width, float height, float radius, float lineWidth, int color) {
        drawRoundedOutline(x, y, x + width, y + height, radius, lineWidth, color);
    }
}
