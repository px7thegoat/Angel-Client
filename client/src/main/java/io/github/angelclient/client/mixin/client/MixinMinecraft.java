package io.github.angelclient.client.mixin.client;

import java.util.ConcurrentModificationException;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.FullscreenToggleEvent;
import io.github.angelclient.client.event.impl.GameQuitEvent;
import io.github.angelclient.client.event.impl.HitboxToggleEvent;
import io.github.angelclient.client.event.impl.InitialOpenGuiEvent;
import io.github.angelclient.client.event.impl.MouseClickEvent;
import io.github.angelclient.client.event.impl.OpenGuiEvent;
import io.github.angelclient.client.event.impl.PostGameStartEvent;
import io.github.angelclient.client.event.impl.PostRenderTickEvent;
import io.github.angelclient.client.event.impl.PostTickEvent;
import io.github.angelclient.client.event.impl.PreRenderTickEvent;
import io.github.angelclient.client.event.impl.PreTickEvent;
import keystrokesmod.event.Render2DEvent;
import keystrokesmod.event.Render3DEvent;
import net.lenni0451.asmevents.EventManager;
import io.github.angelclient.client.event.impl.ScrollEvent;
import io.github.angelclient.client.event.impl.WorldLoadEvent;
import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.mod.impl.TweaksMod;
import io.github.angelclient.client.mushstats.MushMcStats;
import io.github.angelclient.client.ui.component.Screen;
import io.github.angelclient.client.ui.screen.AngelClientMainMenu;
import io.github.angelclient.client.ui.screen.SplashScreen;
import io.github.angelclient.client.ui.screen.UpdateScreen;
import io.github.angelclient.client.util.update.AutoUpdater;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MouseHelper;
import io.github.angelclient.client.util.RawMouseHelper;
import io.github.angelclient.client.util.access.AccessGuiNewChat;
import io.github.angelclient.client.util.access.AccessMinecraft;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements AccessMinecraft {

	private boolean debugPressed;
	private boolean cancelDebug;


	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Session;getSessionID()" +
			"Ljava/lang/String;"))
	public String censorSessionId(Session instance) {
		return "☃︎";
	}

	@Redirect(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
			at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
	public void noneOfYourGarbage() {
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;initStream()V", shift = At.Shift.AFTER))
	public void init(CallbackInfo callback) {
		Client.INSTANCE.init();
		gameSettings.loadOptions();
		MushMcStats.getInstance().onKeyInput();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;<init>()V"))
	public void postStart(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new PostGameStartEvent());
	}

	@Inject(method = "runTick", at = @At("HEAD"))
	public void preRunTick(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new PreTickEvent());
	}

	@Inject(method = "runTick", at = @At("RETURN"))
	public void postRunTick(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new PostTickEvent());
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z"))
	public boolean next() {
		// BUGFIX: the original code called next() recursively to skip cancelled
		// events. If every queued mouse event is cancelled this recurses until the
		// stack is exhausted and the game crashes with StackOverflowError.
		// Replaced with an iterative loop that consumes events until it finds one
		// that is NOT cancelled (or until there are no more events).
		boolean next = Mouse.next();

		while(next && Mouse.getEventButtonState()) {
			if(!Client.INSTANCE.bus.post(new MouseClickEvent(Mouse.getEventButton())).cancelled) {
				break; // event accepted – hand it back to vanilla
			}
			// event cancelled – consume it and try the next one
			next = Mouse.next();
		}

		return next;
	}

	@Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer" +
			"/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.BEFORE))
	public void preRenderTick(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new PreRenderTickEvent());
		EventManager.call(new Render3DEvent(0.0f));
	}

	@Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer" +
			"/EntityRenderer;updateCameraAndRender(FJ)V", shift = At.Shift.AFTER))
	public void postRenderTick(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new PostRenderTickEvent());
		EventManager.call(new Render2DEvent(0.0f));
	}

	@Redirect(method = "rightClickMouse()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;getIsHittingBlock()Z"))
	public boolean allowRightClickWhileMining(PlayerControllerMP instance) {
		if (io.github.angelclient.client.mod.impl.V1_7VisualsMod.enabled && io.github.angelclient.client.mod.impl.V1_7VisualsMod.instance.useAndMine) {
			return false;
		}
		return instance.getIsHittingBlock();
	}

	@Overwrite
	public int getLimitFramerate() {
		if((theWorld == null && !(currentScreen instanceof Screen)) || !Display.isActive()) {
			return 30; 
		}

		return gameSettings.limitFramerate;
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;" +
			"isPressed()Z", ordinal = 2))
	public boolean cancelHotbarSwitch(KeyBinding instance) {
		return instance.isPressed();
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EffectRenderer;updateEffects()V"))
	public void concurrencyHack(EffectRenderer effectRenderer) {
		try {
			effectRenderer.updateEffects();
		}
		catch(NullPointerException | ConcurrentModificationException | ArrayIndexOutOfBoundsException ignored) {
		}
	}

	@Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
	public void loadWorld(WorldClient world, String message, CallbackInfo callback) {
		Client.INSTANCE.bus.post(new WorldLoadEvent(world));
	}

	@Inject(method = "displayGuiScreen", at = @At(value = "HEAD"), cancellable = true)
	public void openGui(GuiScreen screen, CallbackInfo callback) {
		if ((screen == null && theWorld == null) || screen instanceof GuiMainMenu) {
			if (Client.INSTANCE.shouldShowUpdateScreen()) {
				callback.cancel();
				if (screen == null) new GuiMainMenu();
				if (AutoUpdater.isUpdateApplied() || Client.INSTANCE.isUpdateJustApplied()) {
					((Minecraft)(Object)this).displayGuiScreen(new UpdateScreen(
						EnumChatFormatting.GREEN + "Update " + AutoUpdater.getAppliedVersion() + " downloaded!",
						"Restart the client to apply the update."
					));
				} else {
					((Minecraft)(Object)this).displayGuiScreen(new UpdateScreen(AutoUpdater.getRemoteVersion()));
				}
				return;
			}
		}

		if(((screen == null && theWorld == null) || screen instanceof GuiMainMenu) && AngelClientMod.instance.fancyMainMenu) {
			callback.cancel();

			if(screen == null) {
				new GuiMainMenu();
			}
			else {
				Client.INSTANCE.setMainMenu((GuiMainMenu) screen);
			}

			((Minecraft)(Object)this).displayGuiScreen(new AngelClientMainMenu());
			return;
		}
		else if(screen instanceof AngelClientMainMenu && !AngelClientMod.instance.fancyMainMenu) {
			callback.cancel();
			((Minecraft)(Object)this).displayGuiScreen(null);
			return;
		}

		Client.INSTANCE.bus.post(new OpenGuiEvent(screen));
		if(currentScreen == null && screen != null) {
			Client.INSTANCE.bus.post(new InitialOpenGuiEvent(screen));
		}
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
	public int onScroll() {
		int dWheel = Mouse.getEventDWheel();

		if(dWheel != 0) {
			if(Client.INSTANCE.bus.post(new ScrollEvent(dWheel)).cancelled) {
				dWheel = 0;
			}
		}

		// BUGFIX: playerController is null on the main menu / while loading a world.
		if(dWheel != 0 && playerController != null && !playerController.isSpectatorMode()
				&& TweaksMod.enabled && TweaksMod.instance.disableHotbarScrolling) {
			dWheel = 0;
		}

		return dWheel;
	}

	@Redirect(method = "createDisplay",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V"))
	public void setWindowTitle(String title) {
		Display.setTitle(io.github.angelclient.client.Client.NAME);
	}

	@Redirect(method = "createDisplay",
			at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;create(Lorg/lwjgl/opengl/PixelFormat;)V"))
	public void createDisplayWithStencil(PixelFormat format) throws LWJGLException {
		// Garante 8 bits de stencil buffer para permitir mascaramento arredondado nas GUIs
		Display.create(new PixelFormat().withStencilBits(8));
	}

	@Inject(method = "createDisplay", at = @At("RETURN"))
	public void setCustomIcon(CallbackInfo ci) {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			java.io.InputStream icon16  = cl.getResourceAsStream("assets/angel_client/textures/angelclient/icon_16.png");
			java.io.InputStream icon32  = cl.getResourceAsStream("assets/angel_client/textures/angelclient/icon_32.png");
			java.io.InputStream icon48  = cl.getResourceAsStream("assets/angel_client/textures/angelclient/icon_48.png");
			java.io.InputStream icon128 = cl.getResourceAsStream("assets/angel_client/textures/angelclient/icon_128.png");

			java.util.List<java.nio.ByteBuffer> icons = new java.util.ArrayList<>();
			if(icon16  != null) icons.add(loadIcon(icon16));
			if(icon32  != null) icons.add(loadIcon(icon32));
			if(icon48  != null) icons.add(loadIcon(icon48));
			if(icon128 != null) icons.add(loadIcon(icon128));

			if(!icons.isEmpty()) {
				Display.setIcon(icons.toArray(new java.nio.ByteBuffer[0]));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private java.nio.ByteBuffer loadIcon(java.io.InputStream stream) throws Exception {
		java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(stream);
		int[] pixels = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
		java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocateDirect(pixels.length * 4);
		for(int pixel : pixels) {
			buf.put((byte)((pixel >> 16) & 0xFF));
			buf.put((byte)((pixel >> 8) & 0xFF));
			buf.put((byte)(pixel & 0xFF));
			buf.put((byte)((pixel >> 24) & 0xFF));
		}
		return (java.nio.ByteBuffer) ((java.nio.Buffer) buf).flip();
	}

	@Inject(method = "setServerData", at = @At("TAIL"))
	public void onDisconnect(ServerData serverDataIn, CallbackInfo callback) {
		if(serverDataIn == null) {
			Client.INSTANCE.onServerChange(null);
		}
	}

	@Override
	@Accessor
	public abstract boolean isRunning();

	@Override
	@Accessor("mcDefaultResourcePack")
	public abstract DefaultResourcePack getDefaultResourcePack();

	@Override
	@Accessor("metadataSerializer_")
	public abstract IMetadataSerializer getMetadataSerialiser();

	@Override
	@Accessor(value = "timer")
	public abstract Timer getTimerSC();

	@Override
	@Invoker("resize")
	public abstract void resizeWindow(int width, int height);

	@Shadow
	public GuiScreen currentScreen;

	// @Shadow
	// protected abstract void drawSplashScreen(TextureManager textureManagerInstance);

	@Shadow
	private TextureManager renderEngine;

	private boolean loading = true;

	@Overwrite
	private void drawSplashScreen(TextureManager textureManagerInstance) {
		SplashScreen.INSTANCE.draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/SkinManager;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Ljava/io/File;Lcom/mojang/authlib/minecraft/MinecraftSessionService;)V"))
	public void splashSkinManager(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/chunk/storage/AnvilSaveConverter;<init>(Ljava/io/File;)V"))
	public void splashSaveLoader(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/audio/SoundHandler;<init>(Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/client/settings/GameSettings;)V"))
	public void splashSoundHandler(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/audio/MusicTicker;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashMusicTicker(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/FontRenderer;<init>(Lnet/minecraft/client/settings/GameSettings;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureManager;Z)V"))
	public void splashFontRenderer(CallbackInfo callback) {
		draw();
	}

	@Redirect(method = "startGame", at = @At(value = "NEW", target = "net/minecraft/util/MouseHelper"))
	public MouseHelper initMouseHelper() {
		draw();
		if (TweaksMod.instance != null && TweaksMod.instance.rawMouseInput) {
			return new RawMouseHelper();
		}
		return new MouseHelper();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/texture/TextureMap;<init>(Ljava/lang/String;)V"))
	public void splashTextureMap(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/resources/model/ModelManager;<init>(Lnet/minecraft/client/renderer/texture/TextureMap;)V"))
	public void splashModelManager(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/RenderItem;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/resources/model/ModelManager;)V"))
	public void splashRenderItem(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/RenderManager;<init>(Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/client/renderer/entity/RenderItem;)V"))
	public void splashRenderManager(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ItemRenderer;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashItemRenderer(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/EntityRenderer;<init>(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/resources/IResourceManager;)V"))
	public void splashEntityRenderer(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;<init>(Lnet/minecraft/client/renderer/BlockModelShapes;Lnet/minecraft/client/settings/GameSettings;)V"))
	public void splashBlockRenderDispatcher(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/RenderGlobal;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashRenderGlobal(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/achievement/GuiAchievement;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashGuiAchivement(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/particle/EffectRenderer;<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;)V"))
	public void splashEffectRenderer(CallbackInfo callback) {
		draw();
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiIngame;<init>(Lnet/minecraft/client/Minecraft;)V"))
	public void splashGuiIngame(CallbackInfo callback) {
		draw();
	}

	private void draw() {
		SplashScreen.INSTANCE.draw();
	}

	@Redirect(method = "updateFramebufferSize", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;" +
			"entityRenderer:Lnet/minecraft/client/renderer/EntityRenderer;"))
	public EntityRenderer hideEntityRenderer(Minecraft instance) {
		if(loading) {
			return null;
		}

		return instance.entityRenderer;
	}

	@Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;" +
			"showDebugInfo:Z",
			ordinal = 1))
	public void preventDefaultF3(GameSettings settings, boolean f3) {
	}

	@Inject(method = "dispatchKeypresses", at = @At(value = "HEAD"))
	public void updateF3(CallbackInfo callback) {
		boolean debugPressed = Keyboard.isKeyDown(61);
		if(this.debugPressed && !debugPressed) {
			if(cancelDebug) {
				cancelDebug = false;
			}
			else {
				gameSettings.showDebugInfo = !gameSettings.showDebugInfo;
			}
		}
		this.debugPressed = debugPressed;
	}

	private void debugChatInfo(String message) {
		ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("[" + EnumChatFormatting.GREEN + "Debug" + EnumChatFormatting.RESET + "] " + message));
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;" +
			"clearChatMessages()V"))
	public void betterClearMessages(GuiNewChat guiNewChat) {
		((AccessGuiNewChat) guiNewChat).clearChat();
		cancelDebug = true;
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;refreshResources()V"))
	public void betterRefreshResources(CallbackInfo callback) {
		debugChatInfo("Reloaded Resources");
		cancelDebug = true;
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/GameSettings;setOptionValue(Lnet/minecraft/client/settings/GameSettings$Options;I)V"))
	public void betterRenderDistance(CallbackInfo callback) {
		gameSettings.renderDistanceChunks = (int) Options.RENDER_DISTANCE.snapToStepClamp(gameSettings.renderDistanceChunks + (GuiScreen.isShiftKeyDown() ? -1 : 1));

		debugChatInfo("Render Distance: " + gameSettings.renderDistanceChunks);
		gameSettings.saveOptions();
		cancelDebug = true;
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V"))
	public void betterLoadRenderers(CallbackInfo callback) {
		debugChatInfo("Reloaded Chunks");
		cancelDebug = true;
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/settings/GameSettings;saveOptions()V"))
	public void advancedTootipsAndPauseOnLostFocus(CallbackInfo callback) {
		cancelDebug = true;
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/settings/GameSettings;saveOptions()V", ordinal = 0))
	public void advancedTootips(CallbackInfo callback) {
		debugChatInfo("Advanced Item Tooltips: " + gameSettings.advancedItemTooltips);
	}

	@Inject(method = "runTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/settings/GameSettings;saveOptions()V", ordinal = 1))
	public void pauseOnLostFocus(CallbackInfo callback) {
		debugChatInfo("Pause on Lost Focus: " + gameSettings.pauseOnLostFocus);
	}

	@Redirect(method = "runTick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/RenderManager;setDebugBoundingBox(Z)V"))
	public void betterHitboxes(RenderManager instance, boolean value) {
		if(!Client.INSTANCE.bus.post(new HitboxToggleEvent(value)).cancelled) {
			instance.setDebugBoundingBox(value);
			debugChatInfo("Entity Hitboxes: " + value);
			cancelDebug = true;
		}
	}

	@Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V", ordinal = 1))
	public void nextKey(int keyCode, boolean pressed) {
		if(pressed && debugPressed) {
			if (Keyboard.getEventKey() == 32 || Keyboard.getEventKey() == 31 || Keyboard.getEventKey() == 20
					|| Keyboard.getEventKey() == 33 || Keyboard.getEventKey() == 30 || Keyboard.getEventKey() == 35
					|| Keyboard.getEventKey() == 48 || Keyboard.getEventKey() == 25) {
				return;
			}
		}

		KeyBinding.setKeyBindState(keyCode, pressed);
	}

	private boolean hadWorld;

	@Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("RETURN"))
	public void onWorldLoad(WorldClient world, String loadingText, CallbackInfo callback) {
		if(world == null && hadWorld) {
			hadWorld = false;
		}
		else if(world != null && !hadWorld) {
			hadWorld = true;
			Client.INSTANCE.onServerChange(currentServerData);
		}
	}

	@Redirect(method = "startGame", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;serverName:Ljava/lang/String;"))
	public String joinServer(Minecraft instance) {
		return null;
	}

	@Redirect(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;" +
			"displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", ordinal = 1))
	public void display(Minecraft instance, GuiScreen screen) {
		loading = false;

		if(serverName != null && serverName.startsWith("§sc§")) {
			ServerList list = new ServerList((Minecraft) (Object) this);
			instance.displayGuiScreen(new GuiConnecting(new GuiMultiplayer(screen), (Minecraft) (Object) this,
					list.getServerData(Integer.parseInt(serverName.substring(4)))));
			return;
		}
		instance.displayGuiScreen(screen);
	}

	@Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
	public void handleToggle(CallbackInfo callback) {
		FullscreenToggleEvent event = Client.INSTANCE.bus.post(new FullscreenToggleEvent(!fullscreen));

		if(event.cancelled) {
			callback.cancel();
			gameSettings.fullScreen = fullscreen;
		}
		else if(!event.applyState) {
			callback.cancel();
			gameSettings.fullScreen = (fullscreen = !fullscreen);
		}
	}

	@Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
	public void preShutdown(CallbackInfo callback) {
		Client.INSTANCE.bus.post(new GameQuitEvent());
	}

	@Shadow
	public GameSettings gameSettings;

	@Shadow
	private int leftClickCounter;

	@Shadow
	public net.minecraft.client.entity.EntityPlayerSP thePlayer;

	@Shadow
	private ServerData currentServerData;

	@Shadow
	public WorldClient theWorld;

	@Shadow
	public EntityRenderer entityRenderer;



	@Shadow
	private String serverName;

	@Shadow
	public GuiIngame ingameGUI;

	@Shadow
	private RenderManager renderManager;

	@Shadow
	public PlayerControllerMP playerController;

	@Shadow
	private boolean fullscreen;

}