package io.github.angelclient.client.cosmetic.render;

import io.github.angelclient.client.cosmetic.*;
import io.github.angelclient.client.cosmetic.model.BlockbenchModel;
import io.github.angelclient.client.cosmetic.model.BlockbenchModelLoader;
import io.github.angelclient.client.cosmetic.model.ModelOBJ;
import io.github.angelclient.client.cosmetic.model.ObjModelLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.MathHelper;

public class Cosmetic3DRenderer {

    public static boolean skipRendering = false;
    public static boolean skipNametagRendering = false;

    private static final Map<Integer, Float> capeSwingMap = new HashMap<>();

    private static float getSneakOffset(EntityPlayer player, float maxOffset) {
        return player.isSneaking() ? -maxOffset : 0.0F;
    }

    private static float getCapeSwing(EntityPlayer player, float partialTicks) {
        int id = player.getEntityId();
        float f = player.prevDistanceWalkedModified + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * partialTicks;
        float target = MathHelper.sin(f * (float) Math.PI) * 13.0F;
        float current = capeSwingMap.getOrDefault(id, 0f);
        current += (target - current) * 0.15f;
        if (Math.abs(current - target) < 0.01f) current = target;
        capeSwingMap.put(id, current);
        return current;
    }

    private static String resolveId(EntityPlayer player, CosmeticType type, String activeId) {
        return LocalCosmeticManager.getInstance().getActiveWithPreview(player.getName(), type, activeId);
    }

