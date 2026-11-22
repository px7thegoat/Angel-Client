package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.util.render.SmartAnimations;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite {

    @Inject(method = "updateAnimation", at = @At("HEAD"), cancellable = true)
    private void updateAnimation(CallbackInfo ci) {

        TextureAtlasSprite sprite = (TextureAtlasSprite)(Object)this;

        if (!SmartAnimations.isRendered(sprite.getIconName())) {
            ci.cancel();
        }

    }

}