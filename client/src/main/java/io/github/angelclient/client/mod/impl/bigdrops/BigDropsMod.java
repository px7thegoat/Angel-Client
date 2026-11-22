package io.github.angelclient.client.mod.impl.bigdrops;

import com.google.gson.annotations.Expose;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class BigDropsMod extends Mod {

    // Atribuído no construtor — garantia que é a instância registrada pelo Client
    public static BigDropsMod INSTANCE;

    @Expose
    @Option
    @Slider(min = 0.1f, max = 10.0f, step = 0.1f, format = "%s x")
    public float defaultScale = 3.0f;

    @Expose
    @Option
    public boolean scaleAll = false;

    @Expose
    public Map<String, Float> itemScales = new HashMap<>();

    public BigDropsMod() {
        INSTANCE = this;
        itemScales.put("minecraft:diamond", 3.0f);
        itemScales.put("minecraft:diamond_sword", 2.5f);
        itemScales.put("minecraft:nether_star", 4.0f);
    }

    @Override
    public String getId() { return "big_drops"; }

    @Override
    public ModCategory getCategory() { return ModCategory.VISUAL; }

    @Override
    public boolean isEnabledByDefault() { return false; }

    public float getScale(String registryName) {
        if (registryName == null) return 1.0f;
        String key = registryName.toLowerCase();
        if (itemScales.containsKey(key)) return itemScales.get(key);
        return scaleAll ? defaultScale : 1.0f;
    }
}