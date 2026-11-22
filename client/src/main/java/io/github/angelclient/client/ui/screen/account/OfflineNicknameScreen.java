package io.github.angelclient.client.ui.screen.account;

import io.github.angelclient.client.account.Account;
import io.github.angelclient.client.account.AccountManager;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class OfflineNicknameScreen extends GuiScreen {

    private final GuiScreen parent;
    private String nickname = "";
    private boolean typing = true;
    private String error = "";
    private long errorTime;

    public OfflineNicknameScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xCC000000);

        int pw = 320, ph = 140;
        int px = (width - pw) / 2, py = (height - ph) / 2;

        drawGradientRect(px, py, px + pw, py + ph, 0xFF1A1A1A, 0xFF0D0D0D);
        drawRect(px + 1, py + 1, px + pw - 1, py + ph - 1, 0xFF141414);
        drawHorizontalLine(px, px + pw, py, 0xFF333333);
        drawHorizontalLine(px, px + pw, py + ph, 0xFF333333);
        drawVerticalLine(px, py, py + ph, 0xFF333333);
        drawVerticalLine(px + pw, py, py + ph, 0xFF333333);

        fontRendererObj.drawString("Conta Offline", px + pw / 2 - fontRendererObj.getStringWidth("Conta Offline") / 2, py + 14, 0xFFFFFFFF);
        fontRendererObj.drawString("Digite o nickname:", px + 20, py + 40, 0xFFAAAAAA);

        int fieldX = px + 20, fieldY = py + 56;
        int fieldW = pw - 40, fieldH = 22;
        drawRect(fieldX, fieldY, fieldX + fieldW, fieldY + fieldH, 0xFF222222);
        drawRect(fieldX + 1, fieldY + 1, fieldX + fieldW - 1, fieldY + fieldH - 1, 0xFF1A1A1A);

        String display = nickname + (typing && System.currentTimeMillis() % 1000 < 500 ? "_" : " ");
        fontRendererObj.drawString(display, fieldX + 6, fieldY + (fieldH - 8) / 2, nickname.isEmpty() ? 0xFF666666 : 0xFFFFFFFF);

        if (!error.isEmpty() && System.currentTimeMillis() - errorTime < 3000) {
            fontRendererObj.drawString(error, px + pw / 2 - fontRendererObj.getStringWidth(error) / 2, py + ph - 30, 0xFFFF4444);
        }

        int cancelW = 60, createW = 80;
        int btnY = py + ph - 30;
        int cancelX = px + pw / 2 - cancelW - 6;
        int createX = px + pw / 2 + 6;

        boolean hCancel = mx >= cancelX && mx < cancelX + cancelW && my >= btnY && my < btnY + 18;
        boolean hCreate = mx >= createX && mx < createX + createW && my >= btnY && my < btnY + 18;

        drawRect(cancelX, btnY, cancelX + cancelW, btnY + 18, hCancel ? 0xFF333333 : 0xFF222222);
        drawRect(createX, btnY, createX + createW, btnY + 18, hCreate ? 0xFF004400 : 0xFF003300);

        String cancelText = "Cancelar";
        String createText = "Criar Conta";
        fontRendererObj.drawString(cancelText, cancelX + (cancelW - fontRendererObj.getStringWidth(cancelText)) / 2, btnY + 5, 0xFFAAAAAA);
        fontRendererObj.drawString(createText, createX + (createW - fontRendererObj.getStringWidth(createText)) / 2, btnY + 5, 0xFF00FF00);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;

        int pw = 320, ph = 140;
        int px = (width - pw) / 2, py = (height - ph) / 2;

        int cancelW = 60, createW = 80;
        int btnY = py + ph - 30;
        int cancelX = px + pw / 2 - cancelW - 6;
        int createX = px + pw / 2 + 6;

        if (mx >= cancelX && mx < cancelX + cancelW && my >= btnY && my < btnY + 18) {
            mc.displayGuiScreen(parent);
            return;
        }

        if (mx >= createX && mx < createX + createW && my >= btnY && my < btnY + 18) {
            tryCreate();
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (key == 28 || key == 156) {
            tryCreate();
            return;
        }
        if (key == 14 && !nickname.isEmpty()) {
            nickname = nickname.substring(0, nickname.length() - 1);
            return;
        }
        if (nickname.length() < 16 && c >= 32 && c <= 126) {
            nickname += c;
        }
    }

    private void tryCreate() {
        String name = nickname.trim();
        if (name.isEmpty()) {
            error = "Nickname nao pode estar vazio!";
            errorTime = System.currentTimeMillis();
            return;
        }
        if (name.length() < 3) {
            error = "Nickname deve ter pelo menos 3 caracteres!";
            errorTime = System.currentTimeMillis();
            return;
        }
        if (AccountManager.getInstance().getAccounts().size() >= AccountManager.getInstance().getMaxAccounts()) {
            error = "Maximo de 3 contas atingido!";
            errorTime = System.currentTimeMillis();
            return;
        }
        if (AccountManager.isPremiumName(name)) {
            error = "Jogador premium existe com esse nickname! Escolha outro.";
            errorTime = System.currentTimeMillis();
            return;
        }
        String uuid = AccountManager.generateOfflineUuid(name);
        Account account = new Account(Account.Type.OFFLINE, name, uuid);
        AccountManager.getInstance().addAccount(account);
        AccountManager.getInstance().switchTo(account);
        mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
