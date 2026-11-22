package io.github.angelclient.client;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.logisticscraft.occlusionculling.DataProvider;
import com.logisticscraft.occlusionculling.OcclusionCullingInstance;

import io.github.angelclient.client.api.ClientApi;
import io.github.angelclient.client.util.nanovg.NanoVGManager;
import io.github.angelclient.client.api.Popup;
import io.github.angelclient.client.api.PopupManager;
import io.github.angelclient.client.config.ConfigVersion;
import io.github.angelclient.client.cosmetic.LocalCosmeticManager;
import io.github.angelclient.client.cosmetic.MySQLManager;
import io.github.angelclient.client.mod.impl.NametagsMod;
import io.github.angelclient.client.cosmetic.CosmeticManager;
import io.github.angelclient.client.culling.CullTask;
import io.github.angelclient.client.event.EventBus;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.GameQuitEvent;
import io.github.angelclient.client.event.impl.PostGameStartEvent;
import io.github.angelclient.client.event.impl.PreTickEvent;
import keystrokesmod.event.TickEvent;
import net.lenni0451.asmevents.EventManager;
import net.lenni0451.asmevents.event.enums.EnumEventType;
import io.github.angelclient.client.event.impl.SendChatMessageEvent;
import io.github.angelclient.client.event.impl.ServerConnectEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.hud.HudElement;
import io.github.angelclient.client.mod.impl.BlockSelectionMod;
import io.github.angelclient.client.mod.impl.ChunkAnimatorMod;
import io.github.angelclient.client.mod.impl.ColourSaturationMod;
import io.github.angelclient.client.mod.impl.FastRecraftMod;
import io.github.angelclient.client.mod.impl.FreelookMod;
import io.github.angelclient.client.mod.impl.GuiScaleMod;
import io.github.angelclient.client.mod.impl.HitColourMod;
import io.github.angelclient.client.mod.impl.HitboxMod;
import io.github.angelclient.client.mod.impl.ItemScaleMod;
import io.github.angelclient.client.mod.impl.MenuBlurMod;
import io.github.angelclient.client.mod.impl.MotionBlurMod;
import io.github.angelclient.client.mod.impl.PackDisplayMod;
import io.github.angelclient.client.mod.impl.ParticlesMod;
import io.github.angelclient.client.mod.impl.PerformanceMod;
import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.mod.impl.TNTTimerMod;
import io.github.angelclient.client.mod.impl.TaplookMod;
import io.github.angelclient.client.mod.impl.TimeChangerMod;
import io.github.angelclient.client.mod.impl.TitlePositionMod;
import io.github.angelclient.client.mod.impl.TweaksMod;
import io.github.angelclient.client.mod.impl.damageshake.DamageShakeMod;
import io.github.angelclient.client.mod.impl.V1_7VisualsMod;
import io.github.angelclient.client.mod.impl.ZoomMod;
import io.github.angelclient.client.mod.impl.HitDelayFixMod;
import io.github.angelclient.client.mod.impl.MushStatsMod;
import io.github.angelclient.client.mod.impl.WaveyCapesMod;
import io.github.angelclient.client.mod.impl.NametagsMod;
import io.github.angelclient.client.mod.impl.autotext.AutoTextMod;
import io.github.angelclient.client.mod.impl.bigdrops.BigDropsMod;
import io.github.angelclient.client.mod.impl.hud.BossBarMod;
import io.github.angelclient.client.mod.impl.hud.ComboCounterMod;
import io.github.angelclient.client.mod.impl.hud.CoordinatesMod;
import io.github.angelclient.client.mod.impl.hud.CpsMod;
import io.github.angelclient.client.mod.impl.hud.FpsMod;
import io.github.angelclient.client.mod.impl.hud.IpHudMod;
import io.github.angelclient.client.mod.impl.hud.PotionEffectsMod;
import io.github.angelclient.client.mod.impl.hud.ReachDisplayMod;
import io.github.angelclient.client.mod.impl.hud.ScoreboardMod;
import io.github.angelclient.client.mod.impl.hud.armour.ArmourMod;
import io.github.angelclient.client.mod.impl.hud.chat.ChatMod;
import io.github.angelclient.client.mod.impl.hud.crosshair.CrosshairMod;
import io.github.angelclient.client.mod.impl.hud.keystrokes.KeystrokesMod;
import io.github.angelclient.client.mod.impl.hud.ping.PingMod;
import io.github.angelclient.client.mod.impl.hud.speedometer.SpeedometerMod;
import io.github.angelclient.client.mod.impl.hud.tablist.TabListMod;
import io.github.angelclient.client.mod.impl.hud.timers.TimersMod;
import io.github.angelclient.client.mod.impl.hypixeladditions.HypixelAdditionsMod;
import io.github.angelclient.client.mod.impl.itemphysics.ItemPhysicsMod;
import io.github.angelclient.client.mod.impl.quickplay.QuickPlayMod;
import io.github.angelclient.client.mod.impl.togglesprint.ToggleSprintMod;
import io.github.angelclient.client.nametag.NametagManager;
import io.github.angelclient.client.ui.ChatButton;
import io.github.angelclient.client.ui.screen.HubScreen;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;
import io.github.angelclient.client.ui.screen.mods.MoveHudsScreen;

