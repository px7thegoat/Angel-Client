package io.github.angelclient.client.mod.impl.hypixeladditions;

import com.google.gson.annotations.SerializedName;

import net.hypixel.api.data.type.GameType;

public class HypixelLocationData {

	@SerializedName("gametype")
	private String type;
	private String map;
	private String server;
	private String mode;

	public HypixelLocationData(String type, String map, String server, String mode) {
		this.type=type; this.map=map; this.server=server; this.mode=mode;
	}
	public String getType() { return type; }
	public String getMap() { return map; }
	public String getServer() { return server; }
	public String getMode() { return mode; }

}