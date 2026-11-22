package io.github.angelclient.client.ui.component.controller;

import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.impl.ButtonComponent;
import io.github.angelclient.client.ui.component.impl.LabelComponent;
import io.github.angelclient.client.util.data.Alignment;
import io.github.angelclient.client.util.data.Rectangle;

public class AlignedBoundsController implements Controller<Rectangle> {

	private final Alignment xAlignment;
	private final Alignment yAlignment;
	private final Controller<Rectangle> baseController;

	public AlignedBoundsController(Alignment xAlignment, Alignment yAlignment, Controller<Rectangle> baseController) {
		this.xAlignment = xAlignment;
		this.yAlignment = yAlignment;
		this.baseController = baseController;
	}

	public AlignedBoundsController(Alignment xAlignment, Alignment yAlignment) {
		this(xAlignment, yAlignment, (component, defaultBounds) -> defaultBounds);
	}

	@Override
	public Rectangle get(Component component, Rectangle defaultBounds) {
		return baseController.get(component,
				new Rectangle(xAlignment.getPosition(component.getParent().getBounds().getWidth(), defaultBounds.getWidth()),
						yAlignment.getPosition(component.getParent().getBounds().getHeight(), defaultBounds.getHeight()),
						defaultBounds.getWidth(), defaultBounds.getHeight()));
	}

}
