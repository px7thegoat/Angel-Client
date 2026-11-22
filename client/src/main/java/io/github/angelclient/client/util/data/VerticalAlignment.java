package io.github.angelclient.client.util.data;

import net.minecraft.client.resources.I18n;

public enum VerticalAlignment {
	TOP,
	MIDDLE,
	BOTTOM;

	@Override
	public String toString() {
		return I18n.format("angel_client.vertical_alignment." + name().toLowerCase());
	}

}
