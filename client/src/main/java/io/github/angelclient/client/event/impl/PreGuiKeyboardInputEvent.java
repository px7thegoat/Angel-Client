package io.github.angelclient.client.event.impl;

import io.github.angelclient.client.annotation.ForgeCompat;

public class PreGuiKeyboardInputEvent {

	public boolean cancelled;

	@Deprecated
	@ForgeCompat
	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}
