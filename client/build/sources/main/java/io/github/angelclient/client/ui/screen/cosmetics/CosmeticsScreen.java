package io.github.angelclient.client.ui.screen.cosmetics;

import io.github.angelclient.client.cosmetic.Cosmetic;
import io.github.angelclient.client.cosmetic.CosmeticType;
import io.github.angelclient.client.cosmetic.LocalCosmetic;
import io.github.angelclient.client.cosmetic.LocalCosmeticManager;
import io.github.angelclient.client.cosmetic.model.BlockbenchModel;
import io.github.angelclient.client.cosmetic.model.BlockbenchModelLoader;
import io.github.angelclient.client.cosmetic.model.ModelOBJ;
import io.github.angelclient.client.cosmetic.model.ObjModelLoader;
import io.github.angelclient.client.cosmetic.render.Cosmetic3DRenderer;
import io.github.angelclient.client.cosmetic.render.TextureLoader;
import io.github.angelclient.client.cosmetic.wavey.WaveyCapesConfigInstance;
import io.github.angelclient.client.mod.impl.WaveyCapesMod;
import io.github.angelclient.client.util.render.GLRectUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosmeticsScreen extends GuiScreen {

    private static final ResourceLocation BG1 = new ResourceLocation("angel_client", "textures/angelclient/BG/background.png");
    private static final ResourceLocation EXIT_TEX = new ResourceLocation("angel_client", "textures/gui/angel_client_exit_2x.png");

    private static final int ACCENT        = 0xFFFFFFFF;
    private static final int PANEL         = 0xFF141414;
    private static final int PANEL2        = 0xFF1E1E1E;
    private static final int BORDER        = 0xFF2A2A2A;
    private static final int BORDER_LIT    = 0xFF3A3A3A;
    private static final int TEXT          = 0xFFEEEEEE;
    private static final int TEXT_DIM      = 0xFFAAAAAA;
    private static final int TEXT_DARK     = 0xFF666666;
    private static final int ACTIVE_BG     = 0x44005500;
    private static final int ACTIVE_BORDER = 0xFF00FF00;
    private static final int FAV_ACTIVE    = 0xFFFFD700;
    private static final int FAV_INACTIVE  = 0xFF555555;
    private static final int TOG_BG_ON     = 0xFF005500;
    private static final int TOG_BG_OFF    = 0xFF333333;
    private static final int TOG_THUMB_ON  = 0xFF00FF00;
    private static final int TOG_THUMB_OFF = 0xFF999999;
    private static final float RADIUS      = 3f;

    private final GuiScreen parent;

    private String selectedCategory = "All";
    private static final String[] CATEGORIES = {
        "All", "Capas", "Bandanas", "Chap\u00E9us", "Asas", "Itens", "Escudos", "Auras", "Config"
    };

    private int scrollOffset = 0;
    private int sidebarScrollOffset = 0;
    private float playerRotation = 0f;
    private boolean previewDragging = false;
    private int previewDragLastX;

    private String searchQuery = "";
    private boolean typing = false;

    private String notification = "";
    private long notificationTime = 0L;

    private String previewCosmeticId = null;
    private CosmeticType previewCosmeticType = null;

    private final List<Entry> entries = new ArrayList<>();

    private int WIN_W, WIN_H, WIN_X, WIN_Y;
    private int BODY_Y, BODY_H;
    private int SIDEBAR_W, SIDEBAR_BTN_H;
    private int GRID_X, GRID_W, GRID_COLS, GRID_ITEM_SIZE, GRID_CARD_H;
    private int PREVIEW_W, PREVIEW_X;
    private static final int PAD = 6;
    private static final int GAP = 3;
    private static final int TITLE_H = 26;
    private static final int SEARCH_H = 22;

    private static final int CARD_NAME_H = 10;
    private static final int CARD_TOGGLE_H = 8;
    private static final int CARD_EXTRA_H = CARD_NAME_H + CARD_TOGGLE_H + 6;

    private int sidebarCategoryCount;
    private int sidebarVisibleStart = 0;

    private static class Entry {
        final String id;
        final String name;
        final CosmeticType type;
        final Object source;
        Entry(String id, String name, CosmeticType type, Object source) {
            this.id = id; this.name = name; this.type = type; this.source = source;
        }
    }

    public CosmeticsScreen(GuiScreen parent) {
        this.parent = parent;
        rebuild();
    }

    private void rebuild() {
        entries.clear();
        LocalCosmeticManager lcm = LocalCosmeticManager.getInstance();
        String playerName = mc != null ? mc.getSession().getUsername() : "";

        boolean all = matchCategory("All");

        addEntries(all || matchCategory("Capas"), lcm.getCapes(), CosmeticType.CAPE, playerName);
        addEntries(all || matchCategory("Bandanas"), lcm.getBandanas(), CosmeticType.BANDANA, playerName);
        addEntries(all || matchCategory("Chap\u00E9us"), lcm.getHats(), CosmeticType.HAT, playerName);
        addEntries(all || matchCategory("Asas"), lcm.getWings(), CosmeticType.WING, playerName);
        addEntries(all || matchCategory("Itens"), lcm.getItems(), CosmeticType.ITEM, playerName);
        addEntries(all || matchCategory("Escudos"), lcm.getShields(), CosmeticType.SHIELD, playerName);
        addEntries(all || matchCategory("Auras"), lcm.getAuras(), CosmeticType.EFFECT, playerName);
    }

    private void addEntries(boolean condition, Collection<LocalCosmetic> list, CosmeticType type, String playerName) {
        if (!condition) return;
        for (LocalCosmetic c : list) {
            if (passSearch(c) && c.hasAccess(playerName))
                entries.add(new Entry(c.getId(), c.getName(), type, c));
        }
    }

    private boolean matchCategory(String cat) {
        return selectedCategory.equals(cat);
    }

    private boolean passSearch(LocalCosmetic c) {
        return searchQuery.isEmpty() || c.getName().toLowerCase().contains(searchQuery.toLowerCase());
    }

    private void recalc() {
        WIN_W = (int) (width * 0.83);
        WIN_H = (int) (height * 0.88);
        WIN_X = (width - WIN_W) / 2;
        WIN_Y = (height - WIN_H) / 2;
        BODY_Y = WIN_Y + TITLE_H + SEARCH_H;
        BODY_H = WIN_H - TITLE_H - SEARCH_H;

        SIDEBAR_W = 85;
        SIDEBAR_BTN_H = 18;

        PREVIEW_W = 120;
        PREVIEW_X = WIN_X + WIN_W - PREVIEW_W - PAD;

        int availW = PREVIEW_X - (WIN_X + SIDEBAR_W + PAD) - PAD;
        GRID_COLS = Math.max(2, Math.min(8, availW / 60));
        GRID_ITEM_SIZE = Math.max(36, Math.min(68, (availW - GAP * (GRID_COLS - 1)) / GRID_COLS));
        GRID_CARD_H = GRID_ITEM_SIZE + CARD_EXTRA_H;
        GRID_X = WIN_X + SIDEBAR_W + PAD;
        GRID_W = GRID_COLS * (GRID_ITEM_SIZE + GAP);

        int maxVisible = Math.max(1, (BODY_H - 6) / (SIDEBAR_BTN_H + 2));
        if (CATEGORIES.length > maxVisible) {
            sidebarCategoryCount = maxVisible;
        } else {
            sidebarCategoryCount = CATEGORIES.length;
            sidebarVisibleStart = 0;
        }
    }

    @Override
    public void initGui() {
        recalc();
        rebuild();
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        recalc();

        if (mc.theWorld != null) {
            drawRect(0, 0, width, height, 0xCC000000);
        } else {
            mc.getTextureManager().bindTexture(BG1);
            drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
            drawRect(0, 0, width, height, 0xBB000000);
        }

        drawTopBar(mx, my);
        drawSidebar(mx, my);

        if (matchCategory("Config")) {
            drawConfigPanel(mx, my);
        } else {
            drawGrid(mx, my);
            drawPreview(mx, my, pt);
        }

        drawNotification();

        super.drawScreen(mx, my, pt);
    }

    private void drawTopBar(int mx, int my) {
        drawRect(WIN_X, WIN_Y + TITLE_H - 1, WIN_X + WIN_W, WIN_Y + TITLE_H, BORDER);

        ResourceLocation logo = new ResourceLocation("angel_client", "textures/angelclient/logo2.png");
        try {
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(logo);
            drawModalRectWithCustomSizedTexture(WIN_X + PAD, WIN_Y + 4, 0, 0, 18, 18, 18, 18);
            GlStateManager.disableBlend();
        } catch (Exception ignored) {}

        fontRendererObj.drawString("ANGEL CLIENT", WIN_X + PAD + 24, WIN_Y + 6, TEXT_DIM);

        fontRendererObj.drawString("Cosm\u00E9ticos", WIN_X + PAD + 110, WIN_Y + (TITLE_H - 8) / 2, ACCENT);
        fontRendererObj.drawString("(" + selectedCategory + ")", WIN_X + PAD + 110 + fontRendererObj.getStringWidth("Cosméticos") + 8, WIN_Y + (TITLE_H - 8) / 2, TEXT_DIM);

        int closeX = WIN_X + WIN_W - 20, closeY = WIN_Y + 6;
        boolean hClose = mx >= closeX && mx <= closeX + 14 && my >= closeY && my <= closeY + 14;
        if (hClose) {
            GLRectUtils.drawRoundedRect(closeX, closeY, closeX + 14, closeY + 14, RADIUS, 0xFF550000);
            GLRectUtils.drawRoundedOutline(closeX, closeY, closeX + 14, closeY + 14, RADIUS, 1f, 0xFFFF4444);
        }
        try {
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, hClose ? 1f : 0.5f);
            mc.getTextureManager().bindTexture(EXIT_TEX);
            drawModalRectWithCustomSizedTexture(closeX + 2, closeY + 2, 0, 0, 10, 10, 10, 10);
            GlStateManager.disableBlend();
        } catch (Exception ignored) {}

        int searchX = WIN_X + WIN_W - 160;
        int searchY = WIN_Y + TITLE_H + 4;
        drawRect(searchX, searchY, searchX + 150, searchY + SEARCH_H - 8, PANEL2);
        drawRect(searchX + 1, searchY + 1, searchX + 149, searchY + SEARCH_H - 9, BORDER);
        String display = typing ? searchQuery + (System.currentTimeMillis() % 1000 < 500 ? "_" : " ") : (searchQuery.isEmpty() ? "Pesquisar..." : searchQuery);
        fontRendererObj.drawString(display, searchX + 6, searchY + (SEARCH_H - 8 - 8) / 2, searchQuery.isEmpty() && !typing ? TEXT_DARK : TEXT);
    }

    private void drawSidebar(int mx, int my) {
        int x = WIN_X;
        int y = BODY_Y;
        int w = SIDEBAR_W;
        int h = BODY_H;

        GLRectUtils.drawRoundedRect(x + 2, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x + 2, y, x + w, y + h, RADIUS, 1f, BORDER);

        int startY = y + 6;
        int gap = 2;
        int totalItems = CATEGORIES.length;
        int maxVisible = Math.max(1, (h - 6) / (SIDEBAR_BTN_H + gap));

        if (totalItems > maxVisible) {
            sidebarCategoryCount = maxVisible;
            if (sidebarVisibleStart + maxVisible > totalItems) {
                sidebarVisibleStart = totalItems - maxVisible;
            }
            if (sidebarVisibleStart < 0) sidebarVisibleStart = 0;
        } else {
            sidebarCategoryCount = totalItems;
            sidebarVisibleStart = 0;
        }

        for (int i = 0; i < sidebarCategoryCount; i++) {
            int idx = sidebarVisibleStart + i;
            if (idx >= totalItems) break;
            int by = startY + i * (SIDEBAR_BTN_H + gap);
            boolean hover = mx >= x + 4 && mx <= x + w - 4 && my >= by && my <= by + SIDEBAR_BTN_H;
            boolean sel = CATEGORIES[idx].equals(selectedCategory);

            if (sel) {
                GLRectUtils.drawRoundedRect(x + 4, by, x + w - 4, by + SIDEBAR_BTN_H, RADIUS, PANEL2);
                GLRectUtils.drawRoundedOutline(x + 4, by, x + w - 4, by + SIDEBAR_BTN_H, RADIUS, 1f, ACCENT);
            } else if (hover) {
                GLRectUtils.drawRoundedRect(x + 4, by, x + w - 4, by + SIDEBAR_BTN_H, RADIUS, 0xFF1E1E1E);
                GLRectUtils.drawRoundedOutline(x + 4, by, x + w - 4, by + SIDEBAR_BTN_H, RADIUS, 1f, 0xFF3A3A3A);
            }

            fontRendererObj.drawString(CATEGORIES[idx], x + 10, by + (SIDEBAR_BTN_H - 8) / 2, sel ? ACCENT : (hover ? TEXT : TEXT_DIM));
        }

        if (totalItems > maxVisible) {
            int sbH = Math.max(8, (int) ((float) maxVisible / totalItems * h));
            int sbY = y + (int) ((float) sidebarVisibleStart / (totalItems - maxVisible) * (h - sbH));
            GLRectUtils.drawRoundedRect(x + w - 3, y, x + w - 1, y + h, 1f, 0x11FFFFFF);
            GLRectUtils.drawRoundedRect(x + w - 3, sbY, x + w - 1, sbY + sbH, 1f, BORDER_LIT);
        }
    }

    private void drawGrid(int mx, int my) {
        int x = GRID_X;
        int y = BODY_Y;
        int w = GRID_W;
        int h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);

        if (entries.isEmpty()) {
            String msg = "Nenhum cosm\u00E9tico encontrado.";
            fontRendererObj.drawString(msg, x + w / 2 - fontRendererObj.getStringWidth(msg) / 2, y + h / 2 - 4, TEXT_DARK);
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - (y + h)) * scale, w * scale, h * scale);

        int col = 0;
        float drawY = y + GAP;
        String playerName = mc.getSession().getUsername();
        CosmeticType prevType = null;
        Set<String> favorites = LocalCosmeticManager.getInstance().getFavorites();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);

            if (prevType != null && prevType != entry.type) {
                if (col > 0) {
                    drawY += GRID_CARD_H + GAP;
                }
                drawY += 14;
                col = 0;
                int sepY = (int) drawY - scrollOffset;
                drawRect(x + GAP, sepY - 14, x + w - GAP, sepY - 13, BORDER_LIT);
                fontRendererObj.drawString(getSectionName(entry.type), x + GAP + 4, sepY - 24, ACCENT);
            }

            float ix = x + GAP + col * (GRID_ITEM_SIZE + GAP);
            float iy = drawY - scrollOffset;

            boolean hover = mx >= ix && mx < ix + GRID_ITEM_SIZE && my >= iy && my < iy + GRID_CARD_H && my >= y && my < y + h;
            boolean active = isActive(entry, playerName);
            boolean isFav = favorites.contains(entry.id.toLowerCase());
            boolean isPreview = entry.id.equals(previewCosmeticId);

            drawCard(ix, iy, entry, hover, active, isFav, isPreview, mx, my);

            prevType = entry.type;
            col++;
            if (col >= GRID_COLS) { col = 0; drawY += GRID_CARD_H + GAP; }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        int totalH = getTotalGridHeight();
        if (totalH > h) {
            int sbX = x + w - 3;
            int sbH = Math.max(16, (int) ((float) h / totalH * h));
            int sbY = y + (int) ((float) scrollOffset / (totalH - h) * (h - sbH));
            GLRectUtils.drawRoundedRect(sbX, y, sbX + 2, y + h, 1f, 0x11FFFFFF);
            GLRectUtils.drawRoundedRect(sbX, sbY, sbX + 2, sbY + sbH, 1f, BORDER_LIT);
        }
    }

    private void drawCard(float ix, float iy, Entry entry, boolean hover, boolean active, boolean isFav, boolean isPreview, int mx, int my) {
        int bgColor = active ? ACTIVE_BG : (isPreview ? 0x44000055 : (hover ? 0xFF1E1E1E : PANEL2));
        int brdColor = active ? ACTIVE_BORDER : (isPreview ? 0xFF4444FF : (hover ? 0xFF3A3A3A : 0xFF4A4A4A));
        if (isPreview && active) brdColor = 0xFF44FF44;

        GLRectUtils.drawRoundedRect(ix, iy, ix + GRID_ITEM_SIZE, iy + GRID_CARD_H, RADIUS, bgColor);
        GLRectUtils.drawRoundedOutline(ix, iy, ix + GRID_ITEM_SIZE, iy + GRID_CARD_H, RADIUS, 1f, brdColor);

        float starX = ix + GRID_ITEM_SIZE - 18;
        float starY = iy + 2;
        int starColor = isFav ? FAV_ACTIVE : FAV_INACTIVE;
        boolean hStar = mx >= starX - 2 && mx < starX + 14 && my >= starY - 2 && my < starY + 14;
        GlStateManager.pushMatrix();
        GlStateManager.translate((int) starX, (int) starY, 0);
        GlStateManager.scale(1.4f, 1.4f, 1f);
        fontRendererObj.drawString(isFav ? "\u2605" : "\u2606", 0, 0, hStar ? 0xFFFFDD44 : starColor);
        GlStateManager.popMatrix();

        float iconY = iy + 12;
        float iconH = GRID_ITEM_SIZE - 18;
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        drawIcon(ix, iconY, iconH, entry);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();

        String name = entry.name;
        int nw = fontRendererObj.getStringWidth(name);
        int maxNameW = GRID_ITEM_SIZE - 4;
        if (nw > maxNameW) {
            while (fontRendererObj.getStringWidth(name + "..") > maxNameW && name.length() > 1)
                name = name.substring(0, name.length() - 1);
            name += "..";
        }
        float nameY = iconY + iconH + 1;
        fontRendererObj.drawString(name, (int) (ix + GRID_ITEM_SIZE / 2 - fontRendererObj.getStringWidth(name) / 2), (int) nameY, active ? 0xFF00FF00 : TEXT_DIM);

        float toggleX = ix + GRID_ITEM_SIZE / 2 - 11f;
        float toggleY = nameY + CARD_NAME_H + 1;
        float toggleW = 22f;
        float toggleH = 7f;
        boolean hToggle = mx >= toggleX && mx < toggleX + toggleW && my >= toggleY && my < toggleY + toggleH;
        int tBg = active ? TOG_BG_ON : TOG_BG_OFF;
        int tBd = active ? ACTIVE_BORDER : (hToggle ? BORDER_LIT : 0xFF555555);
        int tThumbC = active ? TOG_THUMB_ON : TOG_THUMB_OFF;
        float thumbW = 7f;
        float thumbX = active ? toggleX + toggleW - thumbW : toggleX;
        GLRectUtils.drawRoundedRect(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, 2f, tBg);
        GLRectUtils.drawRoundedOutline(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, 2f, 1f, tBd);
        GLRectUtils.drawRoundedRect(thumbX, toggleY - 1, thumbX + thumbW, toggleY + toggleH + 1, 2f, tThumbC);
    }

    private void drawConfigPanel(int mx, int my) {
        configToggles.clear();

        int x = GRID_X;
        int y = BODY_Y;
        int w = WIN_X + WIN_W - PAD - x;
        int h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);

        int cx = x + PAD;
        int cy = y + PAD + 4;
        int toggleW = 22, toggleH = 8;

        fontRendererObj.drawString("Configura\u00E7\u00F5es", cx, cy, ACCENT);
        cy += 14;

        boolean waveyEnabled = WaveyCapesMod.INSTANCE != null && WaveyCapesMod.enabled;
        boolean windEnabled = WaveyCapesConfigInstance.get().windMode;

        fontRendererObj.drawString("Capa F\u00EDsica (WaveyCapes)", cx, cy + 2, TEXT);
        int tX = x + w - PAD - toggleW;
        int tY = cy;
        int tw = 8;
        int tThumbX = waveyEnabled ? tX + toggleW - tw : tX;
        int tBg = waveyEnabled ? TOG_BG_ON : TOG_BG_OFF;
        int tBd = waveyEnabled ? ACTIVE_BORDER : 0xFF555555;
        int tThumbColor = waveyEnabled ? TOG_THUMB_ON : TOG_THUMB_OFF;
        GLRectUtils.drawRoundedRect(tX, tY, tX + toggleW, tY + toggleH, 2f, tBg);
        GLRectUtils.drawRoundedOutline(tX, tY, tX + toggleW, tY + toggleH, 2f, 1f, tBd);
        GLRectUtils.drawRoundedRect(tThumbX, tY - 1, tThumbX + tw, tY + toggleH + 1, 2f, tThumbColor);
        configToggles.put("wavey", new ConfigToggle(tX, tY, toggleW, toggleH));
        cy += 18;

        fontRendererObj.drawString("Vento na Capa", cx, cy + 2, TEXT_DIM);
        tY = cy;
        int wiThumbX = windEnabled ? tX + toggleW - tw : tX;
        int wiBg = windEnabled ? TOG_BG_ON : TOG_BG_OFF;
        int wiBd = windEnabled ? ACTIVE_BORDER : 0xFF555555;
        int wiThumbColor = windEnabled ? TOG_THUMB_ON : TOG_THUMB_OFF;
        if (waveyEnabled) {
            GLRectUtils.drawRoundedRect(tX, tY, tX + toggleW, tY + toggleH, 2f, wiBg);
            GLRectUtils.drawRoundedOutline(tX, tY, tX + toggleW, tY + toggleH, 2f, 1f, wiBd);
            GLRectUtils.drawRoundedRect(wiThumbX, tY - 1, wiThumbX + tw, tY + toggleH + 1, 2f, wiThumbColor);
        } else {
            GLRectUtils.drawRoundedRect(tX, tY, tX + toggleW, tY + toggleH, 2f, 0xFF1A1A1A);
            GLRectUtils.drawRoundedOutline(tX, tY, tX + toggleW, tY + toggleH, 2f, 1f, 0xFF333333);
            GLRectUtils.drawRoundedRect(tX, tY - 1, tX + tw, tY + toggleH + 1, 2f, 0xFF555555);
        }
        configToggles.put("wind", new ConfigToggle(tX, tY, toggleW, toggleH));
        cy += 18;

        fontRendererObj.drawString("Altura: " + WaveyCapesConfigInstance.get().heightMultiplier, cx, cy + 2, TEXT_DIM);
        int sliderX = x + w - PAD - 60;
        int sliderW = 60;
        int sliderY = cy;
        int sliderH = 6;
        int sliderThumbW = 7;
        float hRatio = (WaveyCapesConfigInstance.get().heightMultiplier - 1) / 15.0F;
        int sThumbX = sliderX + (int) (hRatio * (sliderW - sliderThumbW));
        GLRectUtils.drawRoundedRect(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, 2f, PANEL2);
        GLRectUtils.drawRoundedOutline(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, 2f, 1f, BORDER);
        GLRectUtils.drawRoundedRect(sThumbX, sliderY - 1, sThumbX + sliderThumbW, sliderY + sliderH + 1, 2f, BORDER_LIT);
        configToggles.put("height", new ConfigToggle(sliderX, sliderY, sliderW, sliderH));
        cy += 16;

        cy += 10;
        fontRendererObj.drawString("Perfis", cx, cy, ACCENT);
        cy += 14;

        if (profileNames.isEmpty()) {
            profileNames = LocalCosmeticManager.getInstance().getProfileNames();
        }

        fontRendererObj.drawString("Salvar perfil:", cx, cy + 2, TEXT_DIM);
        int pfInputX = cx + fontRendererObj.getStringWidth("Salvar perfil:") + 10;
        int pfInputY = cy;
        int pfInputW = 120;
        int pfInputH = 14;
        drawRect(pfInputX, pfInputY, pfInputX + pfInputW, pfInputY + pfInputH, PANEL2);
        drawRect(pfInputX + 1, pfInputY + 1, pfInputX + pfInputW - 1, pfInputY + pfInputH - 1, BORDER);
        String pfDisplay = profileTyping ? profileInput + (System.currentTimeMillis() % 1000 < 500 ? "_" : " ") : (profileInput.isEmpty() ? "Nome..." : profileInput);
        fontRendererObj.drawString(pfDisplay, pfInputX + 4, pfInputY + 3, profileInput.isEmpty() && !profileTyping ? TEXT_DARK : TEXT);
        configToggles.put("pf_input", new ConfigToggle(pfInputX, pfInputY, pfInputW, pfInputH));

        int saveBX = pfInputX + pfInputW + 4;
        boolean hSave = mx >= saveBX && mx <= saveBX + 30 && my >= pfInputY && my <= pfInputY + pfInputH;
        GLRectUtils.drawRoundedRect(saveBX, pfInputY, saveBX + 30, pfInputY + pfInputH, 2f, hSave ? 0xFF005500 : PANEL2);
        GLRectUtils.drawRoundedOutline(saveBX, pfInputY, saveBX + 30, pfInputY + pfInputH, 2f, 1f, BORDER_LIT);
        fontRendererObj.drawString("Salvar", saveBX + 5, pfInputY + 3, hSave ? 0xFF00FF00 : TEXT_DIM);
        configToggles.put("pf_save", new ConfigToggle(saveBX, pfInputY, 30, pfInputH));
        cy += pfInputH + 6;

        fontRendererObj.drawString("Carregar perfil:", cx, cy + 2, TEXT_DIM);
        int loadY = cy;
        int loadBtnX = cx + fontRendererObj.getStringWidth("Carregar perfil:") + 10;
        for (int pi = 0; pi < profileNames.size(); pi++) {
            int pBtnX = loadBtnX + pi * 80;
            if (pBtnX + 76 > x + w - PAD) break;
            boolean hP = mx >= pBtnX && mx <= pBtnX + 76 && my >= loadY && my <= loadY + pfInputH;
            boolean isSelected = pi == selectedProfileIndex;
            GLRectUtils.drawRoundedRect(pBtnX, loadY, pBtnX + 76, loadY + pfInputH, 2f, isSelected ? 0xFF002244 : (hP ? 0xFF1E1E1E : PANEL2));
            GLRectUtils.drawRoundedOutline(pBtnX, loadY, pBtnX + 76, loadY + pfInputH, 2f, 1f, isSelected ? 0xFF4444FF : BORDER_LIT);
            String pn = profileNames.get(pi);
            int pnw = fontRendererObj.getStringWidth(pn);
            if (pnw > 72) {
                while (fontRendererObj.getStringWidth(pn + "..") > 72 && pn.length() > 1) pn = pn.substring(0, pn.length() - 1);
                pn += "..";
            }
            fontRendererObj.drawString(pn, pBtnX + 38 - fontRendererObj.getStringWidth(pn)/2, loadY + 3, isSelected ? ACCENT : TEXT_DIM);
            configToggles.put("pf_load_" + pi, new ConfigToggle(pBtnX, loadY, 76, pfInputH));
        }
        cy += pfInputH + 6;

        fontRendererObj.drawString("Excluir perfil:", cx, cy + 2, TEXT_DIM);
        int delY = cy;
        int delBtnX = cx + fontRendererObj.getStringWidth("Excluir perfil:") + 10;
        for (int pi = 0; pi < profileNames.size(); pi++) {
            int dBtnX = delBtnX + pi * 80;
            if (dBtnX + 76 > x + w - PAD) break;
            boolean hD = mx >= dBtnX && mx <= dBtnX + 76 && my >= delY && my <= delY + pfInputH;
            GLRectUtils.drawRoundedRect(dBtnX, delY, dBtnX + 76, delY + pfInputH, 2f, hD ? 0xFF550000 : PANEL2);
            GLRectUtils.drawRoundedOutline(dBtnX, delY, dBtnX + 76, delY + pfInputH, 2f, 1f, 0xFFFF4444);
            String pn = profileNames.get(pi);
            int pnw = fontRendererObj.getStringWidth(pn);
            if (pnw > 68) {
                while (fontRendererObj.getStringWidth(pn + "..") > 68 && pn.length() > 1) pn = pn.substring(0, pn.length() - 1);
                pn += "..";
            }
            fontRendererObj.drawString(pn, dBtnX + 38 - fontRendererObj.getStringWidth(pn)/2, delY + 3, TEXT_DIM);
            configToggles.put("pf_del_" + pi, new ConfigToggle(dBtnX, delY, 76, pfInputH));
        }
    }

    private static class ConfigToggle {
        int x, y, w, h;
        ConfigToggle(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    private final Map<String, ConfigToggle> configToggles = new HashMap<>();
    private String profileInput = "";
    private boolean profileTyping = false;
    private List<String> profileNames = new ArrayList<>();
    private int selectedProfileIndex = -1;

    private void drawIcon(float ix, float iy, float iconH, Entry entry) {
        drawPlaceholderIcon(ix, iy, iconH, entry.name);
    }

    private void drawBandana3DIcon(float ix, float iy, float iconH, LocalCosmetic lc) {
        try {
            ModelOBJ model = ObjModelLoader.loadModel("bandana/bandana.obj");
            if (model.getFaceCount() == 0) {
                drawPlaceholderIcon(ix, iy, iconH, lc.getName());
                return;
            }

            ResourceLocation tex = TextureLoader.loadTexture(lc.getFile());
            if (tex != null) {
                mc.getTextureManager().bindTexture(tex);
            }

            float cx = ix + GRID_ITEM_SIZE / 2;
            float cy = iy + iconH / 2;

            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 2f);
            float s = iconH * 0.018f;
            GlStateManager.scale(s, s, s);
            GlStateManager.rotate(-30f, 1f, 0f, 0f);
            GlStateManager.rotate(45f, 0f, 1f, 0f);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.color(1f, 1f, 1f, 1f);
            model.render();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        } catch (Exception ignored) {
            drawPlaceholderIcon(ix, iy, iconH, lc.getName());
        }
    }

    private void draw3DModelIcon(float ix, float iy, float iconH, LocalCosmetic lc) {
        try {
            String file = lc.getFile();
            if (file == null) {
                drawPlaceholderIcon(ix, iy, iconH, lc.getName());
                return;
            }

            boolean isJson = file.toLowerCase().endsWith(".json");
            boolean loaded = false;

            GlStateManager.pushMatrix();
            float cx = ix + GRID_ITEM_SIZE / 2;
            float cy = iy + iconH / 2;
            GlStateManager.translate(cx, cy, 2f);
            float s = iconH * 0.5f;
            GlStateManager.scale(s, s, s);
            GlStateManager.rotate(-15f, 1f, 0f, 0f);
            GlStateManager.rotate(30f, 0f, 1f, 0f);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);

            ResourceLocation tex = null;
            if (lc.getTexture() != null && !lc.getTexture().isEmpty()) {
                tex = TextureLoader.loadTexture(lc.getTexture());
            }

            if (isJson) {
                BlockbenchModel bm = BlockbenchModelLoader.loadModel(file);
                if (bm.hasElements()) {
                    if (tex != null) mc.getTextureManager().bindTexture(tex);
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    bm.render();
                    loaded = true;
                }
            } else {
                ModelOBJ model = ObjModelLoader.loadModel(file);
                if (model.getFaceCount() > 0) {
                    if (tex != null) mc.getTextureManager().bindTexture(tex);
                    else {
                        String texPath = file.substring(0, file.lastIndexOf('.')) + "/texture.png";
                        ResourceLocation tex2 = TextureLoader.loadTexture(texPath);
                        if (tex2 != null) mc.getTextureManager().bindTexture(tex2);
                    }
                    GlStateManager.color(1f, 1f, 1f, 1f);
                    model.render();
                    loaded = true;
                }
            }

            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();

            if (!loaded) {
                drawPlaceholderIcon(ix, iy, iconH, lc.getName());
            }
        } catch (Exception ignored) {
            drawPlaceholderIcon(ix, iy, iconH, lc.getName());
        }
    }

    private void drawFlatIcon(float ix, float iy, float iconH, ResourceLocation tex) {
        if (tex == null) return;
        try {
            mc.getTextureManager().bindTexture(tex);
            GlStateManager.enableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            int pad = 4;
            int sz = (int) iconH - pad * 2;
            drawModalRectWithCustomSizedTexture((int) ix + pad, (int) iy + pad, 0, 0, sz, sz, sz, sz);
            GlStateManager.disableBlend();
        } catch (Exception ignored) {}
    }

    private void drawPlaceholderIcon(float ix, float iy, float iconH, String name) {
        int cx = (int) (ix + GRID_ITEM_SIZE / 2);
        int cy = (int) (iy + iconH / 2);
        int r = (int) (iconH * 0.35f);
        if (r < 6) r = 6;

        int pad = 4;
        int bgColor = 0xFF2A2A4A;
        drawRect((int) ix + pad, (int) iy + pad, (int) ix + GRID_ITEM_SIZE - pad, (int) iy + (int) iconH - pad, bgColor);
        drawRect((int) ix + pad + 1, (int) iy + pad + 1, (int) ix + GRID_ITEM_SIZE - pad - 1, (int) iy + (int) iconH - pad - 1, 0xFF1C1C3C);

        String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        int letterColor = 0xFFCCCCFF;
        fontRendererObj.drawString(letter, cx - fontRendererObj.getStringWidth(letter) / 2, cy - 4, letterColor);
    }

    private void drawCapeIcon(float ix, float iy, float iconH, LocalCosmetic lc) {
        try {
            String filePath = lc.getTexture();
            if (filePath == null || filePath.isEmpty()) filePath = lc.getFile();
            if (filePath == null) { drawPlaceholderIcon(ix, iy, iconH, lc.getName()); return; }

            ResourceLocation tex = TextureLoader.loadTexture(filePath);
            if (tex == null) tex = lc.getTextureLocation();
            if (tex == null) { drawPlaceholderIcon(ix, iy, iconH, lc.getName()); return; }

            mc.getTextureManager().bindTexture(tex);
            GlStateManager.color(1f, 1f, 1f, 1f);

            int pad = 4;
            int tw = GRID_ITEM_SIZE - pad * 2;
            int th = (int) iconH - 8;
            if (th < 4) th = 4;
            int cx = (int) (ix + GRID_ITEM_SIZE / 2);
            int cy = (int) (iy + iconH / 2 + 2);

            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            GlStateManager.rotate(-20f, 1f, 0f, 0f);
            GlStateManager.rotate(25f, 0f, 1f, 0f);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            drawCapeBack(-tw / 2, -th / 2, tw, th);
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        } catch (Exception ignored) {
            drawPlaceholderIcon(ix, iy, iconH, lc.getName());
        }
    }

    private void drawCapeIcon(float ix, float iy, float iconH, ResourceLocation tex) {
        if (tex == null) return;
        try {
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(tex);
            int pad = 4;
            int tw = GRID_ITEM_SIZE - pad * 2;
            int th = (int) iconH - 8;
            int cx = (int) (ix + GRID_ITEM_SIZE / 2);
            int cy = (int) (iy + iconH / 2 + 2);

            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0);
            GlStateManager.rotate(-20f, 1f, 0f, 0f);
            GlStateManager.rotate(25f, 0f, 1f, 0f);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            drawCapeBack(-tw / 2, -th / 2, tw, th);
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.popMatrix();
        } catch (Exception ignored) {}
    }

    private void drawCapeBack(int dstX, int dstY, int dstW, int dstH) {
        double U1 = 12.0 / 64.0;
        double U2 = 22.0 / 64.0;
        double V1 = 1.0 / 32.0;
        double V2 = 17.0 / 32.0;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(dstX, dstY + dstH, 0).tex(U1, V2).endVertex();
        wr.pos(dstX + dstW, dstY + dstH, 0).tex(U2, V2).endVertex();
        wr.pos(dstX + dstW, dstY, 0).tex(U2, V1).endVertex();
        wr.pos(dstX, dstY, 0).tex(U1, V1).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
    }

    private boolean isActive(Entry e, String p) {
        if (e.source instanceof LocalCosmetic) {
            LocalCosmetic lc = (LocalCosmetic) e.source;
            switch (lc.getType()) {
                case CAPE: return e.id.equals(LocalCosmeticManager.getInstance().getActiveCape(p));
                case BANDANA: return e.id.equals(LocalCosmeticManager.getInstance().getActiveBandana(p));
                case HAT: return e.id.equals(LocalCosmeticManager.getInstance().getActiveHat(p));
                case WING: return e.id.equals(LocalCosmeticManager.getInstance().getActiveWing(p));
                case NAMETAG: return e.id.equals(LocalCosmeticManager.getInstance().getActiveNametag(p));
                case ITEM: return e.id.equals(LocalCosmeticManager.getInstance().getActiveItem(p));
                case SHIELD: return e.id.equals(LocalCosmeticManager.getInstance().getActiveShield(p));
                case EFFECT: return e.id.equals(LocalCosmeticManager.getInstance().getActiveAura(p));
                default: return false;
            }
        }
        return false;
    }

    private static String getSectionName(CosmeticType type) {
        switch (type) {
            case CAPE: return "Capas";
            case BANDANA: return "Bandanas";
            case HAT: return "Chap\u00E9us";
            case WING: return "Asas";
            case NAMETAG: return "Nametags";
            case ITEM: return "Itens";
            case SHIELD: return "Escudos";
            case EFFECT: return "Efeitos";
            default: return type.name();
        }
    }

    private int getTotalGridHeight() {
        int y = GAP;
        int col = 0;
        CosmeticType prevType = null;
        for (Entry e : entries) {
            if (prevType != null && prevType != e.type) {
                if (col > 0) {
                    y += GRID_CARD_H + GAP;
                }
                y += 14;
                col = 0;
            }
            prevType = e.type;
            col++;
            if (col >= GRID_COLS) { col = 0; y += GRID_CARD_H + GAP; }
        }
        if (col > 0) y += GRID_CARD_H + GAP;
        return y;
    }

    private void drawPreview(int mx, int my, float pt) {
        int x = PREVIEW_X;
        int y = BODY_Y;
        int w = PREVIEW_W;
        int h = BODY_H;

        GLRectUtils.drawRoundedRect(x, y, x + w, y + h, RADIUS, PANEL);
        GLRectUtils.drawRoundedOutline(x, y, x + w, y + h, RADIUS, 1f, BORDER);
        drawRect(x, y + 18, x + w, y + 19, BORDER);

        fontRendererObj.drawString("Visualizar", x + w / 2 - fontRendererObj.getStringWidth("Visualizar") / 2, y + 4, TEXT_DARK);

        if (mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - (y + h)) * scale, w * scale, h * scale);

        int entityX = x + w / 2;
        int entityY = y + h - 30;

        float prevRenderYaw = mc.thePlayer.renderYawOffset;
        float prevRotYaw = mc.thePlayer.rotationYaw;
        float prevRotPitch = mc.thePlayer.rotationPitch;
        float prevYawHead = mc.thePlayer.rotationYawHead;
        float prevPrevHead = mc.thePlayer.prevRotationYawHead;
        int prevHurtTime = mc.thePlayer.hurtTime;
        boolean prevHide = mc.thePlayer.isInvisible();

        mc.thePlayer.renderYawOffset = 0f;
        mc.thePlayer.rotationYaw = 0f;
        mc.thePlayer.rotationYawHead = (float) Math.atan((mx - entityX) / 40.0F) * 20.0F;
        mc.thePlayer.rotationPitch = -((float) Math.atan((my - entityY) / 40.0F)) * 10.0F;
        mc.thePlayer.prevRotationYawHead = mc.thePlayer.rotationYawHead;
        mc.thePlayer.prevRotationPitch = mc.thePlayer.rotationPitch;
        mc.thePlayer.hurtTime = 0;
        mc.thePlayer.setInvisible(false);

        if (previewCosmeticId != null && previewCosmeticType != null) {
            LocalCosmeticManager.getInstance().setPreviewOverride(previewCosmeticType, previewCosmeticId);
        }

        boolean prevSkipRender = Cosmetic3DRenderer.skipRendering;
        boolean prevSkipNametag = Cosmetic3DRenderer.skipNametagRendering;
        Cosmetic3DRenderer.skipRendering = false;
        Cosmetic3DRenderer.skipNametagRendering = true;

        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate(entityX, entityY, 50f);
            int sc = (int) ((h - 60) * 0.28f);
            GlStateManager.scale(-sc, sc, sc);
            GlStateManager.rotate(180f, 0f, 0f, 1f);
            GlStateManager.rotate(playerRotation, 0f, 1f, 0f);
            GlStateManager.color(1f, 1f, 1f, 1f);

            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableColorMaterial();
            RenderHelper.enableStandardItemLighting();

            mc.getRenderManager().setPlayerViewY(180f);
            mc.getRenderManager().setRenderShadow(false);
            mc.getRenderManager().renderEntityWithPosYaw(mc.thePlayer, 0, 0, 0, 0f, 1f);
            mc.getRenderManager().setRenderShadow(true);

            GlStateManager.popMatrix();
        } finally {
            Cosmetic3DRenderer.skipNametagRendering = prevSkipNametag;
            Cosmetic3DRenderer.skipRendering = prevSkipRender;
            LocalCosmeticManager.getInstance().clearPreviewOverrides();
        }

        mc.thePlayer.renderYawOffset = prevRenderYaw;
        mc.thePlayer.rotationYaw = prevRotYaw;
        mc.thePlayer.rotationPitch = prevRotPitch;
        mc.thePlayer.rotationYawHead = prevYawHead;
        mc.thePlayer.prevRotationYawHead = prevPrevHead;
        mc.thePlayer.hurtTime = prevHurtTime;
        mc.thePlayer.setInvisible(prevHide);

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawNotification() {
        if (notification.isEmpty()) return;
        long elapsed = System.currentTimeMillis() - notificationTime;
        if (elapsed < 2000) {
            float alpha = Math.min(1.0F, (2000 - elapsed) / 500.0F);
            int color = ((int)(alpha * 255) << 24) | 0xFFFFFF;
            int nw = fontRendererObj.getStringWidth(notification);
            fontRendererObj.drawString(notification, WIN_X + WIN_W / 2 - nw / 2, WIN_Y + WIN_H + 8, color);
        } else {
            notification = "";
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        int closeX = WIN_X + WIN_W - 20, closeY = WIN_Y + 6;
        if (mx >= closeX && mx <= closeX + 14 && my >= closeY && my <= closeY + 14) {
            mc.displayGuiScreen(parent);
            return;
        }

        int searchX = WIN_X + WIN_W - 160;
        int searchY = WIN_Y + TITLE_H + 4;
        if (mx >= searchX && mx <= searchX + 150 && my >= searchY && my <= searchY + SEARCH_H - 8) {
            typing = true;
            return;
        }
        typing = false;

        int startY = BODY_Y + 6;
        int gap = 2;
        for (int i = 0; i < sidebarCategoryCount; i++) {
            int idx = sidebarVisibleStart + i;
            if (idx >= CATEGORIES.length) break;
            int by = startY + i * (SIDEBAR_BTN_H + gap);
            if (mx >= WIN_X + 4 && mx <= WIN_X + SIDEBAR_W - 4 && my >= by && my <= by + SIDEBAR_BTN_H) {
                selectedCategory = CATEGORIES[idx];
                scrollOffset = 0;
                previewCosmeticId = null;
                previewCosmeticType = null;
                rebuild();
                return;
            }
        }

        if (matchCategory("Config")) {
            handleConfigClick(mx, my);
            return;
        }

        if (mx >= PREVIEW_X && mx <= PREVIEW_X + PREVIEW_W && my >= BODY_Y && my <= BODY_Y + BODY_H) {
            previewDragging = true;
            previewDragLastX = mx;
            return;
        }

        String playerName = mc.getSession().getUsername();
        int col = 0;
        float drawY = BODY_Y + GAP;
        CosmeticType prevType = null;
        Set<String> favorites = LocalCosmeticManager.getInstance().getFavorites();

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);

            if (prevType != null && prevType != entry.type) {
                if (col > 0) {
                    drawY += GRID_CARD_H + GAP;
                }
                drawY += 14;
                col = 0;
            }

            float ix = GRID_X + GAP + col * (GRID_ITEM_SIZE + GAP);
            float iy = drawY - scrollOffset;

            if (mx >= ix && mx < ix + GRID_ITEM_SIZE && my >= iy && my < iy + GRID_CARD_H) {

                float starX = ix + GRID_ITEM_SIZE - 18;
                float starY = iy + 2;
                if (mx >= starX - 2 && mx < starX + 14 && my >= starY - 2 && my < starY + 14) {
                    LocalCosmeticManager.getInstance().toggleFavorite(entry.id);
                    notification = (favorites.contains(entry.id.toLowerCase()) ? "Removido dos favoritos: " : "Favoritado: ") + entry.name;
                    notificationTime = System.currentTimeMillis();
                    return;
                }

                float iconY2 = iy + 12;
                float iconH2 = GRID_ITEM_SIZE - 18;
                float nameY2 = iconY2 + iconH2 + 1;
                float toggleX = ix + GRID_ITEM_SIZE / 2 - 11f;
                float toggleY = nameY2 + CARD_NAME_H + 1;
                float toggleW = 22f;
                float toggleH = 7f;
                if (mx >= toggleX && mx < toggleX + toggleW && my >= toggleY && my < toggleY + toggleH) {
                    if (entry.source instanceof LocalCosmetic) {
                        LocalCosmetic lc = (LocalCosmetic) entry.source;
                        boolean currentlyActive = isActive(entry, playerName);
                        String newId = currentlyActive ? null : entry.id;
                        switch (lc.getType()) {
                            case CAPE: LocalCosmeticManager.getInstance().setActiveCape(playerName, newId); break;
                            case BANDANA: LocalCosmeticManager.getInstance().setActiveBandana(playerName, newId); break;
                            case HAT: LocalCosmeticManager.getInstance().setActiveHat(playerName, newId); break;
                            case WING: LocalCosmeticManager.getInstance().setActiveWing(playerName, newId); break;
                            case NAMETAG: LocalCosmeticManager.getInstance().setActiveNametag(playerName, newId); break;
                            case ITEM: LocalCosmeticManager.getInstance().setActiveItem(playerName, newId); break;
                            case SHIELD: LocalCosmeticManager.getInstance().setActiveShield(playerName, newId); break;
                            case EFFECT: LocalCosmeticManager.getInstance().setActiveAura(playerName, newId); break;
                            default: break;
                        }
                        notification = (currentlyActive ? "Removido: " : "Equipado: ") + entry.name;
                        notificationTime = System.currentTimeMillis();
                    }
                    return;
                }

                if (entry.id.equals(previewCosmeticId)) {
                    previewCosmeticId = null;
                    previewCosmeticType = null;
                } else {
                    previewCosmeticId = entry.id;
                    previewCosmeticType = entry.type;
                    notification = "Visualizando: " + entry.name;
                    notificationTime = System.currentTimeMillis();
                }
                return;
            }

            prevType = entry.type;
            col++;
            if (col >= GRID_COLS) { col = 0; drawY += GRID_CARD_H + GAP; }
        }
    }

    private void handleConfigClick(int mx, int my) {
        ConfigToggle waveyT = configToggles.get("wavey");
        if (waveyT != null && mx >= waveyT.x && mx <= waveyT.x + waveyT.w && my >= waveyT.y && my <= waveyT.y + waveyT.h) {
            if (WaveyCapesMod.INSTANCE != null) {
                WaveyCapesMod.INSTANCE.setEnabled(!WaveyCapesMod.enabled);
            }
            return;
        }
        ConfigToggle windT = configToggles.get("wind");
        if (windT != null && WaveyCapesMod.enabled && mx >= windT.x && mx <= windT.x + windT.w && my >= windT.y && my <= windT.y + windT.h) {
            WaveyCapesConfigInstance.get().windMode = !WaveyCapesConfigInstance.get().windMode;
            return;
        }
        ConfigToggle heightT = configToggles.get("height");
        if (heightT != null && mx >= heightT.x && mx <= heightT.x + heightT.w && my >= heightT.y && my <= heightT.y + heightT.h) {
            float v = (float) (mx - heightT.x - 3) / (heightT.w - 7);
            int h = Math.max(1, Math.min(16, (int) (v * 15) + 1));
            WaveyCapesConfigInstance.get().heightMultiplier = h;
            return;
        }

        ConfigToggle pfInputT = configToggles.get("pf_input");
        if (pfInputT != null && mx >= pfInputT.x && mx <= pfInputT.x + pfInputT.w && my >= pfInputT.y && my <= pfInputT.y + pfInputT.h) {
            profileTyping = true;
            return;
        }
        profileTyping = false;

        ConfigToggle pfSaveT = configToggles.get("pf_save");
        if (pfSaveT != null && mx >= pfSaveT.x && mx <= pfSaveT.x + pfSaveT.w && my >= pfSaveT.y && my <= pfSaveT.y + pfSaveT.h) {
            if (!profileInput.isEmpty()) {
                LocalCosmeticManager.getInstance().saveProfile(profileInput);
                profileNames = LocalCosmeticManager.getInstance().getProfileNames();
                notification = "Perfil salvo: " + profileInput;
                notificationTime = System.currentTimeMillis();
                profileInput = "";
            }
            return;
        }

        for (int pi = 0; pi < profileNames.size(); pi++) {
            ConfigToggle loadT = configToggles.get("pf_load_" + pi);
            if (loadT != null && mx >= loadT.x && mx <= loadT.x + loadT.w && my >= loadT.y && my <= loadT.y + loadT.h) {
                if (LocalCosmeticManager.getInstance().loadProfile(profileNames.get(pi))) {
                    selectedProfileIndex = pi;
                    notification = "Perfil carregado: " + profileNames.get(pi);
                    notificationTime = System.currentTimeMillis();
                } else {
                    notification = "Erro ao carregar perfil: " + profileNames.get(pi);
                    notificationTime = System.currentTimeMillis();
                }
                return;
            }
            ConfigToggle delT = configToggles.get("pf_del_" + pi);
            if (delT != null && mx >= delT.x && mx <= delT.x + delT.w && my >= delT.y && my <= delT.y + delT.h) {
                LocalCosmeticManager.getInstance().deleteProfile(profileNames.get(pi));
                profileNames = LocalCosmeticManager.getInstance().getProfileNames();
                if (selectedProfileIndex == pi) selectedProfileIndex = -1;
                notification = "Perfil exclu\u00EDdo: " + profileNames.get(pi);
                notificationTime = System.currentTimeMillis();
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        super.mouseReleased(mx, my, state);
        previewDragging = false;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        super.mouseClickMove(mx, my, btn, time);
        if (btn != 0 || !previewDragging) return;
        int delta = mx - previewDragLastX;
        playerRotation = (playerRotation + delta) % 360f;
        if (playerRotation < 0) playerRotation += 360f;
        previewDragLastX = mx;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;

        boolean overSidebar = mx >= WIN_X && mx <= WIN_X + SIDEBAR_W && my >= BODY_Y && my <= BODY_Y + BODY_H;
        int totalItems = CATEGORIES.length;
        int maxVisible = Math.max(1, (BODY_H - 6) / (SIDEBAR_BTN_H + 2));

        if (overSidebar && totalItems > maxVisible) {
            int step = 1;
            if (wheel > 0) sidebarVisibleStart = Math.max(0, sidebarVisibleStart - step);
            else sidebarVisibleStart = Math.min(totalItems - maxVisible, sidebarVisibleStart + step);
            return;
        }

        if (!entries.isEmpty() && !matchCategory("Config")) {
            int totalH = getTotalGridHeight();
            int maxScroll = Math.max(0, totalH - BODY_H);
            int step = GRID_CARD_H + GAP;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + (wheel > 0 ? -step : step)));
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (typing) {
            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) { typing = false; }
            else if (key == Keyboard.KEY_BACK) {
                if (!searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); rebuild(); scrollOffset = 0; }
            } else if (c >= 32 && c <= 126) { searchQuery += c; rebuild(); scrollOffset = 0; }
            return;
        }
        if (profileTyping) {
            if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN) { profileTyping = false; }
            else if (key == Keyboard.KEY_BACK) {
                if (!profileInput.isEmpty()) { profileInput = profileInput.substring(0, profileInput.length() - 1); }
            } else if (c >= 32 && c <= 126) { profileInput += c; }
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
