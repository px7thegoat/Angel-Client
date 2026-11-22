package io.github.angelclient.client.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class EntityDistanceCulling {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean shouldRender(Entity entity) {

        if (mc.thePlayer == null)
            return true;

        // players sempre renderizam
        if (entity instanceof EntityPlayer)
            return true;

        double distance = mc.thePlayer.getDistanceToEntity(entity);

        // não renderiza entidades muito longe
        if (distance > 96)
            return false;

        return true;
    }

}