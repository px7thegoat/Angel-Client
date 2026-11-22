package io.github.angelclient.client.ui.screen;

import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.discord.DiscordConfig;
import io.github.angelclient.client.ui.screen.DiscordSettingsScreen;
import io.github.angelclient.client.ui.screen.account.AddAccountScreen;
import io.github.angelclient.client.ui.screen.cosmetics.CosmeticsScreen;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;
import io.github.angelclient.client.account.Account;
import io.github.angelclient.client.account.AccountManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Mouse;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.util.video.VideoBackgroundManager;

import java.util.Locale;

import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public class AngelClientMainMenu extends PanoramaBackgroundScreen {

    private static final ResourceLocation LOGO           = new ResourceLocation("angel_client", "textures/angelclient/logo2.png");

    // ICON_CONFIG removido — o botão Options no menu já abre configurações.

    private static final ResourceLocation BACKGROUND = new ResourceLocation("angel_client", "textures/angelclient/BG/background.png");

    private static final ResourceLocation[] SLIDES = {
        new ResourceLocation("angel_client", "textures/angelclient/slide_1.png"),
        new ResourceLocation("angel_client", "textures/angelclient/slide_2.png"),
        new ResourceLocation("angel_client", "textures/angelclient/slide_3.png"),
    };

    private static final float SLIDE_ASPECT = 16f / 9f;

    public AngelClientMainMenu() {
        super(new AngelMenuComponent());
    }

    @Override
    public void initGui() {
        super.initGui();
        VideoBackgroundManager.start();
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        VideoBackgroundManager.stop();
    }

    public static class AngelMenuComponent extends Component {

        private static final float UI_W = 856f;
        private static final float UI_H = 482f;
        private float uiScale = 1f;
        private float offsetX = 0f;
        private float offsetY = 0f;

        private int   currentSlide    = 0;
        private int   prevSlide       = 0;
        private long  lastSlideSwitch = System.currentTimeMillis();
        private float slideTransition = 1f;

        private final float[] hoverAnim = new float[7];
        private final long[]  lastFrame = new long[7];

        // Dimensões botões
        private static final int BTN_W_FULL = 230;
        private static final int BTN_W_HALF = 111;
        private static final int BTN_H      = 22;
        private static final int BTN_GAP    = 6;
        private static final int HALF_GAP   = 8;

        // Account info (top-right)
        private static final int ACC_X = 636;
        private static final int ACC_Y = 10;
        private static final int ACC_W = 210;
        private static final int ACC_H = 28;
        private static final int HEAD_SZ = 20;
        private static final int DISCORD_SZ = 28;
        private static final ResourceLocation DISCORD_ICON = new ResourceLocation("angel_client", "textures/gui/angel_client_discord_1x.png");

        private String playerName;
        private ResourceLocation playerSkin;
        private boolean accountMenuOpen;
        private float accountHover;
        private long accountHoverFrame;

        private boolean discordMenuOpen;
        private ResourceLocation discordAvatar;
        private float discordHover;
        private long discordHoverFrame;
        private long discordMenuHoverFrame;
        private final float[] discordMenuItemHover = new float[2];

        public AngelMenuComponent() {
            // Account info
            Minecraft mc = Minecraft.getMinecraft();
            String name = mc.getSession().getUsername();
            playerName = (name != null && !name.isEmpty()) ? name : "Player";
            accountMenuOpen = false;
            accountHover = 0f;
            accountHoverFrame = System.currentTimeMillis();
            loadPlayerSkin(mc, playerName);
            AccountManager.getInstance().load();

            discordMenuOpen = false;
            discordHover = 0f;
            discordHoverFrame = System.currentTimeMillis();
            discordMenuHoverFrame = System.currentTimeMillis();
            loadDiscordAvatar(mc);
        }

        private void loadPlayerSkin(Minecraft mc, String username) {
            String clean = username.toLowerCase(Locale.ROOT);
            playerSkin = new ResourceLocation("skins/" + clean);
            mc.getTextureManager().loadTexture(playerSkin,
                new ThreadDownloadImageData(
                    null,
                    "https://minotar.net/skin/" + clean,
                    new ResourceLocation("textures/entity/steve.png"),
                    new ImageBufferDownload()
                )
            );
        }

        private void loadDiscordAvatar(Minecraft mc) {
            String discordId = DiscordConfig.getDiscordId();
            String avatarHash = DiscordConfig.getDiscordAvatar();
            if (discordId == null || discordId.isEmpty() || avatarHash == null || avatarHash.isEmpty()) {
                discordAvatar = null;
                return;
            }
            String key = "discord_avatar_" + discordId;
            discordAvatar = new ResourceLocation(key);
            String url = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png?size=64";
            mc.getTextureManager().loadTexture(discordAvatar,
                new ThreadDownloadImageData(null, url,
                    new ResourceLocation("angel_client", "textures/gui/angel_client_discord_1x.png"),
                    null
                )
            );
        }

        private void updateScale() {
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            float sx = sr.getScaledWidth()  / UI_W;
            float sy = sr.getScaledHeight() / UI_H;
            uiScale = Math.min(sx, sy);
            offsetX = (sr.getScaledWidth()  - UI_W * uiScale) / 2f;
            offsetY = (sr.getScaledHeight() - UI_H * uiScale) / 2f;
        }

        @Override
        public void render(ComponentRenderInfo info) {
            Minecraft mc = Minecraft.getMinecraft();
            updateScale();
            ScaledResolution sr = new ScaledResolution(mc);

            drawBackgroundFullScreen(mc, sr);

            int rawMX = (int)((Mouse.getX() * sr.getScaledWidth()  / (float)mc.displayWidth - offsetX) / uiScale);
            int rawMY = (int)((sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / (float)mc.displayHeight - 1 - offsetY) / uiScale);

            GL11.glPushMatrix();
            GL11.glTranslatef(offsetX, offsetY, 0);
            GL11.glScalef(uiScale, uiScale, 1f);
            drawUI(mc, rawMX, rawMY);
            GL11.glPopMatrix();
            super.render(info);
        }

        private void drawBackgroundFullScreen(Minecraft mc, ScaledResolution sr) {
            mc.getTextureManager().bindTexture(BACKGROUND);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GuiScreen.drawModalRectWithCustomSizedTexture(0, 0, 0, 0,
                sr.getScaledWidth(), sr.getScaledHeight(),
                sr.getScaledWidth(), sr.getScaledHeight());
            GlStateManager.disableBlend();
            drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0x85000000);
        }

        private void drawUI(Minecraft mc, int MX, int MY) {
            int W = (int) UI_W;
            int H = (int) UI_H;

            int logoSz = 65;

            // Layout: logo + título + botões
            int totalH = logoSz + 15 + 9
                       + 14
                       + (BTN_H + BTN_GAP) * 4 - BTN_GAP;

            int blockY  = (H - totalH) / 2;
            int centerX = W / 2 - BTN_W_FULL / 2;

            // Logo
            tex(mc, LOGO, W/2 - logoSz/2, blockY, logoSz, logoSz, 1f);

            // Título
            int titleY = blockY + logoSz + 15;
            drawStringCentered(mc, "ANGEL CLIENT", W / 2, titleY, 0xFFFFFFFF);

            // Botões
            int btnY = titleY + 9 + 14;

            drawButton(mc, "Singleplayer", 0, centerX, btnY,                     BTN_W_FULL, BTN_H, MX, MY, false);
            drawButton(mc, "Multiplayer",  1, centerX, btnY + (BTN_H + BTN_GAP), BTN_W_FULL, BTN_H, MX, MY, false);

            // Linha 3: Options | Cosmetics
            int row2Y = btnY + (BTN_H + BTN_GAP) * 2;
            drawButton(mc, "Options",   4, centerX,                         row2Y, BTN_W_HALF, BTN_H, MX, MY, false);
            drawButton(mc, "Cosmetics", 3, centerX + BTN_W_HALF + HALF_GAP, row2Y, BTN_W_HALF, BTN_H, MX, MY, false);

            // Linha 4: Quit Game (full width)
            int row3Y = btnY + (BTN_H + BTN_GAP) * 3;
            drawButton(mc, "Quit Game", 5, centerX, row3Y, BTN_W_FULL, BTN_H, MX, MY, true);

            // Account info (top-right)
            drawAccountInfo(mc, MX, MY);

            // Reload Discord avatar if config changed
            if (DiscordConfig.isVinculado() && (discordAvatar == null)) {
                loadDiscordAvatar(mc);
            } else if (!DiscordConfig.isVinculado()) {
                discordAvatar = null;
            }

            // Discord button
            int dcX = ACC_X + ACC_W + 6;
            drawDiscordButton(mc, dcX, ACC_Y, DISCORD_SZ, DISCORD_SZ, MX, MY);

            // Slides
            drawSlidePanel(mc, W, H);
        }

        private void drawButton(Minecraft mc, String text, int id,
                                 int x, int y, int w, int h,
                                 int mx, int my, boolean danger) {
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            hoverAnim[id] = animateLerp(hoverAnim[id], hov ? 1f : 0f, id);
            float t = hoverAnim[id];

            GlStateManager.color(1f, 1f, 1f, 1f);
            drawRoundedRect(x, y, w, h, 4, ((int)(90 + 40*t) << 24) | 0x000000);

            int borderColor = danger ? 0xFF4444 : 0xFFFFFF;
            drawRoundedOutline(x, y, w, h, 4, 1.5f, ((int)(60 + 100*t) << 24) | borderColor);

            int color = danger
                ? (hov ? 0xFF6666 : 0xBBBBBB)
                : (hov ? 0xFFFFFF : 0xDDDDDD);
            int tw = mc.fontRendererObj.getStringWidth(text);
            int th = mc.fontRendererObj.FONT_HEIGHT;
            mc.fontRendererObj.drawStringWithShadow(text,
                x + (w - tw) / 2f, y + (h - th) / 2f, 0xFF000000 | color);
        }

        private void drawDiscordButton(Minecraft mc, int x, int y, int w, int h,
                                        int mx, int my) {
            boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
            hoverAnim[6] = animateLerp(hoverAnim[6], hov ? 1f : 0f, 6);
            float t = hoverAnim[6];

            boolean vinculado = DiscordConfig.isVinculado()
                && DiscordConfig.getDiscordAvatar() != null
                && !DiscordConfig.getDiscordAvatar().isEmpty();

            GlStateManager.color(1f, 1f, 1f, 1f);
            drawRoundedRect(x, y, w, h, 4, ((int)(90 + 40*t) << 24) | 0x000000);
            drawRoundedOutline(x, y, w, h, 4, 1.5f, ((int)(60 + 100*t) << 24) | 0xFFFFFF);

            if (vinculado && discordAvatar != null) {
                mc.getTextureManager().bindTexture(discordAvatar);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, 1f);
                int pad = 2;
                int area = DISCORD_SZ - pad * 2;
                GuiScreen.drawModalRectWithCustomSizedTexture(x + pad, y + pad, 0, 0, area, area, area, area);
                GlStateManager.disableBlend();
            } else {
                mc.getTextureManager().bindTexture(DISCORD_ICON);
                GlStateManager.enableBlend();
                GlStateManager.color(1f, 1f, 1f, hov ? 1f : 0.8f);
                int pad = 3;
                int area = DISCORD_SZ - pad * 2;
                int iconW = area;
                int iconH = (int)(area * 213f / 280f);
                int iconX = x + (DISCORD_SZ - iconW) / 2;
                int iconY = y + (DISCORD_SZ - iconH) / 2;
                GuiScreen.drawModalRectWithCustomSizedTexture(iconX, iconY, 0, 0,
                    iconW, iconH, iconW, iconH);
                GlStateManager.disableBlend();
            }

            if (vinculado && discordMenuOpen) {
                drawDiscordDropdown(mc, x, mx, my);
            }
        }

        private void drawDiscordDropdown(Minecraft mc, int btnX, int mx, int my) {
            int ddW = 90;
            int itemH = 18;
            int ddX = btnX + DISCORD_SZ / 2 - ddW / 2;
            int ddY = ACC_Y + DISCORD_SZ + 4;

            long now = System.currentTimeMillis();
            long delta = now - discordMenuHoverFrame;
            discordMenuHoverFrame = now;

            // Settings item
            boolean settingsHov = mx >= ddX && mx < ddX + ddW && my >= ddY && my < ddY + itemH;
            discordMenuItemHover[0] = animateLerp(discordMenuItemHover[0], settingsHov ? 1f : 0f, 0);

            // Log out item
            boolean logoutHov = mx >= ddX && mx < ddX + ddW && my >= ddY + itemH && my < ddY + itemH * 2;
            discordMenuItemHover[1] = animateLerp(discordMenuItemHover[1], logoutHov ? 1f : 0f, 0);

            int totalH = itemH * 2;
            int bgAlpha = 200;
            drawRoundedRect(ddX, ddY, ddW, totalH, 4, (bgAlpha << 24) | 0x151515);
            drawRoundedOutline(ddX, ddY, ddW, totalH, 4, 1f, (bgAlpha << 24) | 0x444444);

            // Settings
            float st = discordMenuItemHover[0];
            if (st > 0.01f) drawRect(ddX + 1, ddY, ddX + ddW - 1, ddY + itemH, ((int)(40 * st) << 24) | 0x5865F2);
            int settingsColor = settingsHov ? 0xFFFFFFFF : 0xFFAAAAAA;
            mc.fontRendererObj.drawStringWithShadow("Settings", ddX + 6, ddY + (itemH - 8) / 2f, settingsColor);

            // Divider
            drawRect(ddX + 4, ddY + itemH, ddX + ddW - 4, ddY + itemH + 1, 0xFF333333);

            // Log out
            float lt = discordMenuItemHover[1];
            if (lt > 0.01f) drawRect(ddX + 1, ddY + itemH, ddX + ddW - 1, ddY + itemH * 2, ((int)(40 * lt) << 24) | 0xFF4444);
            int logoutColor = logoutHov ? 0xFF6666 : 0xFF888888;
            mc.fontRendererObj.drawStringWithShadow("Log out", ddX + 6, ddY + itemH + (itemH - 8) / 2f, logoutColor);
        }

        private void drawSlidePanel(Minecraft mc, int W, int H) {
            long now = System.currentTimeMillis();
            if (now - lastSlideSwitch > 4000) {
                prevSlide       = currentSlide;
                currentSlide    = (currentSlide + 1) % SLIDES.length;
                lastSlideSwitch = now;
                slideTransition = 0f;
            }
            if (slideTransition < 1f) slideTransition += 0.02f;

            int sh = 100;
            int sw = (int)(sh * SLIDE_ASPECT);
            int x0 = W - sw - 20;
            int y0 = H - sh - 20;

            if (slideTransition < 1f)
                tex(mc, SLIDES[prevSlide], x0, y0, sw, sh, 1f - slideTransition);
            tex(mc, SLIDES[currentSlide], x0, y0, sw, sh, slideTransition);
            drawRoundedOutline(x0, y0, sw, sh, 4, 1.5f, 0x40FFFFFF);
        }

        private void drawAccountInfo(Minecraft mc, int MX, int MY) {
            boolean hov = MX >= ACC_X && MX < ACC_X + ACC_W && MY >= ACC_Y && MY < ACC_Y + ACC_H;
            long now = System.currentTimeMillis();
            long delta = now - accountHoverFrame;
            accountHoverFrame = now;
            accountHover = accountHover + ((hov ? 1f : 0f) - accountHover) * Math.min(1f, 0.015f * delta);
            float t = accountHover;

            int baseAlpha = 40 + (int)(40 * t);
            int borderAlpha = 50 + (int)(80 * t);
            drawRoundedRect(ACC_X, ACC_Y, ACC_W, ACC_H, 6, (baseAlpha << 24) | 0x000000);
            drawRoundedOutline(ACC_X, ACC_Y, ACC_W, ACC_H, 6, 1.5f, (borderAlpha << 24) | 0xFFFFFF);

            int headY = ACC_Y + (ACC_H - HEAD_SZ) / 2;
            drawPlayerHead(mc, ACC_X + 4, headY, HEAD_SZ);

            int nameX = ACC_X + 4 + HEAD_SZ + 6;
            int nameY = ACC_Y + (ACC_H - mc.fontRendererObj.FONT_HEIGHT) / 2;
            mc.fontRendererObj.drawStringWithShadow(playerName, nameX, nameY, 0xFFFFFFFF);

            mc.fontRendererObj.drawStringWithShadow("\u25BC", ACC_X + ACC_W - 12, nameY, 0xAAAAAA);

            if (accountMenuOpen) {
                drawAccountDropdown(mc, MX, MY);
            }
        }

        private void drawAccountDropdown(Minecraft mc, int MX, int MY) {
            int ddX = ACC_X;
            int ddY = ACC_Y + ACC_H + 2;
            int ddW = ACC_W;

            java.util.List<Account> savedAccounts = AccountManager.getInstance().getAccounts();

            int itemH = 20;
            int totalH = savedAccounts.size() * itemH + 26;

            int alpha = 200;
            drawRoundedRect(ddX, ddY, ddW, totalH, 6, (alpha << 24) | 0x151515);
            drawRoundedOutline(ddX, ddY, ddW, totalH, 6, 1f, (alpha << 24) | 0x444444);

            int yOff = ddY;
            for (Account acc : savedAccounts) {
                boolean hov = MX >= ddX && MX < ddX + ddW && MY >= yOff && MY < yOff + itemH;
                if (hov) drawRect(ddX + 2, yOff, ddX + ddW - 2, yOff + itemH, 0xFF2A2A2A);
                String prefix = acc.getType() == Account.Type.MICROSOFT ? "[MS] " : "";
                mc.fontRendererObj.drawStringWithShadow(prefix + acc.getUsername(), ddX + 6, yOff + (itemH - 8) / 2, hov ? 0xFFFFFFFF : 0xFFAAAAAA);
                yOff += itemH;
            }

            drawRect(ddX + 4, yOff, ddX + ddW - 4, yOff + 1, 0xFF333333);
            yOff += 4;

            boolean addHov = MX >= ddX && MX < ddX + ddW && MY >= yOff && MY < yOff + 22;
            if (addHov) drawRect(ddX + 2, yOff, ddX + ddW - 2, yOff + 22, 0xFF2A2A2A);
            String text = "+ Adicionar Conta";
            int tw = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, ddX + (ddW - tw) / 2, yOff + (22 - 8) / 2, addHov ? 0x888888 : 0x555555);
        }

        private void drawPlayerHead(Minecraft mc, int x, int y, int size) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(playerSkin);
            drawScaledFace(x, y, 8, 8, 8, 8, size, size);
            drawScaledFace(x, y, 40, 8, 8, 8, size, size);
        }

        private void drawScaledFace(int x, int y, float u, float v, int uw, int vh, int w, int h) {
            float su = 1f / 64f;
            float sv = 1f / 64f;
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x, y + h, 0).tex(u * su, (v + vh) * sv).endVertex();
            wr.pos(x + w, y + h, 0).tex((u + uw) * su, (v + vh) * sv).endVertex();
            wr.pos(x + w, y, 0).tex((u + uw) * su, v * sv).endVertex();
            wr.pos(x, y, 0).tex(u * su, v * sv).endVertex();
            tess.draw();
        }

        @Override
        public boolean mouseClicked(ComponentRenderInfo info, int button) {
            // Só processa clique esquerdo
            if (button != 0) return false;

            Minecraft mc = Minecraft.getMinecraft();
            updateScale();
            ScaledResolution sr = new ScaledResolution(mc);
            int MX = (int)((Mouse.getX() * sr.getScaledWidth()  / (float)mc.displayWidth - offsetX) / uiScale);
            int MY = (int)((sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / (float)mc.displayHeight - 1 - offsetY) / uiScale);
            int W  = (int) UI_W;
            int H  = (int) UI_H;

            // Account info (top-right)
            if (hit(MX, MY, ACC_X, ACC_Y, ACC_W, ACC_H)) {
                playClick(mc);
                accountMenuOpen = !accountMenuOpen;
                return true;
            }

            // Discord button
            int dcX = ACC_X + ACC_W + 6;
            boolean vinculado = DiscordConfig.isVinculado()
                && DiscordConfig.getDiscordAvatar() != null
                && !DiscordConfig.getDiscordAvatar().isEmpty();
            if (hit(MX, MY, dcX, ACC_Y, DISCORD_SZ, DISCORD_SZ)) {
                playClick(mc);
                if (vinculado) {
                    discordMenuOpen = !discordMenuOpen;
                } else {
                    mc.displayGuiScreen(new DiscordSettingsScreen());
                }
                return true;
            }

            // Discord dropdown clicks
            if (vinculado && discordMenuOpen) {
                int ddW = 90;
                int itemH = 18;
                int ddX = dcX + DISCORD_SZ / 2 - ddW / 2;
                int ddY = ACC_Y + DISCORD_SZ + 4;

                if (hit(MX, MY, ddX, ddY, ddW, itemH)) {
                    playClick(mc);
                    discordMenuOpen = false;
                    mc.displayGuiScreen(new DiscordSettingsScreen());
                    return true;
                }
                if (hit(MX, MY, ddX, ddY + itemH, ddW, itemH)) {
                    playClick(mc);
                    discordMenuOpen = false;
                    DiscordConfig.limparVinculo();
                    discordAvatar = null;
                    return true;
                }
                discordMenuOpen = false;
            }

            if (accountMenuOpen) {
                int ddX = ACC_X;
                int ddY = ACC_Y + ACC_H + 2;
                int ddW = ACC_W;

                java.util.List<Account> savedAccounts = AccountManager.getInstance().getAccounts();
                int itemH = 20;
                int yOff = ddY;

                for (Account acc : savedAccounts) {
                    if (hit(MX, MY, ddX, yOff, ddW, itemH)) {
                        playClick(mc);
                        AccountManager.getInstance().switchTo(acc);
                        accountMenuOpen = false;
                        playerName = acc.getUsername();
                        loadPlayerSkin(mc, playerName);
                        return true;
                    }
                    yOff += itemH;
                }

                yOff += 5;
                if (hit(MX, MY, ddX, yOff, ddW, 22)) {
                    playClick(mc);
                    mc.displayGuiScreen(new AddAccountScreen(screen));
                    accountMenuOpen = false;
                    return true;
                }
                accountMenuOpen = false;
            }

            // Recalcula posições (espelho do drawUI)
            int logoSz  = 65;
            int totalH  = logoSz + 15 + 9 + 14
                        + (BTN_H + BTN_GAP) * 4 - BTN_GAP;
            int blockY  = (H - totalH) / 2;
            int centerX = W / 2 - BTN_W_FULL / 2;
            int btnY    = blockY + logoSz + 15 + 9 + 14;

            if (hit(MX, MY, centerX, btnY, BTN_W_FULL, BTN_H)) {
                playClick(mc); mc.displayGuiScreen(new GuiSelectWorld(screen));
                return true;
            }
            if (hit(MX, MY, centerX, btnY + (BTN_H + BTN_GAP), BTN_W_FULL, BTN_H)) {
                playClick(mc); mc.displayGuiScreen(new GuiMultiplayer(screen));
                return true;
            }

            int row2Y = btnY + (BTN_H + BTN_GAP) * 2;
            if (hit(MX, MY, centerX, row2Y, BTN_W_HALF, BTN_H)) {
                playClick(mc); mc.displayGuiScreen(new GuiOptions(screen, mc.gameSettings));
                return true;
            }
            if (hit(MX, MY, centerX + BTN_W_HALF + HALF_GAP, row2Y, BTN_W_HALF, BTN_H)) {
                playClick(mc); mc.displayGuiScreen(new CosmeticsScreen(screen));
                return true;
            }

            int row3Y = btnY + (BTN_H + BTN_GAP) * 3;
            if (hit(MX, MY, centerX, row3Y, BTN_W_FULL, BTN_H)) {
                playClick(mc); mc.shutdown();
                return true;
            }

            // Clique em área vazia NÃO faz nada (comportamento de reset removido)
            return true;
        }

        // Helpers
        private void drawStringCentered(Minecraft mc, String text, int cx, int cy, int color) {
            int tw = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, cx - tw / 2f, cy, color);
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

        private float animateLerp(float cur,float target,int id) {
            long now=System.currentTimeMillis(),delta=now-lastFrame[id];lastFrame[id]=now;
            return cur+(target-cur)*Math.min(1f,0.015f*delta);
        }

        private void tex(Minecraft mc,ResourceLocation res,int x,int y,int w,int h,float alpha) {
            mc.getTextureManager().bindTexture(res);
            GlStateManager.enableBlend();GlStateManager.color(1f,1f,1f,alpha);
            GuiScreen.drawModalRectWithCustomSizedTexture(x,y,0,0,w,h,w,h);
            GlStateManager.disableBlend();
        }

        private void drawRect(int l,int t,int r,int b,int color){GuiScreen.drawRect(l,t,r,b,color);}
        private boolean hit(int mx,int my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
        private void playClick(Minecraft mc){mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"),1.0F));}
    }
}
