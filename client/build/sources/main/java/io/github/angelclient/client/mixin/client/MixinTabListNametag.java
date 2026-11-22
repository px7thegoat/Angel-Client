package io.github.angelclient.client.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.nametag.Nametag;
import io.github.angelclient.client.nametag.NametagManager;
import io.github.angelclient.client.nametag.TabIconRenderer;

import java.util.List;

/**
 * Adiciona ícones de nametag no Tab List
 * 
 * Renderiza ao lado do nome do jogador
 */
@Mixin(GuiPlayerTabOverlay.class)
public class MixinTabListNametag {

    @Shadow @Final private Minecraft mc;

    /**
     * Injeta APÓS renderizar cada entrada do tab list
     * Coleta posições para renderizar ícones depois
     */
    @Inject(method = "renderPlayerlist",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V",
                     ordinal = 1,
                     shift = At.Shift.BEFORE))
    private void captureTabListPositions(int width, net.minecraft.scoreboard.Scoreboard scoreboardIn,
                                         net.minecraft.scoreboard.ScoreObjective scoreObjectiveIn,
                                         CallbackInfo ci) {
        // Limpa lista de ícones para renderizar
        TabIconRenderer.getInstance().clear();
    }

    /**
     * Renderiza todos os ícones coletados
     * Chamado DEPOIS que todos os nomes foram renderizados
     */
    @Inject(method = "renderPlayerlist",
            at = @At("RETURN"))
    private void renderTabListIcons(int width, net.minecraft.scoreboard.Scoreboard scoreboardIn,
                                    net.minecraft.scoreboard.ScoreObjective scoreObjectiveIn,
                                    CallbackInfo ci) {
        
        // Renderiza todos os ícones coletados
        for (TabIconRenderer.IconData icon : TabIconRenderer.getInstance().getIcons()) {
            renderIcon(icon.texture, icon.x, icon.y, 8, 8);
        }
    }
    
    /**
     * Renderiza um ícone no tab list
     */
    private void renderIcon(ResourceLocation texture, int x, int y, int width, int height) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(texture);
        
        net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
            x, y, 0, 0, width, height, width, height
        );
    }
}