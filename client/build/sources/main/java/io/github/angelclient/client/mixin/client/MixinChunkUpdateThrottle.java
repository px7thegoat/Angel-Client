package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.PerformanceMod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

import net.minecraft.client.renderer.RenderGlobal;

/**
 * Reduced Chunk Updates — limita os chunk rebuilds por tick a 1.
 *
 * updateChunks() permite por padrão até MAX_REBUILDS_PER_FRAME = 4 rebuilds
 * por tick. Em áreas densas (spawn, lobbies) isso cria lag spikes visíveis
 * ao mover. Reduzir para 1 distribui o custo uniformemente — sem picos,
 * FPS mais estável, custo médio idêntico.
 *
 * Inspirado em BetterframesConfig.isReducedChunkUpdates() do Badlion.
 * Badlion seta esse valor para 1 quando reducedChunkUpdates está ligado.
 */
@Mixin(RenderGlobal.class)
public class MixinChunkUpdateThrottle {

    /**
     * O valor 4 é MAX_REBUILDS_PER_FRAME hardcoded no loop de updateChunks().
     * Substituímos por 1 quando reducedChunkUpdates está ativo.
     */
    @ModifyConstant(
        method = "updateChunks",
        constant = @Constant(intValue = 4)
    )
    private int limitChunkRebuilds(int original) {
        PerformanceMod perf = PerformanceMod.instance;
        if (perf != null && perf.isEnabled() && perf.reducedChunkUpdates) {
            return 1;
        }
        return original;
    }
}