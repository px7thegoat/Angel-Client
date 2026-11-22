package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import io.github.angelclient.client.util.render.GLRectUtils;
import io.github.angelclient.client.util.font.FontHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

/**
 * MixinGuiButton — portado do MelonClient para o AngelClient.
 *
 * Substitui o visual padrão do GuiButton do Minecraft por botões arredondados
 * com outline colorido, usando o GLRectUtils portado do Melon.
 *
 * INSTRUÇÕES DE INSTALAÇÃO:
 *   1. Coloque este arquivo em:
 *      src/main/java/io/github/angelclient/client/mixin/client/MixinGuiButton.java
 *   2. Adicione "client.MixinGuiButton" na lista "mixins" do arquivo:
 *      src/main/resources/mixins.angelclient.json
 */
@Mixin(GuiButton.class)
public abstract class MixinGuiButton {

    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow protected int width;
    @Shadow protected int height;
    @Shadow public boolean visible;
    @Shadow public boolean enabled;
    @Shadow protected boolean hovered;
    @Shadow public String displayString;

    @Shadow protected abstract int getHoverState(boolean p0);
    @Shadow protected abstract void mouseDragged(Minecraft mc, int mouseX, int mouseY);

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    public void drawButton(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        // Sempre cancelar o vanilla — inclusive quando !visible, para evitar double-draw.
        ci.cancel();

        if (!this.visible) return;

        this.hovered = mouseX >= this.xPosition
                && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width
                && mouseY < this.yPosition + this.height;

        // Estilo Angel Client ModsScreen
        int outlineColor = this.enabled 
                ? (this.hovered ? 0xFF3A3A3A : 0xFF2A2A2A) // BORDER3 / BORDER2
                : 0xFF1A1A1A;

        int bgColor = this.enabled
                ? (this.hovered ? 0xFF1E1E1E : 0xFF141414) // BG4 / BG3
                : 0xFF0A0A0A;

        GLRectUtils.drawRoundedRect(
                this.xPosition, this.yPosition,
                this.xPosition + this.width, this.yPosition + this.height,
                3.0f, bgColor);

        GLRectUtils.drawRoundedOutline(
                this.xPosition, this.yPosition,
                this.xPosition + this.width, this.yPosition + this.height,
                3.0f, 1.0f, outlineColor);

        // Texto do botão
        int textColor = this.enabled ? (this.hovered ? 0xFFFFFFFF : 0xFFEEEEEE) : 0xFF555555;
        int textX = this.xPosition + this.width / 2;
        int textY = this.yPosition + (this.height - 8) / 2;

        if (AngelClientMod.instance != null && AngelClientMod.instance.fancyFont) {
            AngelClientMod.getFont().renderCenteredString(
                    this.displayString.toUpperCase(), textX, textY, textColor);
        } else {
            FontHelper.drawCentered(this.displayString, textX, textY, textColor);
        }

        this.mouseDragged(mc, mouseX, mouseY);
    }

    /**
     * Retorna a cor do outline do botão.
     * Tenta usar a accent colour do AngelClient; usa azul como fallback.
     */
    private static int getAccentColor(boolean enabled, boolean hovered) {
        // Cor base: azul Angel (#5B7FFF) — troque por Client.accentColor se disponível
        int baseAlpha = enabled ? (hovered ? 255 : 150) : 80;
        int r = 91, g = 127, b = 255;
        return (baseAlpha << 24) | (r << 16) | (g << 8) | b;
    }
}