    public static void renderBandana(EntityPlayer player, float partialTicks) {
        String playerName = player.getName();
        String bandanaId = resolveId(player, CosmeticType.BANDANA,
            LocalCosmeticManager.getInstance().getActiveBandana(playerName));
        if (bandanaId == null) return;

        LocalCosmetic bandana = LocalCosmeticManager.getInstance().getBandana(bandanaId);
        if (bandana == null) return;

        GlStateManager.pushMatrix();
        try {

        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float neckY = player.getEyeHeight() - 0.25F;

        GlStateManager.translate(0.0F, getSneakOffset(player, 0.15F), 0.0F);
        GlStateManager.translate(0.0F, neckY, 0.0F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.28F, 0.0F);

        float s = 0.06F;
        GlStateManager.scale(s, s, s);

        ModelOBJ model = ObjModelLoader.loadModel("bandana/bandana.obj");
        if (model.getFaceCount() == 0) {
            return;
        }

        ResourceLocation tex = TextureLoader.loadTexture(bandana.getFile());
        if (tex != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    public static void renderHat(EntityPlayer player, float partialTicks) {
        String playerName = player.getName();
        String hatId = resolveId(player, CosmeticType.HAT,
            LocalCosmeticManager.getInstance().getActiveHat(playerName));
        if (hatId == null) return;

        LocalCosmetic hat = LocalCosmeticManager.getInstance().getHat(hatId);
        if (hat == null) return;

        if ("MiniMe".equalsIgnoreCase(hatId)) {
            renderMiniMe(player, hat);
            return;
        }

        if ("CatTailBlack".equalsIgnoreCase(hatId) || "CatTailWhite".equalsIgnoreCase(hatId)) {
            renderTail(player, hat);
            return;
        }

        if ("UrsoBranco".equalsIgnoreCase(hatId)) {
            renderUrsoBranco(player, hat);
            return;
        }

        GlStateManager.pushMatrix();
        try {

        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float neckY = player.getEyeHeight() - 0.25F;

        GlStateManager.translate(0.0F, getSneakOffset(player, 0.15F), 0.0F);
        GlStateManager.translate(0.0F, neckY, 0.0F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.40F, 0.0F);

        float s = 0.9F;
        GlStateManager.scale(s, s, s);

        String file = hat.getFile();
        if (file == null) { return; }

        boolean isJson = file.toLowerCase().endsWith(".json");

        if (isJson) {
            BlockbenchModel bm = BlockbenchModelLoader.loadModel(file);
            if (!bm.hasElements()) { return; }
            ResourceLocation tex = null;
            if (hat.getTexture() != null && !hat.getTexture().isEmpty()) {
                tex = TextureLoader.loadTexture(hat.getTexture());
            }
            if (tex != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            bm.render();
            return;
        }

        ModelOBJ model = ObjModelLoader.loadModel(file);
        if (model.getFaceCount() == 0) {
            return;
        }

        ResourceLocation tex;
        if (hat.getTexture() != null && !hat.getTexture().isEmpty()) {
            tex = TextureLoader.loadTexture(hat.getTexture());
        } else {
            String texPath = file.substring(0, file.lastIndexOf('.')) + "/texture.png";
            tex = TextureLoader.loadTexture(texPath);
        }
        if (tex != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    private static void renderUrsoBranco(EntityPlayer player, LocalCosmetic hat) {
        GlStateManager.pushMatrix();
        try {

        float sneakOffset = player.isSneaking() ? -0.25F : 0.0F;
        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * 1.0F;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * 1.0F;
        float neckY = player.getEyeHeight() - 0.25F;

        GlStateManager.translate(0.0F, sneakOffset, 0.0F);
        GlStateManager.translate(0.0F, neckY, 0.0F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

        // Position on top of head
        GlStateManager.translate(0.0F, 0.55F, 0.0F);

        float s = 0.35F;
        GlStateManager.scale(s, s, s);

        BlockbenchModel model = BlockbenchModelLoader.loadModel(hat.getFile());
        if (!model.hasElements()) {
            return;
        }

        if (hat.getTexture() != null && !hat.getTexture().isEmpty()) {
            ResourceLocation tex = TextureLoader.loadTexture(hat.getTexture());
            if (tex != null) Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    private static void renderMiniMe(EntityPlayer player, LocalCosmetic hat) {
        GlStateManager.pushMatrix();
        try {

        float sneakOffset = player.isSneaking() ? -0.20F : 0.0F;
        float yaw = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * 1.0F;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * 1.0F;
        float neckY = player.getEyeHeight() - 0.25F;

        GlStateManager.translate(0.0F, sneakOffset, 0.0F);
        GlStateManager.translate(0.0F, neckY, 0.0F);
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);

        // Position on top of head
        GlStateManager.translate(0.0F, 0.60F, 0.0F);

        // Face forward (model default is backwards)
        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

        float s = 0.05F;
        GlStateManager.scale(s, s, s);

        GlStateManager.translate(-8.0F, 0.0F, -8.0F);

        BlockbenchModel model = BlockbenchModelLoader.loadModel(hat.getFile());
        if (!model.hasElements()) {
            return;
        }

        if (player instanceof AbstractClientPlayer) {
            ResourceLocation skin = ((AbstractClientPlayer) player).getLocationSkin();
            Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    private static void renderTail(EntityPlayer player, LocalCosmetic hat) {
        GlStateManager.pushMatrix();
        try {

        float bodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * 1.0F;

        GlStateManager.translate(0.0F, getSneakOffset(player, 0.15F), 0.0F);
        GlStateManager.rotate(-bodyYaw, 0.0F, 1.0F, 0.0F);

        GlStateManager.translate(0.0F, 0.45F, -0.25F);

        float s = 0.06F;
        GlStateManager.scale(s, s, s);

        GlStateManager.translate(-8.0F, -0.25F, -11.0F);

        BlockbenchModel model = BlockbenchModelLoader.loadModel(hat.getFile());
        if (!model.hasElements()) {
            return;
        }

        if (hat.getTexture() != null && !hat.getTexture().isEmpty()) {
            ResourceLocation tex = TextureLoader.loadTexture(hat.getTexture());
            if (tex != null) Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    private static long lastAuraTime = 0;
    private static float cachedAuraRot = 0f;

    public static void renderAura(EntityPlayer player, float partialTicks) {
        String playerName = player.getName();
        String auraId = resolveId(player, CosmeticType.EFFECT,
            LocalCosmeticManager.getInstance().getActiveAura(playerName));
        if (auraId == null) return;

        LocalCosmetic aura = LocalCosmeticManager.getInstance().getAura(auraId);
        if (aura == null) return;

        GlStateManager.pushMatrix();
        try {

        long now = System.currentTimeMillis();
        if (now != lastAuraTime) {
            lastAuraTime = now;
            cachedAuraRot = (now % 72000) / 200.0F;
        }
        float auraRot = cachedAuraRot;

        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(auraRot, 0.0F, 1.0F, 0.0F);

        float s = 0.08F;
        GlStateManager.scale(s, s, s);

        GlStateManager.translate(-7.75F, -13.5F, -7.75F);

        BlockbenchModel model = BlockbenchModelLoader.loadModel(aura.getFile());
        if (!model.hasElements()) {
            return;
        }

        if (aura.getTexture() != null && !aura.getTexture().isEmpty()) {
            ResourceLocation tex = TextureLoader.loadTexture(aura.getTexture());
            if (tex != null) Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.8F);
        model.render();
        } finally { GlStateManager.popMatrix(); }
    }

    public static void renderCape(EntityPlayer player, float partialTicks) {
        // Cape rendering is handled by vanilla via getLocationCape mixin override
    }

    public static void renderShield(EntityPlayer player, float partialTicks) {
        String playerName = player.getName();
        String shieldId = resolveId(player, CosmeticType.SHIELD,
            LocalCosmeticManager.getInstance().getActiveShield(playerName));
        if (shieldId == null) return;

        LocalCosmetic shield = LocalCosmeticManager.getInstance().getShield(shieldId);
        if (shield == null) return;

        // Get player model to follow left arm
        net.minecraft.client.model.ModelPlayer model;
        try {
            net.minecraft.client.renderer.entity.RenderPlayer renderPlayer =
                (net.minecraft.client.renderer.entity.RenderPlayer) (
                net.minecraft.client.renderer.entity.Render) Minecraft.getMinecraft()
                .getRenderManager().getEntityRenderObject(player);
            model = (net.minecraft.client.model.ModelPlayer) renderPlayer.getMainModel();
        } catch (Exception e) {
            return;
        }

        GlStateManager.pushMatrix();
        try {

        GlStateManager.translate(0.0F, getSneakOffset(player, 0.15F), 0.0F);

        float bodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        GlStateManager.rotate(-bodyYaw, 0.0F, 1.0F, 0.0F);

        // Follow left arm transformations
        model.bipedLeftArm.postRender(0.0625F);

        // Offset to position shield on forearm
        GlStateManager.translate(0.4F, 0.65F, 0F);

        float s = 0.0012F;
        GlStateManager.scale(-s, s, s);

        ModelOBJ obj = ObjModelLoader.loadModel(shield.getFile());
        if (obj.getFaceCount() == 0) {
            return;
        }

        String texPath = shield.getTexture() != null ? shield.getTexture() : shield.getFile().replace(".obj", ".png");
        ResourceLocation tex = TextureLoader.loadTexture(texPath);
        if (tex != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        obj.render();
        } finally { GlStateManager.popMatrix(); }
    }

    public static void renderItem(EntityPlayer player, float partialTicks) {
        String playerName = player.getName();
        String itemId = resolveId(player, CosmeticType.ITEM,
            LocalCosmeticManager.getInstance().getActiveItem(playerName));
        if (itemId == null) return;

        LocalCosmetic item = LocalCosmeticManager.getInstance().getItem(itemId);
        if (item == null) return;

        GlStateManager.pushMatrix();
        try {

        GlStateManager.translate(0.0F, getSneakOffset(player, 0.15F), 0.0F);

        float bodyYaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        GlStateManager.rotate(-bodyYaw, 0.0F, 1.0F, 0.0F);

        // Position at left hip - Katana on opposite side
        GlStateManager.translate(0.12F, 0.85F, 0.0F);

        // Rotate so blade points downward
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);

        // Center model origin - closer to player body
        GlStateManager.translate(0.25F, 0.03F, 0.25F);

        float s = 0.035F;
        GlStateManager.scale(s, s, s);

        BlockbenchModel model = BlockbenchModelLoader.loadModel(item.getFile());
        if (!model.hasElements()) {
            return;
        }

        if (item.getTexture() != null && !item.getTexture().isEmpty()) {
            ResourceLocation tex = TextureLoader.loadTexture(item.getTexture());
            if (tex != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
            }
        }

        model.render();
        } finally { GlStateManager.popMatrix(); }
    }
}
