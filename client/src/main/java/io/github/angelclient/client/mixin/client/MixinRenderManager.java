package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.util.access.AccessRenderManager;
import net.minecraft.client.renderer.entity.RenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager implements AccessRenderManager {

    @Shadow
    public float playerViewY;

    @Shadow
    public float playerViewX;

    @Override
    public float getPlayerViewY() {
        return playerViewY;
    }

    @Override
    public float getPlayerViewX() {
        return playerViewX;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "cacheActiveRenderInfo", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    public void onCacheActiveRenderInfo(net.minecraft.world.World worldIn, net.minecraft.client.gui.FontRenderer textRendererIn, net.minecraft.entity.Entity playerviewIn, net.minecraft.entity.Entity pointedEntityIn, net.minecraft.client.settings.GameSettings optionsIn, float partialTicks, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (io.github.angelclient.client.mod.impl.FreelookMod.INSTANCE != null && io.github.angelclient.client.mod.impl.FreelookMod.INSTANCE.hasStarted()) {
            this.playerViewY = io.github.angelclient.client.mod.impl.FreelookMod.INSTANCE.getYaw();
            this.playerViewX = io.github.angelclient.client.mod.impl.FreelookMod.INSTANCE.getPitch();
            
            // Re-aplicar a lógica do vanilla para thirdPersonView == 2 no playerViewY do freelook
            if (optionsIn != null && optionsIn.thirdPersonView == 2) {
                this.playerViewY += 180.0F;
            }
        }

        // Fix global para F5 na terceira pessoa (pitch invertido em todas as nametags)
        if (optionsIn != null && optionsIn.thirdPersonView == 2) {
            this.playerViewX = -this.playerViewX;
        }
    }
}
