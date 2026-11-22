package io.github.angelclient.client.coins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

/**
 * HUD de Moedas - Mostra na tela
 */
public class CoinHud extends Gui {
    
    private static final int COIN_COLOR = 0xFFD700; // Dourado
    private static final int LOADING_COLOR = 0xAAAAAA; // Cinza
    private static final int BG_COLOR = 0x80000000; // Preto semi-transparente
    
    /**
     * Renderiza HUD de moedas
     * 
     * @param x Posição X (0 = esquerda, -1 = direita)
     * @param y Posição Y
     */
    public static void render(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);
        
        CoinSystem cs = CoinSystem.getInstance();
        
        String text;
        int textColor;
        
        if (cs.isLoading()) {
            text = "⏳ Carregando...";
            textColor = LOADING_COLOR;
        } else {
            text = "💰 " + formatCoins(cs.getCoins());
            textColor = COIN_COLOR;
        }
        
        int textWidth = fr.getStringWidth(text);
        int textHeight = fr.FONT_HEIGHT;
        
        // Se x = -1, alinha à direita
        int finalX = x;
        if (x == -1) {
            finalX = sr.getScaledWidth() - textWidth - 5;
        }
        
        // Desenha fundo
        drawRect(
            finalX - 2, 
            y - 2, 
            finalX + textWidth + 2, 
            y + textHeight + 2, 
            BG_COLOR
        );
        
        // Desenha texto com sombra
        fr.drawStringWithShadow(text, finalX, y, textColor);
        
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    /**
     * Renderiza versão simples (sem background)
     */
    public static void renderSimple(int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        
        CoinSystem cs = CoinSystem.getInstance();
        
        String text;
        int textColor;
        
        if (cs.isLoading()) {
            text = "Carregando...";
            textColor = LOADING_COLOR;
        } else {
            text = "Moedas: " + formatCoins(cs.getCoins());
            textColor = COIN_COLOR;
        }
        
        fr.drawStringWithShadow(text, x, y, textColor);
    }
    
    /**
     * Formata número de moedas (com separador de milhares)
     */
    private static String formatCoins(int coins) {
        if (coins >= 1000000) {
            return String.format("%.1fM", coins / 1000000.0);
        } else if (coins >= 1000) {
            return String.format("%.1fK", coins / 1000.0);
        } else {
            return String.valueOf(coins);
        }
    }
    
    /**
     * Renderiza no menu principal (canto superior direito)
     */
    public static void renderInMenu() {
        render(-1, 5); // x=-1 = direita, y=5
    }
    
    /**
     * Renderiza in-game (customizável)
     */
    public static void renderInGame(int x, int y) {
        render(x, y);
    }
}
