package io.github.angelclient.client.mod.impl.hud;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Position;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * Mod que mostra o endereço do servidor com ícone (como Badlion Client)
 */
public class IpHudMod extends SimpleHudMod {

    @Expose
    @Option
    private boolean brackets = true;
    
    @Expose
    @Option
    private boolean showIcon = true; // Mostrar ícone do servidor
    
    @Expose
    @Option
    private Colour serverColour = Colour.WHITE;
    
    // ResourceLocation da textura padrão (mundo singleplayer)

    @Override
    public String getId() {
        return "server_address";
    }

    @Override
    public void render(Position position, boolean editMode) {
        // Pega informações do servidor
        ServerData server = editMode ? null : mc.getCurrentServerData();
        String address = getServerAddress(server, editMode);
        
        // Calcula largura do texto
        int textWidth = font.getStringWidth(address);
        
        // Se mostrar ícone, adiciona espaço
        int iconSize = showIcon ? 10 : 0;
        int iconPadding = showIcon ? 3 : 0;
        int totalWidth = (showIcon ? iconSize + iconPadding : 0) + textWidth;
        
        // Renderiza fundo (se habilitado)
        if (background) {
            // Fundo dinâmico que se ajusta ao tamanho
            int bgX = position.getX() - 2;
            int bgY = position.getY() - 2;
            int bgW = totalWidth + 4;
            int bgH = Math.max(iconSize, font.FONT_HEIGHT) + 4;
            
            Gui.drawRect(bgX, bgY, bgX + bgW, bgY + bgH, backgroundColour.getValue());
        }
        
        int x = position.getX();
        int y = position.getY();
        
        // Renderiza ícone do servidor
        if (showIcon) {
            ResourceLocation serverIcon = getServerIcon(server);
            
            if (serverIcon != null) {
                GlStateManager.enableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getTextureManager().bindTexture(serverIcon);
                
                // Desenha ícone 10x10
                Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);
                
                GlStateManager.disableBlend();
            }
            
            x += iconSize + iconPadding;
        }
        
        // Renderiza texto do endereço
        font.drawString(address, x, y + 1, serverColour.getValue(), shadow);
    }
    
    /**
     * Retorna o endereço formatado do servidor
     */
    private String getServerAddress(ServerData server, boolean editMode) {
        if (editMode) {
            return formatText("Singleplayer");
        }
        
        if (server == null) {
            // Singleplayer ou mundo local
            if (mc.isSingleplayer()) {
                return formatText("Singleplayer");
            }
            return formatText("Desconhecido");
        }
        
        // Pega o IP
        String address = server.serverIP;
        
        return formatText(address);
    }
    
    /**
     * Formata o texto com ou sem colchetes
     */
    private String formatText(String address) {
        if (brackets && !background) {
            return "[" + address + "]";
        }
        return address;
    }
    
    /**
     * Retorna o ícone do servidor
     */
    private ResourceLocation getServerIcon(ServerData server) {
        if (server == null || mc.isSingleplayer()) {
            // Sem ícone disponível para singleplayer
            return null;
        }
        
        // Tenta usar o ícone do servidor se ele tiver
        if (server.getBase64EncodedIconData() != null) {
            // O Minecraft já cacheia isso automaticamente
            return new ResourceLocation("servers/" + server.serverIP + "/icon");
        }
        
        // Sem ícone disponível
        return null;
    }
    
    @Override
    public String getText(boolean editMode) {
        // Não usado porque temos render customizado
        return "";
    }
}