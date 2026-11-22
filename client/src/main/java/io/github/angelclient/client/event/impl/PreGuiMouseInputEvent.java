package io.github.angelclient.client.event.impl;

import io.github.angelclient.client.annotation.ForgeCompat;

public class PreGuiMouseInputEvent {

	public boolean cancelled;

	@Deprecated
	@ForgeCompat
	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
