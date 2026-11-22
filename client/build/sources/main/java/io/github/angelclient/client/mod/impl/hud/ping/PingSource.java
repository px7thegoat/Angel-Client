package io.github.angelclient.client.mod.impl.hud.ping;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.DetectedServer;
import net.minecraft.client.resources.I18n;

public enum PingSource {
	AUTO,
	MULTIPLAYER_SCREEN,
	TAB_LIST;

	public PingSource resolve() {
		if(this == AUTO) {
			if(Client.INSTANCE.detectedServer == DetectedServer.HYPIXEL) {
				return MULTIPLAYER_SCREEN;
			}

			return TAB_LIST;
		}

		return this;
	}

	@Override
	public String toString() {
		return I18n.format("angel_client.mod.ping.option.source." + name().toLowerCase());
	}

}
