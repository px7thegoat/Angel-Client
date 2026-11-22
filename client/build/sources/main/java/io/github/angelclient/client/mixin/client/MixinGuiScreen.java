package io.github.angelclient.client.mixin.client;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.ActionPerformedEvent;
import io.github.angelclient.client.event.impl.PostGuiInitEvent;
import io.github.angelclient.client.event.impl.PostGuiRenderEvent;
import io.github.angelclient.client.event.impl.PreGuiInitEvent;
import io.github.angelclient.client.event.impl.PreGuiKeyboardInputEvent;
import io.github.angelclient.client.event.impl.PreGuiMouseInputEvent;
import io.github.angelclient.client.event.impl.PreGuiRenderEvent;
import io.github.angelclient.client.event.impl.RenderGuiBackgroundEvent;
import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.screen.cosmetics.CosmeticsScreen;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.access.AccessGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen implements AccessGuiScreen {

	@Unique
	private static final ResourceLocation BG1 = new ResourceLocation("angel_client", "textures/angelclient/BG/background.png");
	@Unique
	private static final ResourceLocation BG2 = new ResourceLocation("angel_client", "textures/angelclient/BG/background2.png");
	@Unique
	private static final ResourceLocation BG3 = new ResourceLocation("angel_client", "textures/angelclient/BG/background3.png");
	@Unique
	private static final ResourceLocation BG4 = new ResourceLocation("angel_client", "textures/angelclient/BG/background4.png");
	@Unique
	private static final ResourceLocation BG5 = new ResourceLocation("angel_client", "textures/angelclient/BG/background5.png");

	@Unique
	private static final Map<Class<?>, ResourceLocation> BG_MAP = new HashMap<>();
	static {
		BG_MAP.put(GuiSelectWorld.class, BG2);
		BG_MAP.put(GuiMultiplayer.class, BG3);
		BG_MAP.put(GuiOptions.class, BG4);
	}

	@Override
	public boolean canBeForceClosed() {
		return true;
	}

	// =========================================================================
	// PONTE VITAL para o ReplayMod — dispara Pre/PostGuiInitEvent em QUALQUER tela
	// Sem isso, SCReplayModBackend.onGuiInit() nunca é chamado e os botões
	// Stop/Pause/Resume do ReplayMod nunca aparecem no menu ESC.
	// =========================================================================
	@Redirect(method = "setWorldAndResolution", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiScreen;initGui()V"))
	public void guiInit(GuiScreen instance) {
		if (!Client.INSTANCE.bus.post(new PreGuiInitEvent(instance)).cancelled) {
			instance.initGui();
			Client.INSTANCE.bus.post(new PostGuiInitEvent(instance, buttonList));
		}
	}

	// =========================================================================
	// Fundo do menu — imagem personalizada + blur para sub-GUIs
	// =========================================================================
	@Inject(method = "drawWorldBackground", at = @At("HEAD"), cancellable = true)
	public void onDrawWorldBackground(int tint, CallbackInfo ci) {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.theWorld == null && !(mc.currentScreen instanceof GuiMainMenu)) {
			drawCustomBackground();
			ci.cancel();
		}
	}

	@Unique
	private void drawCustomBackground() {
		Minecraft mc = Minecraft.getMinecraft();
		GuiScreen self = (GuiScreen)(Object)this;
		Class<?> clazz = self.getClass();
		ResourceLocation bg = null;
		while (bg == null && clazz != null && clazz != GuiScreen.class) {
			bg = BG_MAP.get(clazz);
			clazz = clazz.getSuperclass();
		}
		if (bg == null) bg = BG1;

		mc.getTextureManager().bindTexture(bg);
		GlStateManager.enableBlend();
		GlStateManager.color(1f, 1f, 1f, 1f);
		GuiScreen.drawModalRectWithCustomSizedTexture(0, 0, 0, 0,
			self.width, self.height, self.width, self.height);

		GuiScreen.drawRect(0, 0, self.width, self.height, 0xAA000000);
		GlStateManager.disableBlend();
	}

	@Redirect(method = "drawWorldBackground",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiScreen;drawGradientRect(IIIIII)V"))
	public void getTopColour(GuiScreen guiScreen, int left, int top, int right, int bottom,
			int startColor, int endColor) {
		if (Minecraft.getMinecraft().theWorld != null
				&& !Client.INSTANCE.bus.post(new RenderGuiBackgroundEvent()).cancelled) {
			Utils.drawGradientRect(left, top, right, bottom, startColor, endColor);
		} else {
			Utils.drawGradientRect(left, top, right, bottom, 0, 0);
		}
	}

	// =========================================================================
	// Clique em botões — suporte a ActionPerformedEvent + GuiScaleMod
	// =========================================================================
	@Redirect(method = "mouseClicked",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiButton;mousePressed(Lnet/minecraft/client/Minecraft;II)Z"))
	public boolean onActionPerformed(GuiButton instance, Minecraft mc, int mouseX, int mouseY) {
		return instance.mousePressed(mc, mouseX, mouseY)
				&& !Client.INSTANCE.bus.post(new ActionPerformedEvent((GuiScreen) (Object) this, instance)).cancelled;
	}

	// =========================================================================
	// Render da GUI — eventos Pre/Post + GuiScaleMod + logo no inventário
	// =========================================================================
	@Inject(method = "drawScreen", at = @At("HEAD"))
	public void preGuiRender(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
		// NanoVG com NVG_STENCIL_STROKES deixa GL_STENCIL_TEST habilitado após nvgEndFrame.
		// Isso faz qualquer tela subsequente (CosmeticMenu, GuiInventory, etc.) renderizar
		// completamente invisível — todos os fragmentos são descartados pelo stencil.
		// Reset centralizado aqui garante que TODA GuiScreen começa com estado GL limpo.
		GL11.glDisable(GL11.GL_STENCIL_TEST);
		GL11.glStencilMask(0xFF);
		GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

		Client.INSTANCE.bus.post(new PreGuiRenderEvent(partialTicks));
	}

	@Inject(method = "drawScreen", at = @At("RETURN"))
	public void postGuiRender(int mouseX, int mouseY, float partialTicks, CallbackInfo callback) {
		GlStateManager.color(1, 1, 1, 1); // Evita que cores vazem entre telas
		Client.INSTANCE.bus.post(new PostGuiRenderEvent(partialTicks));
	}

	// =========================================================================
	// Input do mouse — suporte a PreGuiMouseInputEvent
	// =========================================================================
	@Redirect(method = "handleInput",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiScreen;handleMouseInput()V"))
	public void handleMouseInput(GuiScreen instance) throws IOException {
		if (!Client.INSTANCE.bus.post(new PreGuiMouseInputEvent()).cancelled) {
			instance.handleMouseInput();
		}
	}

	// Botões laterais do mouse dentro do inventário (Mouse 4, Mouse 5)
	@Inject(method = "handleMouseInput",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiScreen;mouseClicked(III)V",
					shift = At.Shift.AFTER))
	private void onMouseClickSideButtons(CallbackInfo ci) throws IOException {
		int button = org.lwjgl.input.Mouse.getEventButton();
		if (button > 2 && org.lwjgl.input.Mouse.getEventButtonState()
				&& (Object) this instanceof GuiContainer) {
			keyTyped((char) 0, button - 100);
		}
	}

	// =========================================================================
	// Teclado — fix de opções não salvas ao pressionar ESC
	// =========================================================================
	@Redirect(method = "keyTyped",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V"))
	public void saveFirst(Minecraft instance, GuiScreen screen) throws IOException {
		// BUG FIX: só interceptar quando screen == null (ESC real).
		// Se screen != null (ex: troca de aba no criativo, bauzinho do Survival
		// Inventory), deixar passar normalmente — sem forçar null.
		if (screen != null) {
			instance.displayGuiScreen(screen);
			return;
		}
		for (GuiButton button : buttonList) {
			if (button.displayString.equals(I18n.format("gui.done"))) {
				actionPerformed(button);
			}
		}
		instance.displayGuiScreen(null);
	}

	// =========================================================================
	// Abre links externos
	// =========================================================================
	@Overwrite
	private void openWebLink(URI uri) {
		Utils.openUrl(uri.toString());
	}

	// =========================================================================
	// Shadows
	// =========================================================================
	@Shadow
	protected List<GuiButton> buttonList;

	@Shadow
	public int width;

	@Shadow
	public int height;

	@Shadow
	protected abstract void actionPerformed(GuiButton button) throws IOException;

	@Shadow
	protected abstract void keyTyped(char typedChar, int keyCode) throws IOException;

	@Shadow
	protected Minecraft mc;

}