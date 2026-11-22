package io.github.angelclient.client.ui.component;

import net.minecraft.client.Minecraft;

public class ComponentRenderInfo {
    private final int relativeMouseX, relativeMouseY;
    private final float partialTicks;
    private final long vg;

    public ComponentRenderInfo(int relativeMouseX, int relativeMouseY, float partialTicks, long vg) {
        this.relativeMouseX = relativeMouseX;
        this.relativeMouseY = relativeMouseY;
        this.partialTicks   = partialTicks;
        this.vg             = vg;
    }

    public int   getRelativeMouseX() { return relativeMouseX; }
    public int   getRelativeMouseY() { return relativeMouseY; }
    public float getPartialTicks()   { return partialTicks; }
    public long  getVg()             { return vg; }

    public int getScreenWidth() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public int getScreenHeight() {
        return Minecraft.getMinecraft().displayHeight;
    }
}