package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.cosmetic.render.Cosmetic3DRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MixinGuiInventoryNametag {

    @Inject(method = "func_73863_a", at = @At("HEAD"), remap = false)
    private void onDrawScreenHead(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if ((GuiContainer) (Object) this instanceof GuiInventory) {
            Cosmetic3DRenderer.skipNametagRendering = true;
        }
    }

    @Inject(method = "func_73863_a", at = @At("RETURN"), remap = false)
    private void onDrawScreenReturn(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if ((GuiContainer) (Object) this instanceof GuiInventory) {
            Cosmetic3DRenderer.skipNametagRendering = false;
        }
    }
}
