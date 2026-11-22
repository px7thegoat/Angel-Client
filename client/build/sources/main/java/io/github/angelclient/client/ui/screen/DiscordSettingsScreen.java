package io.github.angelclient.client.ui.screen;

import io.github.angelclient.client.discord.DiscordApiHelper;
import io.github.angelclient.client.discord.DiscordConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class DiscordSettingsScreen extends GuiScreen {

    private static final ResourceLocation DISCORD_ICON = new ResourceLocation("angel_client", "textures/gui/angel_client_discord_1x.png");

    private String mensagem = null;
    private int mensagemCor = 0xFFFFFF;
    private long mensagemTimer = 0;
    private boolean vinculando = false;
    private float vincularHover = 0f;
    private long vincularHoverFrame = 0L;
    private float fecharHover = 0f;
    private long fecharHoverFrame = 0L;
    private float desvincularHover = 0f;
    private long desvincularHoverFrame = 0L;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xBB000000);

        int cx = width / 2;
        int cy = height / 2;

        if (!DiscordConfig.isVinculado()) {
            drawTelaVinculo(cx, cy, mouseX, mouseY);
        } else {
            drawTelaConfig(cx, cy, mouseX, mouseY);
        }

        if (mensagem != null && System.currentTimeMillis() - mensagemTimer < 3000) {
            drawCenteredString(fontRendererObj, mensagem, cx, height - 20, mensagemCor);
        } else {
            mensagem = null;
        }

        // Botão fechar
        long now = System.currentTimeMillis();
        long delta = now - fecharHoverFrame;
        fecharHoverFrame = now;
        int fY = cy + 60;
        boolean fecharHov = hit(mouseX, mouseY, cx - 25, fY, 50, 14);
        fecharHover = fecharHover + ((fecharHov ? 1f : 0f) - fecharHover) * Math.min(1f, 0.015f * delta);

        float ft = fecharHover;
        int fAlpha = 60 + (int)(60 * ft);
        drawRoundedRect(cx - 25, fY, 50, 14, 3, (fAlpha << 24) | 0x000000);
        drawRoundedOutline(cx - 25, fY, 50, 14, 3, 1f, 0x60FFFFFF);
        drawCenteredString(fontRendererObj, "§7Fechar", cx, fY + 3, fecharHov ? 0xFFFFFF : 0xAAAAAA);
    }

    private void drawTelaVinculo(int cx, int cy, int mx, int my) {
        drawCenteredString(fontRendererObj, "§b✎ Conta Discord", cx, cy - 35, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "§7Vincule sua conta Discord", cx, cy - 22, 0xAAAAAA);

        // Botão Vincular (azul)
        int btnW = 160;
        int btnH = 30;
        int btnX = cx - btnW / 2;
        int btnY = cy - 5;

        boolean hov = hit(mx, my, btnX, btnY, btnW, btnH);
        long now = System.currentTimeMillis();
        long delta = now - vincularHoverFrame;
        vincularHoverFrame = now;
        vincularHover = vincularHover + ((hov ? 1f : 0f) - vincularHover) * Math.min(1f, 0.015f * delta);
        float t = vincularHover;

        int btnColor = 0xFF5865F2;
        if (hov) {
            int r = 88 + (int)(20 * t);
            int g = 101 + (int)(20 * t);
            int b = 242 - (int)(10 * t);
            btnColor = (0xFF << 24) | (r << 16) | (g << 8) | b;
        }
        drawRoundedRect(btnX, btnY, btnW, btnH, 5, btnColor);
        drawRoundedOutline(btnX, btnY, btnW, btnH, 5, 1.5f, 0x40FFFFFF);

        // Icone Discord
        mc.getTextureManager().bindTexture(DISCORD_ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        int iconSize = 18;
        int iconX = btnX + 12;
        int iconY = btnY + (btnH - iconSize) / 2;
        drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0, iconSize, (int)(iconSize * 213f / 280f), iconSize, (int)(iconSize * 213f / 280f));
        GlStateManager.disableBlend();

        String texto = "Vincular Discord";
        int textX = iconX + iconSize + 8;
        int textY = btnY + (btnH - fontRendererObj.FONT_HEIGHT) / 2;
        fontRendererObj.drawStringWithShadow(texto, textX, textY, 0xFFFFFFFF);

        if (vinculando) {
            drawCenteredString(fontRendererObj, "§eAguardando login no navegador...", cx, btnY + btnH + 10, 0xFFFF55);
        }
    }

    private void drawTelaConfig(int cx, int cy, int mx, int my) {
        drawCenteredString(fontRendererObj, "§b✎ Conta Discord", cx, cy - 40, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "§7Conta: §fvinculada ✔", cx, cy - 28, 0xFFFFFF);

        // Botão desvincular
        long now = System.currentTimeMillis();
        long delta = now - desvincularHoverFrame;
        desvincularHoverFrame = now;
        boolean desvincHov = hit(mx, my, cx - 50, cy + 10, 100, 18);
        desvincularHover = desvincularHover + ((desvincHov ? 1f : 0f) - desvincularHover) * Math.min(1f, 0.015f * delta);
        float dt = desvincularHover;

        int dAlpha = 60 + (int)(60 * dt);
        drawRoundedRect(cx - 50, cy + 10, 100, 18, 4, (dAlpha << 24) | 0x440000);
        drawRoundedOutline(cx - 50, cy + 10, 100, 18, 4, 1f, desvincHov ? 0xFF664444 : 0x60FF4444);
        drawCenteredString(fontRendererObj, "§cDesvincular", cx, cy + 13, desvincHov ? 0xFF6666 : 0xFF8888);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0) return;

        int cx = width / 2;
        int cy = height / 2;
        int fY = cy + 60;

        if (hit(mouseX, mouseY, cx - 25, fY, 50, 14)) {
            mc.displayGuiScreen(null);
            return;
        }

        if (!DiscordConfig.isVinculado()) {
            int btnW = 160;
            int btnH = 30;
            int btnX = cx - btnW / 2;
            int btnY = cy - 5;
            if (hit(mouseX, mouseY, btnX, btnY, btnW, btnH) && !vinculando) {
                iniciarVinculacao();
                return;
            }
        }

        if (DiscordConfig.isVinculado()) {
            if (hit(mouseX, mouseY, cx - 50, cy + 10, 100, 18)) {
                DiscordConfig.limparVinculo();
                mostrarMensagem("§cConta Discord desvinculada.", 0xFF5555);
            }
        }
    }

    private void iniciarVinculacao() {
        final String nick = mc.getSession().getUsername();
        vinculando = true;
        DiscordApiHelper.getAuthUrlAsync(nick, result -> {
            if (result == null || !result.sucesso) {
                vinculando = false;
                mostrarMensagem("§cErro: " + (result != null ? result.erro : "sem resposta"), 0xFF5555);
                return;
            }
            abrirNavegador(result.url);
            iniciarPolling(nick, result.state);
        });
    }

    private void abrirNavegador(String url) {
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
            if ((boolean) desktopClass.getMethod("isSupported", java.awt.Desktop.Action.class)
                    .invoke(desktop, java.awt.Desktop.Action.BROWSE)) {
                desktopClass.getMethod("browse", java.net.URI.class)
                        .invoke(desktop, new java.net.URI(url));
                return;
            }
        } catch (Exception ignored) {}

        try {
            Runtime.getRuntime().exec(new String[]{"am", "start", "-a", "android.intent.action.VIEW", "-d", url});
            return;
        } catch (Exception ignored) {}

        mostrarMensagem("§eAbra o link: " + url, 0xFFFF55);
    }

    private void iniciarPolling(final String nick, final String state) {
        new Thread(() -> {
            int tentativas = 40;
            while (tentativas-- > 0) {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) { return; }
                String[] result = DiscordApiHelper.fetchDiscordIdFromToken(state);
                if (result != null) {
                    final String discordId = result[1];
                    final String accessToken = result[3];
                    final String discordUsername = result.length > 4 ? result[4] : "";
                    final String discordAvatar = result.length > 5 ? result[5] : "";
                    DiscordConfig.setDiscordId(discordId);
                    DiscordConfig.setDiscordUsername(discordUsername);
                    DiscordConfig.setDiscordAvatar(discordAvatar);

                    if (accessToken != null && !accessToken.isEmpty()) {
                        DiscordConfig.setOAuthToken(accessToken);
                    }

                    DiscordApiHelper.confirmarVinculo(nick, discordId, state);
                    mc.addScheduledTask(() -> {
                        vinculando = false;
                        mc.displayGuiScreen(null);
                    });
                    return;
                }
            }
            mc.addScheduledTask(() -> {
                vinculando = false;
                mostrarMensagem("§cTempo esgotado. Certifique-se de autorizar o Discord no navegador.", 0xFF5555);
            });
        }, "AngelClient-GuiPoll").start();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void mostrarMensagem(final String msg, final int cor) {
        mc.addScheduledTask(() -> {
            mensagem = msg;
            mensagemCor = cor;
            mensagemTimer = System.currentTimeMillis();
        });
    }

    // ── Helpers de render ──

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

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
