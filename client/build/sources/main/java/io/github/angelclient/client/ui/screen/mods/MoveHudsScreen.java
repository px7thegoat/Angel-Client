package io.github.angelclient.client.ui.screen.mods;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModOption;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.mod.hud.HudElement;
import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.component.Screen;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.util.data.Rectangle;
import io.github.angelclient.client.ui.screen.AngelClientMainMenu;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;

public class MoveHudsScreen extends Screen {

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final int COL_OVERLAY    = 0xBB000000;
    private static final int COL_HUD_BORDER = 0x66FFFFFF;
    private static final int COL_HUD_SEL    = 0xFFFFFFFF;
    private static final int COL_HUD_HOV    = 0xAAFFFFFF;
    private static final int COL_HUD_FILL   = 0x14FFFFFF;
    private static final int COL_HUD_SEL_BG = 0x22FFFFFF;

    // ── Toolbar icons ─────────────────────────────────────────────────────
    private static final ResourceLocation ICON_GEAR = new ResourceLocation("angel_client", "textures/mods/angel_client_settings_small_2x.png");
    private static final ResourceLocation ICON_EXIT = new ResourceLocation("angel_client", "textures/mods/angel_client_exit_2x.png");

    // ── Toolbar dimensions ────────────────────────────────────────────────
    private static final int TB_ICON   = 10;   // icon render size
    private static final int TB_BTN    = 16;   // button hit area
    private static final int TB_PAD    = 3;    // internal padding
    private static final int TB_GAP    = 1;    // gap between buttons
    private static final int TB_BAR_H  = 16;   // bar height

    private static final int COL_PANEL_BG   = 0xF0060606;
    private static final int COL_PANEL_HDR  = 0xFF0D0D0D;
    private static final int COL_PANEL_BRD  = 0xFF222222;
    private static final int COL_PANEL_SEP  = 0xFF1A1A1A;

    private static final int COL_TEXT       = 0xFFEEEEEE;
    private static final int COL_TEXT_DIM   = 0xFF888888;
    private static final int COL_TEXT_TITLE = 0xFFFFFFFF;

    private static final int COL_TOGGLE_ON  = 0xFFCCCCCC;
    private static final int COL_TOGGLE_OFF = 0xFF333333;
    private static final int COL_TOGGLE_KON = 0xFFFFFFFF;
    private static final int COL_TOGGLE_KOF = 0xFF666666;

    private static final int COL_SLIDER_BG  = 0xFF1E1E1E;
    private static final int COL_SLIDER_FG  = 0xFF888888;
    private static final int COL_SLIDER_KN  = 0xFFFFFFFF;

    private static final int COL_BTN_BG     = 0xFF1A1A1A;
    private static final int COL_BTN_HOV    = 0xFF2A2A2A;
    private static final int COL_BTN_BRD    = 0xFF333333;

    private static final int COL_X_BG      = 0xCC550000;
    private static final int COL_X_HOV     = 0xCC880000;
    private static final int COL_X_TEXT    = 0xFFFF5555;

    private static final int COL_DOT_GRID  = 0x18FFFFFF;

    // ── Done button ───────────────────────────────────────────────────────────
    private static final int DONE_BTN_W    = 80;
    private static final int DONE_BTN_H    = 16;
    private static final int DONE_BTN_Y_OFF = 12; // offset from bottom

    // ── Config do painel ──────────────────────────────────────────────────────
    private static final int PANEL_W   = 170;
    private static final int PANEL_PAD = 6;
    private static final int ROW_H     = 20;
    private static final int HDR_H     = 26;

    // ── Estado ────────────────────────────────────────────────────────────────
    private GuiScreen  title;
    private HudElement movingHud;
    private Position   moveOffset;
    private HudElement selectedHud;
    private HudElement resizingHud;
    private String     resizeMode; // "tl", "tr", "bl", "br"
    private int        resizeStartX, resizeStartY;
    private int        resizeStartW, resizeStartH;

    // Painel de configuração
    private int        panelX, panelY, panelH;
    private List<ModOption> panelOptions;
    private ModOption  draggingOption;
    private int        draggingSliderX, draggingSliderW;

