package io.github.angelclient.client.event.impl;

import net.minecraft.client.multiplayer.WorldClient;

public class WorldLoadEvent {

	public final WorldClient world;


	public WorldLoadEvent(WorldClient world) {
		this.world = world;
	}

}