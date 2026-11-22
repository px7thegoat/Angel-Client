package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.performance.FrustumCullingMod;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public class MixinFrustumCulling {

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    public void onDoRender(Entity entity, double x, double y, double z,
                           float entityYaw, float partialTicks,
                           CallbackInfo ci) {

        FrustumCullingMod mod = FrustumCullingMod.getInstance();

        if (!mod.isEnabled()) return;

        try {
            if (!mod.shouldRenderEntity(entity)) {
                ci.cancel();
            }
        } catch (Exception ignored) {
        }
    }
}