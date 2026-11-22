package io.github.angelclient.client.mushstats;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;

public class GuiPlayerStats extends GuiScreen {

    private int startX, startY, endY;

    private List<String> playersToFetch;
    private int totalPlayersToFetch;
    private int playersPending;

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private static final int BACKGROUND_COLOR = Integer.MIN_VALUE;
    private static final int BORDER_COLOR = 1073741824;
    private static final int HEADER_COLOR = 1610612736;
    private static final int ALTERNATE_ROW_COLOR = 271790899;
    private static final int SCROLLBAR_BG = 1078215748;
    private static final int SCROLLBAR_HANDLE = -2134061876;

    private final List<PlayerStats> statsList = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, PlayerStats> cachedStats = new ConcurrentHashMap<>();
    private final Map<String, Boolean> fetchedPlayers = new ConcurrentHashMap<>();

    private boolean loading = true;
    private int scrollOffset = 0;
    
    // Animação de carregamento
    private int dotCount = 0;
    private long lastDotTime = 0;

    private boolean rateLimitHit = false;
    private int playersProcessed = 0;

    private int hoveredRow = -1;
    private int clickedMenuRow = -1;
    private int hoveredLevelRow = -1;

    public GuiPlayerStats(List<String> usernames) {
        this.playersToFetch = usernames;
        this.totalPlayersToFetch = usernames.size();
        this.playersPending = usernames.size();
        fetchPlayerStats(usernames, false);
    }

    public void updatePlayerList(List<String> newPlayerNames) {
        synchronized (statsList) {
            statsList.removeIf(stat -> !newPlayerNames.contains(stat.getUsername()));

            for (String username : newPlayerNames) {
                if (!containsStats(username)) {
                    if (cachedStats.containsKey(username)) {
                        statsList.add(cachedStats.get(username));
                    } else {
                        fetchPlayerStats(Collections.singletonList(username), false);
                    }
                }
            }
            sortStatsList();
        }
    }

    private boolean containsStats(String username) {
        synchronized (statsList) {
            for (PlayerStats s : statsList) {
                if (s.getUsername().equals(username)) return true;
            }
        }
        return false;
    }

    private void reloadPlayerStats(final String username) {
        MushApiClient.removeCachedPlayer(username);
        cachedStats.remove(username);

        synchronized (statsList) {
            statsList.removeIf(s -> s.getUsername().equals(username));
        }

        fetchedPlayers.remove(username);

        MushApiClient.getPlayerStatsAsync(username, new PlayerStatsCallback() {
            @Override
            public void onResult(PlayerStats stats) {
                if (stats.isDataAvailable()) {
                    cachedStats.put(stats.getUsername(), stats);

                    synchronized (statsList) {
                        statsList.add(stats);
                        sortStatsList();
                    }

                    sendChat("§aStats de §f" + username + "§a atualizadas!");
                }
                fetchedPlayers.put(username, true);
            }

            @Override
            public void onError(String u, String error) {
                if (error.equals("RATE_LIMIT")) {
                    rateLimitHit = true;
                    sendChat("§cLimite de requisições atingido!");
                } else {
                    sendChat("§cErro ao recarregar stats de §f" + u);
                }
                fetchedPlayers.put(u, true);
            }
        }, true);

        sendChat("§aRecarregando stats de §f" + username + "§a...");
    }

    private void fetchPlayerStats(List<String> usernames, boolean forceRefresh) {
        if (usernames.isEmpty()) {
            loading = false;
            return;
        }

        for (final String username : usernames) {
            if (!forceRefresh && cachedStats.containsKey(username)) {
                if (!containsStats(username)) {
                    synchronized (statsList) {
                        statsList.add(cachedStats.get(username));
                        sortStatsList();
                    }
                }
                playersProcessed++;
                continue;
            }

            if (!fetchedPlayers.containsKey(username)) {
                fetchedPlayers.put(username, false);

                MushApiClient.getPlayerStatsAsync(username, new PlayerStatsCallback() {
                    @Override
                    public void onResult(PlayerStats stats) {
                        if (stats.isDataAvailable()) {
                            cachedStats.put(stats.getUsername(), stats);

                            synchronized (statsList) {
                                if (!containsStats(stats.getUsername())) {
                                    statsList.add(stats);
                                    sortStatsList();
                                }
                            }
                        }
                        fetchedPlayers.put(username, true);
                        checkLoading();
                    }

                    @Override
                    public void onError(String u, String error) {
                        if (error.equals("RATE_LIMIT")) {
                            rateLimitHit = true;
                        }
                        fetchedPlayers.put(u, true);
                        checkLoading();
                    }
                }, forceRefresh);
            }
        }
    }

