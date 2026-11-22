package io.github.angelclient.client.mixin.mod;

import io.github.angelclient.client.mod.impl.FastRecraftMod;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class MixinFastRecraftMod {

    @Inject(method = "initGui", at = @At("HEAD"))
    private void onCraftingGuiOpen(CallbackInfo ci) {
        if (!((Object) this instanceof GuiCrafting)) return;
        FastRecraftMod mod = FastRecraftMod.INSTANCE;
        if (mod != null && mod.isEnabled()) {
            mod.onCraftingGuiOpen((GuiCrafting) (Object) this);
        }
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onCraftingGuiClose(CallbackInfo ci) {
        if (!((Object) this instanceof GuiCrafting)) return;
        FastRecraftMod mod = FastRecraftMod.INSTANCE;
        if (mod != null && mod.isEnabled()) {
            mod.onCraftingGuiClose((GuiCrafting) (Object) this);
        }
    }
}
