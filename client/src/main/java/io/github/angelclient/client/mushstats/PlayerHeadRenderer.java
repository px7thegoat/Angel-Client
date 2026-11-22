package io.github.angelclient.client.mushstats;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class PlayerHeadRenderer {
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    public static void drawPlayerHead(String username, int x, int y, int size) {
        Minecraft mc = Minecraft.getMinecraft();
        ResourceLocation skin = STEVE_SKIN;

        if (mc.getNetHandler() != null) {
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equalsIgnoreCase(username)) {
                    ResourceLocation playerSkin = info.getLocationSkin();
                    if (playerSkin != null) skin = playerSkin;
                    break;
                }
            }
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(skin);

        drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64.0f, 64.0f);
        drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64.0f, 64.0f);
    }

    private static void drawScaledCustomSizeModalRect(int x, int y, float u, float v, int uWidth, int vHeight, int width, int height, float tileWidth, float tileHeight) {
        float scaleU = 1.0f / tileWidth;
        float scaleV = 1.0f / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(x, y + height, 0.0D).tex(u * scaleU, (v + vHeight) * scaleV).endVertex();
        worldRenderer.pos(x + width, y + height, 0.0D).tex((u + uWidth) * scaleU, (v + vHeight) * scaleV).endVertex();
        worldRenderer.pos(x + width, y, 0.0D).tex((u + uWidth) * scaleU, v * scaleV).endVertex();
        worldRenderer.pos(x, y, 0.0D).tex(u * scaleU, v * scaleV).endVertex();
        tessellator.draw();
    }
}