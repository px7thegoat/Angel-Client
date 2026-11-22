package io.github.angelclient.client.mod.impl.hud;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import net.minecraft.client.Minecraft;

public class FpsMod extends SimpleHudMod {

    @Expose
    @Option
    private boolean brackets = true; // Toggle para colchetes []
    
    @Expose
    @Option
    private boolean showText = true; // Toggle para texto "FPS"

    @Override
    public String getId() {
        return "fps";
    }

    @Override
    public String getText(boolean editMode) {
        int fps = Minecraft.getDebugFPS();
        
        String text = String.valueOf(fps);
        
        if (showText) {
            text += " FPS";
        }
        
        // Adiciona colchetes se necessário
        if (brackets && !background) {
            return "[" + text + "]";
        }
        
        return text;
    }
}