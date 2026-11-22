package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.PerformanceMod;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManagerPerf {
    @Inject(method = "doRenderEntity", at = @At("HEAD"), cancellable = true)
    public void onRenderEntity(Entity entity, double x, double y, double z, float entityYaw, float partialTicks, boolean p_147939_10_, CallbackInfoReturnable<Boolean> cir) {
        if (PerformanceMod.instance == null || !PerformanceMod.instance.isEnabled()) return;

        if (PerformanceMod.instance.hideArmorStands && entity instanceof EntityArmorStand) {
            cir.setReturnValue(false);
        } else if (entity instanceof EntityItemFrame) {
            if (PerformanceMod.instance.hideItemFrames && ((EntityItemFrame) entity).getDisplayedItem() == null) {
                cir.setReturnValue(false);
            } else if (PerformanceMod.instance.hideMapsInItemFrames && ((EntityItemFrame) entity).getDisplayedItem() != null && ((EntityItemFrame) entity).getDisplayedItem().getItem() instanceof net.minecraft.item.ItemMap) {
                cir.setReturnValue(false);
            }
        } else if (entity instanceof EntityArrow) {
            EntityArrow arrow = (EntityArrow) entity;
            if (PerformanceMod.instance.hideGroundArrows && arrow.onGround) {
                cir.setReturnValue(false);
            } else if (PerformanceMod.instance.hideStuckArrows && arrow.ridingEntity != null) {
                cir.setReturnValue(false);
            }
        }
    }
}
