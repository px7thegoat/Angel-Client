package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mod.impl.PerformanceMod;
import io.github.angelclient.client.util.access.AccessRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;

@Mixin(Render.class)
public abstract class MixinRender<T extends Entity> implements AccessRender<T> {

    @Override
    @Invoker("renderName")
    public abstract void doRenderName(T entity, double x, double y, double z);

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    public void onDoRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        
        // Verifica se o AntiHologram do Angel Client está ligado
        if (PerformanceMod.instance != null && PerformanceMod.instance.antiHologram) {
            
            if (entity instanceof EntityArmorStand && entity.isInvisible()) {
                Minecraft mc = Minecraft.getMinecraft();
                
                // Oculta se o jogador estiver a mais de 5 blocos de distância (25m²)
                if (mc.thePlayer != null && entity.getDistanceSqToEntity(mc.thePlayer) > 25.0) {
                    ci.cancel();
                }
            }
        }
    }
}