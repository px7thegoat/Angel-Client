package io.github.angelclient.client.mushstats;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;
// Importação necessária para ler as configurações do mod
import io.github.angelclient.client.mod.impl.MushStatsMod;

public class KeyHandler {
    private static final KeyHandler INSTANCE = new KeyHandler();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static KeyHandler getInstance() {
        return INSTANCE;
    }

    public void handleKey() {
    MushStatsMod mod = MushStatsMod.getInstance();

    // 1. Verifica se o mod existe e está ativado no menu (botão de ligar/desligar)
    if (mod == null || !mod.isEnabled()) return;

    // 2. Compara a tecla pressionada com a tecla configurada no mod
    if (Keyboard.getEventKey() == mod.getKeyCode() && Keyboard.getEventKeyState()) {
        
        // Verifica se está no Mush
        ServerData serverData = this.mc.getCurrentServerData();
        if (serverData == null || serverData.serverIP == null || !serverData.serverIP.toLowerCase().contains("mush")) {
            return;
        }

        // Abre a GUI
        List<String> playerNames = PlayerMonitor.getInstance().getPlayersInWorld();
        GuiPlayerStats gui = new GuiPlayerStats(playerNames);
        this.mc.displayGuiScreen(gui);
    }
}
}