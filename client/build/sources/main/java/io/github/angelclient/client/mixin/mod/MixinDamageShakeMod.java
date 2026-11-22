package io.github.angelclient.client.mixin.mod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.angelclient.client.mod.impl.damageshake.DamageShakeMod;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;

public class MixinDamageShakeMod {

	@Mixin(EntityRenderer.class)
	public static abstract class MixinEntityRenderer {

		/**
		 * Cancels the entire hurtCameraEffect call when intensity is 0,
		 * or lets it run if intensity > 0 (the rotate redirect below scales it).
		 */
		@Redirect(method = "setupCameraTransform",
				at = @At(value = "INVOKE",
						target = "Lnet/minecraft/client/renderer/EntityRenderer;hurtCameraEffect(F)V"))
		public void controlDamageShake(EntityRenderer instance, float partialTicks) {
			if (DamageShakeMod.instance != null
					&& DamageShakeMod.instance.isEnabled()
					&& DamageShakeMod.instance.getIntensity() == 0f) {
				// Intensity is zero — skip the effect entirely
				return;
			}
			hurtCameraEffect(partialTicks);
		}

		/**
		 * Scales the rotation angle inside hurtCameraEffect by the configured intensity.
		 */
		@Redirect(method = "hurtCameraEffect",
				at = @At(value = "INVOKE",
						target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V"))
		public void scaleShakeAngle(float angle, float x, float y, float z) {
			if (DamageShakeMod.instance != null && DamageShakeMod.instance.isEnabled()) {
				angle *= DamageShakeMod.instance.getIntensity();
			}
			GlStateManager.rotate(angle, x, y, z);
		}

		@Shadow
		protected abstract void hurtCameraEffect(float partialTicks);
	}

}