package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * CORREÇÃO COMPLETA DO GUI SCALE
 * Força a escala customizada em TODAS as UIs
 */
@Mixin(ScaledResolution.class)
public class MixinScaledResolution {
    
    @Shadow
    private int scaledWidth;
    
    @Shadow
    private int scaledHeight;
    
    @Shadow
    private double scaledWidthD;
    
    @Shadow
    private double scaledHeightD;
    
    /**
     * Força usar escala customizada do GameSettings
     */
    @Inject(method = "<init>(Lnet/minecraft/client/Minecraft;)V", at = @At("RETURN"))
    private void forceCustomScale(Minecraft mc, CallbackInfo ci) {
        try {
            // Pega a escala das configurações do jogo
            int guiScale = mc.gameSettings.guiScale;
            
            // Se for 0 (auto), não faz nada
            if (guiScale == 0) {
                return;
            }
            
            // Recalcula dimensões com a escala escolhida
            int width = mc.displayWidth;
            int height = mc.displayHeight;
            
            this.scaledWidth = width / guiScale;
            this.scaledHeight = height / guiScale;
            this.scaledWidthD = (double)this.scaledWidth;
            this.scaledHeightD = (double)this.scaledHeight;
            
        } catch (Exception e) {
            // Silencioso
        }
    }
}