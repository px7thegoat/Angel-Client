package io.github.angelclient.client.event.impl;

import io.github.angelclient.client.DetectedServer;
import net.minecraft.client.multiplayer.ServerData;

public class ServerConnectEvent {

	public final ServerData data;
	public final DetectedServer server;


	public ServerConnectEvent(ServerData data, DetectedServer server) {
		this.data = data;
		this.server = server;
	}

}