package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.nametag.TabIconRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import org.lwjgl.opengl.GL11;

/**
 * PARTE 2 do sistema de ícones no TAB
 * Renderiza todos os ícones NO FINAL
 */
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinTabIconRenderer extends Gui {
    
    @Shadow
    protected Minecraft mc;
    
    /**
     * Renderiza todos os ícones registrados
     */
    @Inject(method = "renderPlayerlist", at = @At("RETURN"))
    private void renderAllIcons(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
        try {
            // Pega todos os ícones registrados
            for (TabIconRenderer.IconData icon : TabIconRenderer.getInstance().getIcons()) {
                renderIcon(icon.texture, icon.x, icon.y);
            }
        } catch (Exception e) {
            // Silencioso
        }
    }
    
    /**
     * Renderiza um ícone 8x8
     */
    private void renderIcon(net.minecraft.util.ResourceLocation texture, int x, int y) {
        try {
            GlStateManager.pushMatrix();
            
            // Bind textura
            mc.getTextureManager().bindTexture(texture);
            
            // Ativa blend para transparência
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            // Desenha ícone 8x8
            // Offset: +1 pixel Y para alinhar melhor com texto
            drawTexturedModalRect(x, y + 1, 0, 0, 8, 8);
            
            // Restaura estados
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            
            GlStateManager.popMatrix();
            
        } catch (Exception e) {
            // Silencioso
        }
    }
}