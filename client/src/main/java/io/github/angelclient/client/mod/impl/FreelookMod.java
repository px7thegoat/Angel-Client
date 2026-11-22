package io.github.angelclient.client.mod.impl;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.*;
import io.github.angelclient.client.event.impl.CameraRotateEvent;
import io.github.angelclient.client.event.impl.PlayerHeadRotateEvent;
import io.github.angelclient.client.event.impl.PreTickEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.util.Perspective;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class FreelookMod extends Mod {

	@Option
	private KeyBinding key = null;
	private float yaw;
	private float pitch;
	private int previousPerspective;
	private boolean active;
	private boolean keyWasDown = false; // controla se a tecla já estava pressionada

	@Expose
	@Option
	private boolean toggleMode = false; // nova opção: modo toggle
	private boolean toggled = false;    // estado do toggle

	@Expose
	@Option
	private Perspective perspective = Perspective.THIRD_PERSON_BACK;
	@Expose
	@Option
	private boolean invertPitch;
	@Expose
	@Option
	private boolean invertYaw;

	// Instância estática para o MixinRenderGlobal acessar diretamente
	public static FreelookMod INSTANCE;

	@Override
	public String getId() {
		return "freelook";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.UTILITY;
	}

	@Override
	public void onRegister() {
		super.onRegister();
		INSTANCE = this;
		if (key == null)
			key = new KeyBinding(getTranslationKey() + ".key", Keyboard.KEY_V, Client.KEY_CATEGORY);
		Client.INSTANCE.registerKeyBinding(key);
	}

	@EventHandler
	public void onTick(PreTickEvent event) {
		boolean keyIsDown = key.isKeyDown();

		if(toggleMode) {
			// Detecta borda de subida (tecla acabou de ser pressionada)
			if(keyIsDown && !keyWasDown) {
				toggled = !toggled;
				if(toggled) {
					start();
				} else {
					stop();
				}
			}
		} else {
			// Comportamento original: segura a tecla
			if(keyIsDown) {
				if(!hasStarted()) {
					start();
				}
			} else {
				if(hasStarted()) {
					stop();
				}
			}
		}

		keyWasDown = keyIsDown;
	}

	public boolean hasStarted() {
		return active;
	}

	public void start() {
		active = true;
		previousPerspective = mc.gameSettings.thirdPersonView;
		mc.gameSettings.thirdPersonView = perspective.ordinal();
		Entity renderView = mc.getRenderViewEntity();
		yaw = renderView.rotationYaw;
		pitch = renderView.rotationPitch;
	}

	public void stop() {
		active = false;
		toggled = false;
		mc.gameSettings.thirdPersonView = previousPerspective;
		mc.renderGlobal.setDisplayListEntitiesDirty();
	}

	public float getYaw() {
		return yaw;
	}

	public float getPitch() {
		return pitch;
	}

	@EventHandler
	public void setAngles(CameraRotateEvent event) {
		if(active) {
			event.yaw = yaw;
			event.pitch = pitch;
		}
	}

	@EventHandler
	public void setAngles(PlayerHeadRotateEvent event) {
		if(active) {
			float yaw = event.yaw;
			float pitch = event.pitch;
			event.cancelled = true;
			if(!invertPitch) pitch = -pitch;
			if(invertYaw) yaw = -yaw;
			this.yaw += yaw * 0.15F;
			this.pitch = MathHelper.clamp_float(this.pitch + (pitch * 0.15F), -90, 90);
			mc.renderGlobal.setDisplayListEntitiesDirty();
		}
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

}