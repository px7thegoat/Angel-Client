package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.GuiNewChat;

public class ChatRenderEvent {

	public final GuiNewChat chat;
	public final int updateCounter;
	public final float partialTicks;
	public boolean cancelled;


	public ChatRenderEvent(GuiNewChat chat, int updateCounter, float partialTicks) {
		this.chat = chat;
		this.updateCounter = updateCounter;
		this.partialTicks = partialTicks;
	}

}