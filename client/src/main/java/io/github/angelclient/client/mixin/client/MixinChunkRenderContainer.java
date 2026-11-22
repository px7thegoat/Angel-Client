package io.github.angelclient.client.mixin.client;

import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.performance.FrustumCullingMod;

@Mixin(ChunkRenderContainer.class)
public class MixinChunkRenderContainer {

    @Inject(method = "addRenderChunk", at = @At("HEAD"), cancellable = true)
    public void onRenderChunk(RenderChunk renderChunkIn, EnumWorldBlockLayer layer, CallbackInfo ci) {
        FrustumCullingMod mod = FrustumCullingMod.getInstance();

        if (!mod.isEnabled()) return;

        BlockPos pos = renderChunkIn.getPosition();

        if (!mod.isChunkVisible(pos)) {
            ci.cancel();
        }
    }
}
