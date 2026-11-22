package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.GuiScreen;

public class InitialOpenGuiEvent {

	public final GuiScreen screen;


	public InitialOpenGuiEvent(GuiScreen screen) {
		this.screen = screen;
	}

}