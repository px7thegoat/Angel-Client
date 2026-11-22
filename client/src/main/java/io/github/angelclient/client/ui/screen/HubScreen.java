package io.github.angelclient.client.ui.screen;

import io.github.angelclient.client.ui.screen.cosmetics.CosmeticsScreen;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;

public class HubScreen extends GuiScreen {

    private static final ResourceLocation LOGO = new ResourceLocation("angel_client", "textures/angelclient/logo2.png");
    private static final ResourceLocation ICON_SETTINGS = new ResourceLocation("angel_client", "textures/angelclient/settings.png");
    private static final ResourceLocation ICON_COSMETICS = new ResourceLocation("angel_client", "textures/angelclient/cosmetics.png");
    private static final int LOGO_SZ = 40;

    private long openTime;
    private static final long SLIDE_DURATION = 350;

    private static final int ICON_BTN_W = 34;
    private static final int ICON_BTN_H = 34;
    private static final int MODS_BTN_W = 70;
    private static final int MODS_BTN_H = 34;
    private static final int GAP = 8;

    private static final int BTN_MODS = 0;
    private static final int BTN_CONFIG = 1;
    private static final int BTN_COSMETICS = 2;
    private final float[] hoverAnim = new float[3];
    private final long[] lastHoverFrame = new long[3];

    @Override
    public void initGui() {
        super.initGui();
        openTime = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            if (lastHoverFrame[i] == 0) lastHoverFrame[i] = System.currentTimeMillis();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long elapsed = System.currentTimeMillis() - openTime;
        float progress = Math.min(1f, elapsed / (float) SLIDE_DURATION);
        float ease = 1f - (1f - progress) * (1f - progress) * (1f - progress);

        drawRect(0, 0, width, height, ((int) (0x60 * ease) << 24) | 0x000000);

        int rowW = ICON_BTN_W + GAP + MODS_BTN_W + GAP + ICON_BTN_W;
        int panelH = LOGO_SZ + 8 + MODS_BTN_H + 20;
        int panelW = rowW + 40;
        int px = (width - panelW) / 2;
        float py = height * (1f - ease) + ((height - panelH) / 2f) * ease;

        float alpha = ease;
        int bg = ((int) (0x80 * alpha) << 24) | 0x000000;
        int brd = ((int) (0x30 * alpha) << 24) | 0xFFFFFF;
        drawRoundedRect(px, py, panelW, panelH, 10, bg);
        drawRoundedOutline(px, py, panelW, panelH, 10, 1f, brd);

        int cx = width / 2;
        int logoY = (int) py + 12;

        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha);
        Gui.drawModalRectWithCustomSizedTexture(cx - LOGO_SZ / 2, logoY, 0, 0, LOGO_SZ, LOGO_SZ, LOGO_SZ, LOGO_SZ);
        GlStateManager.disableBlend();

        int btnY = logoY + LOGO_SZ + 12;
        int startX = cx - rowW / 2;

        drawIconButton(ICON_SETTINGS, startX, btnY, ICON_BTN_W, ICON_BTN_H, mouseX, mouseY, BTN_CONFIG, alpha);
        drawModsButton("Mods", startX + ICON_BTN_W + GAP, btnY, MODS_BTN_W, MODS_BTN_H, mouseX, mouseY, BTN_MODS, alpha);
        drawIconButton(ICON_COSMETICS, startX + ICON_BTN_W + GAP + MODS_BTN_W + GAP, btnY, ICON_BTN_W, ICON_BTN_H, mouseX, mouseY, BTN_COSMETICS, alpha);

