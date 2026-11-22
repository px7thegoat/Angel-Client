package io.github.angelclient.client.mod.impl.hud;

import java.text.DecimalFormat;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.EntityAttackEvent;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;

public class ReachDisplayMod extends SimpleHudMod {

    private static final DecimalFormat FORMAT = new DecimalFormat("0.##");

    private double distance = 0;
    private long hitTime = -1;
    
    @Expose
    @Option
    private boolean brackets = true; // Toggle para colchetes []
    
    @Expose
    @Option
    private boolean showText = true; // Toggle para texto "blocks"

    @Override
    public String getId() {
        return "reach_display";
    }

    @Override
    public String getText(boolean editMode) {
        if((System.currentTimeMillis() - hitTime) > 5000) {
            distance = 0;
        }
        
        String value = editMode ? "0" : FORMAT.format(distance);
        String suffixText = "";
        
        String text = value + suffixText;
        
        // Adiciona colchetes se necessário
        if (brackets && !background) {
            return "[" + text + "]";
        }
        
        return text;
    }

    @EventHandler
    public void totallyNoReachHax(EntityAttackEvent event) {
        if(mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null) {
            distance = mc.objectMouseOver.hitVec.distanceTo(mc.thePlayer.getPositionEyes(1.0F));
            hitTime = System.currentTimeMillis();
        }
    }
}