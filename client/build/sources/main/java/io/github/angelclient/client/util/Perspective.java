package io.github.angelclient.client.util;

import net.minecraft.client.resources.I18n;

public enum Perspective {
	FIRST_PERSON,
	THIRD_PERSON_BACK,
	THIRD_PERSON_FRONT;

	@Override
	public String toString() {
		return I18n.format("angel_client.perspective." + name().toLowerCase());
	}

}