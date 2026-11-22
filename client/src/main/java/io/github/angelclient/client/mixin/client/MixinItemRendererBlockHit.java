package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.V1_7VisualsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block Hit 1.7 style para primeira pessoa.
 *
 * O que esse mixin faz:
 *  - Quando o player está quebrando um bloco (mining) com V1_7Visuals ativo,
 *    aplica a pose/animação estilo 1.7 (braço levantado/inclinado para frente).
 *  - Suporte a textura customizada: coloque sua textura em
 *    assets/<seu_modid>/textures/misc/blockhit_overlay.png
 *    e ela será renderizada sobre a tela durante o block hit.
 *
 * Como funciona o block hit 1.7:
 *  Em 1.7, quando o player minera, o item na mão era transladado para frente
 *  e rotacionado levemente, dando a sensação de "empurrar" o bloco.
 *  Em 1.8+, a animação é feita por swingProgress mas sem esse efeito visual.
 *
 * Registro no mixins.json:
 *   "client.MixinItemRendererBlockHit"
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRendererBlockHit {

    // -------------------------------------------------------------------------
    // Textura customizada do blockhit overlay (opcional)
    // Coloque em: assets/<modid>/textures/misc/blockhit_overlay.png
    // Se não quiser textura, deixe BLOCKHIT_TEXTURE = null
    // -------------------------------------------------------------------------
    private static final ResourceLocation BLOCKHIT_TEXTURE =
            new ResourceLocation("angelclient", "textures/misc/blockhit_overlay.png");
    // Para desativar a textura, comente a linha acima e descomente:
    // private static final ResourceLocation BLOCKHIT_TEXTURE = null;

    @Shadow private Minecraft mc;
    @Shadow private ItemStack itemToRender;

    /**
     * Injeta APÓS o transformFirstPersonItem (que já aplicou equip/swing).
     * Aqui verificamos se o player está ativamente minerando e aplicamos
     * a pose adicional estilo 1.7.
     */
    @Inject(method = "transformFirstPersonItem", at = @At("RETURN"))
    public void applyBlockHit17Style(float equipProgress, float swingProgress, CallbackInfo ci) {
        if (!V1_7VisualsMod.enabled) return;
        if (mc.thePlayer == null) return;

        // Detecta se o player está ativamente minerando um bloco
        boolean isMining = mc.thePlayer.isUsingItem() == false
                && mc.playerController != null
                && mc.playerController.getIsHittingBlock();

        if (!isMining) return;

        // Progresso do mining (0.0 = início, 1.0 = bloco quebrado)
        float miningProgress = mc.theWorld != null
                ? getMiningProgress()
                : 0.0f;

        // ----------------------------------------------------------------
        // Pose estilo 1.7: translada o item para frente e rotaciona levemente
        // simulando o "soco" no bloco.
        //
        // Valores baseados no comportamento original do 1.7.10:
        //   - translate Z negativo = empurra o item para frente (em direção à câmera)
        //   - rotate X positivo = inclina o item para baixo (como se batesse no bloco)
        //   - A intensidade varia com o swingProgress para animar suavemente
        // ----------------------------------------------------------------

        float intensity = MathHelper.sin(swingProgress * (float) Math.PI);

        // Translada o item levemente para frente durante o swing
        GlStateManager.translate(
                intensity * 0.05f,   // X: leve deslocamento lateral
                intensity * -0.02f,  // Y: sobe levemente
                intensity * -0.1f    // Z: avança em direção ao bloco
        );

        // Rotaciona como se o braço "batesse" no bloco
        GlStateManager.rotate(
                intensity * -25.0f,  // ângulo: inclina para frente (negativo = para frente)
                1.0f, 0.0f, 0.0f     // eixo X
        );
        GlStateManager.rotate(
                intensity * 10.0f,   // leve rotação lateral
                0.0f, 1.0f, 0.0f     // eixo Y
        );

        // Escala suave durante o hit (ligeiramente menor no pico do swing)
        float scaleEffect = 1.0f - intensity * 0.04f;
        GlStateManager.scale(scaleEffect, scaleEffect, scaleEffect);

        // ----------------------------------------------------------------
        // Textura customizada de overlay (estilo "crack" ou hit flash)
        // Renderiza sobre a tela quando o player está minerando
        // ----------------------------------------------------------------
        if (BLOCKHIT_TEXTURE != null && miningProgress > 0.0f && miningProgress < 1.0f) {
            renderBlockHitOverlay(miningProgress);
        }
    }

    /**
     * Renderiza um overlay 2D na tela durante o block hit.
     * A opacidade aumenta conforme o progresso do mining.
     *
     * Para usar: crie o arquivo de textura em
     * src/main/resources/assets/angelclient/textures/misc/blockhit_overlay.png
     * Tamanho recomendado: 16x16 ou 32x32, fundo transparente.
     */
    private void renderBlockHitOverlay(float progress) {
        // Salva estado OpenGL
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.disableAlpha();

        // Bind da textura customizada
        TextureManager textureManager = mc.getTextureManager();
        textureManager.bindTexture(BLOCKHIT_TEXTURE);

        // Opacidade baseada no progresso (mais opaco quanto mais perto de quebrar)
        float alpha = Math.min(0.6f, progress * 0.8f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);

        // Renderiza como quad 2D na tela (coordenadas de tela normalizadas)
        int w = mc.displayWidth;
        int h = mc.displayHeight;

        net.minecraft.client.renderer.Tessellator tess =
                net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        wr.pos(0, h, -90).tex(0, 1).endVertex();
        wr.pos(w, h, -90).tex(1, 1).endVertex();
        wr.pos(w, 0, -90).tex(1, 0).endVertex();
        wr.pos(0, 0, -90).tex(0, 0).endVertex();
        tess.draw();

        // Restaura estado OpenGL
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Obtém o progresso atual do mining via reflection/acesso ao playerController.
     * Retorna 0.0f se não estiver minerando.
     */
    private float getMiningProgress() {
        try {
            // Acessa o campo curBlockDamageMP do PlayerControllerMP via reflection
            java.lang.reflect.Field f =
                    mc.playerController.getClass().getDeclaredField("curBlockDamageMP");
            f.setAccessible(true);
            return f.getFloat(mc.playerController);
        } catch (Exception e) {
            return 0.0f;
        }
    }
}
