package io.github.angelclient.client.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import io.github.angelclient.client.cosmetic.Cosmetic;
import io.github.angelclient.client.cosmetic.CosmeticManager;
import io.github.angelclient.client.cosmetic.LocalCosmetic;
import io.github.angelclient.client.cosmetic.LocalCosmeticManager;
import io.github.angelclient.client.cosmetic.render.TextureLoader;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {
    
    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void overrideCape(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
        String name = player.getGameProfile().getName();

        // LocalCosmetic capes (new system) — load texture for vanilla rendering
        String capeId = LocalCosmeticManager.getInstance().getActiveCape(name);
        if (capeId != null) {
            LocalCosmetic cape = LocalCosmeticManager.getInstance().getCape(capeId);
            if (cape != null) {
                String texPath = cape.getTexture();
                if ((texPath == null || texPath.isEmpty()) && cape.getFile() != null && !cape.getFile().endsWith(".obj")) {
                    texPath = cape.getFile();
                }
                if (texPath != null && !texPath.isEmpty()) {
                    ResourceLocation tex = TextureLoader.loadTexture(texPath);
                    if (tex != null) {
                        cir.setReturnValue(tex);
                        return;
                    }
                }
            }
        }

        // CosmeticManager capes (old system)
        Cosmetic cosmetic = CosmeticManager.getInstance().getPlayerCosmetic(name);
        if (cosmetic != null && cosmetic.getTexture() != null) {
            cir.setReturnValue(cosmetic.getTexture());
        }
    }

    @Inject(method = "getLocationSkin", at = @At("RETURN"), cancellable = true)
    private void getSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ResourceLocation skin = cir.getReturnValue();
        
        // Se estiver retornando a skin padrão (Steve/Alex), significa que não encontrou skin premium
        if (skin.equals(net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(player.getUniqueID())) || 
            skin.equals(net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkinLegacy())) {
            
            // Gera um ResourceLocation único para essa skin crackeada
            ResourceLocation customSkin = new ResourceLocation("angelclient", "skins/" + net.minecraft.util.StringUtils.stripControlCodes(player.getName()).toLowerCase());
            
            // Verifica se a textura já foi registrada no TextureManager
            net.minecraft.client.renderer.texture.ITextureObject texture = net.minecraft.client.Minecraft.getMinecraft().getTextureManager().getTexture(customSkin);
            
            if (texture == null) {
                // Inicia o download da skin pela Minotar API (que aceita nomes de usuários, perfeito para piratas)
                String url = "https://minotar.net/skin/" + net.minecraft.util.StringUtils.stripControlCodes(player.getName()) + ".png";
                net.minecraft.client.renderer.ThreadDownloadImageData threadDownloadImageData = new net.minecraft.client.renderer.ThreadDownloadImageData(
                        null, url, net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(player.getUniqueID()), new net.minecraft.client.renderer.ImageBufferDownload()
                );
                net.minecraft.client.Minecraft.getMinecraft().getTextureManager().loadTexture(customSkin, threadDownloadImageData);
            }
            
            // Substitui o retorno do Steve pela nossa skin customizada baixada da Minotar
            cir.setReturnValue(customSkin);
        }
    }
}