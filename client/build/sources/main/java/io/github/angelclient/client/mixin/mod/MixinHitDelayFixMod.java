package io.github.angelclient.client.mixin.mod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mod.impl.HitDelayFixMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.MovingObjectPosition;

public class MixinHitDelayFixMod {

	/**
	 * Injected into Minecraft to fix the hit delay on entity attacks.
	 *
	 * The vanilla clickMouse() method checks leftClickCounter > 0 before attacking
	 * an entity, causing a delay. When HitDelayFix is enabled, we redirect that
	 * field read to always return 0 for the entity-attack branch, so the check is
	 * bypassed and every click registers immediately.
	 *
	 * The existing TweaksMod#redirectClickDelayFix already handles ordinal=0 (the
	 * block-break branch). This mixin handles ordinal=1 — the entity-attack branch
	 * — which is what the original HitDelayFix mod targets via its @Overwrite of
	 * func_147116_af (clickMouse).
	 */
	@Mixin(Minecraft.class)
	public static class MixinMinecraft {

		@Shadow
		private int leftClickCounter;

		@Shadow
		public MovingObjectPosition objectMouseOver;

		@Shadow
		public EntityPlayerSP thePlayer;

		@Shadow
		public PlayerControllerMP playerController;

		@Shadow
		public WorldClient theWorld;

		/**
		 * Redirect the leftClickCounter field read at ordinal=1 inside clickMouse.
		 * This is the guard that precedes the entity-attack call
		 * (PlayerControllerMP#attackEntity / func_78764_a).
		 * Returning 0 makes the condition "leftClickCounter <= 0" always true,
		 * so the attack fires every tick the button is held, matching the behaviour
		 * of the original HitDelayFix.
		 */
		@Redirect(
			method = "clickMouse",
			at = @At(
				value = "FIELD",
				target = "Lnet/minecraft/client/Minecraft;leftClickCounter:I",
				opcode = org.objectweb.asm.Opcodes.GETFIELD,
				ordinal = 1
			)
		)
		public int redirectEntityHitDelay(Minecraft instance) {
			if (HitDelayFixMod.enabled) {
				return 0;
			}
			return this.leftClickCounter;
		}

		/**
		 * When the player disconnects / a new world is loaded, reset leftClickCounter
		 * to 0 so no stale delay carries over to the next session.
		 * Mirrors MixinWorldClient#sendQuittingDisconnectingPacket from the original mod.
		 */
		@Inject(
			method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
			at = @At("HEAD")
		)
		public void resetOnWorldLoad(WorldClient world, String message, CallbackInfo ci) {
			if (HitDelayFixMod.enabled) {
				this.leftClickCounter = 0;
			}
		}

	}

}
