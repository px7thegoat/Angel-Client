package io.github.angelclient.client.mod.impl;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import net.minecraft.client.settings.KeyBinding;

public class MushStatsMod extends Mod {

    private static MushStatsMod instance;

    @Expose
    @Option
    private boolean nametagLevelEnabled = true;

    @Option
    private KeyBinding key = null;

    public MushStatsMod() {
        instance = this;
    }

    @Override
    public String getId() {
        return "mush_stats";
    }

    @Override
    public String getName() {
        return "Mush Stats";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.UTILITY;
    }

    @Override
    public void onRegister() {
        super.onRegister();
        if (key == null)
            key = new KeyBinding(getTranslationKey() + ".key", Keyboard.KEY_N, Client.KEY_CATEGORY);
        Client.INSTANCE.registerKeyBinding(key);
    }

    public boolean isNametagLevelEnabled() {
        return isEnabled() && nametagLevelEnabled;
    }

    public void setNametagLevelEnabled(boolean enabled) {
        this.nametagLevelEnabled = enabled;
    }

    public int getKeyCode() {
        return key.getKeyCode();
    }

    public static MushStatsMod getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}