    private synchronized void checkLoading() {
        int done = 0;
        int pending = 0;

        for (Boolean b : fetchedPlayers.values()) {
            if (b) done++;
            else pending++;
        }

        playersProcessed = done;
        playersPending = pending;

        if (done >= totalPlayersToFetch) {
            loading = false;
            synchronized (statsList) {
                sortStatsList();
            }
        }
    }

    private void sortStatsList() {
        statsList.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
    }

    @Override
    public void initGui() {
        startX = (width - 360) / 2;
        startY = ((height - (16 * 12)) / 2) - 15;
        endY = startY + (16 * 12);
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, BACKGROUND_COLOR);
        drawRect(startX - 2, startY - 2, startX + 360 + 2, endY + 2, BORDER_COLOR);
        drawRect(startX, startY, startX + 360, endY, BACKGROUND_COLOR);

        int statsListSize;
        synchronized (statsList) {
            statsListSize = statsList.size();
        }

        hoveredRow = -1;
        hoveredLevelRow = -1;

        String title = EnumChatFormatting.AQUA + "Estatísticas de Jogadores (" + statsListSize + "/" + totalPlayersToFetch + ")";
        fontRendererObj.drawStringWithShadow(title, startX + ((360 - fontRendererObj.getStringWidth(title)) / 2), startY - 10, -1);

        if (rateLimitHit) {
            String warningLine1 = EnumChatFormatting.RED + "⚠ Limite de requisições atingido!";
            String warningLine2 = EnumChatFormatting.YELLOW + "Aguarde alguns minutos.";
            int warning1X = startX + ((360 - fontRendererObj.getStringWidth(warningLine1)) / 2);
            int warning2X = startX + ((360 - fontRendererObj.getStringWidth(warningLine2)) / 2);
            fontRendererObj.drawStringWithShadow(warningLine1, warning1X, startY - 24, -1);
            fontRendererObj.drawStringWithShadow(warningLine2, warning2X, startY - 34, -1);
        }

        int headX = startX + 2;
        int levelNicknameX = headX + 16 + 4;
        int wsX = startX + 180;
        int fkdrX = startX + 210;
        int wlrX = startX + 245;
        int winsX = startX + 280;
        int lossesX = startX + 315;
        int contentY = startY + 16;

        drawRect(startX, startY, startX + 360, startY + 16, HEADER_COLOR);
        fontRendererObj.drawStringWithShadow("JOGADOR", levelNicknameX, startY + 3, 16777215);
        fontRendererObj.drawStringWithShadow("WS", wsX, startY + 3, 16777215);
        fontRendererObj.drawStringWithShadow("FKDR", fkdrX, startY + 3, 16777215);
        fontRendererObj.drawStringWithShadow("WLR", wlrX, startY + 3, 16777215);
        fontRendererObj.drawStringWithShadow("WINS", winsX, startY + 3, 16777215);
        fontRendererObj.drawStringWithShadow("LOSSES", lossesX, startY + 3, 16777215);

