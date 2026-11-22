package io.github.angelclient.client.ui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class JGuiPreviousScreen extends GuiScreen {

	private final GuiScreen previous;

	public JGuiPreviousScreen(GuiScreen previous) {
		this.previous = previous;
	}

	@Override
	public void initGui() {
		super.initGui();
		Minecraft.getMinecraft().displayGuiScreen(previous);
	}

}
