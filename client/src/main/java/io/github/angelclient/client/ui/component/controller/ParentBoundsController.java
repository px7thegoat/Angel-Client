package io.github.angelclient.client.ui.component.controller;

import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.util.data.Rectangle;

public class ParentBoundsController implements Controller<Rectangle> {

	@Override
	public Rectangle get(Component component, Rectangle defaultBounds) {
		return component.getParent().getBounds();
	}

}
