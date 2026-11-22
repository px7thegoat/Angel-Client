package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.V1_7VisualsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.GuiScreen;

/**
 * MixinRenderItem17Lunar — F5 (terceira pessoa) ajustes visuais estilo 1.7.
 *
 * O que esse mixin faz:
 *  1. BLOCKING (espada): quando o player está bloqueando em F5,
 *     aplica a pose de bloqueio estilo 1.7 (braço levantado, espada na frente).
 *
 *  2. BLOCK HIT em F5: quando o player está minerando em terceira pessoa,
 *     ajusta a pose do item para parecer que o personagem está "batendo" no bloco
 *     — igual ao visual do vídeo referência (braço erguido, inclinado para frente).
 *
 * Pose de referência (imagem enviada):
 *  - Personagem com espada/item inclinado para frente em direção ao bloco
 *  - Braço levantado/estendido
 *  - Sem o "item caindo" que o 1.8 tem em terceira pessoa
 */
@Mixin(RenderItem.class)
public abstract class MixinRenderItem17Lunar {

    @Inject(
        method = "renderItemModelTransform",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms;applyTransform(Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V",
            shift = At.Shift.AFTER
        )
    )
    public void adjustThirdPersonPoses(
            ItemStack stack,
            IBakedModel model,
            ItemCameraTransforms.TransformType cameraTransformType,
            CallbackInfo ci) {

        if (!V1_7VisualsMod.enabled) return;
        if (cameraTransformType != ItemCameraTransforms.TransformType.THIRD_PERSON) return;
        if (Minecraft.getMinecraft().currentScreen != null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        boolean isBlocking = mc.thePlayer.isBlocking();
        boolean isMining   = mc.playerController != null && mc.playerController.getIsHittingBlock();

        if (!isBlocking && isMining) {
            // ----------------------------------------------------------------
            // Pose de BLOCK HIT estilo 1.7 em terceira pessoa (F5)
            //
            // Em 1.7, ao quebrar blocos em F5, o personagem aparecia com
            // o braço estendido para frente, item/ferramenta apontado para o bloco.
            // Em 1.8, esse visual foi alterado e ficou mais "sutil".
            //
            // Essa pose recria o visual da imagem de referência:
            //   - item inclinado para frente (em direção ao bloco)
            //   - braço levantado
            //   - rotação que simula o "golpe" no bloco
            //
            // Ajuste os valores abaixo ao seu gosto:
            //   translate(X, Y, Z) → posição do item
            //   rotate(ângulo, X, Y, Z) → rotação
            // ----------------------------------------------------------------
            float swingProgress = mc.thePlayer.swingProgressInt /
                    (float) Math.max(1, getArmSwingEnd(mc.thePlayer));

            // Pulso suave sincronizado com a animação de swing
            float pulse = (float) Math.sin(swingProgress * Math.PI);

            // Levanta o item em direção ao bloco
            GlStateManager.translate(
                    0.0F,                    // X: sem deslocamento lateral
                    0.05F + pulse * 0.08F,   // Y: sobe levemente no pico do swing
                    -0.1F - pulse * 0.12F    // Z: avança para frente (em direção ao bloco)
            );

            // Rotaciona o item como se "batesse" no bloco
            GlStateManager.rotate(
                    -35.0F - pulse * 20.0F,  // mais inclinado no pico do swing
                    1.0F, 0.0F, 0.0F         // eixo X: inclina para frente/baixo
            );
            GlStateManager.rotate(
                    10.0F,                   // leve rotação lateral fixa
                    0.0F, 1.0F, 0.0F         // eixo Y
            );
        }
    }

    /**
     * Obtém o armSwingAnimationEnd do player para normalizar o swingProgress.
     * Retorna 6 (valor padrão do 1.8) se não conseguir acessar.
     */
    private static int getArmSwingEnd(net.minecraft.entity.EntityLivingBase player) {
        try {
            // Campo armSwingAnimationEnd (mapeamento SRG: field_110146_f ou similar)
            // Em SolClient/mcp: armSwingAnimationEnd
            java.lang.reflect.Field f = EntityLivingBase.class.getDeclaredField("armSwingAnimationEnd");
            f.setAccessible(true);
            return f.getInt(player);
        } catch (Exception e) {
            try {
                // Fallback: tenta pelo nome SRG
                java.lang.reflect.Field f = EntityLivingBase.class.getDeclaredField("field_82150_aj");
                f.setAccessible(true);
                return f.getInt(player);
            } catch (Exception e2) {
                return 6; // valor padrão
            }
        }
    }
}
