package io.github.angelclient.client.event.impl;


public class PostGuiRenderEvent {

	public final float partialTicks;


	public PostGuiRenderEvent(float partialTicks) {
		this.partialTicks = partialTicks;
	}

}