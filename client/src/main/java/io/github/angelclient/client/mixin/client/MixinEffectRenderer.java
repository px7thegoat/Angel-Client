package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.util.render.ParticleOptimizer;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {

    @Inject(method = "updateEffects", at = @At("HEAD"))
    private void resetParticleCount(CallbackInfo ci) {
        ParticleOptimizer.reset();
    }

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void addEffect(EntityFX effect, CallbackInfo ci) {

        if (!ParticleOptimizer.allowParticle()) {
            ci.cancel();
        }

    }

}