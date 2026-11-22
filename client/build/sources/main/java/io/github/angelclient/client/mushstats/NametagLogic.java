package io.github.angelclient.client.mushstats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import io.github.angelclient.client.mod.impl.MushStatsMod;
import io.github.angelclient.client.mod.impl.NametagsMod;

public class NametagLogic {

    public static void renderLevel(EntityPlayer player, double x, double y, double z) {
        // Guard: MushStatsMod desligado
        if (MushStatsMod.getInstance() == null || !MushStatsMod.getInstance().isEnabled()) {
            return;
        }

        // Guard: toggle interno do MushStatsMod
        if (!MushStatsMod.getInstance().isNametagLevelEnabled()) {
            return;
        }

        // Guard: Toggle 1 — Show Nametags (NametagsMod)
        NametagsMod nametagsMod = NametagsMod.getInstance();
        if (nametagsMod != null && nametagsMod.isEnabled() && !nametagsMod.isShowNametags()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) return;
        if (player.getDistanceSqToEntity(mc.getRenderViewEntity()) > 4096.0D) return;
        if (player.isSneaking()) return;

        PlayerStats stats = MushApiClient.getCachedPlayer(player.getName());
        if (stats == null || !stats.isDataAvailable()) return;

        int level = stats.getLevel();
        int color = getLevelColor(level);

        String text = "Bedwars Level: " + level + LevelManager.getLevelSymbol(level);

        // Repassa os flags de NametagsMod para o renderizador
        boolean drawBackground = (nametagsMod == null || !nametagsMod.isEnabled()) || nametagsMod.isNametagBackground();
        boolean drawShadow     = (nametagsMod != null && nametagsMod.isEnabled()) && nametagsMod.isTextShadow();

        renderCustomLabel(player, text, x, y, z, color, drawBackground, drawShadow);
    }

    public static int getLevelColor(int level) {
        if (level >= 1000) return -1;
        if (level >= 900)  return -1870225;
        if (level >= 800)  return -197514;
        if (level >= 700)  return -9047699;
        if (level >= 600)  return -11380751;
        if (level >= 500)  return -514;
        if (level >= 400)  return -6874732;
        if (level >= 300)  return -14047837;
        if (level >= 200)  return -870610;
        if (level >= 100)  return -5504767;
        if (level >= 90)   return -239529;
        if (level >= 80)   return -10922248;
        if (level >= 70)   return -16733695;
        if (level >= 60)   return -7075180;
        if (level >= 50)   return -153342;
        if (level >= 40)   return -427;
        if (level >= 30)   return -827404;
        if (level >= 20)   return -10554639;
        if (level >= 10)   return -11141547;
        return -5592406;
    }

    private static void renderCustomLabel(EntityPlayer player, String str,
                                          double x, double y, double z,
                                          int color,
                                          boolean drawBackground,
                                          boolean drawShadow) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fontrenderer = mc.fontRendererObj;
        float f  = 1.6F;
        float f1 = 0.016666668F * f;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + player.height + 0.8F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int halfW = fontrenderer.getStringWidth(str) / 2;

        // ── Toggle 2: Nametag Background ─────────────────────────────────────
        if (drawBackground) {
            GlStateManager.disableTexture2D();

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();

            worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            worldrenderer.pos((double) (-halfW - 1), -1.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) (-halfW - 1),  8.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) ( halfW + 1),  8.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            worldrenderer.pos((double) ( halfW + 1), -1.0D, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
            tessellator.draw();

            GlStateManager.enableTexture2D();
        }

        // ── Toggle 3: Text Shadow ─────────────────────────────────────────────
        fontrenderer.drawString(str, -halfW, 0, (color & 0x00FFFFFF) | 0x20000000, drawShadow);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        fontrenderer.drawString(str, -halfW, 0, color, drawShadow);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}