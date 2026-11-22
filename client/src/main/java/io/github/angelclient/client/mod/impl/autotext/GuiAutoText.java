package io.github.angelclient.client.mod.impl.autotext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

/**
 * Tela principal do AutoText — UI no estilo ModsScreen.
 * Paleta escura, retângulos arredondados via GL_POLYGON, botões flat.
 */
public class GuiAutoText extends GuiScreen {

    // ── Paleta (idêntica ao ModsScreen) ─────────────────────────────────────
    private static final int BG      = 0xFF000000;
    private static final int BG1     = 0xFF0F0F12;
    private static final int BG2     = 0xFF16161B;
    private static final int BG3     = 0xFF1E1E25;
    private static final int BG4     = 0xFF26262F;
    private static final int BORDER  = 0xFF000000;
    private static final int BORDER2 = 0xFF000000;
    private static final int BORDER3 = 0xFF000000;
    private static final int TEXT    = 0xFFEEEEF4;
    private static final int TEXT2   = 0xFF88889A;
    private static final int TEXT3   = 0xFF404050;
    private static final int ACCENT  = 0xFFFFFFFF;
    private static final int ACCENT2 = 0xFFCCCCCC;
    private static final int PILL_ON = 0xFFCCCCCC;
    private static final int PILL_OFF= 0xFF18181E;
    private static final int KNOB_ON = 0xFFFFFFFF;
    private static final int KNOB_OFF= 0xFF44444E;
    private static final int DISABLED= 0xFF222228;
    private static final int SEL_BG  = 0xFF1A1A22;
    private static final int DEL_BG  = 0xFF2A0000;
    private static final int DEL_HOV = 0xFF550000;

    private static final float WIN_RADIUS  = 10f;
    private static final float BTN_RADIUS  = 3f;

    private static final int ROW_H = 22;
    private static final int ROW_G = 2;
    private static final int PAD   = 8;
    private static final int HDR_H = 38;
    private static final int FTR_H = 22;

    private static final int GUI_W_BASE = 460;
    private static final int GUI_H_BASE = 300;

    private int guiW, guiH;

    private String  search      = "";
    private boolean searchFocus = false;
    private int     scrollOffset = 0;

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        guiW = Math.min(GUI_W_BASE, width - 20);
        guiH = Math.min(GUI_H_BASE, height - 20);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private int gx() { return (width - guiW) / 2; }
    private int gy() { return (height - guiH) / 2; }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        // Fundo escurecido
        drawRect(0, 0, width, height, 0xC0000000);

        int gx = gx(), gy = gy();

        // Painel principal
        fillRounded(gx, gy, guiW, guiH, WIN_RADIUS, BG);
        outlineRounded(gx, gy, guiW, guiH, WIN_RADIUS, BORDER);

        // ── Header ──────────────────────────────────────────────────────
        drawRect(gx + (int)WIN_RADIUS, gy, gx + guiW - (int)WIN_RADIUS, gy + HDR_H, BG);
        drawRect(gx, gy + (int)WIN_RADIUS, gx + guiW, gy + HDR_H, BG);
        drawRect(gx, gy + HDR_H - 1, gx + guiW, gy + HDR_H, BORDER);

        // Título
        fontRendererObj.drawString("\u00a7lAutoText", gx + PAD, gy + 6, ACCENT);
        fontRendererObj.drawString("Shortcuts", gx + PAD, gy + 17, TEXT3);

        // Barra de busca
        int sbX = gx + PAD + 80, sbY = gy + (HDR_H - 14) / 2;
        int sbW = guiW - PAD * 2 - 80 - 60;
        fillRounded(sbX, sbY, sbW, 14, BTN_RADIUS, BG2);
        outlineRounded(sbX, sbY, sbW, 14, BTN_RADIUS, searchFocus ? ACCENT2 : BORDER);
        String disp = search.isEmpty() && !searchFocus ? "Search..." : search + (searchFocus ? "|" : "");
        fontRendererObj.drawString(disp, sbX + 4, sbY + 3,
                search.isEmpty() && !searchFocus ? TEXT3 : TEXT);

