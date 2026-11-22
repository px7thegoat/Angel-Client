package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.impl.GuiScaleMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerChest;
import org.lwjgl.opengl.GL11;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen {
    private boolean angel$isScaling = false;

    private boolean angel$shouldScale() {
        GuiScaleMod mod = (GuiScaleMod) Client.INSTANCE.getModById("guiscale");
        if (mod == null || !mod.isEnabled() || mod.inventoryScale == 1.0f) return false;

        // Baús de servidor não escalam
        if (((GuiContainer)(Object)this).inventorySlots instanceof ContainerChest) return false;

        return true;
    }

    @ModifyVariable(method = {"drawScreen", "mouseClicked", "mouseReleased", "mouseClickMove"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int angel$fixMouseX(int mouseX) {
        if (angel$shouldScale()) {
            float cx = this.width / 2.0f;
            float scale = ((GuiScaleMod)Client.INSTANCE.getModById("guiscale")).inventoryScale;
            return (int)((mouseX - cx) / scale + cx);
        }
        return mouseX;
    }

    @ModifyVariable(method = {"drawScreen", "mouseClicked", "mouseReleased", "mouseClickMove"}, at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int angel$fixMouseY(int mouseY) {
        if (angel$shouldScale()) {
            float cy = this.height / 2.0f;
            float scale = ((GuiScaleMod)Client.INSTANCE.getModById("guiscale")).inventoryScale;
            return (int)((mouseY - cy) / scale + cy);
        }
        return mouseY;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void angel$preDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // =====================================================================
        // FIX: Restaura o estado GL antes de qualquer render do inventário.
        //
        // Causa 1 — MenuBlurMod usa ShaderGroup que renderiza para o framebuffer
        // "swap" e deixa ele como framebuffer ativo. Sem restaurar para o main FBO,
        // o inventário renderiza no buffer errado (tela ciana, cliques não funcionam).
        //
        // Causa 2 — NanoVG (ModsScreen) deixa GL_STENCIL_TEST habilitado após
        // nvgEndFrame(), descartando todos os fragmentos do inventário (tela invisível).
        //
        // Causa 3 — O pipeline de shaders pode deixar GL_TEXTURE (5890) como
        // matrix mode ativo, corrompendo todas as texturas do inventário.
        //
        // O reset precisa estar AQUI (no GuiContainer) e não apenas no MixinGuiScreen,
        // porque GuiContainer.drawScreen() sobrescreve o fluxo e a ordem de execução
        // dos mixins pai/filho não é garantida.
        // =====================================================================
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);

        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

        GlStateManager.matrixMode(5888); // GL_MODELVIEW
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        // Aplica escala do GuiScaleMod se necessário
        if (angel$shouldScale()) {
            angel$isScaling = true;
            float scale = ((GuiScaleMod)Client.INSTANCE.getModById("guiscale")).inventoryScale;
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.width / 2.0f, this.height / 2.0f, 0);
            GlStateManager.scale(scale, scale, 1.0f);
            GlStateManager.translate(-this.width / 2.0f, -this.height / 2.0f, 0);
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void angel$postDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (angel$isScaling) {
            GlStateManager.popMatrix();
            angel$isScaling = false;
        }
    }
}