package io.github.angelclient.client.ui.screen;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.cosmetic.CosmeticAPI;
import io.github.angelclient.client.util.update.AutoUpdater;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.Display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateScreen extends GuiScreen {

    private static final int BTN_UPDATE = 0;
    private static final int BTN_CLOSE = 1;
    private static final int BTN_RESTART = 2;

    private final String titleText;
    private final String subtitleText;
    private final String remoteVersion;
    private boolean showUpdateButton;
    private boolean showRestartButton;

    private volatile String statusText;
    private volatile int downloadProgress = -1;
    private volatile boolean downloading;
    private volatile boolean done;
    private volatile boolean pendingReinit;
    private volatile boolean pendingTitleUpdate;
    private volatile String pendingTitle;

    public UpdateScreen(String remoteVersion) {
        this.remoteVersion = remoteVersion;
        this.titleText = EnumChatFormatting.YELLOW + "Update " + remoteVersion + " Available!";
        this.subtitleText = "A new version is ready to download.";
        this.showUpdateButton = true;
        this.showRestartButton = false;
    }

    public UpdateScreen(String title, String subtitle) {
        this.remoteVersion = null;
        this.titleText = title;
        this.subtitleText = subtitle;
        this.showUpdateButton = false;
        this.showRestartButton = true;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int cx = width / 2;
        int by = height / 2 + 20;

        if (showUpdateButton) {
            buttonList.add(new GuiButton(BTN_UPDATE, cx - 75, by, 150, 20, EnumChatFormatting.GREEN + "Update to " + remoteVersion));
        }
        if (showRestartButton) {
            buttonList.add(new GuiButton(BTN_RESTART, cx - 75, by, 150, 20, EnumChatFormatting.GOLD + "Restart Now"));
        }
        buttonList.add(new GuiButton(BTN_CLOSE, cx - 75, by + 28, 150, 20, "Close"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (pendingTitleUpdate) {
            pendingTitleUpdate = false;
            Display.setTitle(pendingTitle);
        }
        if (pendingReinit) {
            pendingReinit = false;
            initGui();
        }

        drawRect(0, 0, width, height, 0x90000000);

        int cx = width / 2;
        mc.fontRendererObj.drawString(titleText, cx - mc.fontRendererObj.getStringWidth(titleText) / 2, height / 2 - 30, 0xFFFFFF);
        mc.fontRendererObj.drawString(subtitleText, cx - mc.fontRendererObj.getStringWidth(subtitleText) / 2, height / 2 - 15, 0xAAAAAA);

        if (downloading) {
            String dlText = statusText != null ? statusText : "Downloading...";
            mc.fontRendererObj.drawString(dlText, cx - mc.fontRendererObj.getStringWidth(dlText) / 2, height / 2 + 55, 0x88FF88);

            if (downloadProgress >= 0) {
                int barW = 200;
                int barH = 6;
                int barX = cx - barW / 2;
                int barY = height / 2 + 70;
                drawRect(barX, barY, barX + barW, barY + barH, 0xFF333333);
                int fill = (int) (barW * downloadProgress / 100f);
                drawRect(barX, barY, barX + fill, barY + barH, 0xFF44CC44);
            }
        }

        if (done) {
            String restartText = "Update applied! Restart to use Angel Client " + Client.VERSION;
            mc.fontRendererObj.drawString(restartText, cx - mc.fontRendererObj.getStringWidth(restartText) / 2, height / 2 + 60, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BTN_UPDATE) {
            startDownload();
        } else if (button.id == BTN_RESTART) {
            restartGame();
        } else if (button.id == BTN_CLOSE) {
            mc.displayGuiScreen(null);
        }
    }

    private void startDownload() {
        if (downloading) return;
        downloading = true;
        downloadProgress = 0;

        new Thread(() -> {
            try {
                URL url = new URL(AutoUpdater.getDownloadUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "token " + CosmeticAPI.GITHUB_TOKEN);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                long totalBytes = conn.getContentLengthLong();
                long downloaded = 0;

                File jarFile = new File(UpdateScreen.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (jarFile.isDirectory()) {
                    statusText = "Dev environment detected.";
                    return;
                }

                File tempFile = new File(jarFile.getParent(), "client.jar.update");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        downloaded += len;
                        if (totalBytes > 0) {
                            int pct = (int) ((downloaded * 100) / totalBytes);
                            if (pct != downloadProgress) {
                                downloadProgress = pct;
                                statusText = "Downloading: " + pct + "%";
                            }
                        } else {
                            int pct = (int) (downloaded / 10000) % 100;
                            downloadProgress = pct;
                            statusText = "Downloading... (" + (downloaded / 1024 / 1024) + " MB)";
                        }
                    }
                }

                statusText = "Download complete. Replacing JAR...";

                File backup = new File(jarFile.getParent(), "client.jar.bak");
                if (backup.exists()) backup.delete();

                boolean replaced = false;
                if (jarFile.delete()) {
                    replaced = tempFile.renameTo(jarFile);
                }

                if (!replaced) {
                    try (FileInputStream fis = new FileInputStream(tempFile);
                         FileOutputStream fos = new FileOutputStream(jarFile)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = fis.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                    }
                    tempFile.delete();
                    replaced = jarFile.exists();
                }

                if (replaced) {
                    Client.VERSION = remoteVersion;
                    Client.INSTANCE.markUpdateJustApplied(remoteVersion);
                    pendingTitle = "Angel Client";
                    pendingTitleUpdate = true;
                    statusText = null;
                    downloadProgress = -1;
                    downloading = false;
                    done = true;
                    showRestartButton = true;
                    showUpdateButton = false;
                    pendingReinit = true;
                } else {
                    statusText = EnumChatFormatting.RED + "Failed to replace JAR.";
                }

            } catch (Exception e) {
                statusText = EnumChatFormatting.RED + "Error: " + e.getMessage();
            } finally {
                downloading = false;
            }
        }, "Update-Download").start();
    }

    private void restartGame() {
        mc.shutdown();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