        String hint = "RSHIFT / ESC";
        int sw = mc.fontRendererObj.getStringWidth(hint);
        mc.fontRendererObj.drawString(hint, (int)(cx - sw / 2f), btnY + MODS_BTN_H + 8, ((int) (0x60 * alpha) << 24) | 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawIconButton(ResourceLocation tex, int x, int y, int w, int h, int mx, int my, int id, float alpha) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        long now = System.currentTimeMillis();
        long delta = now - lastHoverFrame[id];
        lastHoverFrame[id] = now;
        hoverAnim[id] += ((hovered ? 1f : 0f) - hoverAnim[id]) * Math.min(1f, 0.02f * delta);
        float t = hoverAnim[id];

        drawRoundedRect(x, y, w, h, 6, ((int) ((30 + 40 * t) * alpha) << 24) | 0x000000);
        drawRoundedOutline(x, y, w, h, 6, 1f, ((int) ((40 + 60 * t) * alpha) << 24) | 0xFFFFFF);

        int pad = 6;
        int sz = w - pad * 2;
        mc.getTextureManager().bindTexture(tex);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, alpha * (hovered ? 1f : 0.7f));
        Gui.drawModalRectWithCustomSizedTexture(x + pad, y + (h - sz) / 2, 0, 0, sz, sz, sz, sz);
        GlStateManager.disableBlend();
    }

    private void drawModsButton(String text, int x, int y, int w, int h, int mx, int my, int id, float alpha) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        long now = System.currentTimeMillis();
        long delta = now - lastHoverFrame[id];
        lastHoverFrame[id] = now;
        hoverAnim[id] += ((hovered ? 1f : 0f) - hoverAnim[id]) * Math.min(1f, 0.02f * delta);
        float t = hoverAnim[id];

        drawRoundedRect(x, y, w, h, 6, ((int) ((30 + 40 * t) * alpha) << 24) | 0x000000);
        drawRoundedOutline(x, y, w, h, 6, 1f, ((int) ((40 + 60 * t) * alpha) << 24) | 0xFFFFFF);

        int tw = mc.fontRendererObj.getStringWidth(text);
        int th = mc.fontRendererObj.FONT_HEIGHT;
        mc.fontRendererObj.drawString(text, (int)(x + (w - tw) / 2f), (int)(y + (h - th) / 2f), ((int) (0xDD * alpha) << 24) | (hovered ? 0xFFFFFF : 0xCCCCCC));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        int cx = width / 2;
        int rowW = ICON_BTN_W + GAP + MODS_BTN_W + GAP + ICON_BTN_W;
        int panelH = LOGO_SZ + 8 + MODS_BTN_H + 20;
        int panelW = rowW + 40;
        int px = (width - panelW) / 2;
        float py = (height - panelH) / 2f;
        int btnY = (int) py + 12 + LOGO_SZ + 12;
        int startX = cx - rowW / 2;

        if (mouseX >= startX && mouseX < startX + ICON_BTN_W && mouseY >= btnY && mouseY < btnY + ICON_BTN_H) {
            mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
            return;
        }
        int modsX = startX + ICON_BTN_W + GAP;
        if (mouseX >= modsX && mouseX < modsX + MODS_BTN_W && mouseY >= btnY && mouseY < btnY + MODS_BTN_H) {
            mc.displayGuiScreen(new ModsScreen());
            return;
        }
        int cosX = startX + ICON_BTN_W + GAP + MODS_BTN_W + GAP;
        if (mouseX >= cosX && mouseX < cosX + ICON_BTN_W && mouseY >= btnY && mouseY < btnY + ICON_BTN_H) {
            mc.displayGuiScreen(new CosmeticsScreen(this));
            return;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        float a = (color >> 24 & 255) / 255f, r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(770, 771); GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x + radius, y); GL11.glVertex2f(x + w - radius, y); GL11.glVertex2f(x + w - radius, y + h); GL11.glVertex2f(x + radius, y + h);
        GL11.glVertex2f(x, y + radius); GL11.glVertex2f(x + radius, y + radius); GL11.glVertex2f(x + radius, y + h - radius); GL11.glVertex2f(x, y + h - radius);
        GL11.glVertex2f(x + w - radius, y + radius); GL11.glVertex2f(x + w, y + radius); GL11.glVertex2f(x + w, y + h - radius); GL11.glVertex2f(x + w - radius, y + h - radius);
        GL11.glEnd();
        drawCorner(x + radius, y + radius, radius, 180); drawCorner(x + w - radius, y + radius, radius, 270);
        drawCorner(x + radius, y + h - radius, radius, 90); drawCorner(x + w - radius, y + h - radius, radius, 0);
        GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawRoundedOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        float a = (color >> 24 & 255) / 255f, r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(770, 771);
        GL11.glColor4f(r, g, b, a); GL11.glLineWidth(thickness); GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 90; i += 3) { float ang = (float) Math.toRadians(180 + i); GL11.glVertex2f(x + radius + (float) Math.cos(ang) * radius, y + radius + (float) Math.sin(ang) * radius); }
        for (int i = 0; i <= 90; i += 3) { float ang = (float) Math.toRadians(270 + i); GL11.glVertex2f(x + w - radius + (float) Math.cos(ang) * radius, y + radius + (float) Math.sin(ang) * radius); }
        for (int i = 0; i <= 90; i += 3) { float ang = (float) Math.toRadians(i); GL11.glVertex2f(x + w - radius + (float) Math.cos(ang) * radius, y + h - radius + (float) Math.sin(ang) * radius); }
        for (int i = 0; i <= 90; i += 3) { float ang = (float) Math.toRadians(90 + i); GL11.glVertex2f(x + radius + (float) Math.cos(ang) * radius, y + h - radius + (float) Math.sin(ang) * radius); }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH); GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawCorner(float x, float y, float radius, int startAngle) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN); GL11.glVertex2f(x, y);
        for (int i = 0; i <= 90; i += 3) { float ang = (float) Math.toRadians(startAngle + i); GL11.glVertex2f(x + (float) Math.cos(ang) * radius, y + (float) Math.sin(ang) * radius); }
        GL11.glEnd();
    }
}
