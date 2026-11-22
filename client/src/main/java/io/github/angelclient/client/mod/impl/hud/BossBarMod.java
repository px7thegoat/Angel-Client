package io.github.angelclient.client.mod.impl.hud;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.GameOverlayElement;
import io.github.angelclient.client.event.impl.PreGameOverlayRenderEvent;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.HudMod;
import io.github.angelclient.client.mod.hud.HudPosition;
import io.github.angelclient.client.util.data.Position;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;

public class BossBarMod extends HudMod {

    @Expose @Option
    public boolean showText = true;

    @Expose @Option
    public boolean textShadow = true;

    // ✅ Novo botão para mostrar/esconder a bossbar
    @Expose @Option
    public boolean showBossBar = true;

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    @Override
    public String getId() {
        return "bossbar";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.HUD;
    }

    @Override
    public HudPosition getDefaultPosition() {
        return new HudPosition(0.5f, 0.05f);
    }

    @EventHandler
    public void onOverlayRender(PreGameOverlayRenderEvent event) {
        if (isEnabled() && event.type == GameOverlayElement.BOSSHEALTH) {
            event.cancelled = true;
        }
    }

    @Override
    public void render(Position position, boolean editMode) {
        // ✅ Se showBossBar for falso, não renderiza (exceto no modo de edição para permitir mover o HUD)
        if (!isEnabled() || (!showBossBar && !editMode)) return;

        boolean hasBoss = BossStatus.bossName != null && BossStatus.healthScale > 0 && BossStatus.statusBarTime > 0;

        if (!hasBoss && !editMode) return;

        String name = hasBoss ? BossStatus.bossName : "Boss Name";
        float health = hasBoss ? BossStatus.healthScale : 0.66f;

        GlStateManager.pushMatrix();
        float s = getScale();
        GlStateManager.translate(position.getX(), position.getY(), 0);
        GlStateManager.scale(s, s, 1);

        int x = -(BAR_WIDTH / 2);

        if (showText) {
            int textX = x + (BAR_WIDTH / 2) - (mc.fontRendererObj.getStringWidth(name) / 2);
            int textY = -mc.fontRendererObj.FONT_HEIGHT - 2;
            if (textShadow) {
                mc.fontRendererObj.drawStringWithShadow(name, textX, textY, 0xFFFFFF);
            } else {
                mc.fontRendererObj.drawString(name, textX, textY, 0xFFFFFF, false);
            }
        }

        mc.getTextureManager().bindTexture(Gui.icons);
        GlStateManager.color(1, 1, 1, 1);
        mc.ingameGUI.drawTexturedModalRect(x, 0, 0, 74, BAR_WIDTH, BAR_HEIGHT);

        int filled = (int)(health * (BAR_WIDTH + 1));
        if (filled > 0) {
            mc.ingameGUI.drawTexturedModalRect(x, 0, 0, 79, filled, BAR_HEIGHT);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public Rectangle getBounds(Position position) {
        float s = getScale();
        int width = (int)(BAR_WIDTH * s);
        int height = (int)((BAR_HEIGHT + (showText ? mc.fontRendererObj.FONT_HEIGHT + 2 : 0)) * s);
        return new Rectangle(position.getX() - width / 2, position.getY() - (showText ? (int)(mc.fontRendererObj.FONT_HEIGHT * s) : 0), width, height);
    }

}