package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;

public class GuiScaleMod extends Mod {

    @Expose
    @Option
    @Slider(min = 0.5f, max = 2.0f, step = 0.05f, format = "x%.2f")
    public float hotbarScale = 1.0f;

    @Expose
    @Option
    @Slider(min = 1.0f, max = 1.5f, step = 0.05f, format = "x%.2f")
    public float inventoryScale = 1.0f;

    @Override
    public String getId() { return "guiscale"; }

    @Override
    public String getName() { return "Gui Scale"; }

    @Override
    public ModCategory getCategory() { return ModCategory.HUD; }

    @Override
    public void onDisable() {
        hotbarScale = 1.0f;
        inventoryScale = 1.0f;
    }
}