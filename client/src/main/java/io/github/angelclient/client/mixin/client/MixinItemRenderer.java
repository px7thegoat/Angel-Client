package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.TransformFirstPersonItemEvent;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

	@Shadow private ItemStack itemToRender;

	@Inject(method = "transformFirstPersonItem", at = @At("RETURN"))
	public void transformFirstPersonItem(float equipProgress, float swingProgress, CallbackInfo callback) {
		Client.INSTANCE.bus.post(new TransformFirstPersonItemEvent(itemToRender, equipProgress, swingProgress));
	}

}