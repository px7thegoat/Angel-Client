package io.github.angelclient.client.event.impl;


public class HitboxToggleEvent {

	public final boolean state;
	public boolean cancelled;


	public HitboxToggleEvent(boolean state) {
		this.state = state;
	}

}