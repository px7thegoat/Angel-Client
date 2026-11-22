package io.github.angelclient.client.mod.impl;

import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.WorldLoadEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;

public class HitDelayFixMod extends Mod {

	public static boolean enabled;
	public static HitDelayFixMod instance;

	@Override
	public String getId() {
		return "hit_delay_fix";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.UTILITY;
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		instance = this;
		enabled = true;
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		enabled = false;
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		// Reset any internal state when loading/leaving a world
		// (mirrors MixinWorldClient#sendQuittingDisconnectingPacket in the original HitDelayFix)
	}

}
