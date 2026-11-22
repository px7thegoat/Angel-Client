package io.github.angelclient.client.mushstats;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerMonitor {
    private static final PlayerMonitor INSTANCE = new PlayerMonitor();
    private final Minecraft mc = Minecraft.getMinecraft();
    private List<String> lastPlayerNames = new ArrayList<>();
    private GuiPlayerStats activeGui = null;
    private int tickCount = 0;

    public static PlayerMonitor getInstance() {
        return INSTANCE;
    }

    public void setActiveGui(GuiPlayerStats gui) {
        this.activeGui = gui;
    }

    public List<String> getPlayersInWorld() {
        List<String> usernames = new ArrayList<>();
        if (this.mc.theWorld != null) {
            for (EntityPlayer player : this.mc.theWorld.playerEntities) {
                if (player != this.mc.thePlayer) {
                    usernames.add(player.getName());
                }
            }
        }
        return usernames;
    }

    // Chamado manualmente pelo MushMcStats.onTick()
    public void onClientTick() {
        if (this.mc.theWorld != null) {
            if (this.activeGui != null && this.mc.currentScreen != this.activeGui) {
                this.activeGui = null;
            }
            
            this.tickCount++;
            if (this.tickCount % 20 == 0) { // Verifica a cada 1 segundo (20 ticks)
                this.tickCount = 0;
                checkPlayerListChange();
            }
        }
    }

    private void checkPlayerListChange() {
        List<String> currentNames = getPlayersInWorld();
        if (this.activeGui != null) {
            this.activeGui.updatePlayerList(currentNames);
        }
        this.lastPlayerNames = currentNames;
    }
}