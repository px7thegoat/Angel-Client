package io.github.angelclient.client.event.impl;


public class SendChatMessageEvent {

	public final String message;
	public boolean cancelled;


	public SendChatMessageEvent(String message) {
		this.message = message;
	}

}