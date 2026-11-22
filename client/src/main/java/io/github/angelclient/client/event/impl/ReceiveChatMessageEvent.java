package io.github.angelclient.client.event.impl;


public class ReceiveChatMessageEvent {

	public final boolean actionBar;
	public final String message;
	/**
	 * Whether the event is fired from the replay mod.
	 */
	public final boolean replay;
	public boolean cancelled;


	public ReceiveChatMessageEvent(boolean actionBar, String message, boolean replay) {
		this.actionBar = actionBar;
		this.message = message;
		this.replay = replay;
	}

}