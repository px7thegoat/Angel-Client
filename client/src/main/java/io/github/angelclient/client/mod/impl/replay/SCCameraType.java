package io.github.angelclient.client.mod.impl.replay;

import net.minecraft.client.resources.I18n;

public enum SCCameraType {
	CLASSIC,
	VANILLA_ISH;

	@Override
	public String toString() {
		return I18n.format("angel_client.mod.replay.option.camera." + name().toLowerCase());
	}

}
