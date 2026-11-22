package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.PerformanceMod;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySkullRenderer.class)
public class MixinTileEntitySkullRendererPerf {
    @Inject(method = "renderSkull", at = @At("HEAD"), cancellable = true)
    public void onRenderSkull(float x, float y, float z, net.minecraft.util.EnumFacing facing, float p_180543_5_, int p_180543_6_, com.mojang.authlib.GameProfile profile, int p_180543_8_, CallbackInfo ci) {
        if (PerformanceMod.instance != null && PerformanceMod.instance.isEnabled() && PerformanceMod.instance.hideSkulls) {
            ci.cancel();
        }
    }
}
