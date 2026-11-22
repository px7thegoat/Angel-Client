package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mushstats.MushMcStats;
import net.minecraft.client.Minecraft;

/**
 * Mixin para Minecraft - Hook de Teclado para MushMcStats
 * 
 * CORRIGIDO: NÃO consome eventos de teclado, apenas escuta
 */
@Mixin(Minecraft.class)
public class MixinMushMcStatsKeyboard {
    
    /**
     * ESTRATÉGIA CORRETA: Injeta DEPOIS do processamento de teclado do Minecraft
     * Assim não interfere com o jogo
     */
    @Inject(method = "runTick", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V",
            shift = At.Shift.AFTER))
    private void mushstats_onKeyboard(CallbackInfo ci) {
        try {
            // Apenas notifica o MushMcStats para verificar teclas
            // NÃO consome eventos!
            MushMcStats.getInstance().onKeyInput();
        } catch (Exception e) {
            // Silencioso
        }
    }
}