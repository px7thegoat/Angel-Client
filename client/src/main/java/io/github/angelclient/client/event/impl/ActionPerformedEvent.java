package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ActionPerformedEvent {

	public final GuiScreen gui;
	public final GuiButton button;
	public boolean cancelled;


	public ActionPerformedEvent(GuiScreen gui, GuiButton button) {
		this.gui = gui;
		this.button = button;
	}

}