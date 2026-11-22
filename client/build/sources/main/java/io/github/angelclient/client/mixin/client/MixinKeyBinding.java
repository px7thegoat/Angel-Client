package io.github.angelclient.client.mixin.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding {

    @Overwrite
    public int compareTo(KeyBinding other) {
        String cat1 = ((KeyBinding)(Object)this).getKeyCategory();
        String cat2 = other.getKeyCategory();

        String label1 = cat1 != null ? I18n.format(cat1) : "";
        String label2 = cat2 != null ? I18n.format(cat2) : "";

        int cmp = label1.compareTo(label2);
        if (cmp != 0) return cmp;

        String desc1 = ((KeyBinding)(Object)this).getKeyDescription();
        String desc2 = other.getKeyDescription();

        String dLabel1 = desc1 != null ? I18n.format(desc1) : "";
        String dLabel2 = desc2 != null ? I18n.format(desc2) : "";

        return dLabel1.compareTo(dLabel2);
    }
}