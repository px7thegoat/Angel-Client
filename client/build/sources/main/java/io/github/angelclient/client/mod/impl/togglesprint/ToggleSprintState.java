package io.github.angelclient.client.mod.impl.togglesprint;

import net.minecraft.client.resources.I18n;

public enum ToggleSprintState {
	HELD,
	TOGGLED;

	@Override
	public String toString() {
		return I18n.format("angel_client.mod.toggle_sprint." + name().toLowerCase());
	}

}
