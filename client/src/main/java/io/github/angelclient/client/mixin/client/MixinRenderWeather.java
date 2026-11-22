package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.PerformanceMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.EntityRenderer;

/**
 * No Weather — cancela renderRainSnow() quando a opção está ligada.
 *
 * Chuva/neve usa um passe de geometria com centenas de quads alpha-tested
 * por frame, mais uma chamada de textura e GL state changes adicionais.
 * Em PvP não tem impacto visual relevante mas pode custar 5-15% de FPS.
 *
 * Inspirado em BetterframesConfig.isWeather() do Badlion.
 */
@Mixin(EntityRenderer.class)
public class MixinRenderWeather {

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true)
    private void cancelWeather(float partialTicks, CallbackInfo ci) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.noWeather) {
            ci.cancel();
        }
    }
}