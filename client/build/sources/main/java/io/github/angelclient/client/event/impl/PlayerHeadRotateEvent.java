package io.github.angelclient.client.event.impl;


public class PlayerHeadRotateEvent {

	public final float yaw;
	public final float pitch;
	public boolean cancelled;


	public PlayerHeadRotateEvent(float yaw, float pitch) {
		this.yaw = yaw;
		this.pitch = pitch;
	}

}