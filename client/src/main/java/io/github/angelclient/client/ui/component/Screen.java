package io.github.angelclient.client.ui.component;

import java.io.IOException;

import org.lwjgl.input.Mouse;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.ui.component.controller.ParentBoundsController;
import io.github.angelclient.client.util.access.AccessMinecraft;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Rectangle;
import io.github.angelclient.client.util.nanovg.NanoVGManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class Screen extends GuiScreen {

    protected GuiScreen parentScreen;
    protected Component root;
    private Component rootWrapper;
    private int mouseX;
    private int mouseY;
    protected boolean background = true;

    public Screen(Component root) {
        this.parentScreen = Minecraft.getMinecraft().currentScreen;
        rootWrapper = new Component() {
            @Override
            public Rectangle getBounds() {
                return new Rectangle(0, 0, width, height);
            }
        };

        rootWrapper.add(root, new ParentBoundsController());
        rootWrapper.setScreen(this);
        rootWrapper.setFont(AngelClientMod.getFont());
        this.root = root;
    }

    public Component getRoot() {
        return root;
    }

    @Override
    public void initGui() {
        super.initGui();
        // O contexto NanoVG é global (criado em Client.init) — não precisamos criar aqui
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;

        // GL state reset (fixes Fast Render dependency)
        mc.getFramebuffer().bindFramebuffer(true);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
        org.lwjgl.opengl.GL11.glStencilMask(0xFF);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT);
        GlStateManager.matrixMode(5888);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();

        if (background) {
            if (mc.theWorld == null) {
                drawRect(0, 0, width, height, Colour.BACKGROUND.getValue());
            } else {
                drawDefaultBackground();
            }
        }

        long vg = NanoVGManager.getNvg();

        if (vg != 0L) {
            Minecraft _mc = Minecraft.getMinecraft();
            float dpr = (_mc.displayWidth > 0 && width > 0)
                    ? (float) _mc.displayWidth / (float) width
                    : 1.0f;
            org.lwjgl.nanovg.NanoVG.nvgBeginFrame(vg, width, height, dpr);

            rootWrapper.render(new ComponentRenderInfo(mouseX, mouseY, partialTicks, vg));

            org.lwjgl.nanovg.NanoVG.nvgEndFrame(vg);

            net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.enableTexture2D();
            net.minecraft.client.renderer.GlStateManager.enableAlpha();
            net.minecraft.client.renderer.GlStateManager.blendFunc(
                org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
            );
        } else {
            rootWrapper.render(new ComponentRenderInfo(mouseX, mouseY, partialTicks, 0L));
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (Mouse.getEventDWheel() != 0) {
            rootWrapper.mouseScroll(getInfo(), Mouse.getEventDWheel());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!rootWrapper.keyPressed(getInfo(), keyCode, typedChar)) {
            if (keyCode == 1) {
                closeAll();
            } else {
                super.keyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!rootWrapper.mouseClickedAnywhere(getInfo(), mouseButton, true, false)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (!rootWrapper.mouseReleasedAnywhere(getInfo(), state, true)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        rootWrapper.tick();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public void updateFont() {
        rootWrapper.setFont(AngelClientMod.getFont());
    }

    public void close() {
        mc.displayGuiScreen(parentScreen);
    }

    public void closeAll() {
        mc.displayGuiScreen(null);
    }

    private ComponentRenderInfo getInfo() {
        return new ComponentRenderInfo(mouseX, mouseY,
                AccessMinecraft.getInstance().getTimerSC().renderPartialTicks,
                NanoVGManager.getNvg());
    }

    public net.minecraft.client.gui.GuiScreen getParentScreen() { return parentScreen; }

    protected void mouseClickedMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }
}