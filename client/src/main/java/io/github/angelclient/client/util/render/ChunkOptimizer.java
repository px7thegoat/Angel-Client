package io.github.angelclient.client.util.render;

import net.minecraft.client.Minecraft;

public class ChunkOptimizer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean shouldRenderChunk(double distance) {

        int renderDistance = mc.gameSettings.renderDistanceChunks;

        if (distance > renderDistance * 16 * 1.5)
            return false;

        return true;
    }

}