import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.update.AutoUpdater;
import io.github.angelclient.client.util.access.AccessMinecraft;
import io.github.angelclient.client.util.font.SlickFontRenderer;
import keystrokesmod.Raven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

/**
 * Main class for Angel Client.
 */
public class Client {

	public static final Client INSTANCE = new Client();

	private final Minecraft mc = Minecraft.getMinecraft();
	private JsonObject data;
	private List<Mod> mods = new ArrayList<Mod>();
	private Map<String, Mod> modsById = new HashMap<>();
	private List<HudElement> huds = new ArrayList<HudElement>();
	public static final Logger LOGGER = LogManager.getLogger();

	private final File DATA_FILE = new File(mc.mcDataDir, "angel_client_mods.json");
	// data file for beta versions - this is no longer very neccessary.
	private final File LEGACY_DATA_FILE = new File(mc.mcDataDir, "parrot_client_mods.json");

	public DetectedServer detectedServer;

	public EventBus bus = new EventBus();

	private Map<ResourceLocation, IResource> resources = new HashMap<>();
	private Map<String, CommandBase> commands = new HashMap<>();
	private List<ChatButton> chatButtons = new ArrayList<>();

	private ChatChannelSystem chatChannelSystem;

	public static String VERSION = System.getProperty("io.github.angelclient.client.version", "DEVELOPMENT TEST");
	public static String NAME = "Angel Client";
	public static final String KEY_TRANSLATION_KEY = "angel_client.key";
	public static final String KEY_CATEGORY = KEY_TRANSLATION_KEY + ".category";
	public static final boolean DEV = isDevelopment();

	private PopupManager popupManager;
	private GuiMainMenu mainMenu;

	public List<Mod> getMods() { return mods; }
	public List<HudElement> getHuds() { return huds; }
	public PopupManager getPopupManager() { return popupManager; }
	public GuiMainMenu getMainMenu() { return mainMenu; }
	public void setMainMenu(GuiMainMenu mainMenu) { this.mainMenu = mainMenu; }

