package keystrokesmod.ui;

import keystrokesmod.manager.ModuleManager;
import keystrokesmod.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AngelRavenGui extends GuiScreen {

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 200;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 4;

    private final Map<Module.Category, List<Module>> categorized = new LinkedHashMap<>();
    private Module.Category selectedCategory;
    private final float[] hoverAnim = new float[32];
    private final long[] lastFrame = new long[32];
    private float titleAlpha = 0f;
    private long titleFrame = System.currentTimeMillis();

    @Override
    public void initGui() {
        super.initGui();
        categorized.clear();
        for (Module m : ModuleManager.modules) {
            if (m.isHidden()) continue;
            categorized.computeIfAbsent(m.moduleCategory(), k -> new ArrayList<>()).add(m);
        }
        if (!categorized.isEmpty()) {
            selectedCategory = categorized.keySet().iterator().next();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Overlay escuro
        Gui.drawRect(0, 0, width, height, 0xAA000000);

        int cx = width / 2;
        int cy = height / 2;

        // Painel central
        int panelX = cx - PANEL_W / 2;
        int panelY = cy - PANEL_H / 2 - 20;

        drawRoundedRect(panelX, panelY, PANEL_W, PANEL_H, 8, 0xD0151515);
        drawRoundedOutline(panelX, panelY, PANEL_W, PANEL_H, 8, 1.5f, 0x60FFFFFF);

        // Título
        String title = "Raven B++ Modules";
        int tw = mc.fontRendererObj.getStringWidth(title);
        mc.fontRendererObj.drawStringWithShadow(title, cx - tw / 2f, panelY - 14, 0xFFD633);

        // Categorias tabs
        int tabX = panelX + 12;
        int tabY = panelY + 10;
        int tabH = 14;
        for (Module.Category cat : categorized.keySet()) {
            String name = Character.toUpperCase(cat.name().charAt(0)) + cat.name().substring(1);
            boolean sel = cat == selectedCategory;
            int tabW = mc.fontRendererObj.getStringWidth(name) + 14;
            int tc = sel ? 0xFFD633 : 0x888888;

            if (sel) {
                drawRoundedRect(tabX - 2, tabY - 1, tabW + 4, tabH + 2, 4, 0x40FFD633);
            }
            mc.fontRendererObj.drawStringWithShadow(name, tabX + tabW / 2f - mc.fontRendererObj.getStringWidth(name) / 2f, tabY + 2, tc);

            tabX += tabW + 6;
        }

        // Linha separadora
        int sepY = tabY + tabH + 6;
        Gui.drawRect(panelX + 10, sepY, panelX + PANEL_W - 10, sepY + 1, 0x30FFFFFF);

        // Módulos da categoria selecionada
        List<Module> modules = categorized.get(selectedCategory);
        if (modules != null) {
            int mx = panelX + 12;
            int my = sepY + 8;
            int maxPerCol = (PANEL_H - (my - panelY) - 12) / (BTN_H + BTN_GAP);
            int colW = (PANEL_W - 36) / 2;

            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int col = i / maxPerCol;
                int row = i % maxPerCol;
                int bx = mx + col * (colW + 8);
                int by = my + row * (BTN_H + BTN_GAP);

                if (bx + colW > panelX + PANEL_W - 10) break;
                if (by + BTN_H > panelY + PANEL_H - 10) break;

                drawModuleButton(mod, bx, by, colW, BTN_H, mouseX, mouseY, i);
            }
        }

        // Hint de tecla
        String hint = "Press ESC to close";
        mc.fontRendererObj.drawStringWithShadow(hint, width - mc.fontRendererObj.getStringWidth(hint) - 6, height - 12, 0x555555);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModuleButton(Module mod, int x, int y, int w, int h, int mx, int my, int id) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        long now = System.currentTimeMillis();
        long delta = now - lastFrame[id];
        lastFrame[id] = now;
        hoverAnim[id] = hoverAnim[id] + ((hov ? 1f : 0f) - hoverAnim[id]) * Math.min(1f, 0.025f * delta);
        float t = hoverAnim[id];

        boolean enabled = mod.isEnabled();

        int bgColor = enabled
            ? ((int)(60 + 50 * t) << 24) | 0x003366
            : ((int)(40 + 40 * t) << 24) | 0x000000;
        int borderColor = enabled
            ? ((int)(80 + 100 * t) << 24) | 0x3399FF
            : ((int)(50 + 80 * t) << 24) | 0xFFFFFF;

        drawRoundedRect(x, y, w, h, 4, bgColor);
        drawRoundedOutline(x, y, w, h, 4, 1.5f, borderColor);

        String name = mod.getName();
        String status = enabled ? "ON" : "OFF";
        int nameColor = enabled ? 0xFFFFFF : (hov ? 0xDDDDDD : 0xAAAAAA);
        int statusColor = enabled ? 0x33FF66 : 0x888888;

        mc.fontRendererObj.drawStringWithShadow(name, x + 6, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2f, 0xFF000000 | nameColor);

        int sw = mc.fontRendererObj.getStringWidth(status);
        mc.fontRendererObj.drawStringWithShadow(status, x + w - sw - 6, y + (h - mc.fontRendererObj.FONT_HEIGHT) / 2f, 0xFF000000 | statusColor);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;

        // Check category tabs
        int cx = width / 2;
        int cy = height / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = cy - PANEL_H / 2 - 20;
        int tabX = panelX + 12;
        int tabY = panelY + 10;
        int tabH = 14;

        for (Module.Category cat : categorized.keySet()) {
            String name = Character.toUpperCase(cat.name().charAt(0)) + cat.name().substring(1);
            int tabW = mc.fontRendererObj.getStringWidth(name) + 14;
            if (mouseX >= tabX - 2 && mouseX <= tabX + tabW + 2 && mouseY >= tabY - 1 && mouseY <= tabY + tabH + 1) {
                selectedCategory = cat;
                mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                return;
            }
            tabX += tabW + 6;
        }

        // Check module buttons
        List<Module> modules = categorized.get(selectedCategory);
        if (modules != null) {
            int mx = panelX + 12;
            int my = tabY + tabH + 6 + 8;
            int maxPerCol = (PANEL_H - (my - panelY) - 12) / (BTN_H + BTN_GAP);
            int colW = (PANEL_W - 36) / 2;

            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int col = i / maxPerCol;
                int row = i % maxPerCol;
                int bx = mx + col * (colW + 8);
                int by = my + row * (BTN_H + BTN_GAP);

                if (bx + colW > panelX + PANEL_W - 10) break;
                if (by + BTN_H > panelY + PANEL_H - 10) break;

                if (mouseX >= bx && mouseX < bx + colW && mouseY >= by && mouseY < by + BTN_H) {
                    mod.toggle();
                    mc.getSoundHandler().playSound(net.minecraft.client.audio.PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // Helpers de renderização (mesmo estilo do AngelClientMainMenu)
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
