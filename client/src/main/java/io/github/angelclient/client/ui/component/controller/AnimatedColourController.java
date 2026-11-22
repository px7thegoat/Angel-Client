package io.github.angelclient.client.ui.component.controller;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.util.data.Colour;

public class AnimatedColourController implements Controller<Colour> {

	private final Controller<Colour> base;
	private final int duration;
	private Colour last;
	private long currentTime;
	private Colour current;

	public AnimatedColourController(Controller<Colour> base, int duration) {
		this.base = base;
		this.duration = duration;
	}

	public AnimatedColourController(Controller<Colour> base) {
		this(base, 200);
	}

	@Override
	public Colour get(Component component, Colour defaultValue) {
		Colour baseValue = base.get(component, defaultValue);

		if(!baseValue.equals(current)) {
			last = current;
			current = baseValue;
			currentTime = System.currentTimeMillis();
		}

		return animate(AngelClientMod.instance.smoothUIColours ? Math.max(0, (System.currentTimeMillis() - currentTime) / ((float) duration)) : 1);
	}

	private Colour animate(float progress) {
		if(last == null) {
			return current;
		}

		return last.lerp((Colour) current, progress);
	}

}
