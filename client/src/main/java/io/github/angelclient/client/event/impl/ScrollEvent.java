package io.github.angelclient.client.event.impl;


public class ScrollEvent {

	public final int amount;
	public boolean cancelled;


	public ScrollEvent(int amount) {
		this.amount = amount;
	}

}