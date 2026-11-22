package io.github.angelclient.client.optimization;

import net.minecraft.client.Minecraft;

public class DynamicRenderDistance {

    public static void update(){

        Minecraft mc = Minecraft.getMinecraft();

        if(mc.theWorld == null)
            return;

        int entities = mc.theWorld.loadedEntityList.size();

        if(entities > 150)
            mc.gameSettings.renderDistanceChunks = 5;

        else
            mc.gameSettings.renderDistanceChunks = 9;

    }

}