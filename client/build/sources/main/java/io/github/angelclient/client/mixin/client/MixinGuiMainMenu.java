package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;
import io.github.angelclient.client.util.access.AccessGuiMainMenu;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen implements AccessGuiMainMenu {

	@Inject(method = "<init>", at = @At("RETURN"))
	public void setActiveMainMenu(CallbackInfo callback) {
		Client.INSTANCE.setMainMenu((GuiMainMenu) (Object) this);
	}

	@Inject(method = "addSingleplayerMultiplayerButtons", at = @At("RETURN"))
	public void getModsButton(int x, int y, CallbackInfo callback) {
		buttonList.remove(realmsButton);
		buttonList.add(new GuiButton(realmsButton.id, realmsButton.xPosition, realmsButton.yPosition, I18n.format("angel_client.mod.screen.title")));

	}

	@Redirect(method = "actionPerformed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMainMenu;" +
			"switchToRealms()V"))
	public void openModsMenu(GuiMainMenu guiMainMenu) {
		mc.displayGuiScreen(new ModsScreen());
	}


	@Shadow
	private GuiButton realmsButton;

	@Invoker("drawPanorama")
	@Override
	public abstract void renderPanorama(int mouseX, int mouseY, float partialTicks);

	@Invoker("rotateAndBlurSkybox")
	@Override
	public abstract void rotateAndBlurPanorama(float partialTicks);

}
