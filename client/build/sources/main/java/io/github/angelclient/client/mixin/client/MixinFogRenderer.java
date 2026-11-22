package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.PerformanceMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.EntityRenderer;

/**
 * No Fog — cancela setupFog() quando a opção está ligada.
 *
 * O fog do 1.8.9 recalcula parâmetros GL_FOG por frame (glFogi, glFogf,
 * glFogfv), mesmo que o mundo não tenha neblina — são ~6 chamadas OpenGL
 * extras por frame. Em servidores sem fog visível é custo puro.
 *
 * Inspirado em BetterframesConfig.isFogEnabled() do Badlion.
 */
@Mixin(EntityRenderer.class)
public class MixinFogRenderer {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void cancelFog(int startCoords, float partialTicks, CallbackInfo ci) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.noFog) {
            ci.cancel();
        }
    }

    @Inject(method = "setupFogColor", at = @At("HEAD"), cancellable = true)
    private void cancelFogColor(float partialTicks, CallbackInfo ci) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.noFog) {
            ci.cancel();
        }
    }
}