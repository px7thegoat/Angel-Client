package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.angelclient.client.mod.impl.AngelClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

@Mixin(GuiContainer.class)
public abstract class MixinGuiInventory extends GuiScreen {

    @Shadow protected int guiLeft;
    @Shadow protected int guiTop;
    @Shadow protected int xSize;

    private static final ResourceLocation LOGO_TEXTURE =
            new ResourceLocation("angel_client", "textures/angelclient/angel_logo_big.png");

    private static final int LOGO_W = 100; 
    private static final int LOGO_H = 35;

    @Inject(method = "drawScreen", at = @At("RETURN"))
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!((Object) this instanceof GuiInventory)) return;
        if (!AngelClientMod.instance.logoInInventory) return;

        Minecraft mc = Minecraft.getMinecraft();

        // Agora usamos LOGO_W para centralizar e LOGO_H para a altura
        int logoX = this.width - LOGO_W - 10;       
        int logoY = this.height - LOGO_H - 10;

        mc.getTextureManager().bindTexture(LOGO_TEXTURE);
        
        // --- PROTEÇÃO DO OPENGL (Deixa a logo na frente e brilhante) ---
        GlStateManager.disableLighting(); // Remove sombras deixadas pelos itens
        GlStateManager.disableDepth();    // Ignora o 3D e força a imagem na tela
        GlStateManager.enableAlpha();     // Garante que o fundo do PNG fique invisível
        GlStateManager.enableTexture2D(); // Garante que a textura seja aplicada
        // ---------------------------------------------------------------

        // Força a cor branca e a opacidade máxima (1.0 = 100%)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); 
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        
        wr.pos(logoX,          logoY + LOGO_H, 0).tex(0, 1).endVertex(); // Inferior Esquerdo
        wr.pos(logoX + LOGO_W, logoY + LOGO_H, 0).tex(1, 1).endVertex(); // Inferior Direito
        wr.pos(logoX + LOGO_W, logoY,          0).tex(1, 0).endVertex(); // Superior Direito
        wr.pos(logoX,          logoY,          0).tex(0, 0).endVertex(); // Superior Esquerdo
        
        tess.draw();

        // --- DEVOLVE O ESTADO NORMAL AO MINECRAFT ---
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }
}