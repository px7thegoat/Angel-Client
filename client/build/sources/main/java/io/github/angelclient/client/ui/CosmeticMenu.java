package io.github.angelclient.client.ui;

import io.github.angelclient.client.cosmetic.Cosmetic;
import io.github.angelclient.client.cosmetic.CosmeticManager;
import io.github.angelclient.client.cosmetic.render.Cosmetic3DRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import io.github.angelclient.client.util.render.GLRectUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CosmeticMenu extends GuiScreen {

    private final GuiScreen parentScreen;
    private final List<Cosmetic> cosmetics;
    private int   scrollOffset    = 0;
    private float sliderValue     = 0f;
    private float playerRotation  = 0f;
    private boolean sliderDrag    = false;
    private int   sliderDragX;
    private float sliderDragStart;
    private String lastEquippedId = null;
    private boolean capeEnabled   = true;
    private int hoveredIndex      = -1;
    private long openTime         = System.currentTimeMillis();

    // ── Paleta ModsScreen ──────────────────────────────────────────────────────
    private static final int BG          = 0xFF0D0D0D;
    private static final int PANEL       = 0xFF141414;
    private static final int PANEL2      = 0xFF1E1E1E;
    private static final int BORDER      = 0xFF2A2A2A;
    private static final int BORDER_LIT  = 0xFF3A3A3A;
    private static final int ACCENT      = 0xFFFFFFFF;
    private static final int ACCENT_DIM  = 0x44FFFFFF;
    private static final int TEXT        = 0xFFEEEEEE;
    private static final int TEXT_DIM    = 0xFFAAAAAA;
    private static final int TEXT_DARK   = 0xFF666666;
    private static final int SEL_BG      = 0xFF1E1E1E;
    private static final int SEL_BORDER  = 0xFFFFFFFF;
    private static final int HOV_BG      = 0xFF1E1E1E;
    private static final int HOV_BORDER  = 0xFF3A3A3A;
    private static final float RADIUS    = 3f;

    // ── Layout ─────────────────────────────────────────────────────────────────
    private static final int COLS     = 3;
    private static final int PAD      = 12;
    private static final int GAP      = 8;
    private static final int TITLE_H  = 28;
    private static final int FOOTER_H = 32;
    private static final int THUMB_W  = 10;
    private static final int SLIDER_H = 6;

    private int WIN_W, WIN_H, WIN_X, WIN_Y;
    private int LEFT_W, MID_W, RIGHT_W;
    private int LEFT_X, MID_X, RIGHT_X;
    private int BODY_Y, BODY_H;
    private int ITEM_W, ITEM_H;

    public CosmeticMenu(GuiScreen parent) {
        this.parentScreen = parent;
        this.cosmetics = new ArrayList<>(CosmeticManager.getInstance().getCurrentPlayerCosmetics());
    }

    private void recalc() {
        WIN_W  = Math.min(Math.max((int)(width  * 0.82f), 460), 700);
        WIN_H  = Math.min(Math.max((int)(height * 0.82f), 280), 420);
        WIN_X  = (width  - WIN_W) / 2;
        WIN_Y  = (height - WIN_H) / 2;
        BODY_Y = WIN_Y + TITLE_H;
        BODY_H = WIN_H - TITLE_H - FOOTER_H;
        LEFT_W = (int)(WIN_W * 0.20f);
        RIGHT_W= (int)(WIN_W * 0.30f);
        MID_W  = WIN_W - LEFT_W - RIGHT_W - PAD * 4;
        LEFT_X = WIN_X + PAD;
        MID_X  = LEFT_X + LEFT_W + PAD;
        RIGHT_X= MID_X + MID_W + PAD;
        ITEM_W = (MID_W - GAP * (COLS - 1)) / COLS;
        ITEM_H = (int)(ITEM_W * 1.30f);
    }

    @Override
    public void initGui() {
        recalc();
        buttonList.clear();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (cosmetics.isEmpty())
            cosmetics.addAll(CosmeticManager.getInstance().getCurrentPlayerCosmetics());

        // Recarrega a lista periodicamente para pegar capas que ainda estavam baixando
        if (System.currentTimeMillis() - openTime < 10000) {
            List<Cosmetic> fresh = CosmeticManager.getInstance().getCurrentPlayerCosmetics();
            if (fresh.size() != cosmetics.size()) {
                cosmetics.clear();
                cosmetics.addAll(fresh);
            }
        }
        recalc();

        // Drag slider
        if (sliderDrag) {
            if (Mouse.isButtonDown(0)) {
                int sliderX = RIGHT_X + PAD;
                int sliderW = RIGHT_W - PAD * 2;
                int track   = sliderW - THUMB_W;
                float v = sliderDragStart + (float)(mouseX - sliderDragX) / track;
                sliderValue    = Math.max(0f, Math.min(1f, v));
                playerRotation = sliderValue * 360f;
            } else {
                sliderDrag = false;
            }
        }

        // ── Fundo escuro ──────────────────────────────────────────────────────
        drawRect(0, 0, width, height, 0xCC000000);

        // ── Janela principal ──────────────────────────────────────────────────
        GLRectUtils.drawRoundedRect(WIN_X + 5, WIN_Y + 5, WIN_X + WIN_W + 5, WIN_Y + WIN_H + 5, RADIUS, 0x44000000);
        GLRectUtils.drawRoundedRect(WIN_X, WIN_Y, WIN_X + WIN_W, WIN_Y + WIN_H, RADIUS, BG);
        GLRectUtils.drawRoundedOutline(WIN_X, WIN_Y, WIN_X + WIN_W, WIN_Y + WIN_H, RADIUS, 1f, BORDER);

        // ── Título ────────────────────────────────────────────────────────────
        drawRect(WIN_X, WIN_Y + TITLE_H - 1, WIN_X + WIN_W, WIN_Y + TITLE_H, BORDER);
        String title = "\u00A7lANGEL \u00A7rCOSMETICS";
        fontRendererObj.drawString(title, WIN_X + PAD + 4, WIN_Y + (TITLE_H - 8) / 2, ACCENT);

        // ── Botão fechar ──────────────────────────────────────────────────────
        int closeX = WIN_X + WIN_W - 20, closeY = WIN_Y + 4;
        boolean hClose = mouseX >= closeX && mouseX <= closeX + 16
                      && mouseY >= closeY && mouseY <= closeY + 16;
        if (hClose) {
            GLRectUtils.drawRoundedRect(closeX, closeY, closeX + 16, closeY + 16, RADIUS, 0xFF550000);
            GLRectUtils.drawRoundedOutline(closeX, closeY, closeX + 16, closeY + 16, RADIUS, 1f, 0xFFFF4444);
        }
        
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, hClose ? 1f : 0.5f);
        mc.getTextureManager().bindTexture(new net.minecraft.util.ResourceLocation("angel_client", "textures/mods/angel_client_exit_2x.png"));
        drawModalRectWithCustomSizedTexture(closeX + 3, closeY + 3, 0, 0, 10, 10, 10, 10);
        GlStateManager.disableBlend();

        // ── Painéis ───────────────────────────────────────────────────────────
        drawLeftPanel(mouseX, mouseY);
        drawGridPanel(mouseX, mouseY);
        drawRightPanel(mouseX, mouseY, partialTicks);

        // ── Footer ────────────────────────────────────────────────────────────
        int footerY = WIN_Y + WIN_H - FOOTER_H;
        drawRect(WIN_X, footerY, WIN_X + WIN_W, footerY + 1, BORDER);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // ── Helper: desenha APENAS a face frontal do corpo da capa ─────────────
    private void drawCapeFront(int dstX, int dstY, int dstW, int dstH) {
        final double U1 =  1.0 / 64.0;
        final double U2 = 11.0 / 64.0;
        final double V1 =  1.0 / 32.0;
        final double V2 = 17.0 / 32.0;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(dstX,        dstY + dstH, 0).tex(U1, V2).endVertex();
        wr.pos(dstX + dstW, dstY + dstH, 0).tex(U2, V2).endVertex();
        wr.pos(dstX + dstW, dstY,        0).tex(U2, V1).endVertex();
        wr.pos(dstX,        dstY,        0).tex(U1, V1).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
    }

    // ── Painel esquerdo: capa ativa ──────────────────────────────────────────
    private void drawLeftPanel(int mx, int my) {
        int x = LEFT_X, y = BODY_Y, w = LEFT_W, h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);

        // ── Se o jogador não tem nenhuma capa, mostra painel vazio ───────────
        if (cosmetics.isEmpty()) {
            String ph = "Nenhuma";
            fontRendererObj.drawString(ph,
                x + w/2 - fontRendererObj.getStringWidth(ph)/2,
                y + h/2 - 4, TEXT_DARK);
            return;
        }

        // ── Botão ativar/desativar (só aparece se tem capa) ──────────────────
        final int BTN_H_LEFT = 22;
        final int BTN_GAP    = 6;

        int btnY = y + h - BTN_H_LEFT - BTN_GAP;
        boolean hBtn = mx >= x && mx < x + w && my >= btnY && my < btnY + BTN_H_LEFT;
        GLRectUtils.drawRoundedRect(x + 4, btnY, x + w - 4, btnY + BTN_H_LEFT, RADIUS, hBtn ? PANEL2 : BG);
        GLRectUtils.drawRoundedOutline(x + 4, btnY, x + w - 4, btnY + BTN_H_LEFT, RADIUS, 1f, hBtn ? BORDER_LIT : BORDER);
        String btnLbl = capeEnabled ? "Desativar capa" : "Ativar capa";
        fontRendererObj.drawString(btnLbl,
            x + w/2 - fontRendererObj.getStringWidth(btnLbl)/2,
            btnY + (BTN_H_LEFT - 8) / 2, hBtn ? TEXT : TEXT_DIM);

        int cx2 = x + PAD / 2;
        int cy2 = y + 24;
        int cw2 = w - PAD;
        int ch2 = btnY - cy2 - BTN_GAP;

        String equippedId = CosmeticManager.getInstance().getEquippedCosmeticId();
        Cosmetic equipped = cosmetics.stream()
            .filter(c -> c.getId().equals(equippedId)).findFirst().orElse(null);

        boolean hasEquipped = equipped != null && capeEnabled;
        GLRectUtils.drawRoundedRect(cx2, cy2, cx2 + cw2, cy2 + ch2, RADIUS, PANEL2);
        GLRectUtils.drawRoundedOutline(cx2, cy2, cx2 + cw2, cy2 + ch2, RADIUS, 1f, hasEquipped ? BORDER_LIT : BORDER);

        if (equipped != null && equipped.getTexture() != null) {
            GlStateManager.color(1f, 1f, 1f, capeEnabled ? 1f : 0.35f);
            mc.getTextureManager().bindTexture(equipped.getTexture());

            int maxW = cw2 - 8;
            int maxH = ch2 - 20;
            int imgW, imgH;
            if (maxW * 16 / 10 <= maxH) {
                imgW = maxW;
                imgH = maxW * 16 / 10;
            } else {
                imgH = maxH;
                imgW = maxH * 10 / 16;
            }
            int imgX = cx2 + (cw2 - imgW) / 2;
            int imgY = cy2 + 6;
            drawCapeFront(imgX, imgY, imgW, imgH);

            GlStateManager.color(1f, 1f, 1f, 1f);
            String name = equipped.getName();
            int nw = fontRendererObj.getStringWidth(name);
            if (nw > cw2 - 4) {
                while (fontRendererObj.getStringWidth(name + "..") > cw2 - 4 && name.length() > 1)
                    name = name.substring(0, name.length() - 1);
                name += "..";
            }
            fontRendererObj.drawString(name,
                cx2 + cw2/2 - fontRendererObj.getStringWidth(name)/2,
                cy2 + ch2 - 10, TEXT);
        } else {
            String ph = equipped == null ? "Nenhuma" : equipped.getName();
            fontRendererObj.drawString(ph,
                cx2 + cw2/2 - fontRendererObj.getStringWidth(ph)/2,
                cy2 + ch2/2 - 4, TEXT_DARK);
        }

        String badge = capeEnabled ? "ON" : "OFF";
        int badgeColor = capeEnabled ? 0xFF44FF44 : 0xFF666666;
        fontRendererObj.drawString(badge,
            cx2 + cw2 - fontRendererObj.getStringWidth(badge) - 3,
            cy2 + 3, badgeColor);
    }

    // ── Painel central: grid de cosméticos ───────────────────────────────────
    private void drawGridPanel(int mx, int my) {
        int x = MID_X, y = BODY_Y, w = MID_W, h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);

        // ── Se não tem capas, mostra mensagem centralizada ────────────────────
        if (cosmetics.isEmpty()) {
            String msg1 = "Você não possui nenhuma capa.";
            String msg2 = "Adquira uma capa para acessar o menu.";
            fontRendererObj.drawString(msg1,
                x + w/2 - fontRendererObj.getStringWidth(msg1)/2,
                y + h/2 - 12, TEXT_DARK);
            fontRendererObj.drawString(msg2,
                x + w/2 - fontRendererObj.getStringWidth(msg2)/2,
                y + h/2 + 2, TEXT_DARK);
            return;
        }

        String equippedId = CosmeticManager.getInstance().getEquippedCosmeticId();

        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - (y + h)) * scale,
                       w * scale, h * scale);

        int xPos = x + GAP, yPos = y + GAP - scrollOffset;
        int col  = 0;
        hoveredIndex = -1;

        for (int i = 0; i < cosmetics.size(); i++) {
            Cosmetic c = cosmetics.get(i);
            boolean selected = c.getId().equals(equippedId);
            boolean hov = mx >= xPos && mx < xPos + ITEM_W
                       && my >= yPos && my < yPos + ITEM_H
                       && my >= y && my < y + h;
            if (hov) hoveredIndex = i;

            int bg     = selected ? SEL_BG : (hov ? HOV_BG : PANEL2);
            int border = selected ? SEL_BORDER : (hov ? HOV_BORDER : BORDER);

            GLRectUtils.drawRoundedRect(xPos, yPos, xPos + ITEM_W, yPos + ITEM_H, RADIUS, bg);
            GLRectUtils.drawRoundedOutline(xPos, yPos, xPos + ITEM_W, yPos + ITEM_H, RADIUS, 1f, border);



            if (c.getTexture() != null) {
                int maxW = ITEM_W - 8;
                int maxH = (int)(ITEM_H * 0.72f);
                int imgW2, imgH2;
                if (maxW * 16 / 10 <= maxH) {
                    imgW2 = maxW;
                    imgH2 = maxW * 16 / 10;
                } else {
                    imgH2 = maxH;
                    imgW2 = maxH * 10 / 16;
                }
                int imgX2 = xPos + (ITEM_W - imgW2) / 2;
                int imgY2 = yPos + 6;
                GlStateManager.color(1f, 1f, 1f, 1f);
                mc.getTextureManager().bindTexture(c.getTexture());
                drawCapeFront(imgX2, imgY2, imgW2, imgH2);
            }

            String name = c.getName();
            int nw = fontRendererObj.getStringWidth(name);
            if (nw > ITEM_W - 4) {
                while (fontRendererObj.getStringWidth(name + "..") > ITEM_W - 4 && name.length() > 1)
                    name = name.substring(0, name.length() - 1);
                name += "..";
                nw = fontRendererObj.getStringWidth(name);
            }
            fontRendererObj.drawString(name,
                xPos + ITEM_W/2 - nw/2,
                yPos + ITEM_H - 10,
                selected ? TEXT : (hov ? TEXT : TEXT_DIM));

            xPos += ITEM_W + GAP;
            col++;
            if (col >= COLS) {
                col  = 0;
                xPos = x + GAP;
                yPos += ITEM_H + GAP;
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        int totalRows = (int)Math.ceil((float)cosmetics.size() / COLS);
        int totalH    = totalRows * (ITEM_H + GAP);
        if (totalH > h) {
            int sbX  = x + w - 4;
            int sbH  = Math.max(16, (int)((float)h / totalH * h));
            int sbY  = y + (int)((float)scrollOffset / (totalH - h) * (h - sbH));
            GLRectUtils.drawRoundedRect(sbX, y, sbX + 3, y + h, 1f, 0x11FFFFFF);
            GLRectUtils.drawRoundedRect(sbX, sbY, sbX + 3, sbY + sbH, 1f, BORDER_LIT);
        }
    }

    // ── Painel direito: player 3D ────────────────────────────────────────────
    private void drawRightPanel(int mx, int my, float pt) {
        int x = RIGHT_X, y = BODY_Y, w = RIGHT_W, h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);

        drawRect(x, y + 18, x + w, y + 19, BORDER);

        if (mc.thePlayer != null) {
            int entityX = x + w / 2;
            int entityY = y + h - 36;
            int sc      = (int)((h - 60) * 0.35f);

            net.minecraft.entity.EntityLivingBase ent = mc.thePlayer;

            float prevRenderYaw  = ent.renderYawOffset;
            float prevRotYaw     = ent.rotationYaw;
            float prevRotPitch   = ent.rotationPitch;
            float prevYawHead    = ent.rotationYawHead;
            float prevPrevHead   = ent.prevRotationYawHead;
            int prevHurtTime     = ent.hurtTime;

            float headLook = (float)Math.atan((mx - entityX) / 80.0F) * 20f;
            ent.renderYawOffset     = 0f;
            ent.rotationYaw         = 0f;
            ent.rotationPitch       = (entityY - my) * 0.5f;
            ent.rotationYawHead     = headLook;
            ent.prevRotationYawHead = headLook;
            ent.hurtTime            = 0;

            Cosmetic3DRenderer.skipRendering = true;
            Cosmetic3DRenderer.skipNametagRendering = true;
            try {
                GlStateManager.enableColorMaterial();
                GlStateManager.pushMatrix();
                GlStateManager.translate(entityX, entityY, 50f);
                GlStateManager.scale(-sc, sc, sc);
                GlStateManager.rotate(180f, 0f, 0f, 1f);
                GlStateManager.rotate(playerRotation, 0f, 1f, 0f);

                GlStateManager.color(1f, 1f, 1f, 1f);
                RenderHelper.enableStandardItemLighting();

                net.minecraft.client.renderer.entity.RenderManager rm = mc.getRenderManager();
                rm.setPlayerViewY(180f);
                rm.setRenderShadow(false);
                rm.renderEntityWithPosYaw(ent, 0, 0, 0, 0f, 1f);
                rm.setRenderShadow(true);

                GlStateManager.popMatrix();
            } finally {
                Cosmetic3DRenderer.skipNametagRendering = false;
                Cosmetic3DRenderer.skipRendering = false;
            }

            ent.renderYawOffset     = prevRenderYaw;
            ent.rotationYaw         = prevRotYaw;
            ent.rotationPitch       = prevRotPitch;
            ent.rotationYawHead     = prevYawHead;
            ent.prevRotationYawHead = prevPrevHead;
            ent.hurtTime            = prevHurtTime;

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }

        int sliderAreaY = y + h - 22;
        int sx = x + PAD, sw2 = w - PAD * 2;
        int sy = sliderAreaY + (14 - SLIDER_H) / 2;
        int tx = sx + (int)(sliderValue * (sw2 - THUMB_W));

        fontRendererObj.drawString("ROTAÇÃO",
            x + w/2 - fontRendererObj.getStringWidth("ROTAÇÃO")/2,
            sliderAreaY - 10, TEXT_DARK);

        GLRectUtils.drawRoundedRect(sx, sy, sx + sw2, sy + SLIDER_H, 2f, PANEL2);
        GLRectUtils.drawRoundedOutline(sx, sy, sx + sw2, sy + SLIDER_H, 2f, 1f, BORDER);
        GLRectUtils.drawRoundedRect(sx + 1, sy + 1, tx + THUMB_W/2, sy + SLIDER_H - 1, 2f, BORDER_LIT);

        GLRectUtils.drawRoundedRect(tx, sy - 1, tx + THUMB_W, sy + SLIDER_H + 1, 2f, sliderDrag ? BORDER : PANEL2);
        GLRectUtils.drawRoundedOutline(tx, sy - 1, tx + THUMB_W, sy + SLIDER_H + 2, 2f, 1f, sliderDrag ? ACCENT : BORDER_LIT);

        String deg = (int)playerRotation + "°";
        fontRendererObj.drawString(deg,
            x + w - PAD - fontRendererObj.getStringWidth(deg),
            sliderAreaY - 10, TEXT_DARK);
    }

    // ── Mouse click ──────────────────────────────────────────────────────────
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;

        int closeX = WIN_X + WIN_W - 20, closeY = WIN_Y + 4;
        if (mouseX >= closeX && mouseX <= closeX + 16
         && mouseY >= closeY && mouseY <= closeY + 16) {
            mc.displayGuiScreen(parentScreen);
            return;
        }

        // Botão ativar/desativar só funciona se o jogador tem capas
        if (!cosmetics.isEmpty()) {
            int btnY = BODY_Y + BODY_H - 22 - 6;
            if (mouseX >= LEFT_X && mouseX < LEFT_X + LEFT_W
             && mouseY >= btnY && mouseY < btnY + 22) {
                capeEnabled = !capeEnabled;
                if (capeEnabled && lastEquippedId != null)
                    CosmeticManager.getInstance().equipCosmetic(lastEquippedId);
                else if (!capeEnabled) {
                    lastEquippedId = CosmeticManager.getInstance().getEquippedCosmeticId();
                    CosmeticManager.getInstance().unequipCosmetic();
                }
                return;
            }
        }

        int sx = RIGHT_X + PAD, sw2 = RIGHT_W - PAD * 2;
        int sliderAreaY = BODY_Y + BODY_H - 22;
        int sy = sliderAreaY + (14 - SLIDER_H) / 2;
        if (mouseX >= sx && mouseX <= sx + sw2
         && mouseY >= sy - 2 && mouseY <= sy + SLIDER_H + 2) {
            sliderDrag      = true;
            sliderDragX     = mouseX;
            float v = (float)(mouseX - sx - THUMB_W / 2) / (sw2 - THUMB_W);
            sliderValue     = Math.max(0f, Math.min(1f, v));
            sliderDragStart = sliderValue;
            playerRotation  = sliderValue * 360f;
            return;
        }

        // Clique no grid só funciona se tem capas
        if (!cosmetics.isEmpty()) {
            int x = MID_X, y = BODY_Y, h = BODY_H;
            int xPos = x + GAP, yPos = y + GAP - scrollOffset;
            int col  = 0;
            for (Cosmetic c : cosmetics) {
                if (mouseX >= xPos && mouseX < xPos + ITEM_W
                 && mouseY >= yPos && mouseY < yPos + ITEM_H
                 && mouseY >= y    && mouseY < y + h) {
                    CosmeticManager.getInstance().equipCosmetic(c.getId());
                    capeEnabled = true;
                    break;
                }
                xPos += ITEM_W + GAP;
                col++;
                if (col >= COLS) { col = 0; xPos = x + GAP; yPos += ITEM_H + GAP; }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        sliderDrag = false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !cosmetics.isEmpty()) {
            int totalRows = (int)Math.ceil((float)cosmetics.size() / COLS);
            int totalH    = totalRows * (ITEM_H + GAP) + GAP;
            int maxScroll = Math.max(0, totalH - BODY_H);
            scrollOffset  = Math.max(0, Math.min(maxScroll,
                scrollOffset + (wheel > 0 ? -(ITEM_H + GAP) : (ITEM_H + GAP))));
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

}