        // Botão + Novo
        int nbX = gx + guiW - PAD - 50, nbY = gy + (HDR_H - 14) / 2;
        boolean hNew = mx >= nbX && mx < nbX + 50 && my >= nbY && my < nbY + 14;
        fillRounded(nbX, nbY, 50, 14, BTN_RADIUS, hNew ? ACCENT2 : ACCENT);
        outlineRounded(nbX, nbY, 50, 14, BTN_RADIUS, ACCENT);
        String nl = "+ New";
        fontRendererObj.drawString(nl, nbX + (50 - fontRendererObj.getStringWidth(nl)) / 2,
                nbY + 3, BG1);

        // ── Área de conteúdo ─────────────────────────────────────────────
        int cx = gx + 2, cy = gy + HDR_H;
        int cw = guiW - 4, ch = guiH - HDR_H - FTR_H;
        drawRect(cx, cy, cx + cw, cy + ch, BG1);

        // Cabeçalho da tabela
        fillRounded(cx + 1, cy + 2, cw - 2, 14, BTN_RADIUS, BG2);
        outlineRounded(cx + 1, cy + 2, cw - 2, 14, BTN_RADIUS, BORDER);
        fontRendererObj.drawString("Name", cx + 10, cy + 5, TEXT2);
        fontRendererObj.drawString("Key", cx + 130, cy + 5, TEXT2);
        fontRendererObj.drawString("Message", cx + 200, cy + 5, TEXT2);
        fontRendererObj.drawString("Actions", cx + cw - 110, cy + 5, TEXT2);

        // Linhas
        List<Atalho> list = AutoTextMod.instance.atalhoManager.getAtalhos();
        String filter = search.toLowerCase().trim();
        int ly = cy + 19;
        int maxRows = (ch - 19) / (ROW_H + ROW_G);
        int vis = 0;

