package io.github.angelclient.client.ui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class SplashScreen {

    private static final int BG_COLOR = 0xFF14141A;
    private static final int FG = 0xFF3B82F6;
    private static final int BAR_BG = 0x33FFFFFF;
    private static final ResourceLocation BG_IMAGE = new ResourceLocation("angel_client", "textures/angelclient/splash_bg.png");

    public static final SplashScreen INSTANCE = new SplashScreen();
    private Minecraft mc;
    private int stage;
    private int stages = 50;
    private long finishTime = -1;

    private long startTime = -1;
    private float logoYOffset = 0f;
    private long logoAnimStart = -1;

    public void reset() {
        stage = 0;
        finishTime = -1;
        startTime = -1;
        logoAnimStart = -1;
    }

    public void setStages(int stages) {
        this.stages = stages;
    }

    public boolean isFinished() {
        return finishTime >= 0 && (System.currentTimeMillis() - finishTime) > 800;
    }

    public void draw() {
        try {
            if (mc == null) mc = Minecraft.getMinecraft();
            if (finishTime >= 0) {
                long elapsed = System.currentTimeMillis() - finishTime;
                if (elapsed > 800) return;
            }
            if (startTime < 0) startTime = System.currentTimeMillis();

            ScaledResolution sr = new ScaledResolution(mc);
            int factor = sr.getScaleFactor();
            int w = sr.getScaledWidth() * factor;
            int h = sr.getScaledHeight() * factor;

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, w, h, 0, 1000, 3000);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glTranslatef(0, 0, -2000);

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.disableFog();

            float alpha = 1.0f;
            if (finishTime >= 0) {
                long elapsed = System.currentTimeMillis() - finishTime;
                alpha = Math.max(0.0f, 1.0f - elapsed / 800.0f);
            }

            // Solid background fallback (renders even if texture fails)
            Gui.drawRect(0, 0, w, h, BG_COLOR);

            // Try to draw background texture
            try {
                GlStateManager.enableTexture2D();
                mc.getTextureManager().bindTexture(BG_IMAGE);
                GlStateManager.color(1f, 1f, 1f, alpha);
                Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, w, h, (float) w, (float) h);
            } catch (Exception e) {
                // texture not available, fallback already drawn
            }

            // Logo drop animation on startup
            if (logoAnimStart < 0) logoAnimStart = System.currentTimeMillis();
            long logoElapsed = System.currentTimeMillis() - logoAnimStart;
            float logoAnim = Math.min(1f, logoElapsed / 600f);
            float bounce = 1f - (float) Math.pow(1f - logoAnim, 3f);
            logoYOffset = (1f - bounce) * 30f;

            // Progress bar with animation
            int barWidth = w / 2;
            int barHeight = 6;
            int barX = (w - barWidth) / 2;
            int barY = (int) (h * 0.80f) + (int) logoYOffset;

            int progress = stage <= stages ? barWidth / stages * stage : barWidth;
            if (stage <= stages) {
                int filledW = barWidth / stages * stage;
                Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, BAR_BG);
                if (filledW > 0) Gui.drawRect(barX, barY, barX + filledW, barY + barHeight, FG);
            } else {
                Gui.drawRect(barX, barY, barX + barWidth, barY + barHeight, FG);
            }

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();

            Display.update();

            if (stage <= stages) {
                stage++;
                if (stage > stages) finishTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
        }
    }
}
