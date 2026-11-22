package io.github.angelclient.client.event.impl;


public class PostGameOverlayRenderEvent {

	public final float partialTicks;
	public final GameOverlayElement type;


	public PostGameOverlayRenderEvent(float partialTicks, GameOverlayElement type) {
		this.partialTicks = partialTicks;
		this.type = type;
	}

}