	public void init() {
		try {
			io.github.angelclient.client.mushstats.MushMcStats.getInstance().init();
		} catch (Exception e) {
			// ignored
		}

		Utils.resetLineWidth();

		// NOTA: NanoVGManager.createContext() foi movido para onPostStart(),
		// pois o contexto OpenGL só está garantidamente ativo após o game start.
		new File(mc.mcDataDir, "server-resource-packs").mkdirs(); // Fix crash
		System.setProperty("http.agent", "Angel Client/" + Client.VERSION);

		// Inicia os managers em background — sem bloquear a render thread
		CosmeticManager.getInstance().loadCosmetics();
		NametagManager.getInstance();
		LocalCosmeticManager.getInstance();
		AutoUpdater.checkForUpdates();

		LOGGER.info("Initialising...");
		bus.register(this);

		CpsMonitor.forceInit();

		LOGGER.info("Loading settings...");

		if(!DATA_FILE.exists() && LEGACY_DATA_FILE.exists()) {
			LEGACY_DATA_FILE.renameTo(DATA_FILE);
		}

		load();

		LOGGER.info("Loading mods...");

		bulkApplying = true; // suprime saves durante o registro inicial
		register(new AngelClientMod());
		register(new FpsMod());
		register(new CoordinatesMod());
		register(new KeystrokesMod());
		register(new CpsMod());
		register(new PingMod());
		register(new SpeedometerMod());
		register(new ReachDisplayMod());
		register(new ComboCounterMod());
		register(new PotionEffectsMod());
		register(new ArmourMod());
		register(new TimersMod());
		register(new ChatMod());
		register(new BossBarMod());
		register(new TabListMod());
		register(new CrosshairMod());
		register(new ScoreboardMod());
		register(new TweaksMod());
		register(new DamageShakeMod());
		register(new MotionBlurMod());
		register(new MenuBlurMod());
		register(new ColourSaturationMod());
		register(new ChunkAnimatorMod());
		register(new FreelookMod());
		register(new TaplookMod());
		register(new ToggleSprintMod());
		register(new TNTTimerMod());
		register(new V1_7VisualsMod());
		register(new ItemPhysicsMod());
		register(new BigDropsMod());
		register(new ZoomMod());
		register(new ParticlesMod());
		register(new TimeChangerMod());
		register(new BlockSelectionMod());
		register(new HitboxMod());
		register(new HitDelayFixMod());
		register(new NametagsMod());
		register(new HitColourMod());
		register(new HypixelAdditionsMod());
		register(new QuickPlayMod());
		register(new GuiScaleMod());
		register(new FastRecraftMod());
		register(new ItemScaleMod());
		register(new IpHudMod());
		register(new TitlePositionMod());
		register(new PackDisplayMod());
		register(new PerformanceMod());
		register(new MushStatsMod());
		register(new AutoTextMod());
		register(new WaveyCapesMod());

		LOGGER.info("Initialising Raven B++ modules...");
		try {
			new Raven().init();
			LOGGER.info("Raven B++ initialized with {} modules", keystrokesmod.manager.ModuleManager.modules.size());
		} catch (Throwable e) {
			LOGGER.error("Failed to initialize Raven B++", e);
		}

		LOGGER.info("Loaded {} mods", mods.size());

		bulkApplying = false; // reativa saves normais
		LOGGER.info("Saving settings...");
		save();

		LOGGER.info("Starting culling thread...");

		CullTask cullingTask = new CullTask(new OcclusionCullingInstance(128, new DataProvider() {

			private WorldClient world;

			@Override
			public boolean prepareChunk(int x, int z) {
				return (world = mc.theWorld) != null;
			}

			@Override
			public boolean isOpaqueFullCube(int x, int y, int z) {
				return world.isBlockNormalCube(new BlockPos(x, y, z), false);
			}

		}));

		try {
			// Group together the mod file listener and culling thread
			// as it makes sense considering both tasks can deal with a 10ms pause,
			// and file listeners will not take much time.

			FilePollingTask filePolling = new FilePollingTask(mods);

			Thread generalUpdateThread = new Thread(() -> {
				while(((AccessMinecraft) mc).isRunning()) {
					try {
						Thread.sleep(10);
					}
					catch(InterruptedException error) {
						return;
					}

					if (io.github.angelclient.client.mod.impl.PerformanceMod.instance == null 
    		|| io.github.angelclient.client.mod.impl.PerformanceMod.instance.isEnabled()
    	) {

    cullingTask.run();
}

					if(filePolling != null) {
						filePolling.run();
					}
				}

				filePolling.close();
			}, "Async Updates");
			generalUpdateThread.setDaemon(true);
			generalUpdateThread.setUncaughtExceptionHandler((thread, error) -> {
				LOGGER.error("Async updates has crashed", error);
			});
			generalUpdateThread.start();
		}
		catch(IOException error) {
			LOGGER.error("Could not start async updates thread", error);
		}

		bus.register(new ClientApi());
		bus.register(popupManager = new PopupManager());
	}

	private static boolean isDevelopment() {
		for(StackTraceElement element : Thread.currentThread().getStackTrace()) {
			if(element.getClassName().equals("GradleStart")) {
				return true;
			}
		}

		return false;
	}

	public void registerKeyBinding(KeyBinding keyBinding) {
		mc.gameSettings.keyBindings = ArrayUtils.add(mc.gameSettings.keyBindings, keyBinding);
	}

