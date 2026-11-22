package io.github.angelclient.client.mushstats;

import net.minecraft.client.Minecraft;

/**
 * LevelManager - Gerencia símbolos e cores de níveis
 * 
 * CORRIGIDO: Package correto
 */
public class LevelManager {

    /**
     * Retorna o símbolo correspondente ao nível
     */
    public static String getLevelSymbol(int level) {
        return level >= 1000 ? "Ω"
             : level >= 900  ? "✪"
             : level >= 800  ? "☯"
             : level >= 700  ? "✌"
             : level >= 600  ? "✟"
             : level >= 500  ? "✪"
             : level >= 400  ? "❁"
             : level >= 300  ? "❂"
             : level >= 200  ? "✪"
             : level >= 100  ? "❤"
             : level >= 90   ? "❊"
             : level >= 80   ? "✵"
             : level >= 70   ? "✦"
             : level >= 60   ? "✣"
             : level >= 50   ? "✹"
             : level >= 40   ? "✸"
             : level >= 30   ? "✿"
             : level >= 20   ? "✽"
             : level >= 10   ? "✶"
             : "✫";
    }

    /**
     * Desenha a tag de nível com símbolo
     * 
     * @param level Nível do jogador
     * @param x Posição X
     * @param y Posição Y
     * @param colorInt Cor em formato int (RGB)
     */
    public static void drawLevelTag(int level, int x, int y, int colorInt) {
        String symbol = getLevelSymbol(level);
        String levelTag = "[" + level + symbol + "]";
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(levelTag, x, y, colorInt);
    }

    /**
     * Calcula a largura da tag de nível
     * 
     * @param level Nível do jogador
     * @return Largura em pixels
     */
    public static int getLevelTagWidth(int level) {
        String symbol = getLevelSymbol(level);
        String levelTag = "[" + level + symbol + "]";
        return Minecraft.getMinecraft().fontRendererObj.getStringWidth(levelTag);
    }
}