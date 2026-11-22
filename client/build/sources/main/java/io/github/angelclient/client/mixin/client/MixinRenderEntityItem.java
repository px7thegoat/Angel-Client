package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.ItemEntityRenderEvent;
import io.github.angelclient.client.mod.impl.bigdrops.BigDropsMod;
import io.github.angelclient.client.mod.impl.itemphysics.ItemPhysicsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEntityItem.class)
public class MixinRenderEntityItem {

    private boolean bigDropsScaleActive = false;

    @Inject(method = "doRender", at = @At("HEAD"), cancellable = true)
    private void onDoRenderHead(EntityItem entity, double x, double y, double z,
                                float yaw, float partialTicks, CallbackInfo ci) {
        bigDropsScaleActive = false;
        ItemPhysicsMod.hasPendingTransform = false;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Cull itens distantes
        double dx = mc.thePlayer.posX - entity.posX;
        double dy = mc.thePlayer.posY - entity.posY;
        double dz = mc.thePlayer.posZ - entity.posZ;
        if (dx * dx + dy * dy + dz * dz > 64 * 64) {
            ci.cancel();
            return;
        }

        // Resolve modelo para o evento
        ItemStack stack = entity.getEntityItem();
        IBakedModel model = null;
        try {
            model = mc.getRenderItem().getItemModelMesher().getItemModel(stack);
        } catch (Exception ignored) {}

        // Dispara evento — ItemPhysicsMod calcula a rotação e seta hasPendingTransform
        Client.INSTANCE.bus.post(new ItemEntityRenderEvent(entity, x, y, z, partialTicks, model));

        // BigDrops scale — empurra matrix antes do vanilla renderizar
        if (BigDropsMod.INSTANCE != null && BigDropsMod.INSTANCE.isEnabled()
                && stack != null && stack.getItem() != null) {
            String registryName = Item.itemRegistry.getNameForObject(stack.getItem()).toString();
            float scale = BigDropsMod.INSTANCE.getScale(registryName);
            if (scale != 1.0f) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x, (float) y, (float) z);
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(-(float) x, -(float) y, -(float) z);
                bigDropsScaleActive = true;
            }
        }
    }

    // Injeta DENTRO do doRender, após o translate/rotate do vanilla mas antes do renderItem
    // Usamos o ponto onde o vanilla chama setupRotation — logo após o translate inicial
    @Inject(method = "doRender",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V",
                    ordinal = 0))
    private void injectPhysicsRotation(EntityItem entity, double x, double y, double z,
                                       float yaw, float partialTicks, CallbackInfo ci) {
        if (ItemPhysicsMod.hasPendingTransform) {
            // Após a rotação vanilla (rotationYaw), adiciona a rotação de física
            GlStateManager.rotate(ItemPhysicsMod.pendingRotation, 1, 0, 0);
        }
    }

    @Inject(method = "doRender", at = @At("RETURN"))
    private void onDoRenderReturn(EntityItem entity, double x, double y, double z,
                                  float yaw, float partialTicks, CallbackInfo ci) {
        if (bigDropsScaleActive) {
            GlStateManager.popMatrix();
            bigDropsScaleActive = false;
        }
    }
}