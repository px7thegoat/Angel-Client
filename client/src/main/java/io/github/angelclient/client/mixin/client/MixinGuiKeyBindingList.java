package io.github.angelclient.client.mixin.client;

import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(GuiKeyBindingList.class)
public class MixinGuiKeyBindingList {

    @Shadow
    private GuiListExtended.IGuiListEntry[] listEntries;

    // Filtra keybinds com categoria null antes do loop
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/commons/lang3/ArrayUtils;clone([Ljava/lang/Object;)[Ljava/lang/Object;",
            remap = false
        )
    )
    private Object[] filtrarKeybindsNulos(Object[] array) {
        KeyBinding[] bindings = (KeyBinding[]) array;
        return Arrays.stream(bindings)
            .filter(kb -> kb != null && kb.getKeyCategory() != null)
            .toArray(KeyBinding[]::new);
    }

    // Remove os slots nulos que sobram no array ao final da construção
    @Inject(method = "<init>", at = @At("RETURN"))
    private void compactarListEntries(CallbackInfo ci) {
        int count = 0;
        for (GuiListExtended.IGuiListEntry entry : listEntries) {
            if (entry != null) count++;
        }

        GuiListExtended.IGuiListEntry[] compactado = new GuiListExtended.IGuiListEntry[count];
        int i = 0;
        for (GuiListExtended.IGuiListEntry entry : listEntries) {
            if (entry != null) compactado[i++] = entry;
        }

        listEntries = compactado;
    }
}