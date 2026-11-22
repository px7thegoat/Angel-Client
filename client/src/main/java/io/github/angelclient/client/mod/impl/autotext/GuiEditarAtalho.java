package io.github.angelclient.client.mod.impl.autotext;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

/**
 * Tela de criação/edição de um Atalho — UI no estilo ModsScreen.
 * Paleta escura, retângulos arredondados via GL_POLYGON, botões flat.
 */
public class GuiEditarAtalho extends GuiScreen {

    // ── Paleta (idêntica ao ModsScreen) ─────────────────────────────────────
    private static final int BG      = 0xFF000000;
    private static final int BG1     = 0xFF0F0F12;
    private static final int BG2     = 0xFF16161B;
    private static final int BG3     = 0xFF1E1E25;
    private static final int BG4     = 0xFF26262F;
    private static final int BORDER  = 0xFF242428;
    private static final int BORDER2 = 0xFF303038;
    private static final int BORDER3 = 0xFF42424E;
    private static final int TEXT    = 0xFFEEEEF4;
    private static final int TEXT2   = 0xFF88889A;
    private static final int TEXT3   = 0xFF404050;
    private static final int ACCENT  = 0xFFFFFFFF;
    private static final int ACCENT2 = 0xFFCCCCCC;
    private static final int DISABLED= 0xFF222228;
    private static final int ERR_BD  = 0xFF552222;

    private static final float WIN_RADIUS = 8f;
    private static final float BTN_RADIUS = 3f;
    private static final int PAD = 8;

    // ── Estado ───────────────────────────────────────────────────────────────
    private final GuiScreen telaAnterior;
    private final int       editIndex;
    private final Atalho    atalhoRef;

    private String  nomeText = "";
    private String  msgText  = "";
    private int     keyCapturado    = 0;
    private boolean capturandoTecla = false;
    private int     focusedField    = 0; // 0=nome, 1=msg, -1=none

    private boolean errNome = false;
    private boolean errMsg  = false;
    private boolean errKey  = false;

    private static final int PW = 280;
    private static final int PH = 210;

    public GuiEditarAtalho(GuiScreen anterior, int editIndex, Atalho atalho) {
        this.telaAnterior = anterior;
        this.editIndex    = editIndex;
        this.atalhoRef    = atalho;
        if (atalho != null) {
            this.keyCapturado = atalho.getKeyCode();
            this.nomeText     = atalho.getNome();
            this.msgText      = atalho.getMensagem();
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        focusedField = 0;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    // ── Render ───────────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        // Fundo escurecido
        drawRect(0, 0, width, height, 0xC0000000);

        int cx = width / 2, cy = height / 2;
        int px = cx - PW / 2, py = cy - PH / 2;

        // Painel
        fillRounded(px, py, PW, PH, WIN_RADIUS, BG);
        outlineRounded(px, py, PW, PH, WIN_RADIUS, BG);

        // Header
        int hdrH = 30;
        int rv = (int) WIN_RADIUS;
        drawRect(px + rv, py, px + PW - rv, py + hdrH, BG);
        drawRect(px, py + rv, px + PW, py + hdrH, BG);
        drawRect(px, py + hdrH - 1, px + PW, py + hdrH, 0xFF2A2A2A);

        String titulo = editIndex < 0 ? "New Shortcut" : "Edit Shortcut";
        fontRendererObj.drawString("\u00a7l" + titulo, px + PAD, py + (hdrH - 8) / 2, ACCENT);

        int bodyY = py + hdrH + PAD;

        // ── Campo Nome ──────────────────────────────────────────────────
        fontRendererObj.drawString("Name", px + PAD, bodyY, TEXT2);
        if (errNome) {
            String e = "Required";
            fontRendererObj.drawString(e, px + PW - PAD - fontRendererObj.getStringWidth(e), bodyY, ERR_BD);
        }
        bodyY += 11;
        int fW = PW - PAD * 2;
        int nBd = errNome ? ERR_BD : (focusedField == 0 ? ACCENT2 : BORDER);
        fillRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, BG2);
        outlineRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, nBd);
        String nd = nomeText + (focusedField == 0 ? "|" : "");
        fontRendererObj.drawString(nd, px + PAD + 4, bodyY + 4, nomeText.isEmpty() && focusedField != 0 ? TEXT3 : TEXT);

        bodyY += 24;

        // ── Campo Tecla ─────────────────────────────────────────────────
        fontRendererObj.drawString("Key", px + PAD, bodyY, TEXT2);
        if (errKey) {
            String e = "Set key";
            fontRendererObj.drawString(e, px + PW - PAD - fontRendererObj.getStringWidth(e), bodyY, ERR_BD);
        }
        bodyY += 11;

