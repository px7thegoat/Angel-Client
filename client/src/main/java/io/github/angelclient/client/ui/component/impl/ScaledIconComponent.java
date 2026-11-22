package io.github.angelclient.client.ui.component.impl;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.Controller;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class ScaledIconComponent extends ColouredComponent {

	private final Controller<String> iconName;
	private final int width;
	private final int height;

	public ScaledIconComponent(String iconName, int width, int height) {
		this((component, defaultName) -> iconName, width, height, (component, defaultColour) -> defaultColour);
	}

	public ScaledIconComponent(String iconName, int width, int height, Controller<Colour> colour) {
		this((component, defaultName) -> iconName, width, height, colour);
	}

	public ScaledIconComponent(Controller<String> iconName, int width, int height, Controller<Colour> colour) {
		super(colour);
		this.iconName = iconName;
		this.width = width;
		this.height = height;
	}

	public void renderFallback(ComponentRenderInfo info) {
	}

	public boolean useFallback() {
		return false;
	}

	@Override
	public void render(ComponentRenderInfo info) {
		if(useFallback() && !AngelClientMod.instance.roundedUI) {
			renderFallback(info);
		}
		else {
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();

			getColour().bind();

			mc.getTextureManager().bindTexture(new ResourceLocation(
					"textures/gui/" + iconName.get(this, "angel_client_confusion") + "_" + Utils.getTextureScale() + ".png"));
			Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
		}

		super.render(info);
	}

	@Override
	protected Rectangle getDefaultBounds() {
		return new Rectangle(0, 0, width, height);
	}

}
