package io.github.angelclient.client.ui.component.impl;

import java.util.function.Consumer;

import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.ComponentRenderInfo;
import io.github.angelclient.client.ui.component.controller.AnimatedColourController;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Colour;

public class TickboxComponent extends ScaledIconComponent {

	private boolean value;
	private final Consumer<Boolean> booleanConsumer;
	private final Component hoverController;

	public TickboxComponent(boolean value, Consumer<Boolean> booleanConsumer, Component hoverController) {
		super("angel_client_tickbox", 16, 16,
				new AnimatedColourController((component, defaultColour) -> Colour.WHITE));

		this.value = value;
		this.booleanConsumer = booleanConsumer;
		this.hoverController = hoverController;

		hoverController.onClick((info, button) -> {
			if(!super.isHovered() && button == 0) {
				mouseClicked(info, button);
			}

			return true;
		});

		add(new ScaledIconComponent("angel_client_small_tick", 16, 16,
				new AnimatedColourController((component, defaultColour) -> this.value
						? Colour.WHITE
						: Colour.TRANSPARENT)),
				(component, defaultBounds) -> defaultBounds);
	}

	@Override
	public boolean isHovered() {
		return hoverController.isHovered();
	}

	@Override
	public boolean mouseClicked(ComponentRenderInfo info, int button) {
		if(button != 0) {
			return false;
		}

		Utils.playClickSound(true);
		value = !value;
		booleanConsumer.accept(value);

		return true;
	}

	@Override
	public boolean useFallback() {
		return true;
	}

	@Override
	public void renderFallback(ComponentRenderInfo info) {
		Utils.drawOutline(getRelativeBounds(), getColour());
	}

}