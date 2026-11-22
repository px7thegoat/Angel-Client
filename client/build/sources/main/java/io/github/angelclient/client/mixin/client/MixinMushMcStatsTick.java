package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mushstats.MushMcStats;
import net.minecraft.client.Minecraft;

/**
 * Mixin para MushMC Stats - Hook de Tick
 * 
 * CRÍTICO: Precisa estar registrado em mixins.angelclient.json!
 */
@Mixin(Minecraft.class)
public class MixinMushMcStatsTick {
    
    /**
     * Injeta no início de runTick()
     * Chama o tick do MushMcStats
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void mushstats_onRunTick(CallbackInfo ci) {
        try {
            MushMcStats.getInstance().onTick();
        } catch (Exception e) {
            // Silencioso para não crashar
        }
    }
}