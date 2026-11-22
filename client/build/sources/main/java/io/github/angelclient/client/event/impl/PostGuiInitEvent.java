package io.github.angelclient.client.event.impl;

import java.util.Collection;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class PostGuiInitEvent {

	public final GuiScreen screen;
	public final Collection<GuiButton> buttonList;


	public PostGuiInitEvent(GuiScreen screen, Collection<GuiButton> buttonList) {
		this.screen = screen;
		this.buttonList = buttonList;
	}

}