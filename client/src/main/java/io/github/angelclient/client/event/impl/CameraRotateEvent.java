package io.github.angelclient.client.event.impl;


public class CameraRotateEvent {

	public float yaw;
	public float pitch;
	public float roll;


	public CameraRotateEvent(float yaw, float pitch, float roll) {
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}

}