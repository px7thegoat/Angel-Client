package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.cosmetic.wavey.CapeHolder;
import io.github.angelclient.client.cosmetic.wavey.MinecraftPlayer;
import io.github.angelclient.client.cosmetic.wavey.StickSimulation3d;
import io.github.angelclient.client.cosmetic.wavey.Vector3;
import io.github.angelclient.client.cosmetic.wavey.WaveyCapesConfigInstance;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PreTickEvent;
import io.github.angelclient.client.mod.ConfigOnlyMod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaveyCapesMod extends ConfigOnlyMod {

    public static WaveyCapesMod INSTANCE;
    public static boolean enabled;

    private final Map<UUID, CapeHolder> capeHolders = new ConcurrentHashMap<>();

    @Expose
    @Option
    private boolean capeWind = false;

    @Expose
    @Option
    @Slider(min = 5, max = 40, step = 1)
    private int capeGravity = 25;

    @Expose
    @Option
    @Slider(min = 1, max = 16, step = 1)
    private int capeHeight = 6;

    @Override
    public String getId() {
        return "wavey_capes";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        INSTANCE = this;
        enabled = true;
        WaveyCapesConfigInstance.get().windMode = capeWind;
        WaveyCapesConfigInstance.get().gravity = capeGravity;
        WaveyCapesConfigInstance.get().heightMultiplier = capeHeight;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        enabled = false;
        capeHolders.clear();
    }

    public CapeHolder getCapeHolder(UUID uuid) {
        return capeHolders.get(uuid);
    }

    @EventHandler
    public void onTick(PreTickEvent event) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            capeHolders.clear();
            return;
        }

        WaveyCapesConfigInstance.get().windMode = capeWind;
        WaveyCapesConfigInstance.get().gravity = capeGravity;
        WaveyCapesConfigInstance.get().heightMultiplier = capeHeight;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player.isInvisible()) continue;
            UUID uuid = player.getUniqueID();
            CapeHolder holder = capeHolders.computeIfAbsent(uuid, k -> new CapeHolder());
            holder.simTick(new PlayerAdapter(player));
        }
    }

    public static void renderWaveyCape(EntityPlayer player, ResourceLocation capeTexture, float partialTicks) {
        if (!enabled || INSTANCE == null) return;
        CapeHolder holder = INSTANCE.capeHolders.get(player.getUniqueID());
        if (holder == null || holder.getSimulation() == null || holder.getSimulation().empty()) return;

        StickSimulation3d sim = holder.getSimulation();

        GlStateManager.pushMatrix();

        float bodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        GlStateManager.rotate(-bodyYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.125D);

        float sneakOffset = player.isSneaking() ? -0.15F : 0.0F;
        GlStateManager.translate(0.0F, sneakOffset, 0.0F);

        if (capeTexture != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(capeTexture);
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        int partCount = sim.points.size();
        float capeWidth = 10.0F / 16.0F;
        float capeHeight = 1.0F;
        float capeDepth = 1.0F / 16.0F;

        for (int part = 0; part < partCount; part++) {
            GlStateManager.pushMatrix();

            float x = sim.points.get(part).getLerpX(partialTicks) - sim.points.get(0).getLerpX(partialTicks);
            if (x > 0) x = 0;
            float y = sim.points.get(0).getLerpY(partialTicks) - part - sim.points.get(part).getLerpY(partialTicks);
            float z = sim.points.get(0).getLerpZ(partialTicks) - sim.points.get(part).getLerpZ(partialTicks);

            float partRotation = 0;
            if (part < partCount - 1) {
                Vector3 a = sim.points.get(part).getLerpedPos(partialTicks);
                Vector3 b = sim.points.get(part + 1).getLerpedPos(partialTicks);
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                partRotation = (float) Math.toDegrees(Math.atan2(dx, dy)) + 180;
            } else {
                partRotation = 0;
            }

            float windSwing = 0;
            if (WaveyCapesConfigInstance.get().windMode) {
                long phase = (System.currentTimeMillis() / 3) % 360;
                float rel = (float) (part + 1) / partCount;
                windSwing = (float) (Math.sin(Math.toRadians(rel * 360 - phase)) * 3);
            }

            GlStateManager.rotate(6.0F + windSwing, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

            GlStateManager.translate(-z / partCount, y / partCount, x / partCount);
            GlStateManager.translate(0, 0.48F / 16.0F, -(0.48F / 16.0F));
            GlStateManager.translate(0, part * 1.0F / partCount, 0);
            GlStateManager.rotate(-partRotation, 1.0F, 0.0F, 0.0F);
            GlStateManager.translate(0, -part * 1.0F / partCount, 0);
            GlStateManager.translate(0, -(0.48F / 16.0F), 0.48F / 16.0F);

            float uMin = 1.0F / 64.0F;
            float uMax = 11.0F / 64.0F;
            float vMin = (1.0F / 32.0F) * part;
            float vMax = (1.0F / 32.0F) * (part + 1);

            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();

            wr.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            wr.pos(-capeWidth / 2, -(capeHeight / partCount) * (part + 1), -capeDepth).tex(uMin, vMax).normal(0, 0, -1).endVertex();
            wr.pos(capeWidth / 2, -(capeHeight / partCount) * (part + 1), -capeDepth).tex(uMax, vMax).normal(0, 0, -1).endVertex();
            wr.pos(capeWidth / 2, -(capeHeight / partCount) * part, -capeDepth).tex(uMax, vMin).normal(0, 0, -1).endVertex();
            wr.pos(-capeWidth / 2, -(capeHeight / partCount) * part, -capeDepth).tex(uMin, vMin).normal(0, 0, -1).endVertex();
            tess.draw();

            wr.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            wr.pos(-capeWidth / 2, -(capeHeight / partCount) * part, 0).tex(uMin + 11.0F/64.0F, vMin).normal(0, 0, 1).endVertex();
            wr.pos(capeWidth / 2, -(capeHeight / partCount) * part, 0).tex(uMax + 11.0F/64.0F, vMin).normal(0, 0, 1).endVertex();
            wr.pos(capeWidth / 2, -(capeHeight / partCount) * (part + 1), 0).tex(uMax + 11.0F/64.0F, vMax).normal(0, 0, 1).endVertex();
            wr.pos(-capeWidth / 2, -(capeHeight / partCount) * (part + 1), 0).tex(uMin + 11.0F/64.0F, vMax).normal(0, 0, 1).endVertex();
            tess.draw();

            GlStateManager.popMatrix();
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static class PlayerAdapter implements MinecraftPlayer {
        private final EntityPlayer player;

        PlayerAdapter(EntityPlayer player) {
            this.player = player;
        }

        public boolean isVisuallySwimming() { return player.isInWater() && !player.isSneaking(); }
        public float getXRot() { return player.rotationPitch; }
        public boolean isCrouching() { return player.isSneaking(); }
        public double getY() { return player.posY; }
        public float getYRot() { return player.rotationYaw; }
        public double getZ() { return player.posZ; }
        public double getX() { return player.posX; }
        public boolean isUnderWater() { return player.isInWater(); }
        public double getXCloak() { return player.posX; }
        public double getZCloak() { return player.posZ; }
        public float getYBodyRotO() { return player.prevRenderYawOffset; }
        public float getYBodyRot() { return player.renderYawOffset; }
        public double getYo() { return player.prevPosY; }
        public double getXo() { return player.prevPosX; }
        public double getZo() { return player.prevPosZ; }
    }
}
