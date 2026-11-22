package io.github.angelclient.client.mixin.mod;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.angelclient.client.mod.impl.V1_7VisualsMod;
import io.github.angelclient.client.util.access.AccessMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.Entity;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.Item;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;

/**
 * Animatium-Legacy 1.7 Animations ported to AngelClient.
 *
 * Based on: https://github.com/Legacy-Visuals-Project/Animatium-Legacy
 * Credits: Polyfrost / Legacy Visuals Project
 */
public class MixinV1_7VisualsMod {

    // =========================================================================
    // FIRST PERSON — ItemRenderer
    // =========================================================================
    @Mixin(ItemRenderer.class)
    public static abstract class MixinItemRenderer {
        @Shadow private @Final Minecraft mc;
        @Shadow private ItemStack itemToRender;
        @Shadow protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

        // Animatium: capture f1 (the real swing progress from player)
        @Unique private static float angelclient$f1 = 0.0F;

        @ModifyVariable(
                method = "renderItemInFirstPerson",
                at = @At(value = "STORE"),
                index = 4
        )
        private float captureF1(float f1) {
            angelclient$f1 = f1;
            return f1;
        }

        // Animatium: 1.7 item rotation (50 degrees instead of 45)
        @Redirect(method = "transformFirstPersonItem",
                  at = @At(value = "INVOKE",
                           target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V",
                           ordinal = 0))
        public void rotateItem(float angle, float x, float y, float z) {
            GlStateManager.rotate(V1_7VisualsMod.enabled ? 50.0F : angle, x, y, z);
        }

