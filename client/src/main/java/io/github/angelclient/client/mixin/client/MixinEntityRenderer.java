package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.impl.BlockHighlightRenderEvent;
import io.github.angelclient.client.event.impl.CameraRotateEvent;
import io.github.angelclient.client.event.impl.FovEvent;
import io.github.angelclient.client.event.impl.GammaEvent;
import io.github.angelclient.client.event.impl.PlayerHeadRotateEvent;
import io.github.angelclient.client.event.impl.PostProcessingEvent;
import io.github.angelclient.client.performance.FrustumCullingMod;
import io.github.angelclient.client.util.access.AccessMinecraft;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

	private static final Client CLIENT = Client.INSTANCE;

	@Shadow
	private Minecraft mc;

	@Shadow
	protected abstract boolean isDrawBlockOutline();

	// =========================
	// Gamma Optimization
	// =========================

	@Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;gammaSetting:F"))
	public float overrideGamma(GameSettings settings) {
		return CLIENT.bus.post(new GammaEvent(settings.gammaSetting)).gamma;
	}

	// =========================
	// Shader Rendering
	// =========================

	@Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/shader/Framebuffer;bindFramebuffer(Z)V",
			shift = At.Shift.BEFORE))
	public void addShaders(float partialTicks, long nanoTime, CallbackInfo ci) {

		PostProcessingEvent event = CLIENT.bus.post(
				new PostProcessingEvent(PostProcessingEvent.Type.RENDER));

		if (event.groups.isEmpty()) return;

		float pt = AccessMinecraft.getInstance().getTimerSC().renderPartialTicks;

		for (ShaderGroup group : event.groups) {

			GlStateManager.matrixMode(5890); // GL_TEXTURE
			GlStateManager.pushMatrix();
			GlStateManager.loadIdentity();

			group.loadShaderGroup(pt);

			GlStateManager.popMatrix();
		}

		// FIX 1: Restaura o matrix mode para GL_MODELVIEW (5888) após manipular a
		// matrix de textura (GL_TEXTURE = 5890). Sem isso, o OpenGL fica com a matrix
		// de textura ativa e todos os renders posteriores ficam corrompidos:
		// o inventário do survival não renderiza pois as texturas ficam invisíveis.
		GlStateManager.matrixMode(5888); // GL_MODELVIEW

		// FIX 2: Restaura o framebuffer principal após o pipeline de shaders.
		// Sem isso, o framebuffer ativo é o buffer intermediário do blur ("swap"),
		// causando tela ciana e cliques não funcionando no inventário.
		mc.getFramebuffer().bindFramebuffer(true);
	}

	@Inject(method = "updateShaderGroupSize", at = @At("RETURN"))
	public void updateShaders(int width, int height, CallbackInfo ci) {

		if (ShaderLinkHelper.getStaticShaderLinkHelper() == null || !OpenGlHelper.shadersSupported)
			return;

		PostProcessingEvent event = CLIENT.bus.post(
				new PostProcessingEvent(PostProcessingEvent.Type.UPDATE));

		for (ShaderGroup group : event.groups) {
			group.createBindFramebuffers(width, height);
		}
	}

	// =========================
	// Camera Rotation (Freelook)
	// =========================

	private static float rotationYaw;
	private static float prevRotationYaw;
	private static float rotationPitch;
	private static float prevRotationPitch;

	@Inject(method = "orientCamera", at = @At("HEAD"))
	public void orientCamera(float partialTicks, CallbackInfo ci) {

		Entity view = mc.getRenderViewEntity();

		rotationYaw = view.rotationYaw;
		prevRotationYaw = view.prevRotationYaw;
		rotationPitch = view.rotationPitch;
		prevRotationPitch = view.prevRotationPitch;

		float roll = 0;

		CameraRotateEvent event = CLIENT.bus.post(
				new CameraRotateEvent(rotationYaw, rotationPitch, roll));

		rotationYaw = event.yaw;
		rotationPitch = event.pitch;
		roll = event.roll;

		event = CLIENT.bus.post(
				new CameraRotateEvent(prevRotationYaw, prevRotationPitch, roll));

		prevRotationYaw = event.yaw;
		prevRotationPitch = event.pitch;

		GlStateManager.rotate(event.roll, 0, 0, 1);
	}

	@Redirect(method = "orientCamera", at = @At(value = "FIELD",
			target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
	public float getRotationYaw(Entity entity) {
		return rotationYaw;
	}

	@Redirect(method = "orientCamera", at = @At(value = "FIELD",
			target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F"))
	public float getPrevRotationYaw(Entity entity) {
		return prevRotationYaw;
	}

	@Redirect(method = "orientCamera", at = @At(value = "FIELD",
			target = "Lnet/minecraft/entity/Entity;rotationPitch:F"))
	public float getRotationPitch(Entity entity) {
		return rotationPitch;
	}

	@Redirect(method = "orientCamera", at = @At(value = "FIELD",
			target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F"))
	public float getPrevRotationPitch(Entity entity) {
		return prevRotationPitch;
	}

	// =========================
	// Player Head Rotation
	// =========================

	@Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
	public void lookinAround(EntityPlayerSP player, float yaw, float pitch) {

		PlayerHeadRotateEvent event = new PlayerHeadRotateEvent(yaw, pitch);
		CLIENT.bus.post(event);

		if (!event.cancelled) {
			player.setAngles(event.yaw, event.pitch);
		}
	}

	@Inject(method = "renderWorldPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;setupCameraTransform(FI)V", shift = At.Shift.AFTER))
    private void onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
        FrustumCullingMod.getInstance().onRenderTick(partialTicks);
    }

	// =========================
	// FOV
	// =========================

	@Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true)
	public void getFov(float partialTicks, boolean useFOVSetting,
			CallbackInfoReturnable<Float> cir) {

		cir.setReturnValue(
				CLIENT.bus.post(new FovEvent(cir.getReturnValue(), partialTicks)).fov);
	}

	// =========================
	// Block Highlight
	// =========================

	@Redirect(method = "renderWorldPass", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/EntityRenderer;isDrawBlockOutline()Z"))
	public boolean overrideCanDraw(EntityRenderer renderer) {
		return true;
	}

	@Redirect(method = "renderWorldPass", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z",
			ordinal = 0))
	public boolean overrideWetBlockHighlight(Entity entity, Material material) {

		boolean inside = entity.isInsideOfMaterial(material);
		boolean draw = inside && isDrawBlockOutline();

		if (inside && CLIENT.bus.post(
				new BlockHighlightRenderEvent(mc.objectMouseOver,
						AccessMinecraft.getInstance().getTimerSC().renderPartialTicks)).cancelled) {
			return false;
		}

		return draw;
	}

	@Redirect(method = "renderWorldPass", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z",
			ordinal = 1))
	public boolean overrideBlockHighlight(Entity entity, Material material) {

		boolean inside = entity.isInsideOfMaterial(material);
		boolean result = inside || !isDrawBlockOutline();

		if (!inside && CLIENT.bus.post(
				new BlockHighlightRenderEvent(mc.objectMouseOver,
						AccessMinecraft.getInstance().getTimerSC().renderPartialTicks)).cancelled) {
			return true;
		}

		return result;
	}

}