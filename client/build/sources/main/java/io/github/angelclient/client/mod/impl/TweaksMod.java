package io.github.angelclient.client.mod.impl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.FullscreenToggleEvent;
import io.github.angelclient.client.event.impl.GammaEvent;
import io.github.angelclient.client.event.impl.PreRenderTickEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.event.impl.PreTickEvent;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import io.github.angelclient.client.util.access.AccessMinecraft;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.util.MouseHelper;
import io.github.angelclient.client.util.RawMouseHelper;

public class TweaksMod extends Mod {

	public static boolean enabled;
	public static TweaksMod instance;

	@Expose
	@Option
	private boolean fullbright;
	@Option
	@Expose
	public boolean mouseDelayFix = true;
	@Expose
	@Option
	public boolean arabicNumerals;
	@Expose
	@Option
	public boolean betterTooltips = true;
	@Expose
	@Option
	public boolean minimalViewBobbing;
	@Expose
	@Option
	public boolean rawMouseInput;
	@Expose
	@Option
	public boolean confirmDisconnect;
	@Expose
	@Option
	public boolean betterKeyBindings = true;
	@Expose
	@Option
	public boolean disableHotbarScrolling;
	@Expose
	@Option
	private boolean borderlessFullscreen;
	@Expose
	@Option
	public boolean centredInventory = true;
	@Expose
	@Option
	public boolean showInventoryPotionEffects = false;
	@Option
	private final KeyBinding dropStackKey = new KeyBinding(getTranslationKey() + ".drop_stack_key", Keyboard.KEY_NONE, Client.KEY_CATEGORY);

	private Rectangle previousBounds;
	private long fullscreenTime = -1;

	@Override
	public String getId() {
		return "tweaks";
	}

	@Override
	public String getName() {
		return "Options";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.GENERAL;
	}

	@Override
	public void onRegister() {
		super.onRegister();
		instance = this;
		Client.INSTANCE.registerKeyBinding(dropStackKey);
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		enabled = true;
		if(borderlessFullscreen && mc.isFullScreen()) {
			setBorderlessFullscreen(true);
		}
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		enabled = false;
		if(borderlessFullscreen && mc.isFullScreen()) {
			setBorderlessFullscreen(false);
			mc.toggleFullscreen();
			mc.toggleFullscreen();
		}
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return true;
	}

	@Override
	public void postOptionChange(String key, Object value) {
		if(isEnabled() && key.equals("borderlessFullscreen")) {
			if(mc.isFullScreen()) {
				if((boolean) value) {
					setBorderlessFullscreen(true);
				}
				else {
					setBorderlessFullscreen(false);
					mc.toggleFullscreen();
					mc.toggleFullscreen();
				}
			}
		}

		if (key.equals("rawMouseInput")) {
			updateMouseHelper((boolean) value);
		}
	}

	private void updateMouseHelper(boolean raw) {
		if (mc == null) return;

		if (raw) {
			mc.mouseHelper = new RawMouseHelper();
		} else {
			mc.mouseHelper = new MouseHelper();
		}
		
		// Re-grab the cursor to apply changes
		if (mc.inGameHasFocus) {
			mc.mouseHelper.grabMouseCursor();
		}
	}

	@EventHandler
	public void onGamma(GammaEvent event) {
		if(fullbright) {
			event.gamma = 20F;
		}
	}

	@EventHandler
	public void onFullscreenToggle(FullscreenToggleEvent event) {
		if(borderlessFullscreen) {
			event.applyState = false;
			setBorderlessFullscreen(event.state);
		}
	}

	@EventHandler
	public void onRender(PreRenderTickEvent event) {
		if(fullscreenTime != -1
				&& System.currentTimeMillis() - fullscreenTime >= 100) {
			fullscreenTime = -1;
			if(mc.inGameHasFocus) {
				mc.mouseHelper.grabMouseCursor();
			}
		}
	}

	private boolean dropKeyWasDown = false;

	@EventHandler
	public void onDropStackTick(PreTickEvent event) {
		if(!isEnabled()) return;
		if(dropStackKey.getKeyCode() == Keyboard.KEY_NONE) return;
		boolean dropKeyIsDown = dropStackKey.isKeyDown();
		if(dropKeyIsDown && !dropKeyWasDown) {
			if(mc.thePlayer == null || mc.thePlayer.inventory == null) return;
			int savedSlot = mc.thePlayer.inventory.currentItem;
			for(int i = 0; i < 9; i++) {
				mc.thePlayer.inventory.currentItem = i;
				mc.thePlayer.dropOneItem(true);
			}
			mc.thePlayer.inventory.currentItem = savedSlot;
		}
		dropKeyWasDown = dropKeyIsDown;
	}

	private void setBorderlessFullscreen(boolean state) {
		try {
			System.setProperty("org.lwjgl.opengl.Window.undecorated", Boolean.toString(state));
			Display.setFullscreen(false);
			Display.setResizable(!state);

			if(state) {
				previousBounds = new Rectangle(Display.getX(), Display.getY(), mc.displayWidth, mc.displayHeight);

				Display.setDisplayMode(new DisplayMode(Display.getDesktopDisplayMode().getWidth(), Display.getDesktopDisplayMode().getHeight()));
				Display.setLocation(0, 0);
				AccessMinecraft.getInstance().resizeWindow(Display.getDesktopDisplayMode().getWidth(), Display.getDesktopDisplayMode().getHeight());
			}
			else {
				Display.setDisplayMode(new DisplayMode(previousBounds.getWidth(), previousBounds.getHeight()));
				Display.setLocation(previousBounds.getX(), previousBounds.getY());
				AccessMinecraft.getInstance().resizeWindow(previousBounds.getWidth(), previousBounds.getHeight());

				if(mc.inGameHasFocus) {
					mc.mouseHelper.ungrabMouseCursor();
					fullscreenTime = System.currentTimeMillis();
				}
			}
		}
		catch(LWJGLException error) {
			logger.error("Could not totggle borderless fullscreen", error);
		}
	}

}