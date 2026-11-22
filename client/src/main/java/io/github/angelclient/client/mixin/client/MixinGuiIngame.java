package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import io.github.angelclient.client.Client;
import net.minecraft.scoreboard.ScoreObjective;
import io.github.angelclient.client.event.impl.GameOverlayElement;
import io.github.angelclient.client.event.impl.PostGameOverlayRenderEvent;
import io.github.angelclient.client.event.impl.PreGameOverlayRenderEvent;
import io.github.angelclient.client.event.impl.ScoreboardRenderEvent;
import io.github.angelclient.client.mod.impl.GuiScaleMod;
import io.github.angelclient.client.util.access.AccessMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    @Final
    @Shadow
    private Minecraft mc;

    @Shadow
    protected abstract boolean showCrosshair();

    private boolean guiscale$hudScaled = false;

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;enableBlend()V", ordinal = 1, shift = At.Shift.AFTER), cancellable = true)
    public void preRenderGameOverlay(float partialTicks, CallbackInfo callback) {
        GlStateManager.disableLighting();
        if(Client.INSTANCE.bus.post(new PreGameOverlayRenderEvent(partialTicks, GameOverlayElement.ALL)).cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "renderGameOverlay", at = @At("RETURN"))
    public void postRenderGameOverlay(float partialTicks, CallbackInfo callback) {
        GlStateManager.disableLighting();
        Client.INSTANCE.bus.post(new PostGameOverlayRenderEvent(partialTicks, GameOverlayElement.ALL));
    }

    @Redirect(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;showCrosshair()Z"))
    public boolean preRenderCrosshair(GuiIngame guiIngame) {
        boolean result = !Client.INSTANCE.bus.post(new PreGameOverlayRenderEvent(AccessMinecraft.getInstance().getTimerSC().renderPartialTicks, GameOverlayElement.CROSSHAIRS)).cancelled && showCrosshair();
        mc.getTextureManager().bindTexture(Gui.icons);
        return result;
    }

    @Inject(method = "renderGameOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;drawTexturedModalRect(IIIIII)V"))
    public void overrideCrosshair(float partialTicks, CallbackInfo ci) {
        Client.INSTANCE.bus.post(new PostGameOverlayRenderEvent(partialTicks, GameOverlayElement.CROSSHAIRS));
    }

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    public void overrideScoreboard(ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo callback) {
        if(Client.INSTANCE.bus.post(new ScoreboardRenderEvent(objective, scaledRes)).cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "renderHorseJumpBar", at = @At("HEAD"), cancellable = true)
    public void preJumpBar(ScaledResolution scaledRes, int x, CallbackInfo callback) {
        if(Client.INSTANCE.bus.post(new PreGameOverlayRenderEvent(AccessMinecraft.getInstance().getTimerSC().renderPartialTicks, GameOverlayElement.JUMPBAR)).cancelled) {
            mc.getTextureManager().bindTexture(Gui.icons);
            callback.cancel();
        }
    }

    @Inject(method = "renderHorseJumpBar", at = @At("RETURN"))
    public void postJumpBar(ScaledResolution scaledRes, int x, CallbackInfo callback) {
        Client.INSTANCE.bus.post(new PostGameOverlayRenderEvent(AccessMinecraft.getInstance().getTimerSC().renderPartialTicks, GameOverlayElement.JUMPBAR));
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    public void preRenderBossHealth(CallbackInfo callback) {
        if (Client.INSTANCE.bus.post(new PreGameOverlayRenderEvent(
                AccessMinecraft.getInstance().getTimerSC().renderPartialTicks,
                GameOverlayElement.BOSSHEALTH)).cancelled) {
            callback.cancel();
        }
    }

    @Inject(method = "renderBossHealth", at = @At("RETURN"))
    public void postRenderBossHealth(CallbackInfo callback) {
        Client.INSTANCE.bus.post(new PostGameOverlayRenderEvent(
                AccessMinecraft.getInstance().getTimerSC().renderPartialTicks,
                GameOverlayElement.BOSSHEALTH));
    }

    // =========================================================
    // SISTEMA DE ESCALA DO HUD (REVISADO)
    // =========================================================

    private void guiscale$applyHudScale(ScaledResolution sr) {
        GuiScaleMod mod = (GuiScaleMod) Client.INSTANCE.getModById("guiscale");
        
        // Verificação de segurança para evitar erros de compilação/execução
        if (mod == null || !mod.isEnabled() || mod.hotbarScale == 1.0f) return;

        float scale = mod.hotbarScale;

        GlStateManager.pushMatrix();

        float centerX = sr.getScaledWidth() / 2.0f;
        float bottomY = (float) sr.getScaledHeight();

        // Faz o HUD crescer a partir do centro inferior da tela
        GlStateManager.translate(centerX, bottomY, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-centerX, -bottomY, 0);

        guiscale$hudScaled = true;
    }

    private void guiscale$removeHudScale() {
        if (guiscale$hudScaled) {
            GlStateManager.popMatrix();
            guiscale$hudScaled = false;
        }
        // Garante que a cor não fique "presa" em algum estado anterior
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // HOTBAR & TOOLTIP
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void preHotbar(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        guiscale$applyHudScale(sr);
    }

    @Inject(method = "renderTooltip", at = @At("RETURN"))
    private void postHotbar(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        guiscale$removeHudScale();
    }

    // STATUS (CORAÇÕES, FOME, ARMADURA)
    @Inject(method = "renderPlayerStats", at = @At("HEAD"))
    private void preStats(ScaledResolution sr, CallbackInfo ci) {
        guiscale$applyHudScale(sr);
    }

    @Inject(method = "renderPlayerStats", at = @At("RETURN"))
    private void postStats(ScaledResolution sr, CallbackInfo ci) {
        guiscale$removeHudScale();
    }

    // BARRA DE EXPERIÊNCIA
    @Inject(method = "renderExpBar", at = @At("HEAD"))
    private void preXP(ScaledResolution sr, int x, CallbackInfo ci) {
        guiscale$applyHudScale(sr);
    }

    @Inject(method = "renderExpBar", at = @At("RETURN"))
    private void postXP(ScaledResolution sr, int x, CallbackInfo ci) {
        guiscale$removeHudScale();
    }
}