        // Contar itens filtrados
        int filteredCount = 0;
        for (Atalho a : list) {
            if (!filter.isEmpty()
                    && !a.getNome().toLowerCase().contains(filter)
                    && !a.getMensagem().toLowerCase().contains(filter)) continue;
            filteredCount++;
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredCount - maxRows)));

        int skipped = 0;
        for (int i = 0; i < list.size(); i++) {
            Atalho a = list.get(i);
            if (!filter.isEmpty()
                    && !a.getNome().toLowerCase().contains(filter)
                    && !a.getMensagem().toLowerCase().contains(filter)) continue;
            if (skipped < scrollOffset) { skipped++; continue; }
            if (vis >= maxRows) break;
            drawRow(a, i, cx, ly, cw, mx, my);
            ly += ROW_H + ROW_G;
            vis++;
        }

        if (list.isEmpty()) {
            String empty = "No shortcuts. Click + New to create one.";
            fontRendererObj.drawString(empty,
                    cx + (cw - fontRendererObj.getStringWidth(empty)) / 2,
                    cy + ch / 2 - 4, TEXT3);
        }

        // Scrollbar
        if (filteredCount > maxRows) {
            int sbW2 = 3, sbPad = 4;
            int sbXr = cx + cw - sbW2 - sbPad;
            int sbTrackH = ch - 19 - sbPad * 2;
            fillRounded(sbXr, cy + 19 + sbPad, sbW2, sbTrackH, 1, BG3);
            int sbH2 = Math.max(16, (int)((float)maxRows / filteredCount * sbTrackH));
            int sbYr = cy + 19 + sbPad + (int)((float)scrollOffset / filteredCount * sbTrackH);
            fillRounded(sbXr, sbYr, sbW2, sbH2, 1, ACCENT2);
        }

        // ── Footer ───────────────────────────────────────────────────────
        int fr = (int) WIN_RADIUS;
        drawRect(gx + fr, gy + guiH - FTR_H, gx + guiW - fr, gy + guiH, BG);
        drawRect(gx, gy + guiH - FTR_H, gx + guiW, gy + guiH - fr, BG);
        drawRect(gx, gy + guiH - FTR_H, gx + guiW, gy + guiH - FTR_H + 1, BORDER);

        // Botão Close
        int cbX = gx + guiW - PAD - 40, cbY = gy + guiH - FTR_H + 4;
        boolean hClose = mx >= cbX && mx < cbX + 40 && my >= cbY && my < cbY + 14;
        fillRounded(cbX, cbY, 40, 14, BTN_RADIUS, hClose ? BG4 : BG3);
        outlineRounded(cbX, cbY, 40, 14, BTN_RADIUS, hClose ? BORDER3 : BORDER2);
        String cl = "Close";
        fontRendererObj.drawString(cl, cbX + (40 - fontRendererObj.getStringWidth(cl)) / 2,
                cbY + 3, hClose ? TEXT : TEXT2);

        super.drawScreen(mx, my, pt);
    }

    // ── Linha de atalho ──────────────────────────────────────────────────────

    private void drawRow(Atalho a, int idx, int cx, int y, int cw, int mx, int my) {
        boolean ena = a.isAtivo();
        boolean hov = mx >= cx + 3 && mx < cx + cw - 3 && my >= y && my < y + ROW_H;

        fillRounded(cx + 3, y, cw - 6, ROW_H, BTN_RADIUS, hov ? BG3 : BG2);
        // Barra de status à esquerda
        drawRect(cx + 3, y + 2, cx + 5, y + ROW_H - 2, ena ? ACCENT : DISABLED);
        drawRect(cx + 3, y + ROW_H - 1, cx + cw - 3, y + ROW_H, BORDER);

        int ty = y + (ROW_H - 8) / 2;

        // Nome
        String nome = a.getNome();
        if (fontRendererObj.getStringWidth(nome) > 110) {
            while (fontRendererObj.getStringWidth(nome + "..") > 110 && nome.length() > 1)
                nome = nome.substring(0, nome.length() - 1);
            nome += "..";
        }
        fontRendererObj.drawString(nome, cx + 10, ty, ena ? TEXT : TEXT2);

        // Tecla — pill
        String kn = a.getKeyCode() > 0 ? Keyboard.getKeyName(a.getKeyCode()) : "---";
        int kw = fontRendererObj.getStringWidth(kn) + 8;
        int kx = cx + 130, ky = y + (ROW_H - 12) / 2;
        fillRounded(kx, ky, kw, 12, 2, BG);
        outlineRounded(kx, ky, kw, 12, 2, BORDER2);
        fontRendererObj.drawString(kn, kx + 4, ky + 2, ACCENT2);

        // Mensagem
        String msg = a.getMensagem();
        int maxMsgW = cw - 320;
        if (fontRendererObj.getStringWidth(msg) > maxMsgW) {
            while (fontRendererObj.getStringWidth(msg + "..") > maxMsgW && msg.length() > 1)
                msg = msg.substring(0, msg.length() - 1);
            msg += "..";
        }
        fontRendererObj.drawString(msg, cx + 200, ty, TEXT2);

        // Botões de ação
        int bx = cx + cw - 110;
        int by = y + (ROW_H - 12) / 2;

        // Editar
        boolean hE = mx >= bx && mx < bx + 30 && my >= by && my < by + 12;
        fillRounded(bx, by, 30, 12, BTN_RADIUS, hE ? BG4 : BG3);
        outlineRounded(bx, by, 30, 12, BTN_RADIUS, hE ? BORDER3 : BORDER2);
        String el = "Edit";
        fontRendererObj.drawString(el, bx + (30 - fontRendererObj.getStringWidth(el)) / 2,
                by + 2, hE ? TEXT : TEXT2);

        // Toggle
        drawPill(bx + 34, by + 1, ena);

        // Excluir
        int dx = bx + 66;
        boolean hX = mx >= dx && mx < dx + 18 && my >= by && my < by + 12;
        fillRounded(dx, by, 18, 12, BTN_RADIUS, hX ? DEL_HOV : DEL_BG);
        outlineRounded(dx, by, 18, 12, BTN_RADIUS, hX ? 0xFF550000 : BORDER2);
        String xl = "X";
        fontRendererObj.drawString(xl, dx + (18 - fontRendererObj.getStringWidth(xl)) / 2,
                by + 2, hX ? 0xFFFF5555 : TEXT3);
    }

    private void drawPill(int x, int y, boolean on) {
        fillRounded(x, y, 26, 11, 3, on ? PILL_ON : PILL_OFF);
        outlineRounded(x, y, 26, 11, 3, on ? ACCENT2 : BORDER);
        int kx = on ? x + 14 : x + 2;
        fillRounded(kx, y + 2, 9, 7, 2, on ? KNOB_ON : KNOB_OFF);
    }

    // ── Primitivas GL (estilo ModsScreen) ────────────────────────────────────

    private static void drawRoundedRect(float x1, float y1, float x2, float y2,
                                         float radius, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i <= 90; i += 6)
            GL11.glVertex2d(x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 90; i <= 180; i += 6)
            GL11.glVertex2d(x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 180; i <= 270; i += 6)
            GL11.glVertex2d(x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 270; i <= 360; i += 6)
            GL11.glVertex2d(x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void drawHollowRoundedRect(float x1, float y1, float x2, float y2,
                                               float radius, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 90; i += 6)
            GL11.glVertex2d(x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 90; i <= 180; i += 6)
            GL11.glVertex2d(x1 + radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 180; i <= 270; i += 6)
            GL11.glVertex2d(x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y2 - radius + Math.cos(Math.toRadians(i)) * radius * -1);
        for (int i = 270; i <= 360; i += 6)
            GL11.glVertex2d(x2 - radius + Math.sin(Math.toRadians(i)) * radius * -1,
                            y1 + radius + Math.cos(Math.toRadians(i)) * radius * -1);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void fillRounded(float x, float y, float w, float h, float radius, int color) {
        drawRoundedRect(x, y, x + w, y + h, radius, color);
    }

    private static void outlineRounded(float x, float y, float w, float h, float radius, int color) {
        drawHollowRoundedRect(x, y, x + w, y + h, radius, color);
    }

    // ── Interação ────────────────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        int gx = gx(), gy = gy();

        // Search focus
        int sbX = gx + PAD + 80, sbY = gy + (HDR_H - 14) / 2;
        int sbW = guiW - PAD * 2 - 80 - 60;
        searchFocus = mx >= sbX && mx < sbX + sbW && my >= sbY && my < sbY + 14;

        // Botão + Novo
        int nbX = gx + guiW - PAD - 50, nbY = gy + (HDR_H - 14) / 2;
        if (mx >= nbX && mx < nbX + 50 && my >= nbY && my < nbY + 14) {
            mc.displayGuiScreen(new GuiEditarAtalho(this, -1, null)); return;
        }

        // Botão Close
        int cbX = gx + guiW - PAD - 40, cbY = gy + guiH - FTR_H + 4;
        if (mx >= cbX && mx < cbX + 40 && my >= cbY && my < cbY + 14) {
            mc.displayGuiScreen(null); return;
        }

        // Cliques nas linhas
        List<Atalho> list = AutoTextMod.instance.atalhoManager.getAtalhos();
        String filter = search.toLowerCase().trim();
        int cx = gx + 2, cy = gy + HDR_H;
        int cw = guiW - 4, ch = guiH - HDR_H - FTR_H;
        int ly = cy + 19;
        int maxRows = (ch - 19) / (ROW_H + ROW_G);
        int vis = 0, skipped = 0;

        for (int i = 0; i < list.size(); i++) {
            Atalho a = list.get(i);
            if (!filter.isEmpty()
                    && !a.getNome().toLowerCase().contains(filter)
                    && !a.getMensagem().toLowerCase().contains(filter)) continue;
            if (skipped < scrollOffset) { skipped++; continue; }
            if (vis >= maxRows) break;

            int bx = cx + cw - 110;
            int by = ly + (ROW_H - 12) / 2;

            if (mx >= bx && mx < bx + 30 && my >= by && my < by + 12) {
                mc.displayGuiScreen(new GuiEditarAtalho(this, i, a)); return;
            }
            if (mx >= bx + 34 && mx < bx + 60 && my >= by + 1 && my < by + 12) {
                AutoTextMod.instance.atalhoManager.toggleAtivo(i); return;
            }
            int dx = bx + 66;
            if (mx >= dx && mx < dx + 18 && my >= by && my < by + 12) {
                AutoTextMod.instance.atalhoManager.remover(i); return;
            }

            ly += ROW_H + ROW_G;
            vis++;
        }
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (searchFocus) {
            if (key == 14) { if (!search.isEmpty()) search = search.substring(0, search.length() - 1); }
            else if (ch >= 32) search += ch;
            scrollOffset = 0;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (dWheel != 0) {
            scrollOffset += dWheel > 0 ? -1 : 1;
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