	public void unregisterKeyBinding(KeyBinding keyBinding) {
		mc.gameSettings.keyBindings = ArrayUtils.removeElement(mc.gameSettings.keyBindings, keyBinding);
		keyBinding.setKeyCode(0);
	}

	private Gson getGson(Mod mod) {
		GsonBuilder builder = new GsonBuilder();
		if(mod != null) {
			builder.registerTypeAdapter(mod.getClass(), (InstanceCreator<Mod>) (type) -> mod);
		}
		return builder.excludeFieldsWithoutExposeAnnotation().create();
	}

	private void cacheHudList() {
		huds.clear();
		for(Mod mod : mods) {
			huds.addAll(mod.getHudElements());
		}
	}

	@SuppressWarnings("deprecation")
	public boolean load() {
		try {
			if(DATA_FILE.exists()) {
				// 1.8 uses old libraries, so this warning cannot be easily fixed.
				data = new JsonParser().parse(FileUtils.readFileToString(DATA_FILE)).getAsJsonObject();
				data = ConfigVersion.migrate(data);
			}
			else {
				data = new JsonObject();
				data.addProperty("version", ConfigVersion.values()[ConfigVersion.values().length - 1].name());
			}

			// Carrega configurações do MySQL e faz merge com dados locais
			try {
				String player = mc.getSession().getUsername();
				if (player != null && !player.isEmpty()) {
					String remoteData = MySQLManager.getInstance().loadSettings(player);
					if (remoteData != null && !remoteData.isEmpty()) {
						JsonObject remoteJson = new JsonParser().parse(remoteData).getAsJsonObject();
						// Mescla: dados remotos sobrescrevem locais para cada mod
						for (Map.Entry<String, com.google.gson.JsonElement> e : remoteJson.entrySet()) {
							data.add(e.getKey(), e.getValue());
						}
					}
				}
			} catch (Exception ignored) {}

			return true;
		}
		catch(IOException error) {
			LOGGER.error("Could not load data", error);
			data = new JsonObject();
			return false;
		}
	}

	/** Suprime saves intermediários durante applyToMods() em lote. */
	private boolean bulkApplying = false;

	/**
	 * Aplica os dados do JSON já carregado em data para todos os mods registrados,
	 * sem precisar reiniciar o jogo. Chamado após loadProfile().
	 */
	public void applyToMods() {
		bulkApplying = true; // suprime optionChanged() durante o loop
		try {
			for (Mod mod : mods) {
				if (!data.has(mod.getId())) continue;
				try {
					boolean wasEnabled = mod.isEnabled();
					if (wasEnabled) mod.setEnabled(false);

					getGson(mod).fromJson(data.get(mod.getId()), mod.getClass());

					boolean newEnabled = mod.isEnabled();
					mod.setEnabled(false);
					if (newEnabled) mod.setEnabled(true);

					mod.postOptionChange("*", null);
				} catch (Throwable e) {
					LOGGER.error("Could not apply profile data to mod " + mod.getId(), e);
				}
			}
		} finally {
			bulkApplying = false;
			// Um único save ao final da aplicação em lote
			save();
		}
	}

	public JsonObject getData() { return data; }

	public boolean save() {
		Gson gson = getGson(null);

		for(Mod mod : mods) {
			data.add(mod.getId(), gson.toJsonTree(mod));
		}

		try {
			FileUtils.writeStringToFile(DATA_FILE, gson.toJson(data));
			syncSettingsToMySQL();
			return true;
		}
		catch(IOException error) {
			LOGGER.error("Could not save data", error);
			return false;
		}
	}

	private void syncSettingsToMySQL() {
		try {
			String player = mc.getSession().getUsername();
			if (player != null && !player.isEmpty()) {
				MySQLManager.getInstance().saveSettings(player, data.toString());
			}
		} catch (Exception e) {
			LOGGER.error("Could not sync mod settings to MySQL", e);
		}
	}

	private void register(Mod mod) {
		try {
			if(data.has(mod.getId())) {
				getGson(mod).fromJson(data.get(mod.getId()), mod.getClass());
			}
			mods.add(mod);

			modsById.put(mod.getId(), mod);

			mod.onRegister();
		}
		catch(Throwable error) {
			LOGGER.error("Could not register mod " + mod.getId(), error);
			mods.remove(mod);
		}
	}

