package io.github.angelclient.client.ui.component.impl;

import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.Controller;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;

public class BlockComponent extends ColouredComponent {

	public BlockComponent(Colour colour) {
		super((component, defaultColour) -> colour);
	}

	public BlockComponent(Controller<Colour> colour) {
		super(colour);
	}

	@Override
	public void render(ComponentRenderInfo info) {
		super.render(info);

		Rectangle.ofDimensions(getBounds().getWidth(), getBounds().getHeight()).fill(getColour());
	}

}
