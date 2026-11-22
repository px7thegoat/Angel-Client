package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.PerformanceMod;
import net.minecraft.client.renderer.tileentity.TileEntityMobSpawnerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityMobSpawnerRenderer.class)
public class MixinTileEntityMobSpawnerRendererPerf {
    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntityMobSpawner;DDDFI)V", at = @At("HEAD"), cancellable = true)
    public void onRenderSpawner(net.minecraft.tileentity.TileEntityMobSpawner te, double x, double y, double z, float partialTicks, int destroyStage, CallbackInfo ci) {
        if (PerformanceMod.instance != null && PerformanceMod.instance.isEnabled() && PerformanceMod.instance.hideMobInSpawner) {
            ci.cancel();
        }
    }
}
