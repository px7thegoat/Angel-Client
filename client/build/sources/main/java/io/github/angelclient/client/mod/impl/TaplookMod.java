package io.github.angelclient.client.mod.impl;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.*;
import io.github.angelclient.client.event.impl.PreTickEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.util.Perspective;
import net.minecraft.client.settings.KeyBinding;

public class TaplookMod extends Mod {

	@Option
	private KeyBinding key = null;
	private int previousPerspective;
	private boolean active;
	@Expose
	@Option
	private Perspective perspective = Perspective.THIRD_PERSON_BACK;

	@Override
	public String getId() {
		return "taplook";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.UTILITY;
	}

	@Override
	public void onRegister() {
		super.onRegister();
		if (key == null)
			key = new KeyBinding(getTranslationKey() + ".key", 0, Client.KEY_CATEGORY);
		Client.INSTANCE.registerKeyBinding(key);
	}

	@EventHandler
	public void onTick(PreTickEvent event) {
		if(key.isKeyDown()) {
			if(!active) {
				start();
			}
		}
		else if(active) {
			stop();
		}
	}

	public void start() {
		active = true;
		previousPerspective = mc.gameSettings.thirdPersonView;
		mc.gameSettings.thirdPersonView = perspective.ordinal();
		mc.renderGlobal.setDisplayListEntitiesDirty();
	}

	public void stop() {
		active = false;
		mc.gameSettings.thirdPersonView = previousPerspective;
		mc.renderGlobal.setDisplayListEntitiesDirty();
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

}