    public MoveHudsScreen() {
        super(new Component() {
            @Override
            public Rectangle getBounds() {
                return new Rectangle(0, 0, 0, 0);
            }
        });
        background = false;

        if (parentScreen instanceof Screen) {
            GuiScreen grandparent = ((Screen) parentScreen).getParentScreen();
            if (grandparent instanceof GuiMainMenu || grandparent instanceof AngelClientMainMenu) {
                title = grandparent;
            }
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    public HudElement getSelectedHud(int mouseX, int mouseY) {
        for (HudElement hud : Client.INSTANCE.getHuds()) {
            if (!hud.isVisible()) continue;
            if (hud.isSelected(mouseX, mouseY)) return hud;
            // Also check the button area above the HUD box
            if (isInButtonArea(hud, mouseX, mouseY)) return hud;
        }
        return null;
    }

    /**
     * Checks if the mouse is within the icon button areas inside the HUD box
     * (gear at bottom-left, X at bottom-right).
     */
    private boolean isInButtonArea(HudElement hud, int mouseX, int mouseY) {
        Rectangle b = hud.getMultipliedBounds();
        if (b == null) return false;
        int pad = 2;
        // X button — bottom-right inside box
        int xBtnX = b.getX() + b.getWidth() - TB_BTN - pad;
        int xBtnY = b.getY() + b.getHeight() - TB_BTN - pad;
        if (mouseX >= xBtnX && mouseX <= xBtnX + TB_BTN && mouseY >= xBtnY && mouseY <= xBtnY + TB_BTN) return true;
        // Gear button — bottom-left inside box
        int gBtnX = b.getX() + pad;
        int gBtnY = b.getY() + b.getHeight() - TB_BTN - pad;
        if (mouseX >= gBtnX && mouseX <= gBtnX + TB_BTN && mouseY >= gBtnY && mouseY <= gBtnY + TB_BTN) return true;
        return false;
    }

    private String getResizeMode(HudElement hud, int mouseX, int mouseY) {
        if (hud == null) return null;
        Rectangle b = hud.getMultipliedBounds();
        if (b == null) return null;

        int threshold = 8;
        int x1 = b.getX();
        int y1 = b.getY();
        int x2 = b.getX() + b.getWidth();
        int y2 = b.getY() + b.getHeight();

        // Corners
        if (mouseX >= x1 - threshold && mouseX <= x1 + threshold && 
            mouseY >= y1 - threshold && mouseY <= y1 + threshold) return "tl";
        if (mouseX >= x2 - threshold && mouseX <= x2 + threshold && 
            mouseY >= y1 - threshold && mouseY <= y1 + threshold) return "tr";
        if (mouseX >= x1 - threshold && mouseX <= x1 + threshold && 
            mouseY >= y2 - threshold && mouseY <= y2 + threshold) return "bl";
        if (mouseX >= x2 - threshold && mouseX <= x2 + threshold && 
            mouseY >= y2 - threshold && mouseY <= y2 + threshold) return "br";

        return null;
    }

    private void selectHud(HudElement hud) {
        selectedHud = hud;
    }

    /**
     * Clamps a HUD position so it stays fully within screen boundaries.
     */
    private Position clampHudPosition(HudElement hud, int rawX, int rawY) {
        Rectangle b = hud.getMultipliedBounds();
        int elemW = b != null ? b.getWidth()  : 10;
        int elemH = b != null ? b.getHeight() : 10;

        // Keep the entire element inside the screen
        int clampedX = Math.max(0, Math.min(width  - elemW, rawX));
        int clampedY = Math.max(0, Math.min(height - elemH, rawY));

        return new Position(clampedX, clampedY);
    }

    // ── Done button helpers ───────────────────────────────────────────────────

    private int getDoneBtnX() {
        return (width - DONE_BTN_W) / 2;
    }

    private int getDoneBtnY() {
        return height - DONE_BTN_Y_OFF - DONE_BTN_H;
    }

    private boolean isDoneHovered(int mouseX, int mouseY) {
        int bx = getDoneBtnX();
        int by = getDoneBtnY();
        return mouseX >= bx && mouseX <= bx + DONE_BTN_W
                && mouseY >= by && mouseY <= by + DONE_BTN_H;
    }

    // ── Render principal ──────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Fundo do mundo ou panorama
        if (title != null) title.drawScreen(0, 0, partialTicks);

        // Overlay escuro
        drawRect(0, 0, width, height, COL_OVERLAY);

        // Grade de pontos (estilo Badlion)
        drawDotGrid();

        HudElement hoveredHud = getSelectedHud(mouseX, mouseY);

        // Renderizar elementos HUD
        for (HudElement hud : Client.INSTANCE.getHuds()) {
            if (!hud.isVisible()) continue;

            Rectangle bounds = hud.getMultipliedBounds();
            if (mc.theWorld == null) hud.render(true);

            if (bounds == null) continue;

            boolean isSel  = hud == selectedHud;
            boolean isHov  = hud == hoveredHud;
            boolean isMov  = hud == movingHud;

            // Fundo
            if (isSel || isMov) {
                drawRect(bounds.getX(), bounds.getY(),
                        bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(),
                        COL_HUD_SEL_BG);
            } else if (isHov) {
                drawRect(bounds.getX(), bounds.getY(),
                        bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(),
                        COL_HUD_FILL);
            }

            // Borda
            int border = isSel ? COL_HUD_SEL : isHov ? COL_HUD_HOV : COL_HUD_BORDER;
            drawHudBorder(bounds, border);

            // Cantos decorativos quando selecionado
            if (isSel) {
                drawCornerHandles(bounds);
            }

            // Rótulo do mod
            if (hud.getMod() != null) {
                String label = hud.getMod().getName();
                int lw = mc.fontRendererObj.getStringWidth(label);
                int tx = bounds.getX() + (bounds.getWidth() - lw) / 2;
                int ty = bounds.getY() + (bounds.getHeight() - mc.fontRendererObj.FONT_HEIGHT) / 2;
                if (isSel || isHov || bounds.getWidth() < lw + 4) {
                    // Tooltip acima se o HUD for pequeno
                    if (bounds.getWidth() < lw + 4) {
                        int ttop = bounds.getY() - mc.fontRendererObj.FONT_HEIGHT - 4;
                        drawRect(tx - 3, ttop - 1, tx + lw + 3, ttop + mc.fontRendererObj.FONT_HEIGHT + 2, 0xDD000000);
                        mc.fontRendererObj.drawStringWithShadow(label, tx, ttop, COL_TEXT_TITLE);
                    } else {
                        mc.fontRendererObj.drawStringWithShadow(label, tx, ty, COL_TEXT_TITLE);
                    }
                }
            }

            // Icon buttons inside the HUD box (no background)
            if (isHov || isSel) {
                int pad = 2;

                // ── X button (close/disable) — bottom-right inside box ──
                int xBtnX = bounds.getX() + bounds.getWidth() - TB_BTN - pad;
                int xBtnY = bounds.getY() + bounds.getHeight() - TB_BTN - pad;
                boolean xHov = mouseX >= xBtnX && mouseX <= xBtnX + TB_BTN && mouseY >= xBtnY && mouseY <= xBtnY + TB_BTN;
                int xIconX = xBtnX + (TB_BTN - TB_ICON) / 2;
                int xIconY = xBtnY + (TB_BTN - TB_ICON) / 2;
                drawIcon(ICON_EXIT, xIconX, xIconY, TB_ICON, xHov ? 1.0f : 0.5f);

                // ── Gear button (settings) — bottom-left inside box, only when selected ──
                if (isSel) {
                    int gBtnX = bounds.getX() + pad;
                    int gBtnY = bounds.getY() + bounds.getHeight() - TB_BTN - pad;
                    boolean gearHov = mouseX >= gBtnX && mouseX <= gBtnX + TB_BTN && mouseY >= gBtnY && mouseY <= gBtnY + TB_BTN;
                    int gIconX = gBtnX + (TB_BTN - TB_ICON) / 2;
                    int gIconY = gBtnY + (TB_BTN - TB_ICON) / 2;
                    drawIcon(ICON_GEAR, gIconX, gIconY, TB_ICON, gearHov ? 1.0f : 0.5f);
                }
            }
        }

        // Arrastar
        if (Mouse.isButtonDown(0)) {
            if (resizingHud != null) {
                // Redimensionar
                handleResizeDrag(mouseX, mouseY);
            } else if (movingHud == null) {
                HudElement hov = hoveredHud;
                if (hov != null) {
                    Rectangle b = hov.getMultipliedBounds();
                    if (b != null) {
                        // Verify click is not on the toolbar bar
                        if (!isInButtonArea(hov, mouseX, mouseY)) {
                            // Verificar se não clicou nas bordas de resize
                            String resizeModeCheck = getResizeMode(hov, mouseX, mouseY);
                            if (resizeModeCheck == null) {
                                movingHud  = hov;
                                moveOffset = new Position(
                                        hov.getPosition().getX() - mouseX,
                                        hov.getPosition().getY() - mouseY);
                            }
                        }
                    }
                }
            } else {
                int rawX = mouseX + moveOffset.getX();
                int rawY = mouseY + moveOffset.getY();
                Position clamped = clampHudPosition(movingHud, rawX, rawY);
                movingHud.setPosition(clamped);
            }
        } else {
            movingHud = null;
            resizingHud = null;
        }

        // ── Botão Done (centro inferior, transparente) ─────────────────────────
        int bx = getDoneBtnX();
        int by = getDoneBtnY();
        boolean doneHov = isDoneHovered(mouseX, mouseY);

        // Fundo bem transparente; um pouco mais visível no hover
        drawRect(bx, by, bx + DONE_BTN_W, by + DONE_BTN_H,
                doneHov ? 0x55FFFFFF : 0x22FFFFFF);
        // Borda fina
        drawRect(bx, by, bx + DONE_BTN_W, by + 1,          doneHov ? 0xAAFFFFFF : 0x44FFFFFF);
        drawRect(bx, by + DONE_BTN_H - 1, bx + DONE_BTN_W, by + DONE_BTN_H, doneHov ? 0xAAFFFFFF : 0x44FFFFFF);
        drawRect(bx, by, bx + 1, by + DONE_BTN_H,          doneHov ? 0xAAFFFFFF : 0x44FFFFFF);
        drawRect(bx + DONE_BTN_W - 1, by, bx + DONE_BTN_W, by + DONE_BTN_H, doneHov ? 0xAAFFFFFF : 0x44FFFFFF);

        String doneLabel = "Done";
        int dlw = mc.fontRendererObj.getStringWidth(doneLabel);
        mc.fontRendererObj.drawStringWithShadow(doneLabel,
                bx + (DONE_BTN_W - dlw) / 2,
                by + (DONE_BTN_H - mc.fontRendererObj.FONT_HEIGHT) / 2,
                doneHov ? 0xFFFFFFFF : 0xAAFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Renderização de subelementos ──────────────────────────────────────────

    private void drawDotGrid() {
        int spacing = 18;
        for (int x = spacing; x < width; x += spacing) {
            for (int y = spacing; y < height; y += spacing) {
                drawRect(x, y, x + 1, y + 1, COL_DOT_GRID);
            }
        }
    }

    private void drawHudBorder(Rectangle b, int color) {
        // Borda completa (1px)
        drawRect(b.getX(), b.getY(), b.getX() + b.getWidth(), b.getY() + 1, color);
        drawRect(b.getX(), b.getY() + b.getHeight() - 1, b.getX() + b.getWidth(), b.getY() + b.getHeight(), color);
        drawRect(b.getX(), b.getY(), b.getX() + 1, b.getY() + b.getHeight(), color);
        drawRect(b.getX() + b.getWidth() - 1, b.getY(), b.getX() + b.getWidth(), b.getY() + b.getHeight(), color);
    }

    private void drawCornerHandles(Rectangle b) {
        // Quadradinhos de resize nos 4 cantos — 6x6 preenchidos, bem visíveis
        int s = 6;
        int half = s / 2;
        int c = 0xFFFFFFFF;
        int shadow = 0x88000000;

        int x1 = b.getX();
        int y1 = b.getY();
        int x2 = b.getX() + b.getWidth();
        int y2 = b.getY() + b.getHeight();

        // Sombra (offset +1) para cada canto
        drawRect(x1 - half + 1, y1 - half + 1, x1 - half + s + 1, y1 - half + s + 1, shadow);
        drawRect(x2 - half + 1, y1 - half + 1, x2 - half + s + 1, y1 - half + s + 1, shadow);
        drawRect(x1 - half + 1, y2 - half + 1, x1 - half + s + 1, y2 - half + s + 1, shadow);
        drawRect(x2 - half + 1, y2 - half + 1, x2 - half + s + 1, y2 - half + s + 1, shadow);

        // Handle branco
        drawRect(x1 - half, y1 - half, x1 - half + s, y1 - half + s, c);
        drawRect(x2 - half, y1 - half, x2 - half + s, y1 - half + s, c);
        drawRect(x1 - half, y2 - half, x1 - half + s, y2 - half + s, c);
        drawRect(x2 - half, y2 - half, x2 - half + s, y2 - half + s, c);
    }

    /**
     * Draws a textured icon at the given position with alpha.
     */
    private void drawIcon(ResourceLocation icon, int x, int y, int size, float alpha) {
        mc.getTextureManager().bindTexture(icon);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(1f, 1f, 1f, alpha);
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x,        y + size, 0).tex(0, 1).endVertex();
        wr.pos(x + size, y + size, 0).tex(1, 1).endVertex();
        wr.pos(x + size, y,        0).tex(1, 0).endVertex();
        wr.pos(x,        y,        0).tex(0, 0).endVertex();
        tess.draw();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
    }

    private int computePanelHeight() {
        int h = HDR_H + PANEL_PAD + ROW_H;
        if (panelOptions != null) {
            for (ModOption opt : panelOptions) {
                Class<?> type = opt.getType();
                if (type == boolean.class || type == float.class || type == int.class || type.isEnum()) {
                    h += ROW_H;
                }
            }
        }
        return h + PANEL_PAD;
    }

    private void drawConfigPanel(int mouseX, int mouseY) {
        panelH = computePanelHeight();
        Mod mod = selectedHud.getMod();

        // Fundo do painel
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + panelH, COL_PANEL_BG);
        // Borda
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 1, COL_PANEL_BRD);
        drawRect(panelX, panelY + panelH - 1, panelX + PANEL_W, panelY + panelH, COL_PANEL_BRD);
        drawRect(panelX, panelY, panelX + 1, panelY + panelH, COL_PANEL_BRD);
        drawRect(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + panelH, COL_PANEL_BRD);

        // Header
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + HDR_H, COL_PANEL_HDR);
        drawRect(panelX, panelY + HDR_H, panelX + PANEL_W, panelY + HDR_H + 1, COL_PANEL_BRD);
        String name = mod.getName();
        mc.fontRendererObj.drawString(name, panelX + PANEL_PAD, panelY + (HDR_H - 8) / 2, COL_TEXT_TITLE);

