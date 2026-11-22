package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.impl.TitlePositionMod;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinTitlePosition {

    @Shadow private String displayedTitle;
    @Shadow private String displayedSubTitle;
    @Shadow private int titleDisplayTime;

    @Inject(method = "displayTitle", at = @At("HEAD"), cancellable = true)
    private void angelclient_onDisplayTitle(String title, String subtitle, int timeFadeIn, int timeDisplay, int timeFadeOut, CallbackInfo ci) {
        // Busca o mod pelo ID que definimos no getId()
        TitlePositionMod mod = (TitlePositionMod) Client.INSTANCE.getModById("titleposition");

        if (mod != null && mod.isEnabled()) {
            // Calcula o tempo total em ticks
            int totalTicks = (timeFadeIn >= 0 ? timeFadeIn : 0) + 
                             (timeDisplay >= 0 ? timeDisplay : 0) + 
                             (timeFadeOut >= 0 ? timeFadeOut : 0);
            
            // Passa os dados para o Mod
            mod.setTitle(title, subtitle, totalTicks);
            
            // Cancela a renderização nativa do Minecraft para usar apenas a do HUD Mod
            this.displayedTitle = "";
            this.displayedSubTitle = "";
            this.titleDisplayTime = 0;
            ci.cancel();
        }
    }
}