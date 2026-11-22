package io.github.angelclient.client.event.impl;

import io.github.angelclient.client.annotation.ForgeCompat;

public class PreGameOverlayRenderEvent {

	public final float partialTicks;
	public final GameOverlayElement type;
	public boolean cancelled;

	@Deprecated
	@ForgeCompat
	public void setCanceled(boolean cancelled) {
		this.cancelled = cancelled;
	}


	public PreGameOverlayRenderEvent(float partialTicks, GameOverlayElement type) {
		this.partialTicks = partialTicks;
		this.type = type;
	}

}