        // Toggle enable/disable (canto direito do header)
        boolean enabled = mod.isEnabled();
        int toggleX = panelX + PANEL_W - PANEL_PAD - 26;
        int toggleY = panelY + (HDR_H - 11) / 2;
        drawPill(toggleX, toggleY, enabled);

        int ry = panelY + HDR_H + PANEL_PAD;

        // Separador
        drawRect(panelX + PANEL_PAD, ry + ROW_H + 4, panelX + PANEL_W - PANEL_PAD, ry + ROW_H + 5, COL_PANEL_SEP);

        // Scale slider (sempre presente)
        int slW = PANEL_W - PANEL_PAD * 2 - 6;
        int slX = panelX + PANEL_PAD;
        if (selectedHud.getMod() instanceof io.github.angelclient.client.mod.hud.HudMod) {
            io.github.angelclient.client.mod.hud.HudMod hm =
                    (io.github.angelclient.client.mod.hud.HudMod) selectedHud.getMod();
            // scale range: 10..100, normalizado
            float scaleMin = 10f, scaleMax = 100f;
            hm.scale = Math.max(scaleMin, Math.min(scaleMax, hm.scale));
            float scaleVal = (hm.scale - scaleMin) / (scaleMax - scaleMin);
            String scaleTxt = (int) hm.scale + "%";
            mc.fontRendererObj.drawString("Scale " + scaleTxt, panelX + PANEL_PAD, ry + 2, COL_TEXT_DIM);
            drawSliderBar(slX, ry + 11, slW, scaleVal, mouseX, mouseY);
        }
        ry += ROW_H;

