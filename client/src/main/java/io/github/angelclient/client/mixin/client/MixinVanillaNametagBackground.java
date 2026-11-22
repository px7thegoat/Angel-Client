package io.github.angelclient.client.mixin.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.angelclient.client.mod.impl.NametagsMod;

/**
 * Intercepta o fundo preto translucido E o texto desenhados pelo metodo
 * vanilla {@code Render#renderLivingLabel} para TODAS as entidades:
 *   - Outros jogadores sem tag Angel Client
 *   - NPCs / ArmorStands com holograma
 *   - Mobs com nome customizado
 *
 * Registro em mixins.angelclient.json:
 *   "client.MixinVanillaNametagBackground"
 */
@Mixin(Render.class)
public abstract class MixinVanillaNametagBackground<T extends Entity> {

    // -- Toggle: Nametag Background -------------------------------------------

    /**
     * Redireciona worldRenderer.pos() para no-op quando background=OFF.
     * Os 4 vertices do quad nao sao adicionados, mas begin() e draw()
     * rodam normalmente com buffer vazio — sem retangulo, sem crash.
     */
    @Redirect(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;color(FFFF)Lnet/minecraft/client/renderer/WorldRenderer;"
        )
    )
    private WorldRenderer angelclient_suppressBackgroundColor(
            WorldRenderer worldRenderer, float r, float g, float b, float a) {

        NametagsMod mod = NametagsMod.getInstance();
        if (mod != null && mod.isEnabled() && !mod.isNametagBackground()) {
            return worldRenderer.color(r, g, b, 0.0F); // torna o fundo invisível de forma segura
        }

        return worldRenderer.color(r, g, b, a);
    }

    // -- Toggle: Text Shadow --------------------------------------------------

    /**
     * O vanilla chama drawString(String, int, int, int) — 3 ints, sem
     * parametro shadow (equivale a shadow=false hardcoded).
     * Descritor correto: (Ljava/lang/String;III)I
     *
     * A versao anterior usava (Ljava/lang/String;IIII)I (4 ints) — descritor
     * errado, o Redirect nunca era aplicado, por isso o shadow so funcionava
     * no nick proprio (coberto pelo MixinNametagRenderer).
     *
     * Duas passagens dentro de renderLivingLabel:
     *   ordinal=0 -> fantasma (sem depth, cor com alpha baixo)
     *   ordinal=1 -> solido   (com depth, cor final)
     */
    @Redirect(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            ordinal = 0
        )
    )
    private int angelclient_vanillaNametagShadow_pass1(
            FontRenderer fontRenderer, String text, int x, int y, int color) {
        return fontRenderer.drawString(text, x, y, color, isShadowEnabled());
    }

    @Redirect(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I",
            ordinal = 1
        )
    )
    private int angelclient_vanillaNametagShadow_pass2(
            FontRenderer fontRenderer, String text, int x, int y, int color) {
        return fontRenderer.drawString(text, x, y, color, isShadowEnabled());
    }

    // -- Helper ---------------------------------------------------------------

    private static boolean isShadowEnabled() {
        NametagsMod mod = NametagsMod.getInstance();
        return mod != null && mod.isEnabled() && mod.isTextShadow();
    }
}