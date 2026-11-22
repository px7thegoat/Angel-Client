package io.github.angelclient.client.mixin.client;

import io.github.angelclient.client.mod.impl.V1_7VisualsMod;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishingRod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderItem.class)
public class MixinRenderItem {

    /**
     * Replicando a lógica do Badlion: força o ItemFishingRod a retornar true em shouldRotateAroundWhenRendering
     * quando as animações 1.7 estão ativas. Isso afeta como o item é posicionado na mão.
     */
    @Redirect(method = "renderItemModelForEntity",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;shouldRotateAroundWhenRendering()Z"))
    public boolean rodRotationFix(Item item) {
        if (V1_7VisualsMod.enabled && item instanceof ItemFishingRod) {
            return true;
        }
        return item.shouldRotateAroundWhenRendering();
    }
}
