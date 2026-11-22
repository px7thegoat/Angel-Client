package io.github.angelclient.client.event.impl;


public class PreGuiRenderEvent {

	public final float partialTicks;


	public PreGuiRenderEvent(float partialTicks) {
		this.partialTicks = partialTicks;
	}

}