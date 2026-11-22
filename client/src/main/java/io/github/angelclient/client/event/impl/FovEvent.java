package io.github.angelclient.client.event.impl;


public class FovEvent {

	public float fov;
	public final float partialTicks;


	public FovEvent(float fov, float partialTicks) {
		this.fov = fov;
		this.partialTicks = partialTicks;
	}

}