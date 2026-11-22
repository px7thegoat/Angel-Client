package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.GuiScreen;

public class PreGuiInitEvent {

	public final GuiScreen screen;
	public boolean cancelled;


	public PreGuiInitEvent(GuiScreen screen) {
		this.screen = screen;
	}

}