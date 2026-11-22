package io.github.angelclient.client.event.impl;


public class FullscreenToggleEvent {

	public final boolean state;
	public boolean applyState = true;
	public boolean cancelled;


	public FullscreenToggleEvent(boolean state) {
		this.state = state;
	}

}