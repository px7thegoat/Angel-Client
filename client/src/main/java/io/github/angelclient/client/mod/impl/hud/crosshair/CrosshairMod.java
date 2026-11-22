package io.github.angelclient.client.mod.impl.hud.crosshair;

import org.lwjgl.opengl.GL11;
import com.google.gson.annotations.Expose;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.GameOverlayElement;
import io.github.angelclient.client.event.impl.PreGameOverlayRenderEvent;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.hud.HudMod;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Colour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldSettings.GameType;

public class CrosshairMod extends HudMod {

    private static final ResourceLocation CLIENT_CROSSHAIRS = new ResourceLocation("textures/gui/angel_client_crosshairs.png");

    @Expose
    @Option
    private CrosshairStyle style = CrosshairStyle.DEFAULT;

    @Expose
    @Option
    private boolean thirdPerson = true;

    @Expose
    @Option
    private boolean spectatorAlways = false;

    @Expose
    @Option
    private boolean debug = false;

    @Expose
    @Option
    private boolean blending = true;

    @Expose
    @Option
    private Colour crosshairColour = Colour.WHITE;

    @Expose
    @Option
    private boolean highlightEntities = false;

    @Expose
    @Option
    private Colour entityColour = Colour.PURE_RED;

    @Expose
    @Option
    private boolean customDraw = false;

    @Expose
    private boolean[][] customPixels = new boolean[16][16];

    public void setCustomPixels(boolean[][] pixels) {
        this.customPixels = pixels;
        this.customDraw = true;
    }

    public boolean[][] getCustomPixels() {
        return customPixels;
    }

    // ── Setter de cor adicionado para o editor ─────────────────────────────────
    public void setCrosshairColour(Colour colour) {
        this.crosshairColour = colour;
    }

    public Colour getCrosshairColour() {
        return crosshairColour;
    }

    public void openEditor() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new CrosshairEditorScreen(mc.currentScreen, this, customPixels));
    }

    @Override
    public void postOptionChange(String key, Object value) {
        if (key.equals("customDraw") && (boolean) value) {
            openEditor();
        }
        super.postOptionChange(key, value);
    }

    @Override
    public String getId() {
        return "crosshair";
    }

    @EventHandler
    public void onCrosshairRender(PreGameOverlayRenderEvent event) {
        if (event.type == GameOverlayElement.CROSSHAIRS) {
            event.cancelled = true;

            if ((!debug && mc.gameSettings.showDebugInfo) ||
                    (!spectatorAlways && (mc.playerController.getCurrentGameType() == GameType.SPECTATOR
                            && mc.objectMouseOver.typeOfHit != MovingObjectType.ENTITY)) ||
                    (!thirdPerson && mc.gameSettings.thirdPersonView != 0)) {
                return;
            }

            ScaledResolution resolution = new ScaledResolution(mc);
            GlStateManager.pushMatrix();
            GlStateManager.scale(getScale(), getScale(), getScale());
            // -8 para centralizar o pixel [8][8] no centro da tela
            GlStateManager.translate(
                resolution.getScaledWidth() / getScale() / 2 - 8,
                resolution.getScaledHeight() / getScale() / 2 - 8, 0);

            crosshairColour.bind();
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            if (highlightEntities && mc.objectMouseOver != null
                    && mc.objectMouseOver.entityHit != null
                    && !(mc.objectMouseOver.entityHit.isInvisible()
                    || mc.objectMouseOver.entityHit.isInvisibleToPlayer(mc.thePlayer))) {
                entityColour.bind();
            } else if (blending && !customDraw) {
                GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
                GlStateManager.enableAlpha();
            }

            if (customDraw && customPixels != null) {
                // Usa a cor do crosshairColour que agora é setada pelo editor
                int color = crosshairColour.getValue();
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        if (customPixels[x][y]) {
                            Gui.drawRect(x, y, x + 1, y + 1, color);
                        }
                    }
                }
            } else if (style == CrosshairStyle.DEFAULT) {
                mc.getTextureManager().bindTexture(Gui.icons);
                Utils.drawTexture(0, 0, 0, 0, 16, 16, 0);
            } else {
                mc.getTextureManager().bindTexture(CLIENT_CROSSHAIRS);
                int v = (style.ordinal() - 2) * 16;
                Utils.drawTexture(0, 0, 0, v, 16, 16, 0);
                mc.getTextureManager().bindTexture(Gui.icons);
            }

            GL11.glColor4f(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }
}