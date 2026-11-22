package io.github.angelclient.client.util.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;

public class ArmorStandOptimizer {

    public static boolean shouldRender(Entity entity){

        if(!(entity instanceof EntityArmorStand))
            return true;

        if(entity.isInvisible())
            return false;

        return true;
    }

}