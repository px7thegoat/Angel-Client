package io.github.angelclient.client.ui.screen;

import java.io.IOException;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.Screen;
import io.github.angelclient.client.util.access.AccessGuiMainMenu;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.video.VideoBackgroundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public abstract class PanoramaBackgroundScreen extends Screen {

	private GuiScreen mainMenu = Client.INSTANCE.getMainMenu();

	public PanoramaBackgroundScreen(Component root) {
		super(root);
		background = false;
	}

	@Override
	public void setWorldAndResolution(Minecraft mc, int width, int height) {
		super.setWorldAndResolution(mc, width, height);

		if(mc.theWorld == null) {
			mainMenu.setWorldAndResolution(mc, width, height);
		}
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		mainMenu.updateScreen();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		VideoBackgroundManager.render(width, height);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void drawPanorama(int mouseX, int mouseY, float partialTicks) {
		AccessGuiMainMenu access = (AccessGuiMainMenu) mainMenu;

		mc.getFramebuffer().unbindFramebuffer();

        GlStateManager.viewport(0, 0, 256, 256);
        access.renderPanorama(mouseX, mouseY, partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        access.rotateAndBlurPanorama(partialTicks);
        mc.getFramebuffer().bindFramebuffer(true);

        GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);

        float uvBase = width > height ? 120.0F / width : 120.0F / height;
        float uBase = height * uvBase / 256.0F;
        float vBase = width * uvBase / 256.0F;

		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer renderer = tessellator.getWorldRenderer();
		renderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		renderer.pos(0.0D, height, zLevel).tex((0.5F - uBase), (0.5F + vBase)).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		renderer.pos(width, height, zLevel).tex(0.5F - uBase, 0.5F - vBase).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		renderer.pos(width, 0.0D, zLevel).tex(0.5F + uBase, 0.5F - vBase).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		renderer.pos(0.0D, 0.0D, zLevel).tex(0.5F + uBase, 0.5F + vBase).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
		tessellator.draw();

		drawRect(0, 0, width, height, new Colour(0, 0, 0, 100).getValue());

		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(1f, 1f, 1f, 1f);
		GlStateManager.disableBlend();
		GlStateManager.enableAlpha();
		GlStateManager.enableTexture2D();
	}

}