	public Mod getModById(String id) {
		return modsById.get(id);
	}

	public void addResource(ResourceLocation location, IResource resource) {
		resources.put(location, resource);
	}

	public IResource getResource(ResourceLocation location) {
		return resources.get(location);
	}

	public CommandBase getCommand(String name) {
		return commands.get(name);
	}

	public Set<String> getCommandNames() {
		return commands.keySet();
	}

	public CommandBase registerCommand(String name, CommandBase command) {
		commands.put(name, command);
		for(String alias : command.getCommandAliases()) {
			commands.put(alias, command);
		}

		return command;
	}

	public CommandBase unregisterCommand(String name) {
		CommandBase command;

		for(String alias : (command = commands.remove(name)).getCommandAliases()) {
			commands.remove(alias);
		}

		return command;
	}

	public void setChatChannelSystem(ChatChannelSystem chatChannelSystem) {
		this.chatChannelSystem = chatChannelSystem;

		if(chatChannelSystem != null) {
			registerChatButton(ChatChannelSystem.ChatChannelButton.INSTANCE);
		}
		else {
			unregisterChatButton(ChatChannelSystem.ChatChannelButton.INSTANCE);
		}
	}

	public ChatChannelSystem getChatChannelSystem() {
		return chatChannelSystem;
	}

	@EventHandler
	public void onQuit(GameQuitEvent event) {
		// BUG FIX: GameQuitEvent estava importado mas sem handler.
		// O estado dos mods nunca era salvo ao fechar o jogo —
		// apenas o save() inicial (no final de init()) era executado,
		// antes de qualquer mudança do usuário.
		LOGGER.info("Saving on quit...");
		save();
		io.github.angelclient.client.cosmetic.CosmeticAPI.syncPlayerToMySQL();
		NanoVGManager.closeContext();
	}

	private boolean updateNotified = false;

	@EventHandler
	public void onPostStart(PostGameStartEvent event) {
		// Inicializa NanoVG AQUI — GL context já está ativo e estável neste ponto.
		// Antes (no init()) o contexto GL ainda não estava totalmente pronto,
		// causando nvgCreate retornar 0L silenciosamente → fallback sem bordas.
		try {
			NanoVGManager.createContext();
			LOGGER.info("NanoVG context created successfully.");
		} catch (Throwable e) {
			LOGGER.error("Failed to create NanoVG context, UI will render without rounded corners.", e);
		}

		mods.forEach(Mod::postStart);
		cacheHudList();

		notifyUpdate();

		try {
			unregisterKeyBinding((KeyBinding) GameSettings.class.getField("ofKeyBindZoom").get(mc.gameSettings));
		}
		catch(NoSuchFieldException | IllegalAccessException | ClassCastException ignored) {
			// OptiFine is not enabled.
		}
	}

	private boolean updateScreenShown;
	private boolean updateJustApplied;

	public void markUpdateJustApplied(String version) {
		updateJustApplied = true;
		updateScreenShown = false;
		AutoUpdater.setAppliedVersion(version);
	}

	public boolean isUpdateJustApplied() {
		return updateJustApplied;
	}

	private void notifyUpdate() {
		if (updateNotified) return;
		updateNotified = true;

		if (AutoUpdater.isUpdateApplied()) {
			updateJustApplied = true;
			System.out.println("[AngelClient] Update " + AutoUpdater.getAppliedVersion() + " downloaded. Restart to apply.");
		} else if (AutoUpdater.isUpdateAvailable()) {
			System.out.println("[AngelClient] Update " + AutoUpdater.getRemoteVersion() + " available.");
		}
	}

	public boolean shouldShowUpdateScreen() {
		if (AutoUpdater.isUpdateAvailable()) {
			if (!updateScreenShown) {
				updateScreenShown = true;
				return true;
			}
			return false;
		}
		if (updateJustApplied) {
			if (!updateScreenShown) {
				updateScreenShown = true;
				return true;
			}
			return false;
		}
		return false;
	}

