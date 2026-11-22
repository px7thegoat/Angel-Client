package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.PerformanceMod;
import net.minecraft.client.renderer.RenderGlobal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    // ── Entity Render Limit ───────────────────────────────────────────────────
    // Cap de 150 entidades por frame quando limitEntityRender está ligado.
    // BUGFIX: o redirect original não especificava um ordinal, capturando TODAS
    // as chamadas a List.size() dentro de renderEntities (lista de tile entities,
    // passes de render, etc.), o que causava crashes e corrupção visual.
    // O ordinal = 0 garante que só a primeira chamada – a lista de entidades –
    // seja interceptada.
    @Redirect(
        method = "renderEntities",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;size()I",
            ordinal = 0
        )
    )
    private int limitEntityListSize(java.util.List<?> list) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.limitEntityRender) {
            return Math.min(list.size(), 150);
        }
        return list.size();
    }

    // ── No Clouds ─────────────────────────────────────────────────────────────
    // Cancela o método renderClouds inteiro quando noClouds está ligado.
    // renderClouds constrói e envia geometria com blending alpha para a GPU
    // a cada frame — desativá-lo é custo zero e o impacto visual em PvP é nulo.
    @Inject(
        method = "renderClouds",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelClouds(float partialTicks, int pass, CallbackInfo ci) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.noClouds) {
            ci.cancel();
        }
    }
}