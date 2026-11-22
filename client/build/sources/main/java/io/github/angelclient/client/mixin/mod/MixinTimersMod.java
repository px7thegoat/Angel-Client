package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.hud.timers.TimersMod;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinTimersMod {

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void onCustomPayload(S3FPacketCustomPayload packet, CallbackInfo ci) {
        TimersMod mod = (TimersMod) io.github.angelclient.client.Client.INSTANCE.getModById("timers");
        if (mod != null && mod.isEnabled()) {
            mod.onCustomPayload(packet);
        }
    }
}
