package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.SendChatMessageEvent;
import io.github.angelclient.client.mod.impl.TweaksMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.world.World;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP extends AbstractClientPlayer {

	public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
		super(worldIn, playerProfile);
	}

	@Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
	public void sendChatMessage(String message, CallbackInfo callback) {
		if(Client.INSTANCE.bus.post(new SendChatMessageEvent(message)).cancelled) {
			callback.cancel();
		}
	}

	@Override
	public boolean isWearing(EnumPlayerModelParts part) {
		return Minecraft.getMinecraft().gameSettings.getModelParts().contains(part);
	}

	@Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
	public void onUpdateWalkingPlayer(CallbackInfo ci) {
		if (TweaksMod.enabled && TweaksMod.instance.mouseDelayFix) {
			EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
			this.rotationYaw = player.rotationYawHead;
			this.rotationPitch = player.rotationPitch;
		}
	}

}