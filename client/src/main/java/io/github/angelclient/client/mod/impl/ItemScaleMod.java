package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.TransformFirstPersonItemEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import net.minecraft.client.renderer.GlStateManager;

public class ItemScaleMod extends Mod {

    public static final ItemScaleMod INSTANCE = new ItemScaleMod();

    @Expose
    @Option
    @Slider(min = 0.1f, max = 10.0f, step = 0.1f, format = "%s x")
    public float scale = 1.0f;

    @Expose
    @Option
    @Slider(min = -2.0f, max = 10.0f, step = 0.1f, format = "%s")
    public float posX = 0.0f;

    @Expose
    @Option
    @Slider(min = -2.0f, max = 10.0f, step = 0.1f, format = "%s")
    public float posY = 0.0f;

    @Expose
    @Option
    @Slider(min = -2.0f, max = 10.0f, step = 0.1f, format = "%s")
    public float posZ = 0.0f;

    @Override
    public String getId() {
        return "item_scale";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.UTILITY;
    }

    @EventHandler
    public void onTransformItem(TransformFirstPersonItemEvent event) {
        GlStateManager.translate(posX, posY, posZ);
        GlStateManager.scale(scale, scale, scale);
    }
}