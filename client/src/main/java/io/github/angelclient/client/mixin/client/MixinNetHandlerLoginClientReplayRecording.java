package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mod.impl.replay.fix.SCReplayModRecording;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.S02PacketLoginSuccess;

@Mixin(NetHandlerLoginClient.class)
public class MixinNetHandlerLoginClientReplayRecording {
	@Shadow
	@Final
	private NetworkManager networkManager;

	@Inject(method = "handleLoginSuccess", at = @At("HEAD"))
	private void angelClient$startReplayRecording(S02PacketLoginSuccess packetIn, CallbackInfo ci) {
		SCReplayModRecording recording = SCReplayModRecording.instance;
		if(recording != null) {
			recording.initiateRecording(this.networkManager);
		}
	}
}
