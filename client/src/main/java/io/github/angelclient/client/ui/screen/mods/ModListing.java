package io.github.angelclient.client.ui.screen.mods;

import org.lwjgl.opengl.GL11;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.impl.ColouredComponent;
import io.github.angelclient.client.ui.component.impl.LabelComponent;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class ModListing extends ColouredComponent {

    private final Mod mod;
    private final ModsScreen.ModsScreenComponent screen;

    private float hoverAnim = 0f;

    public ModListing(Mod mod, ModsScreen.ModsScreenComponent screen) {
        super((c, col) -> Colour.WHITE);
        this.mod = mod;
        this.screen = screen;

        add(new LabelComponent(
                (comp, def) -> mod.getName(),
                (comp, def) -> mod.isEnabled()
                        ? new Colour(0xEE, 0xEE, 0xF4, 0xFF)
                        : new Colour(0x88, 0x88, 0x9A, 0xFF)),
                (c, b) -> {
                    Rectangle rb = getRelativeBounds();
                    int tw = Minecraft.getMinecraft()
                            .fontRendererObj.getStringWidth(mod.getName());
                    return new Rectangle(
                            (rb.getWidth() - tw) / 2,
                            rb.getHeight() - 14,
                            rb.getWidth(), 8);
                });
    }

    @Override
    public void render(ComponentRenderInfo info) {
        Rectangle b = getRelativeBounds();
        int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();

        boolean hovered = isHovered();
        boolean enabled = mod.isEnabled();

        // ─────────────────────────────
        // ANIMAÇÃO SUAVE
        // ─────────────────────────────
        float speed = 0.15f;
        hoverAnim += (hovered ? 1f : 0f - hoverAnim) * speed;

        // ─────────────────────────────
        // SHADOW (profundidade)
        // ─────────────────────────────
        fillRounded(x, y + 3, w, h, 6, 0x55000000);

        // ─────────────────────────────
        // GLOW HOVER
        // ─────────────────────────────
        if (hoverAnim > 0.01f) {
            int glowAlpha = (int)(40 * hoverAnim);
            fillRounded(x - 1, y - 1, w + 2, h + 2, 6,
                    (glowAlpha << 24) | 0x3B82F6);
        }

        // ─────────────────────────────
        // ANIMAÇÃO DE TOGGLE
        // ─────────────────────────────
        float toggleAnim = mod.getToggleProgress();
        float togglePulse = (float) Math.sin(toggleAnim * Math.PI);

        // ─────────────────────────────
        // FUNDO (INTERPOLADO)
        // ─────────────────────────────
        int base = 0xCC1E1E25;
        int hover = 0xCC26262F;

        int bg = interpolateColor(base, hover, hoverAnim);

        // Destaque se ativado
        if (enabled) {
            bg = interpolateColor(bg, 0xFF2A4FFF, 0.15f);
        }

        // Pulso de toggle
        if (toggleAnim > 0f && toggleAnim < 1f) {
            int pulseColor = enabled
                ? interpolateColor(0xFF222222, 0xFF3B82F6, togglePulse)
                : interpolateColor(0xFF3B82F6, 0xFF222222, togglePulse);
            bg = interpolateColor(bg, pulseColor, 0.3f * (1f - Math.abs(toggleAnim - 0.5f) * 2f));
        }

        fillRounded(x, y, w, h, 6, bg);

        // ─────────────────────────────
        // BORDA
        // ─────────────────────────────
        int border = interpolateColor(0xFF303038, 0xFF4A4A5A, hoverAnim);
        outlineRounded(x, y, w, h, 6, 1f, border);

        // ─────────────────────────────
        // BARRA LATERAL ATIVA
        // ─────────────────────────────
        if (enabled) {
            fillRounded(x + 2, y + 4, 3, h - 8, 2, 0xFF3B82F6);
        }

        // ─────────────────────────────
        // TEXTO ON/OFF
        // ─────────────────────────────
        String status = enabled ? "ON" : "OFF";
        int statusCol = enabled ? 0xFF3B82F6 : 0xFF666666;

        Minecraft.getMinecraft().fontRendererObj.drawString(
                status,
                x + w - Minecraft.getMinecraft().fontRendererObj.getStringWidth(status) - 6,
                y + 6,
                statusCol
        );

        super.render(info);
    }

    // ─────────────────────────────
    // INTERPOLAÇÃO DE COR
    // ─────────────────────────────
    private int interpolateColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 255;
        int r1 = (c1 >> 16) & 255;
        int g1 = (c1 >> 8) & 255;
        int b1 = c1 & 255;

        int a2 = (c2 >> 24) & 255;
        int r2 = (c2 >> 16) & 255;
        int g2 = (c2 >> 8) & 255;
        int b2 = c2 & 255;

        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    public static void fillRounded(int x, int y, int w, int h, int r, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r1 = (color >> 16 & 255) / 255.0F;
        float g1 = (color >> 8 & 255) / 255.0F;
        float b1 = (color & 255) / 255.0F;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r1, g1, b1, a);

        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            double ex = (i < 90 || i > 270) ? x + w - r : x + r;
            double ey = (i < 180) ? y + h - r : y + r;
            GL11.glVertex2d(ex + Math.sin(angle) * r, ey + Math.cos(angle) * r);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void outlineRounded(int x, int y, int w, int h, int r, int color) {
    outlineRounded(x, y, w, h, r, 1.0f, color);
}

    public static void outlineRounded(int x, int y, int w, int h, int r, float thickness, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r1 = (color >> 16 & 255) / 255.0F;
        float g1 = (color >> 8 & 255) / 255.0F;
        float b1 = (color & 255) / 255.0F;

        GL11.glLineWidth(thickness);
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r1, g1, b1, a);

        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            double ex = (i < 90 || i > 270) ? x + w - r : x + r;
            double ey = (i < 180) ? y + h - r : y + r;
            GL11.glVertex2d(ex + Math.sin(angle) * r, ey + Math.cos(angle) * r);
        }
        GL11.glEnd();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static int toInt(Colour colour) {
        return (colour.getAlpha() << 24) | (colour.getRed() << 16) | (colour.getGreen() << 8) | colour.getBlue();
    }
}