package io.github.angelclient.client.mod.impl.hud.speedometer;

import java.text.DecimalFormat;
import com.google.gson.annotations.Expose;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import net.minecraft.util.MathHelper;

public class SpeedometerMod extends SimpleHudMod {
    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");
    
    @Expose
    @Option
    private boolean graphMode;
    
    @Expose
    @Option
    private boolean showText = true;
    
    @Expose
    @Option
    private boolean customText = false;
    
    @Expose
    private String suffix = "m/s";

    @Override
    public String getId() {
        return "speedometer";
    }

    private double getSpeed() {
        if(mc.thePlayer == null) return 0;
        double x = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double z = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        return MathHelper.sqrt_double(x * x + z * z) * 20.0; // Multiplicado por 20 para m/s
    }

    @Override
    public String getText(boolean editMode) {
        if(graphMode) return "";
        String value = editMode ? "0.00" : FORMAT.format(getSpeed());
        return showText ? value + (customText ? " " + suffix : " m/s") : value;
    }
}