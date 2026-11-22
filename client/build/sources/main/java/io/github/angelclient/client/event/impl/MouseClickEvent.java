package io.github.angelclient.client.event.impl;


public class MouseClickEvent {

	public final int button;
	public boolean cancelled;


	public MouseClickEvent(int button) {
		this.button = button;
	}

}