package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.GuiScreen;

public class OpenGuiEvent {

	public final GuiScreen screen;


	public OpenGuiEvent(GuiScreen screen) {
		this.screen = screen;
	}

}