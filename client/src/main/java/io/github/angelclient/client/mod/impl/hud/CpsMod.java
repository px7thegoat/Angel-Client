package io.github.angelclient.client.mod.impl.hud;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.CpsMonitor;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Position;

public class CpsMod extends SimpleHudMod {

    @Expose
    @Option
    private boolean rmb;
    
    @Expose
    @Option
    private boolean brackets = true; // Toggle para colchetes []
    
    @Expose
    @Option
    private boolean showText = true; // Toggle para texto "CPS"
    
    @Expose
    @Option
    private Colour separatorColour = new Colour(64, 64, 64);

    @Override
    public String getId() {
        return "cps";
    }

    @Override
    public void render(Position position, boolean editMode) {
        super.render(position, editMode);
        
        if(rmb) {
            // Prefixo e sufixo (colchetes) - SEM DUPLICAR!
            String prefix = (background || !brackets) ? "" : "[";
            String suffix = (background || !brackets) ? "" : "]";
            
            // Texto "CPS"
            String cpsText = showText ? " CPS" : "";

            // Calcula largura
            int width = font.getStringWidth(prefix + CpsMonitor.LMB.getCps() + " | " + CpsMonitor.RMB.getCps() + cpsText + suffix) - 2;

            int x = position.getX() + (53 / 2) - (width / 2);
            int y = position.getY() + 4;

            // Desenha LMB
            x = font.drawString(prefix + Integer.toString(CpsMonitor.LMB.getCps()), x, y, textColour.getValue(), shadow);

            x--;
            if(shadow) x--;

            x += font.getCharWidth(' ');

            // Separador vertical
            Utils.drawVerticalLine(x, y - 1, y + 7, separatorColour.getValue());

            if(shadow) {
                Utils.drawVerticalLine(x + 1, y, y + 8, separatorColour.getShadowValue());
            }

            x += 1;
            x += font.getCharWidth(' ');

            // Desenha RMB
            font.drawString(CpsMonitor.RMB.getCps() + cpsText + suffix, x, y, textColour.getValue(), shadow);
        }
    }

    @Override
    public String getText(boolean editMode) {
        if (rmb) return ""; // A renderização do RMB já cuida disso
        return showText ? CpsMonitor.LMB.getCps() + " CPS" : String.valueOf(CpsMonitor.LMB.getCps());
    }
}