        boolean keyHov = mx >= px + PAD && mx < px + PAD + fW && my >= bodyY && my < bodyY + 16;
        int kBg = capturandoTecla ? BG4 : (keyHov ? BG3 : BG2);
        int kBd = errKey ? ERR_BD : (capturandoTecla ? ACCENT : (keyHov ? BORDER3 : BORDER));
        fillRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, kBg);
        outlineRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, kBd);

        String keyLabel;
        int keyColor;
        if (capturandoTecla) {
            keyLabel = "Press a key...";
            keyColor = ACCENT;
        } else if (keyCapturado > 0) {
            keyLabel = Keyboard.getKeyName(keyCapturado);
            keyColor = ACCENT2;
        } else {
            keyLabel = "Click to set key";
            keyColor = TEXT3;
        }
        int klx = px + PAD + (fW - fontRendererObj.getStringWidth(keyLabel)) / 2;
        fontRendererObj.drawString(keyLabel, klx, bodyY + 4, keyColor);

        bodyY += 24;

        // ── Campo Mensagem ──────────────────────────────────────────────
        fontRendererObj.drawString("Message", px + PAD, bodyY, TEXT2);
        if (errMsg) {
            String e = "Required";
            fontRendererObj.drawString(e, px + PW - PAD - fontRendererObj.getStringWidth(e), bodyY, ERR_BD);
        }
        bodyY += 11;
        int mBd = errMsg ? ERR_BD : (focusedField == 1 ? ACCENT2 : BORDER);
        fillRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, BG2);
        outlineRounded(px + PAD, bodyY, fW, 16, BTN_RADIUS, mBd);
        String md = msgText + (focusedField == 1 ? "|" : "");
        fontRendererObj.drawString(md, px + PAD + 4, bodyY + 4, msgText.isEmpty() && focusedField != 1 ? TEXT3 : TEXT);

        bodyY += 22;

        // Dica placeholders

        // ── Botões ───────────────────────────────────────────────────────
        int btnY = py + PH - 26;
        int btnW = 60;
        int saveX = cx - btnW - 4;
        int cancelX = cx + 4;

        // Save
        boolean hSave = mx >= saveX && mx < saveX + btnW && my >= btnY && my < btnY + 16;
        fillRounded(saveX, btnY, btnW, 16, BTN_RADIUS, hSave ? ACCENT2 : ACCENT);
        outlineRounded(saveX, btnY, btnW, 16, BTN_RADIUS, ACCENT);
        String sl = "Save";
        fontRendererObj.drawString(sl, saveX + (btnW - fontRendererObj.getStringWidth(sl)) / 2,
                btnY + 4, BG1);

        // Cancel
        boolean hCancel = mx >= cancelX && mx < cancelX + btnW && my >= btnY && my < btnY + 16;
        fillRounded(cancelX, btnY, btnW, 16, BTN_RADIUS, hCancel ? BG4 : BG3);
        outlineRounded(cancelX, btnY, btnW, 16, BTN_RADIUS, hCancel ? BORDER3 : BORDER2);
        String cll = "Cancel";
        fontRendererObj.drawString(cll, cancelX + (btnW - fontRendererObj.getStringWidth(cll)) / 2,
                btnY + 4, hCancel ? TEXT : TEXT2);

        super.drawScreen(mx, my, pt);
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

        int cx = width / 2, cy = height / 2;
        int px = cx - PW / 2, py = cy - PH / 2;
        int fW = PW - PAD * 2;
        int bodyY = py + 30 + PAD;

        // Campo nome
        int nfY = bodyY + 11;
        if (mx >= px + PAD && mx < px + PAD + fW && my >= nfY && my < nfY + 16) {
            focusedField = 0; capturandoTecla = false; errNome = false; return;
        }

        // Campo tecla
        int kfY = nfY + 35;
        if (mx >= px + PAD && mx < px + PAD + fW && my >= kfY && my < kfY + 16) {
            capturandoTecla = true; focusedField = -1; errKey = false; return;
        }

        // Campo mensagem
        int mfY = kfY + 35;
        if (mx >= px + PAD && mx < px + PAD + fW && my >= mfY && my < mfY + 16) {
            focusedField = 1; capturandoTecla = false; errMsg = false; return;
        }

        // Botões
        int btnY = py + PH - 26;
        int btnW = 60;
        int saveX = cx - btnW - 4;
        int cancelX = cx + 4;

        if (mx >= saveX && mx < saveX + btnW && my >= btnY && my < btnY + 16) {
            salvar(); return;
        }
        if (mx >= cancelX && mx < cancelX + btnW && my >= btnY && my < btnY + 16) {
            mc.displayGuiScreen(telaAnterior); return;
        }
    }

    @Override
    protected void keyTyped(char ch, int key) throws IOException {
        if (capturandoTecla) {
            if (key == Keyboard.KEY_ESCAPE) { capturandoTecla = false; }
            else { keyCapturado = key; capturandoTecla = false; }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(telaAnterior); return; }
        if (key == Keyboard.KEY_TAB) {
            focusedField = focusedField == 0 ? 1 : 0; return;
        }

        if (focusedField == 0) {
            if (key == 14) { if (!nomeText.isEmpty()) nomeText = nomeText.substring(0, nomeText.length() - 1); }
            else if (ch >= 32 && nomeText.length() < 32) nomeText += ch;
        } else if (focusedField == 1) {
            if (key == 14) { if (!msgText.isEmpty()) msgText = msgText.substring(0, msgText.length() - 1); }
            else if (ch >= 32 && msgText.length() < 200) msgText += ch;
        }
    }

    // ── Salvar ───────────────────────────────────────────────────────────────

    private void salvar() {
        String nome     = nomeText.trim();
        String mensagem = msgText.trim();

        errNome = nome.isEmpty();
        errMsg  = mensagem.isEmpty();
        errKey  = keyCapturado <= 0;

        if (errNome || errMsg || errKey) {
            if (errKey) capturandoTecla = false;
            return;
        }

        AtalhoManager am = AutoTextMod.instance.atalhoManager;
        if (editIndex < 0) am.adicionar(nome, keyCapturado, mensagem);
        else               am.editar(editIndex, nome, keyCapturado, mensagem);

        mc.displayGuiScreen(telaAnterior);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
