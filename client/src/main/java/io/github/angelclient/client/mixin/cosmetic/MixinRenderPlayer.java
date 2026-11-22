package io.github.angelclient.client.mixin.cosmetic;

import io.github.angelclient.client.cosmetic.LocalCosmeticManager;
import io.github.angelclient.client.cosmetic.render.Cosmetic3DRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {

    private static final double MAX_COSMETIC_DISTANCE = 20.0;

    @Inject(method = "doRender", at = @At("RETURN"))
    private void onPostRenderPlayer(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (entity.isInvisible() || entity.isSpectator()) return;
        if (Cosmetic3DRenderer.skipRendering) return;
        String name = entity.getName();
        if (name == null || name.isEmpty()) return;

        EntityPlayer localPlayer = Minecraft.getMinecraft().thePlayer;
        if (localPlayer == null) return;

        if (entity.worldObj != null && entity.getDistanceSqToEntity(localPlayer) > MAX_COSMETIC_DISTANCE * MAX_COSMETIC_DISTANCE) return;

        if (!LocalCosmeticManager.getInstance().playerHasAnyActive(name)) return;

        boolean isLocalInThirdPerson = entity.getEntityId() == localPlayer.getEntityId()
            && Minecraft.getMinecraft().gameSettings.thirdPersonView != 0;

        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate((float) x, (float) y, (float) z);
            GlStateManager.translate(0.0F, 0.0F, 0.01F);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            Cosmetic3DRenderer.renderBandana(entity, partialTicks);
            Cosmetic3DRenderer.renderCape(entity, partialTicks);
            Cosmetic3DRenderer.renderShield(entity, partialTicks);
            Cosmetic3DRenderer.renderHat(entity, partialTicks);
            Cosmetic3DRenderer.renderItem(entity, partialTicks);
            if (!isLocalInThirdPerson) Cosmetic3DRenderer.renderAura(entity, partialTicks);
        } catch (Exception e) {
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }
}
