package io.github.angelclient.client.mod.impl.damageshake;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;

public class DamageShakeMod extends Mod {

	public static DamageShakeMod instance;

	@Expose
	@Option
	@Slider(min = 0, max = 100, step = 1, format = "angel_client.slider.percent")
	private float intensity = 100;

	@Override
	public String getId() {
		return "damage_shake";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.VISUAL;
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

	@Override
	public void onRegister() {
		super.onRegister();
		instance = this;
	}

	/**
	 * Returns intensity as a multiplier between 0.0 and 1.0.
	 * 0 = no shake, 1 = full vanilla shake.
	 */
	public float getIntensity() {
		return intensity / 100f;
	}

}