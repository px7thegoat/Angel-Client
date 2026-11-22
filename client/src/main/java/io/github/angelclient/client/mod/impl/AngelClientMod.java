package io.github.angelclient.client.mod.impl;

import org.lwjgl.input.Keyboard;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.mod.ConfigOnlyMod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.ui.screen.mods.ModsScreen;
import io.github.angelclient.client.util.font.Font;
import io.github.angelclient.client.util.font.SlickFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class AngelClientMod extends ConfigOnlyMod {

    public static AngelClientMod instance;

    @Expose
    @Option
    public boolean fancyMainMenu = true;

    @Expose
    @Option
    public boolean logoInInventory = true;

    // ✅ FIX: não inicializar inline — getTranslationKey() não pode ser
    // chamado antes do objeto estar completamente construído
    @Option
    public KeyBinding modsKey = null;

    @Option
    public KeyBinding editHudKey = null;

    @Expose
    @Option
    public boolean smoothUIColours = true;

    @Expose
    @Option
    public boolean roundedUI = true;

    @Expose
    @Option
    public boolean buttonClicks = true;

    @Expose
    @Option
    public boolean smoothScrolling = true;

    @Expose
    @Option
    public boolean fancyFont = true;

    @Override
    public String getId() {
        return "angel_client";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.GENERAL;
    }

    @Override
    public void onRegister() {
        super.onRegister();

        instance = this;

        // ✅ FIX: inicializar aqui, onde getTranslationKey() já funciona corretamente
        if (modsKey == null)
            modsKey = new KeyBinding(getTranslationKey() + ".mods", Keyboard.KEY_RSHIFT, Client.KEY_CATEGORY);

        if (editHudKey == null)
            editHudKey = new KeyBinding(getTranslationKey() + ".edit_hud", 0, Client.KEY_CATEGORY);

        Client.INSTANCE.registerKeyBinding(modsKey);
        Client.INSTANCE.registerKeyBinding(editHudKey);
    }

    public static Font getFont() {
        if (instance.fancyFont) {
            return SlickFontRenderer.DEFAULT;
        } else {
            return (Font) Minecraft.getMinecraft().fontRendererObj;
        }
    }

    @Override
    public void postOptionChange(String key, Object value) {
        super.postOptionChange(key, value);

        if (key.equals("fancyFont") && mc.currentScreen instanceof ModsScreen) {
            ModsScreen screen = (ModsScreen) mc.currentScreen;
            screen.updateFont();
        }
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }
}