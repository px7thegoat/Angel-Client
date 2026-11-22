package io.github.angelclient.client.nametag;

import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper para renderizar ícones no TAB
 * Armazena posições e texturas para renderizar depois
 */
public class TabIconRenderer {
    
    private static TabIconRenderer instance;
    
    private List<IconData> iconsToRender = new ArrayList<>();
    
    public static TabIconRenderer getInstance() {
        if (instance == null) {
            instance = new TabIconRenderer();
        }
        return instance;
    }
    
    /**
     * Limpa lista (chamado no início da renderização)
     */
    public void clear() {
        iconsToRender.clear();
    }
    
    /**
     * Adiciona ícone para renderizar
     */
    public void addIcon(ResourceLocation texture, int x, int y) {
        iconsToRender.add(new IconData(texture, x, y));
    }
    
    /**
     * Retorna todos os ícones
     */
    public List<IconData> getIcons() {
        return iconsToRender;
    }
    
    /**
     * Dados de um ícone
     */
    public static class IconData {
        public ResourceLocation texture;
        public int x;
        public int y;
        
        public IconData(ResourceLocation texture, int x, int y) {
            this.texture = texture;
            this.x = x;
            this.y = y;
        }
    }
}