        // Opções do mod
        if (panelOptions != null) {
            for (ModOption opt : panelOptions) {
                Class<?> type = opt.getType();
                if (type == boolean.class) {
                    drawBoolRow(opt, ry, mouseX, mouseY);
                    ry += ROW_H;
                } else if ((type == float.class || type == int.class)) {
                    drawSliderRow(opt, ry, mouseX, mouseY);
                    ry += ROW_H;
                } else if (type.isEnum()) {
                    drawEnumRow(opt, ry, mouseX, mouseY);
                    ry += ROW_H;
                }
            }
        }
    }

    private void drawBoolRow(ModOption opt, int ry, int mx, int my) {
        drawRect(panelX, ry, panelX + PANEL_W, ry + ROW_H, 0);
        String n = truncate(opt.getName(), PANEL_W - PANEL_PAD * 2 - 30);
        mc.fontRendererObj.drawString(n, panelX + PANEL_PAD, ry + 6, COL_TEXT_DIM);
        boolean val = (Boolean) opt.getValue();
        int tx = panelX + PANEL_W - PANEL_PAD - 26;
        int ty = ry + (ROW_H - 11) / 2;
        drawPill(tx, ty, val);
        drawRect(panelX + PANEL_PAD, ry + ROW_H - 1, panelX + PANEL_W - PANEL_PAD, ry + ROW_H, COL_PANEL_SEP);
    }

    private void drawSliderRow(ModOption opt, int ry, int mx, int my) {
        String n = truncate(opt.getName(), 38);
        mc.fontRendererObj.drawString(n, panelX + PANEL_PAD, ry + 6, COL_TEXT_DIM);
        int slW = PANEL_W - PANEL_PAD * 2 - 40;
        int slX = panelX + PANEL_PAD + 40;

        Slider slider = null;
        try { slider = opt.getField().getAnnotation(Slider.class); } catch (Exception ignored) {}

        float min = slider != null ? slider.min() : 0f;
        float max = slider != null ? slider.max() : 100f;
        Object raw = opt.getValue();
        float cur = raw instanceof Float ? (Float) raw : raw instanceof Integer ? (Integer) raw : 0f;
        float norm = Math.max(0, Math.min(1, (cur - min) / (max - min)));

        drawSliderBar(slX, ry + 6, slW, norm, mx, my);
        drawRect(panelX + PANEL_PAD, ry + ROW_H - 1, panelX + PANEL_W - PANEL_PAD, ry + ROW_H, COL_PANEL_SEP);
    }

    private void drawSliderBar(int sx, int sy, int sw, float norm, int mx, int my) {
        // Track
        drawRect(sx, sy + 3, sx + sw, sy + 8, COL_SLIDER_BG);
        // Fill
        int fill = (int)(norm * (sw - 6));
        if (fill > 0) drawRect(sx, sy + 3, sx + fill, sy + 8, COL_SLIDER_FG);
        // Knob — always inside bar bounds
        int kx = sx + fill;
        kx = Math.max(sx, Math.min(sx + sw - 6, kx));
        drawRect(kx, sy,     kx + 6, sy + 11, 0xFF444444); // sombra
        drawRect(kx, sy,     kx + 6, sy + 1,  COL_SLIDER_KN); // borda top
        drawRect(kx, sy,     kx + 1, sy + 11, COL_SLIDER_KN); // borda left
        drawRect(kx, sy + 1, kx + 5, sy + 10, COL_SLIDER_KN); // face
    }

    private void drawEnumRow(ModOption opt, int ry, int mx, int my) {
        String n = truncate(opt.getName(), 38);
        mc.fontRendererObj.drawString(n, panelX + PANEL_PAD, ry + 6, COL_TEXT_DIM);
        String val = opt.getValue().toString();
        int bw = mc.fontRendererObj.getStringWidth(val) + 10;
        int bx = panelX + PANEL_W - PANEL_PAD - bw;
        int by = ry + 2;
        boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + ROW_H - 4;
        drawRect(bx, by, bx + bw, by + ROW_H - 4, hov ? COL_BTN_HOV : COL_BTN_BG);
        drawRect(bx, by, bx + bw, by + 1, COL_BTN_BRD);
        drawRect(bx, by + ROW_H - 5, bx + bw, by + ROW_H - 4, COL_BTN_BRD);
        mc.fontRendererObj.drawString(val, bx + 5, ry + 6, COL_TEXT);
        drawRect(panelX + PANEL_PAD, ry + ROW_H - 1, panelX + PANEL_W - PANEL_PAD, ry + ROW_H, COL_PANEL_SEP);
    }

    private void drawPill(int x, int y, boolean on) {
        // Trilho
        drawRect(x, y + 2, x + 26, y + 9, on ? COL_TOGGLE_ON : COL_TOGGLE_OFF);
        // Knob
        int kx = on ? x + 15 : x + 1;
        drawRect(kx, y, kx + 10, y + 11, on ? COL_TOGGLE_KON : COL_TOGGLE_KOF);
    }

    // ── Interação ─────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Done button
        if (mouseButton == 0 && isDoneHovered(mouseX, mouseY)) {
            Utils.playClickSound(true);
            Client.INSTANCE.save();
            io.github.angelclient.client.cosmetic.CosmeticAPI.syncPlayerToMySQL();
            if (title != null) mc.displayGuiScreen(title);
            else               mc.displayGuiScreen(null);
            return;
        }

        if (mouseButton == 0) {
            HudElement hud = getSelectedHud(mouseX, mouseY);
            if (hud != null) {
                Rectangle b = hud.getMultipliedBounds();
                // Button click detection — bottom corners inside box
                if (b != null) {
                    boolean hudIsSel = (hud == selectedHud);
                    int pad = 2;

                    // Gear button click — bottom-left (only when selected)
                    if (hudIsSel) {
                        int gBtnX = b.getX() + pad;
                        int gBtnY = b.getY() + b.getHeight() - TB_BTN - pad;
                        if (mouseX >= gBtnX && mouseX <= gBtnX + TB_BTN && mouseY >= gBtnY && mouseY <= gBtnY + TB_BTN) {
                            Utils.playClickSound(true);
                            if (parentScreen instanceof ModsScreen) {
                                ((ModsScreen) parentScreen).switchMod(hud.getMod());
                                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
                            }
                            return;
                        }
                    }

                    // X button click — bottom-right (close/disable)
                    int xBtnX = b.getX() + b.getWidth() - TB_BTN - pad;
                    int xBtnY = b.getY() + b.getHeight() - TB_BTN - pad;
                    if (mouseX >= xBtnX && mouseX <= xBtnX + TB_BTN && mouseY >= xBtnY && mouseY <= xBtnY + TB_BTN) {
                        Utils.playClickSound(true);
                        hud.getMod().setEnabled(false);
                        if (selectedHud == hud) selectHud(null);
                        Client.INSTANCE.save();
                        return;
                    }
                    
                    // Check for corner resize
                    String resizeModeCheck = getResizeMode(hud, mouseX, mouseY);
                    if (resizeModeCheck != null) {
                        selectHud(hud); // Select if not already selected
                        resizingHud = hud;
                        this.resizeMode = resizeModeCheck;
                        resizeStartX = mouseX;
                        resizeStartY = mouseY;
                        resizeStartW = b.getWidth();
                        resizeStartH = b.getHeight();
                        Utils.playClickSound(false);
                        return;
                    }
                }
                // Selecionar
                Utils.playClickSound(true);
                selectHud(hud);
            } else {
                selectHud(null);
            }
        }

        if (mouseButton == 1) {
            HudElement hud = getSelectedHud(mouseX, mouseY);
            if (hud != null && parentScreen instanceof ModsScreen) {
                Utils.playClickSound(true);
                ((ModsScreen) parentScreen).switchMod(hud.getMod());
                Minecraft.getMinecraft().displayGuiScreen(parentScreen);
            }
        }
    }

    private void handlePanelClick(int mouseX, int mouseY, int button) {
        if (button != 0 || selectedHud == null) return;
        Mod mod = selectedHud.getMod();

        // Toggle enable (header)
        int toggleX = panelX + PANEL_W - PANEL_PAD - 26;
        int toggleY = panelY + (HDR_H - 11) / 2;
        if (mouseX >= toggleX && mouseX <= toggleX + 26 && mouseY >= toggleY && mouseY <= toggleY + 11) {
            Utils.playClickSound(true);
            mod.setEnabled(!mod.isEnabled());
            return;
        }

        int ry = panelY + HDR_H + PANEL_PAD;

        // Scale slider click
        if (mod instanceof io.github.angelclient.client.mod.hud.HudMod) {
            int slW = PANEL_W - PANEL_PAD * 2 - 6;
            int slX = panelX + PANEL_PAD;
            if (mouseX >= slX && mouseX <= slX + slW && mouseY >= ry && mouseY <= ry + ROW_H) {
                io.github.angelclient.client.mod.hud.HudMod hm =
                        (io.github.angelclient.client.mod.hud.HudMod) mod;
                float norm = Math.max(0, Math.min(1, (float)(mouseX - slX) / slW));
                hm.scale = 10f + norm * 90f; // range 10..100
                hm.scale = Math.max(10f, Math.min(100f, hm.scale));
                draggingSliderX = slX;
                draggingSliderW = slW;
                draggingOption = null;
                return;
            }
        }
        ry += ROW_H;

        // Opções
        if (panelOptions == null) return;
        for (ModOption opt : panelOptions) {
            Class<?> type = opt.getType();
            if (type == boolean.class) {
                int tx = panelX + PANEL_W - PANEL_PAD - 26;
                int ty = ry + (ROW_H - 11) / 2;
                if (mouseX >= tx && mouseX <= tx + 26 && mouseY >= ty && mouseY <= ty + 11) {
                    Utils.playClickSound(true);
                    opt.setValue(!(Boolean) opt.getValue());
                    return;
                }
                ry += ROW_H;
            } else if (type == float.class || type == int.class) {
                int slW = PANEL_W - PANEL_PAD * 2 - 40;
                int slX = panelX + PANEL_PAD + 40;
                if (mouseX >= slX && mouseX <= slX + slW && mouseY >= ry && mouseY <= ry + ROW_H) {
                    draggingOption  = opt;
                    draggingSliderX = slX;
                    draggingSliderW = slW;
                    applySliderDrag(mouseX);
                    return;
                }
                ry += ROW_H;
            } else if (type.isEnum()) {
                String val = opt.getValue().toString();
                int bw = mc.fontRendererObj.getStringWidth(val) + 10;
                int bx = panelX + PANEL_W - PANEL_PAD - bw;
                if (mouseX >= bx && mouseX <= bx + bw && mouseY >= ry + 2 && mouseY <= ry + ROW_H - 2) {
                    Utils.playClickSound(true);
                    cycleEnum(opt);
                    return;
                }
                ry += ROW_H;
            }
        }
    }

    @Override
    protected void mouseClickedMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingOption != null) {
            applySliderDrag(mouseX);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingOption = null;
        movingHud = null;
        resizingHud = null;
    }

    private void handleResizeDrag(int mouseX, int mouseY) {
        if (resizingHud == null || resizeMode == null) return;
        if (!(resizingHud.getMod() instanceof io.github.angelclient.client.mod.hud.HudMod)) return;

        io.github.angelclient.client.mod.hud.HudMod hm =
                (io.github.angelclient.client.mod.hud.HudMod) resizingHud.getMod();

        // Delta em pixels desde o último frame
        int deltaX = mouseX - resizeStartX;
        int deltaY = mouseY - resizeStartY;

        // Escolher o maior delta para redimensionar proporcionalmente
        int delta = 0;
        if (resizeMode.equals("br") || resizeMode.equals("tr")) delta =  deltaX;
        if (resizeMode.equals("bl") || resizeMode.equals("tl")) delta = -deltaX;
        if (Math.abs(deltaY) > Math.abs(Math.abs(deltaX))) {
            if (resizeMode.equals("br") || resizeMode.equals("bl")) delta =  deltaY;
            if (resizeMode.equals("tr") || resizeMode.equals("tl")) delta = -deltaY;
        }

        // Aplicar delta de escala suavemente (2px = 1% de scale)
        float scaleDelta = delta * 0.8f;
        hm.scale = Math.max(10f, Math.min(100f, hm.scale + scaleDelta));

        // Atualizar referência para o próximo frame
        resizeStartX = mouseX;
        resizeStartY = mouseY;
    }

    private void applySliderDrag(int mouseX) {
        if (draggingOption == null) return;
        float norm = Math.max(0, Math.min(1, (float)(mouseX - draggingSliderX) / draggingSliderW));

        Slider slider = null;
        try { slider = draggingOption.getField().getAnnotation(Slider.class); } catch (Exception ignored) {}

        float min = slider != null ? slider.min() : 0f;
        float max = slider != null ? slider.max() : 100f;
        float step = slider != null ? slider.step() : 1f;

        float raw = min + norm * (max - min);
        if (step > 0) raw = Math.round(raw / step) * step;

        if (draggingOption.getType() == int.class) {
            draggingOption.setValue((int) raw);
        } else {
            draggingOption.setValue(raw);
        }
    }

    private void cycleEnum(ModOption opt) {
        try {
            Method valuesMethod = opt.getType().getMethod("values");
            valuesMethod.setAccessible(true);
            Enum<?>[] vals = (Enum<?>[]) valuesMethod.invoke(null);
            Enum<?> cur = (Enum<?>) opt.getValue();
            int next = (cur.ordinal() + 1) % vals.length;
            opt.setValue(vals[next]);
        } catch (Exception ignored) {}
    }

    private boolean isInPanel(int mx, int my) {
        return selectedHud != null && mx >= panelX && mx < panelX + PANEL_W
                && my >= panelY && my < panelY + panelH;
    }

    private String truncate(String s, int maxWidth) {
        if (mc.fontRendererObj.getStringWidth(s) <= maxWidth) return s;
        while (s.length() > 1 && mc.fontRendererObj.getStringWidth(s + "..") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "..";
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        if (title != null) title.setWorldAndResolution(mc, width, height);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (title != null) title.updateScreen();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 || keyCode == AngelClientMod.instance.editHudKey.getKeyCode()) {
            Client.INSTANCE.save();
            if (title != null) mc.displayGuiScreen(title);
            else               mc.displayGuiScreen(null);
        }
        if (keyCode == 1) selectHud(null);
    }
}