	@EventHandler
	public void onSendMessage(SendChatMessageEvent event) {
		if (event.message.equals("/angel_update")) {
			event.cancelled = true;
			LOGGER.info("Downloading update...");
			if (mc.thePlayer != null) {
				mc.thePlayer.addChatMessage(new ChatComponentText(
					EnumChatFormatting.GREEN + "[Angel Client] " + EnumChatFormatting.RESET +
					"Downloading update " + AutoUpdater.getRemoteVersion() + "..."
				));
			}
			AutoUpdater.performUpdate();
			return;
		}

		if(event.message.startsWith("/")) {
			List<String> args = new ArrayList<>(Arrays.asList(event.message.split(" ")));
			String commandKey = args.get(0).substring(1);
			if(commands.containsKey(commandKey)) {
				event.cancelled = true;

				try {
					args.remove(0);

					commands.get(commandKey).processCommand(mc.thePlayer, args.toArray(new String[0]));
				}
				catch(CommandException error) {
					mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.RED + error.getMessage()));
				}
				catch(Exception error) {
					mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Could " +
							"not execute client-sided command, see log for details"));
					LOGGER.info("Could not execute client-sided command: " + event.message + ", error: ", error);
				}
			}
		}
		else if(getChatChannelSystem() != null) {
			event.cancelled = true;
			getChatChannelSystem().getChannel().sendMessage(mc.thePlayer, event.message);
		}
	}

	@EventHandler
	public void onTick(PreTickEvent event) {
		// Persiste mudanças pendentes quando o ModsScreen fecha
		flushPendingSave();

		notifyUpdate();

		EventManager.call(new TickEvent(EnumEventType.PRE));

        if(AngelClientMod.instance.modsKey.isPressed()) {
            if (!(mc.currentScreen instanceof HubScreen)) {
                mc.displayGuiScreen(new HubScreen());
            }
        }
		else if(AngelClientMod.instance.editHudKey.isPressed()) {
			mc.displayGuiScreen(new ModsScreen());
			mc.displayGuiScreen(new MoveHudsScreen());
		}


	}

	public void registerChatButton(ChatButton button) {
		chatButtons.add(button);
		chatButtons.sort(Comparator.comparingInt(ChatButton::getPriority));
	}

	public void unregisterChatButton(ChatButton button) {
		chatButtons.remove(button);
	}

	public void onServerChange(ServerData data) {
		setChatChannelSystem(null);

		if(data == null) {
			detectedServer = null;
			mods.forEach(Mod::unblock);
			io.github.angelclient.client.cosmetic.LocalCosmeticManager.getInstance().clearActiveAndRemote();
		}

		if(data != null) {
			for(DetectedServer server : DetectedServer.values()) {
				if(server.matches(data)) {
					detectedServer = server;
					mods.stream().filter(server::shouldBlockMod).forEach(Mod::block);
					break;
				}
			}
			io.github.angelclient.client.cosmetic.LocalCosmeticManager.getInstance().forceRefresh();
		}

		bus.post(new ServerConnectEvent(data, detectedServer));
	}

	public List<ChatButton> getChatButtons() {
		return chatButtons;
	}

	/**
	 * Salva se o ModsScreen não estiver aberto.
	 * Chamado por Mod.setEnabled() e ModOption.setValue() via optionChanged().
	 *
	 * BUG FIX: antes, optionChanged() existia mas NUNCA era invocado.
	 * Ativar/desativar um mod pela UI não persistia nada — as mudanças
	 * sumiam ao fechar o jogo. Agora Mod.setEnabled() e ModOption.setValue()
	 * devem chamar Client.INSTANCE.optionChanged() após toda mudança de estado.
	 *
	 * Para evitar escrever no disco a cada frame, o save só acontece quando
	 * o ModsScreen não está na tela (usuário fechou o menu) OU via saveDeferred()
	 * que agenda um save para o próximo tick fora do menu.
	 */
	public void optionChanged() {
		// Não salva durante applyToMods() — um save em lote será feito ao final
		if (bulkApplying) return;

		if (!(mc.currentScreen instanceof ModsScreen)) {
			save();
		} else {
			pendingSave = true;
		}
	}

	/** Flag: indica que há mudanças não salvas enquanto o ModsScreen está aberto. */
	private boolean pendingSave = false;

	/** Chamado no PreTickEvent para persistir mudanças pendentes ao fechar o menu. */
	private void flushPendingSave() {
		if (pendingSave && !(mc.currentScreen instanceof ModsScreen)) {
			pendingSave = false;
			save();
		}
	}

}