        // Animatium: 1.7 Block-Hitting — pass real swing progress when blocking
        // Uses Slice to target ONLY the blocking transformFirstPersonItem call
        @ModifyArg(method = "renderItemInFirstPerson",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V"),
                   slice = @Slice(
                           from = @At(value = "INVOKE",
                                      target = "Lnet/minecraft/client/renderer/ItemRenderer;performDrinking(Lnet/minecraft/client/entity/AbstractClientPlayer;F)V"),
                           to = @At(value = "INVOKE",
                                    target = "Lnet/minecraft/client/renderer/ItemRenderer;doBowTransformations(FLnet/minecraft/client/entity/AbstractClientPlayer;)V")
                   ),
                   index = 1)
        private float useRealSwingForBlocking(float swingProgress) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.blocking) {
                return angelclient$f1;
            }
            return swingProgress;
        }

        // Animatium: 1.7 Blocking position (scale + translate)
        @Inject(method = "doBlockTransformations", at = @At("RETURN"))
        public void oldBlocking(CallbackInfo callback) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.blocking) {
                V1_7VisualsMod.oldBlocking();
            }
        }

        // Animatium: 1.7 Eating/Drinking animation
        @Inject(method = "performDrinking", at = @At("HEAD"), cancellable = true)
        public void oldDrinking(AbstractClientPlayer clientPlayer, float partialTicks, CallbackInfo callback) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.eatingAndDrinking) {
                callback.cancel();
                V1_7VisualsMod.oldDrinking(itemToRender, clientPlayer, partialTicks);
            }
        }

        // Animatium: 1.7 Bow position in first person
        @Inject(method = "doBowTransformations",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
        private void oldBowTransform(float partialTicks, AbstractClientPlayer clientPlayer, CallbackInfo ci) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.bow) {
                GlStateManager.rotate(-335.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.5F, 0.0F);
            }
        }

        @Inject(method = "doBowTransformations", at = @At(value = "TAIL"))
        private void oldBowTransformPost(float partialTicks, AbstractClientPlayer clientPlayer, CallbackInfo ci) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.bow) {
                GlStateManager.translate(0.0F, -0.5F, 0.0F);
                GlStateManager.rotate(50.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(335.0F, 0.0F, 0.0F, 1.0F);
            }
        }

        // Animatium: 1.7 First-person item positions (non-3D items like fishing rod)
        @Inject(method = "renderItemInFirstPerson",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"))
        private void firstPersonItemPositions(float partialTicks, CallbackInfo ci) {
            if (V1_7VisualsMod.enabled && itemToRender != null) {
                RenderItem renderItem = mc.getRenderItem();
                if (renderItem != null && !renderItem.shouldRenderItemIn3D(itemToRender)) {
                    if (V1_7VisualsMod.instance.rod && itemToRender.getItem().shouldRotateAroundWhenRendering()) {
                        GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                        overflowAnimations$itemTransforms();
                    } else if (!(itemToRender.getItem() instanceof ItemSword)) {
                        overflowAnimations$itemTransforms();
                    }
                }
            }
        }

        @Unique
        private static void overflowAnimations$itemTransforms() {
            float scale = 1.5F / 1.7F;
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.rotate(5.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(-0.29F, 0.149F, -0.0328F);
        }
    }

    // =========================================================================
    // CAMERA — Smooth sneaking
    // =========================================================================
    @Mixin(EntityRenderer.class)
    public static abstract class MixinEntityRenderer {
        @Shadow private Minecraft mc;
        private float eyeHeightSubtractor;

        @Redirect(method = "orientCamera",
                  at = @At(value = "INVOKE",
                           target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
        public float smoothSneaking(Entity entity) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.sneaking && entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                float height = player.getEyeHeight();

                if (player.isSneaking()) {
                    height += 0.08F;
                }

                float target = player.isSneaking() ? 0.08F : 0.0F;

                if (Math.abs(eyeHeightSubtractor - target) > 0.005F) {
                    if (eyeHeightSubtractor < target) {
                        eyeHeightSubtractor += (target - eyeHeightSubtractor) / 2.3F;
                    } else {
                        eyeHeightSubtractor -= (eyeHeightSubtractor - target) / 5.0F;
                    }
                } else {
                    eyeHeightSubtractor = target;
                }

                return height - eyeHeightSubtractor;
            }
            return entity.getEyeHeight();
        }
    }

    // =========================================================================
    // ARMOR — 1.7 damage tint
    // =========================================================================
    @Mixin(LayerArmorBase.class)
    public static class MixinLayerArmorBase {
        @Inject(method = "shouldCombineTextures", at = @At("HEAD"), cancellable = true)
        public void oldArmour(CallbackInfoReturnable<Boolean> callback) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.armourDamage) {
                callback.setReturnValue(true);
            }
        }
    }

    // =========================================================================
    // THIRD PERSON — ModelBiped arm rotation (Animatium: oldArmPosition)
    // =========================================================================
    @Mixin(ModelBiped.class)
    public static class MixinModelBiped {
        @Shadow public ModelRenderer bipedRightArm;
        @Shadow public int heldItemRight;

        // Animatium: reset arm Y rotation when blocking in 3rd person
        @Inject(method = "setRotationAngles", at = @At("TAIL"))
        public void oldThirdPersonBlock(float p1, float p2, float p3, float p4, float p5, float p6, Entity entityIn, CallbackInfo ci) {
            if (V1_7VisualsMod.enabled && V1_7VisualsMod.instance.blocking && this.heldItemRight == 3) {
                this.bipedRightArm.rotateAngleY = 0.0F;
            }
        }
    }

    // =========================================================================
    // THIRD PERSON — LayerHeldItem (Animatium: thirdPersonBlock + thirdTransformations)
    // =========================================================================
    @Mixin(LayerHeldItem.class)
    public static class MixinLayerHeldItem {

        // Animatium: Old sneaking item offset in third person
        @Inject(method = "doRenderLayer",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/model/ModelBiped;postRenderArm(F)V"))
        private void oldSneakingOffset(EntityLivingBase entitylivingbaseIn, float f, float g, float partialTicks, float h, float i, float j, float scale, CallbackInfo ci) {
            if (V1_7VisualsMod.enabled && entitylivingbaseIn.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
        }

        // Animatium: Cancel vanilla sneaking translate
        @Redirect(method = "doRenderLayer",
                  at = @At(value = "INVOKE",
                           target = "Lnet/minecraft/entity/EntityLivingBase;isSneaking()Z"))
        private boolean cancelNewSneaking(EntityLivingBase instance) {
            return instance.isSneaking() && !V1_7VisualsMod.enabled;
        }

        // Animatium: Third-person item positions (blocking + item transforms)
        @Inject(method = "doRenderLayer",
                at = @At(value = "INVOKE",
                         target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"))
        private void thirdPersonItemPositions(EntityLivingBase entitylivingbaseIn, float f, float g, float partialTicks, float h, float i, float j, float s, CallbackInfo ci) {
            if (!V1_7VisualsMod.enabled) return;

            // 1.7 Third-person blocking position (sword in front of face)
            if (V1_7VisualsMod.instance.blocking
                    && entitylivingbaseIn instanceof AbstractClientPlayer
                    && ((AbstractClientPlayer) entitylivingbaseIn).isBlocking()) {
                GlStateManager.translate(0.05F, 0.0F, -0.1F);
                GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-10.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
            }

            // 1.7 Third-person item transforms
            if (entitylivingbaseIn instanceof EntityPlayer) {
                ItemStack stack = entitylivingbaseIn.getHeldItem();
                if (stack != null) {
                    Item item = stack.getItem();
                    RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
                    if (renderItem != null && !renderItem.shouldRenderItemIn3D(stack)) {
                        float scale = 1.5F * 0.625F;
                        if (item instanceof ItemBow) {
                            GlStateManager.rotate(-12.0F, 0.0f, 1.0f, 0.0f);
                            GlStateManager.rotate(-7.0F, 1.0f, 0.0f, 0.0f);
                            GlStateManager.rotate(10.0F, 0.0f, 0.0f, 1.0f);
                            GlStateManager.rotate(1.0F, 0.0f, 1.0f, 0.0f);
                            GlStateManager.rotate(-4.5F, 1.0f, 0.0f, 0.0f);
                            GlStateManager.rotate(-1.5F, 0.0f, 0.0f, 1.0f);
                            GlStateManager.translate(0.022F, -0.01F, -0.108F);
                            GlStateManager.scale(scale, scale, scale);
                        } else if (item.isFull3D()) {
                            if (item.shouldRotateAroundWhenRendering()) {
                                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                            }
                            GlStateManager.scale(scale / 0.85F, scale / 0.85F, scale / 0.85F);
                            GlStateManager.rotate(-2.4F, 0.0F, 1.0F, 0.0F);
                            GlStateManager.rotate(-20.0F, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(4.5F, 0.0F, 0.0F, 1.0F);
                            GlStateManager.translate(-0.013F, 0.01F, 0.125F);
                        } else {
                            scale = 1.5F * 0.375F;
                            GlStateManager.scale(scale / 0.55, scale / 0.55, scale / 0.55);
                            GlStateManager.rotate(-195.0F, 0.0F, 1.0F, 0.0F);
                            GlStateManager.rotate(-168.0F, 1.0F, 0.0F, 0.0F);
                            GlStateManager.rotate(15.0F, 0.0F, 0.0F, 1.0F);
                            GlStateManager.translate(-0.047F, -0.28F, 0.038F);
                        }
                    }
                }
            }
        }
    }

}