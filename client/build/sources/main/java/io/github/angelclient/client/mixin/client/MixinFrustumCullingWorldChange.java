package io.github.angelclient.client.mixin.client;

import net.minecraft.client.multiplayer.WorldClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.performance.FrustumCullingMod;

@Mixin(WorldClient.class)
public class MixinFrustumCullingWorldChange {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onWorldLoad(CallbackInfo ci) {
        // Frustum is now updated every frame, so no cache clearing is needed
    }
}