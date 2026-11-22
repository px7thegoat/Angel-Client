package io.github.angelclient.client.ui.screen.account;

import io.github.angelclient.client.account.AccountManager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class AddAccountScreen extends GuiScreen {

    private static final ResourceLocation MS_ICON = new ResourceLocation("angel_client", "textures/angelclient/microsoft.png");

    private final GuiScreen parent;
    private float hoverMicrosoft, hoverOffline;
    private long hoverFrame;

    private static final int PW = 240;
    private static final int PH = 170;
    private static final int BTN_H = 34;
    private static final int BTN_GAP = 8;
    private static final int ICON_SZ = 22;

    public AddAccountScreen(GuiScreen parent) {
        this.parent = parent;
        hoverFrame = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0x88000000);

        int px = (width - PW) / 2, py = (height - PH) / 2;

        drawGradientRect(px, py, px + PW, py + PH, 0xFF1A1A1A, 0xFF0D0D0D);
        drawRect(px + 1, py + 1, px + PW - 1, py + PH - 1, 0xFF141414);
        drawHorizontalLine(px, px + PW, py, 0xFF333333);
        drawHorizontalLine(px, px + PW, py + PH, 0xFF333333);
        drawVerticalLine(px, py, py + PH, 0xFF333333);
        drawVerticalLine(px + PW, py, py + PH, 0xFF333333);

        String title = "Adicionar Conta";
        fontRendererObj.drawString(title, px + PW / 2 - fontRendererObj.getStringWidth(title) / 2, py + 10, 0xFFFFFFFF);

        long now = System.currentTimeMillis();
        long delta = now - hoverFrame;
        hoverFrame = now;

        int btnW = PW - 24;
        int btnX = px + 12;
        int btnY0 = py + 32;

        boolean maxReached = AccountManager.getInstance().getAccounts().size() >= AccountManager.getInstance().getMaxAccounts();

        boolean hMs = mx >= btnX && mx < btnX + btnW && my >= btnY0 && my < btnY0 + BTN_H;
        boolean hOff = mx >= btnX && mx < btnX + btnW && my >= btnY0 + BTN_H + BTN_GAP && my < btnY0 + BTN_H + BTN_GAP + BTN_H;

        hoverMicrosoft += ((hMs && !maxReached ? 1f : 0f) - hoverMicrosoft) * Math.min(1f, 0.02f * delta);
        hoverOffline += ((hOff && !maxReached ? 1f : 0f) - hoverOffline) * Math.min(1f, 0.02f * delta);

        drawMainButton(btnX, btnY0, btnW, BTN_H, hoverMicrosoft, MS_ICON, "Microsoft", maxReached);
        drawMainButton(btnX, btnY0 + BTN_H + BTN_GAP, btnW, BTN_H, hoverOffline, null, "Offline", maxReached);

        if (maxReached) {
            String warn = "Maximo de 3 contas atingido!";
            int ww = fontRendererObj.getStringWidth(warn);
            fontRendererObj.drawString(warn, px + PW / 2 - ww / 2, btnY0 + (BTN_H + BTN_GAP) * 2 + 2, 0xFFFF4444);
        }

        String close = "ESC para voltar";
        int cw = fontRendererObj.getStringWidth(close);
        fontRendererObj.drawString(close, px + PW / 2 - cw / 2, py + PH - 12, 0xFF666666);
    }

    private void drawMainButton(int x, int y, int w, int h, float hover, ResourceLocation icon, String label, boolean maxReached) {
        float t = maxReached ? 0f : hover;

        GlStateManager.color(1f, 1f, 1f, 1f);
        drawRoundedRect(x, y, w, h, 4, ((int)(90 + 40 * t) << 24) | 0x000000);
        drawRoundedOutline(x, y, w, h, 4, 1.5f, ((int)(60 + 100 * t) << 24) | 0xFFFFFF);

        int textColor = maxReached ? 0xFF666666 : (t > 0.5f ? 0xFFFFFFFF : 0xFFDDDDDD);

        if (icon != null) {
            mc.getTextureManager().bindTexture(icon);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, t > 0.5f ? 1f : 0.8f);
            int iconX = x + 8;
            int iconY = y + (h - ICON_SZ) / 2;
            drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0, ICON_SZ, ICON_SZ, ICON_SZ, ICON_SZ);
            GlStateManager.disableBlend();

            int tw = fontRendererObj.getStringWidth(label);
            fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2f, y + (h - 8) / 2f, 0xFF000000 | textColor);
        } else {
            int tw = fontRendererObj.getStringWidth(label);
            fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2f, y + (h - 8) / 2f, 0xFF000000 | textColor);
        }
    }

    private void drawRoundedRect(float x, float y, float w, float h, float radius, int color) {
        float a=(color>>24&255)/255f,r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(770,771);GL11.glColor4f(r,g,b,a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x+radius,y);GL11.glVertex2f(x+w-radius,y);GL11.glVertex2f(x+w-radius,y+h);GL11.glVertex2f(x+radius,y+h);
        GL11.glVertex2f(x,y+radius);GL11.glVertex2f(x+radius,y+radius);GL11.glVertex2f(x+radius,y+h-radius);GL11.glVertex2f(x,y+h-radius);
        GL11.glVertex2f(x+w-radius,y+radius);GL11.glVertex2f(x+w,y+radius);GL11.glVertex2f(x+w,y+h-radius);GL11.glVertex2f(x+w-radius,y+h-radius);
        GL11.glEnd();
        drawCorner(x+radius,y+radius,radius,180);drawCorner(x+w-radius,y+radius,radius,270);
        drawCorner(x+radius,y+h-radius,radius,90);drawCorner(x+w-radius,y+h-radius,radius,0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawRoundedOutline(float x,float y,float w,float h,float radius,float thickness,int color) {
        float a=(color>>24&255)/255f,r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;
        GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(770,771);
        GL11.glColor4f(r,g,b,a);GL11.glLineWidth(thickness);GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for(int i=0;i<=90;i+=3){float ang=(float)Math.toRadians(180+i);GL11.glVertex2f(x+radius+(float)Math.cos(ang)*radius,y+radius+(float)Math.sin(ang)*radius);}
        for(int i=0;i<=90;i+=3){float ang=(float)Math.toRadians(270+i);GL11.glVertex2f(x+w-radius+(float)Math.cos(ang)*radius,y+radius+(float)Math.sin(ang)*radius);}
        for(int i=0;i<=90;i+=3){float ang=(float)Math.toRadians(i);GL11.glVertex2f(x+w-radius+(float)Math.cos(ang)*radius,y+h-radius+(float)Math.sin(ang)*radius);}
        for(int i=0;i<=90;i+=3){float ang=(float)Math.toRadians(90+i);GL11.glVertex2f(x+radius+(float)Math.cos(ang)*radius,y+h-radius+(float)Math.sin(ang)*radius);}
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_BLEND);
    }

    private void drawCorner(float x,float y,float radius,int startAngle) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);GL11.glVertex2f(x,y);
        for(int i=0;i<=90;i+=3){float ang=(float)Math.toRadians(startAngle+i);GL11.glVertex2f(x+(float)Math.cos(ang)*radius,y+(float)Math.sin(ang)*radius);}
        GL11.glEnd();
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;

        int px = (width - PW) / 2, py = (height - PH) / 2;
        int btnW = PW - 24;
        int btnX = px + 12;
        int btnY0 = py + 32;

        boolean maxReached = AccountManager.getInstance().getAccounts().size() >= AccountManager.getInstance().getMaxAccounts();

        if (!maxReached && mx >= btnX && mx < btnX + btnW && my >= btnY0 && my < btnY0 + 34) {
            return;
        }
        if (!maxReached && mx >= btnX && mx < btnX + btnW && my >= btnY0 + 34 + BTN_GAP && my < btnY0 + 34 + BTN_GAP + 34) {
            mc.displayGuiScreen(new OfflineNicknameScreen(this));
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
