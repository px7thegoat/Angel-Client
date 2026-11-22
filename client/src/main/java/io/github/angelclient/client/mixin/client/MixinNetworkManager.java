package io.github.angelclient.client.mixin.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.socket.SocketChannelConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.NetworkManager;

/**
 * TCP_NODELAY — desativa o algoritmo de Nagle no socket TCP.
 *
 * Por padrão o Minecraft deixa o TCP juntar pacotes pequenos antes de enviar
 * (Nagle ON), o que adiciona até 40ms de latência artificial por pacote.
 * Com TCP_NODELAY cada pacote é enviado imediatamente, reduzindo o ping
 * percebido em ~10-40ms em servidores de PvP.
 *
 * Idêntico ao que o Badlion faz diretamente no NetworkManager.java deles.
 */
@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelActive", at = @At("HEAD"))
    private void enableTcpNoDelay(
            io.netty.channel.ChannelHandlerContext ctx, CallbackInfo ci) {
        Channel ch = ctx.channel();
        ChannelConfig cfg = ch.config();
        if (cfg instanceof SocketChannelConfig) {
            ((SocketChannelConfig) cfg).setTcpNoDelay(true);
        }
    }
}