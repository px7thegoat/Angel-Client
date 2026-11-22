package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import io.github.angelclient.client.ui.screen.IngameServerList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;

@Mixin(GuiIngameMenu.class)
public abstract class MixinGuiIngameMenu extends GuiScreen {

    private static final ResourceLocation LOGO_TEXTURE =
            new ResourceLocation("angel_client", "textures/angelclient/logo2.png");

    // Tamanho do logo em GUI pixels
    private static final int LOGO_SIZE = 32;

    // =========================================================================
    // Suprime o título "Game menu" em TODOS os métodos de texto
    // =========================================================================
    @Redirect(method = "drawScreen",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiScreen;drawCenteredString(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;III)V"))
    public void suppressCentered(GuiScreen self, FontRenderer fr, String text, int x, int y, int color) {
        if (!text.equals(I18n.format("menu.game")))
            self.drawCenteredString(fr, text, x, y, color);
    }

    @Redirect(method = "drawScreen",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    public int suppressShadow(FontRenderer fr, String text, float x, float y, int color) {
        if (text.equals(I18n.format("menu.game"))) return 0;
        return fr.drawStringWithShadow(text, x, y, color);
    }

    @Redirect(method = "drawScreen",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    public int suppressPlain(FontRenderer fr, String text, int x, int y, int color) {
        if (text.equals(I18n.format("menu.game"))) return 0;
        return fr.drawString(text, x, y, color);
    }

    // =========================================================================
    // Desenha o logo centralizado acima do primeiro botão
    // O primeiro botão "Back to Game" fica em height/4 + 48
    // Logo fica com LOGO_SIZE + 8px de margem acima dele
    // =========================================================================
    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();

        int logoX = (width - LOGO_SIZE) / 2;
        int logoY = height / 4 - 40 + 12;

        mc.getTextureManager().bindTexture(LOGO_TEXTURE);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();

        net.minecraft.client.renderer.Tessellator tess =
                net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        wr.pos(logoX,             logoY + LOGO_SIZE, 0).tex(0, 1).endVertex();
        wr.pos(logoX + LOGO_SIZE, logoY + LOGO_SIZE, 0).tex(1, 1).endVertex();
        wr.pos(logoX + LOGO_SIZE, logoY,             0).tex(1, 0).endVertex();
        wr.pos(logoX,             logoY,             0).tex(0, 0).endVertex();
        tess.draw();

        GlStateManager.disableBlend();
    }

    @Inject(method = "initGui", at = @At("RETURN"))
    public void replaceAndAddButtons(CallbackInfo callback) {

        boolean replay = false;

        buttonList.add(new GuiButton(5000,
                replay ? ((GuiButton) buttonList.get(2)).xPosition + 102 : width / 2 - 100,
                replay ? ((GuiButton) buttonList.get(2)).yPosition       : height / 4 + 56,
                98, 20, I18n.format("angel_client.mod.screen.title")));

        if (!replay) {
            buttonList.add(new GuiButton(5001,
                    width / 2 + 2, height / 4 + 56,
                    98, 20, I18n.format("angel_client.servers")));
        }

    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    public void actionPerformed(GuiButton button, CallbackInfo callback) {
        if (button.id == 5000) {
            mc.displayGuiScreen(new ModsScreen());
        } else if (button.id == 5001) {
            mc.displayGuiScreen(new IngameServerList(this));
        }
    }
}