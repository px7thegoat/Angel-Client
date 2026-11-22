package io.github.angelclient.client.mod.impl.quickplay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.DetectedServer;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PreTickEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.impl.quickplay.database.QuickPlayDatabase;
import io.github.angelclient.client.mod.impl.quickplay.database.QuickPlayGame;
import io.github.angelclient.client.mod.impl.quickplay.database.QuickPlayGameMode;
import io.github.angelclient.client.mod.impl.quickplay.ui.QuickPlayOption;
import io.github.angelclient.client.mod.impl.quickplay.ui.QuickPlayPalette;
import io.github.angelclient.client.util.Utils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;

public class QuickPlayMod extends Mod {

	@Option
	private KeyBinding menuKey = null;
	private boolean got;
	private QuickPlayDatabase database;
	@Expose
	private final List<String> recentlyPlayed = new ArrayList<>();

	@Override
	public void onRegister() {
		super.onRegister();
		if (menuKey == null)
			menuKey = new KeyBinding(getTranslationKey() + ".key", Keyboard.KEY_M, Client.KEY_CATEGORY);
		Client.INSTANCE.registerKeyBinding(menuKey);
	}

	@Override
	public String getId() {
		return "quickplay";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.INTEGRATION;
	}

	public List<QuickPlayOption> getRecentlyPlayed() {
		return recentlyPlayed.stream().map((fullId) -> database.getGame(fullId.substring(0, fullId.indexOf('.')))
						.getMode(fullId.substring(fullId.indexOf('.') + 1)))
				.collect(Collectors.toList());
	}

	public List<QuickPlayOption> getGameOptions() {
		return new ArrayList<>(database.getGames().values());
	}

	public List<QuickPlayGame> getGames() {
		return new ArrayList<>(database.getGames().values());
	}

	@EventHandler
	public void onTick(PreTickEvent event) {
		if(database != null && menuKey.isPressed()
				&& Client.INSTANCE.detectedServer == DetectedServer.HYPIXEL) {
			mc.displayGuiScreen(new QuickPlayPalette(this));
		}
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		if(!got) {
			got = true;
			Utils.MAIN_EXECUTOR.submit(() -> {
				database = new QuickPlayDatabase();
			});
		}
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

	public void playGame(QuickPlayGameMode mode) {
		mc.thePlayer.sendChatMessage(mode.getCommand());

		if(!GuiScreen.isShiftKeyDown()) {
			mc.displayGuiScreen(null);
		}

		recentlyPlayed.removeIf(mode.getFullId()::equals);
		recentlyPlayed.add(0, mode.getFullId());

		if(recentlyPlayed.size() > 15) {
			recentlyPlayed.remove(recentlyPlayed.size() - 1);
		}

		Client.INSTANCE.save();
	}

}