        if (loading && statsListSize == 0) {
            drawLoadingSkeleton(headX, levelNicknameX, wsX, fkdrX, wlrX, winsX, lossesX, contentY);
        } else if (statsListSize == 0 && totalPlayersToFetch > 0) {
            String errorText = EnumChatFormatting.RED + "Não foi possível buscar as estatísticas.";
            int errorX = startX + ((360 - fontRendererObj.getStringWidth(errorText)) / 2);
            int errorY = (startY + 96) - (fontRendererObj.FONT_HEIGHT / 2);
            fontRendererObj.drawStringWithShadow(errorText, errorX, errorY, -1);
        } else {
            int maxOffset = Math.max(0, statsListSize - 12);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));

            synchronized (statsList) {
                for (int i = 0; i < 12; i++) {
                    int listIndex = i + scrollOffset;
                    int rowY = contentY + (i * 16);

                    if (listIndex < statsList.size() && rowY + 16 <= endY) {
                        PlayerStats stats = statsList.get(listIndex);

                        if (listIndex % 2 == 1) {
                            drawRect(startX, rowY, startX + 360, rowY + 16, ALTERNATE_ROW_COLOR);
                        }

                        int menuButtonX = (startX + 360) - 15;
                        int menuButtonY = rowY + 4;
                        boolean isHoveringMenu = mouseX >= menuButtonX && mouseX <= menuButtonX + 10 && mouseY >= menuButtonY && mouseY <= menuButtonY + 8;
                        if (isHoveringMenu) hoveredRow = listIndex;

                        if (stats.isDataAvailable()) {
                            // Renderiza a cabeça do jogador
                            PlayerHeadRenderer.drawPlayerHead(stats.getUsername(), headX, rowY + 1, 14);
                            LevelManager.drawLevelTag(stats.getLevel(), levelNicknameX, rowY + 3, stats.getLevelColorInt());
                            
                            String username = EnumChatFormatting.RESET + stats.getUsername();
                            int levelTextWidth = LevelManager.getLevelTagWidth(stats.getLevel());
                            
                            boolean isHoveringLevel = mouseX >= levelNicknameX && mouseX <= ((levelNicknameX + levelTextWidth) + 4) + fontRendererObj.getStringWidth(username) && mouseY >= rowY + 3 && mouseY <= rowY + 13;
                            if (isHoveringLevel) hoveredLevelRow = listIndex;

                            int maxNameWidth = wsX - ((levelNicknameX + levelTextWidth) + 4);
                            String displayName = username;
                            if (fontRendererObj.getStringWidth(username) > maxNameWidth) {
                                displayName = trimTextToWidth(username, maxNameWidth);
                            }

                            fontRendererObj.drawStringWithShadow(displayName, levelNicknameX + levelTextWidth + 2, rowY + 3, 16777215);

                            String wsText = String.valueOf(stats.getWinstreak());
                            fontRendererObj.drawStringWithShadow(wsText, wsX, rowY + 3, getColoredWS(stats.getWinstreak()));

                            String fkdrString = DECIMAL_FORMAT.format(stats.getFKDRValue());
                            fontRendererObj.drawStringWithShadow(fkdrString, fkdrX, rowY + 3, getColoredFKDR(stats.getFKDRValue()));

                            String wlrString = DECIMAL_FORMAT.format(stats.getWLRValue());
                            fontRendererObj.drawStringWithShadow(wlrString, wlrX, rowY + 3, getColoredWLR(stats.getWLRValue()));

                            fontRendererObj.drawStringWithShadow(String.valueOf(stats.getWins()), winsX, rowY + 3, 5635925);
                            fontRendererObj.drawStringWithShadow(String.valueOf(stats.getLosses()), lossesX, rowY + 3, 16733525);

                            int menuColor = isHoveringMenu ? -1 : -7829368;
                            fontRendererObj.drawString("⋮", menuButtonX, rowY + 3, menuColor);
                        } else {
                            String errorMsg = EnumChatFormatting.RED + "ERRO: " + stats.getUsername();
                            int maxErrorWidth = wsX - levelNicknameX;
                            String displayError = trimTextToWidth(errorMsg, maxErrorWidth);
                            fontRendererObj.drawStringWithShadow(displayError, levelNicknameX, rowY + 3, 16777215);
                        }
                    } else if (listIndex < totalPlayersToFetch && listIndex % 2 == 1) {
                        drawRect(startX, rowY, startX + 360, rowY + 16, ALTERNATE_ROW_COLOR);
                    }
                }
            }
            drawScrollbar(statsListSize, 12);
        }

        if (hoveredRow != -1) {
            synchronized (statsList) {
                if (hoveredRow >= 0 && hoveredRow < statsList.size()) {
                    drawMenu(statsList.get(hoveredRow), mouseX, mouseY);
                }
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawMenu(PlayerStats stats, int mouseX, int mouseY) {
        int menuX = (mouseX - 100) - 5;
        int menuY = mouseY - 5;
        
        if (menuX < startX) {
            menuX = mouseX + 5;
        }
        
        // Correção do erro grave de sintaxe que havia aqui
        if (menuY + 45 > endY) {
            menuY = endY - 45;
        }
        
        drawRect(menuX, menuY, menuX + 100, menuY + 45, -1442840576);
        clickedMenuRow = -1;
        
        if (mouseX >= menuX && mouseX <= menuX + 100) {
            if (mouseY >= menuY && mouseY < menuY + 16) {
                drawRect(menuX, menuY, menuX + 100, menuY + 16, SCROLLBAR_BG);
                clickedMenuRow = 0;
            } else if (mouseY >= menuY + 16 && mouseY < menuY + 32) {
                drawRect(menuX, menuY + 16, menuX + 100, menuY + 32, SCROLLBAR_BG);
                clickedMenuRow = 1;
            } else if (mouseY >= menuY + 32 && mouseY < menuY + 48) {
                drawRect(menuX, menuY + 32, menuX + 100, menuY + 48, SCROLLBAR_BG);
                clickedMenuRow = 2;
            }
        }
        
        fontRendererObj.drawString("Reportar", menuX + 4, menuY + 4, -43691);
        fontRendererObj.drawString("Recarregar Stats", menuX + 4, menuY + 18, -11141121);
        fontRendererObj.drawString("Ver Perfil", menuX + 4, menuY + 32, -1);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton); // Importante para manter cliques no Minecraft

        PlayerStats stats = null;
        if (hoveredRow != -1 && clickedMenuRow != -1 && mouseButton == 0) {
            synchronized (statsList) {
                if (hoveredRow < statsList.size()) {
                    stats = statsList.get(hoveredRow);
                }
            }
            if (stats != null) {
                handleMenuClick(stats, mouseX, mouseY, mouseX - 5, mouseY - 5);
            }
        }
    }

    private void handleMenuClick(PlayerStats stats, int mouseX, int mouseY, int menuX, int menuY) {
        if (clickedMenuRow == 0) { // Reportar
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage("/rp " + stats.getUsername() + " hack");
            }
            mc.displayGuiScreen(null);
        } else if (clickedMenuRow == 1) { // Recarregar
            reloadPlayerStats(stats.getUsername());
        } else if (clickedMenuRow == 2) { // Perfil
            if (mc.thePlayer != null) {
                mc.thePlayer.sendChatMessage("/profile " + stats.getUsername());
            }
            mc.displayGuiScreen(null);
        }
    }

    private void drawLoadingSkeleton(int headX, int levelNicknameX, int wsX, int fkdrX, int wlrX, int winsX, int lossesX, int contentY) {
        long time = System.currentTimeMillis();
        float shimmerOffset = (time % 2000) / 2000.0f;

        for (int i = 0; i < 12; i++) {
            int rowY = contentY + (i * 16);
            if (rowY + 16 > endY) break;

            if (i % 2 == 1) {
                drawRect(startX, rowY, startX + 360, rowY + 16, ALTERNATE_ROW_COLOR);
            }

            float rowProgress = (shimmerOffset + (i * 0.08f)) % 1.0f;
            int shimmerAlpha = (int) (30.0d + (Math.sin(rowProgress * Math.PI) * 40.0d));
            int shimmerColor = (shimmerAlpha << 24) | 16777215;

            drawRect(headX, rowY + 2, headX + 14, rowY + 14, shimmerColor);
            drawRect(headX + 1, rowY + 3, headX + 13, rowY + 13, 536870912);
            drawRoundedRect(levelNicknameX, rowY + 4, levelNicknameX + 28, rowY + 12, shimmerColor);

            int[] nameWidths = {80, 95, 70, 88, 75};
            int nameWidth = nameWidths[i % nameWidths.length];

            drawRoundedRect(levelNicknameX + 32, rowY + 4, levelNicknameX + 32 + nameWidth, rowY + 12, shimmerColor);
            drawRoundedRect(wsX, rowY + 4, wsX + 20, rowY + 12, shimmerColor);
            drawRoundedRect(fkdrX, rowY + 4, fkdrX + 28, rowY + 12, shimmerColor);
            drawRoundedRect(wlrX, rowY + 4, wlrX + 28, rowY + 12, shimmerColor);
            drawRoundedRect(winsX, rowY + 4, winsX + 25, rowY + 12, shimmerColor);
            drawRoundedRect(lossesX, rowY + 4, lossesX + 25, rowY + 12, shimmerColor);
        }

        updateLoadingDots();
        String message = EnumChatFormatting.GRAY + "Buscando estatísticas" + getLoadingDots();
        int messageX = startX + ((360 - fontRendererObj.getStringWidth(message)) / 2);
        int messageY = (startY + 96) - (fontRendererObj.FONT_HEIGHT / 2);
        
        int textBGWidth = fontRendererObj.getStringWidth(message) + 10;
        int textBGX = startX + ((360 - textBGWidth) / 2);
        
        drawRect(textBGX, messageY - 2, textBGX + textBGWidth, messageY + fontRendererObj.FONT_HEIGHT + 2, BACKGROUND_COLOR);
        fontRendererObj.drawStringWithShadow(message, messageX, messageY, -1);
    }

    private void drawRoundedRect(int x1, int y1, int x2, int y2, int color) {
        drawRect(x1 + 1, y1, x2 - 1, y2, color);
        drawRect(x1, y1 + 1, x2, y2 - 1, color);
    }

    private void updateLoadingDots() {
        if (System.currentTimeMillis() - lastDotTime > 500) {
            dotCount = (dotCount + 1) % 4;
            lastDotTime = System.currentTimeMillis();
        }
    }

    private String getLoadingDots() {
        switch (dotCount) {
            case 1: return ".";
            case 2: return "..";
            case 3: return "...";
            default: return "";
        }
    }

    private void sendChat(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }

    private String trimTextToWidth(String text, int maxWidth) {
        if (fontRendererObj.getStringWidth(text) <= maxWidth) return text;
        String trimmed = text + "...";
        while (fontRendererObj.getStringWidth(trimmed) > maxWidth && trimmed.length() > 3) {
            trimmed = trimmed.substring(0, trimmed.length() - 4) + "...";
        }
        return trimmed;
    }

    private void drawScrollbar(int totalLines, int visibleLines) {
        if (totalLines > visibleLines) {
            int scrollbarHeight = endY - startY;
            int x = ((startX + 360) - 4) - 1;
            int y = startY + 1;
            float visibleRatio = (float) visibleLines / totalLines;
            int handleHeight = Math.max(16, (int) (scrollbarHeight * visibleRatio));
            int scrollableArea = scrollbarHeight - handleHeight;
            int maxOffset = totalLines - visibleLines;
            float scrollRatio = (float) scrollOffset / maxOffset;
            int handleY = y + ((int) (scrollableArea * scrollRatio));
            
            drawRect(x, y, x + 4, y + scrollbarHeight, SCROLLBAR_BG);
            drawRect(x, handleY, x + 4, handleY + handleHeight, SCROLLBAR_HANDLE);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput(); // Isso é vital para os comandos do mouse e scroll continuarem funcionando
        int statsSize;
        synchronized (statsList) {
            statsSize = statsList.size();
        }
        
        int delta = Mouse.getEventDWheel();
        if (delta != 0 && statsSize > 0) {
            int scrollDirection = delta > 0 ? -1 : 1;
            int maxOffset = Math.max(0, statsSize - 12);
            scrollOffset = Math.max(0, Math.min(scrollOffset + scrollDirection, maxOffset));
        }
    }

    private int getColoredWS(int ws) {
        if (ws >= 5) return 11141290; // Amarelo/Dourado
        if (ws >= 2) return 5635925;  // Verde
        return 16777215;              // Branco
    }

    private int getColoredFKDR(float fkdr) {
        if (fkdr >= 5.0f) return 11141290;
        if (fkdr >= 3.0f) return 65280;
        if (fkdr >= 1.5f) return 16776960;
        if (fkdr >= 1.0f) return 16733525;
        return 16777215;
    }

    private int getColoredWLR(float wlr) {
        if (wlr >= 1.5f) return 5635925;
        if (wlr >= 1.0f) return 16777045;
        return 16777215;
    }

    private String formatNumber(long number) {
        if (number >= 1000000) return String.format("%.1fM", number / 1000000.0d);
        if (number >= 1000) return String.format("%.1fK", number / 1000.0d);
        return String.valueOf(number);
    }
}