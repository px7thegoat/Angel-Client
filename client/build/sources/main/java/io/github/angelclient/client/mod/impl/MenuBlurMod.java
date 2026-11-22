package io.github.angelclient.client.mod.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.InitialOpenGuiEvent;
import io.github.angelclient.client.event.impl.PostProcessingEvent;
import io.github.angelclient.client.event.impl.RenderGuiBackgroundEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.PrimaryIntegerSettingMod;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.access.AccessShaderGroup;
import io.github.angelclient.client.util.data.Colour;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;

public class MenuBlurMod extends Mod implements PrimaryIntegerSettingMod {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation("minecraft:shaders/post/menu_blur.json");

    @Expose
    @Option
    @Slider(min = 0, max = 100, step = 1)
    private float blur = 8;
    @Expose
    @Option
    @Slider(min = 0, max = 1, step = 0.1F, format = "angel_client.slider.seconds")
    private float fadeTime = 0.1F;
    @Expose
    @Option
    private Colour backgroundColour = new Colour(0, 0, 0, 100);
    private ShaderGroup group;
    private long openTime;

    @Override
    public String getId() {
        return "menu_blur";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    @Override
    public void onRegister() {
        super.onRegister();
        Client.INSTANCE.addResource(RESOURCE_LOCATION, new MenuBlurShader());
    }

    @EventHandler
    public void onOpenGui(InitialOpenGuiEvent event) {
        openTime = System.currentTimeMillis();
    }

    // ✅ FIX BUG 3: Verifica se Fast Render está ligado 
    private boolean isFastRenderEnabled() {
        try {
            Field fastRenderField = mc.gameSettings.getClass().getDeclaredField("ofFastRender");
            return fastRenderField.getBoolean(mc.gameSettings);
        } catch (Exception e) {
            return false;
        }
    }

    @EventHandler
    public void onPostProcessing(PostProcessingEvent event) {
        // Bloqueia shader se Fast Render estiver ativo
        if (isFastRenderEnabled()) {
            if (group != null) group = null;
            return;
        }

        if(event.type == PostProcessingEvent.Type.UPDATE || (blur != 0
                && (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat)
               ))) {
            update();
            if (group != null) {
                event.groups.add(group);
            }
        }
    }

    @EventHandler
    public void onRenderGuiBackground(RenderGuiBackgroundEvent event) {
        event.cancelled = true;
        ScaledResolution resolution = new ScaledResolution(mc);
        Gui.drawRect(0, 0, resolution.getScaledWidth(), resolution.getScaledHeight(),
                Utils.lerpColour(0, backgroundColour.getValue(), getProgress()));
    }

    public void update() {
        if(group == null) {
            try {
                group = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(),
                       RESOURCE_LOCATION);
                group.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
            }
            catch(JsonSyntaxException | IOException error) {
                logger.error("Could not load menu blur", error);
            }
        }

        ((AccessShaderGroup) group).getListShaders().forEach((shader) -> {
            ShaderUniform radius = shader.getShaderManager().getShaderUniform("Radius");
            ShaderUniform progress = shader.getShaderManager().getShaderUniform("Progress");

            if(radius != null) {
                radius.set(blur);
            }

            if(progress != null) {
                if(fadeTime > 0) {
                    progress.set(getProgress());
                }
                else {
                    progress.set(1);
                }
            }
        });
    }

    public float getProgress() {
        return Math.min((System.currentTimeMillis() - openTime) / (fadeTime * 1000F), 1);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        group = null;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        group = null;
    }

    @Override
    public void decrement() {
        blur = Math.max(0, blur - 1);
        group = null;
    }

    @Override
    public void increment() {
        blur = Math.min(100, blur + 1);
        group = null;
    }

    public class MenuBlurShader implements IResource {
        @Override
        public ResourceLocation getResourceLocation() { return null; }

        @Override
        public InputStream getInputStream() {
            return IOUtils.toInputStream("{\n" +
                    "    \"targets\": [\n" +
                    "        \"swap\"\n" +
                    "    ],\n" +
                    "    \"passes\": [\n" +
                    "        {\n" +
                    "            \"name\": \"menu_blur\",\n" +
                    "            \"intarget\": \"minecraft:main\",\n" +
                    "            \"outtarget\": \"swap\",\n" +
                    "            \"uniforms\": [\n" +
                    "                {\n" +
                    "                    \"name\": \"BlurDir\",\n" +
                    "                    \"values\": [ 1.0, 0.0 ]\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"name\": \"Radius\",\n" +
                    "                    \"values\": [ 0.0 ]\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"name\": \"menu_blur\",\n" +
                    "            \"intarget\": \"swap\",\n" +
                    "            \"outtarget\": \"minecraft:main\",\n" +
                    "            \"uniforms\": [\n" +
                    "                {\n" +
                    "                    \"name\": \"BlurDir\",\n" +
                    "                    \"values\": [ 0.0, 1.0 ]\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"name\": \"Radius\",\n" +
                    "                    \"values\": [ 0.0 ]\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"name\": \"menu_blur\",\n" +
                    "            \"intarget\": \"minecraft:main\",\n" +
                    "            \"outtarget\": \"swap\",\n" +
                    "            \"uniforms\": [\n" +
                    "                {\n" +
                    "                    \"name\": \"BlurDir\",\n" +
                    "                    \"values\": [ 1.0, 0.0 ]\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"name\": \"Radius\",\n" +
                    "                    \"values\": [ 0.0 ]\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"name\": \"menu_blur\",\n" +
                    "            \"intarget\": \"swap\",\n" +
                    "            \"outtarget\": \"minecraft:main\",\n" +
                    "            \"uniforms\": [\n" +
                    "                {\n" +
                    "                    \"name\": \"BlurDir\",\n" +
                    "                    \"values\": [ 0.0, 1.0 ]\n" +
                    "                },\n" +
                    "                {\n" +
                    "                    \"name\": \"Radius\",\n" +
                    "                    \"values\": [ 0.0 ]\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}");
        }

        @Override
        public boolean hasMetadata() { return false; }

        @Override
        public <T extends IMetadataSection> T getMetadata(String p_110526_1_) { return null; }

        @Override
        public String getResourcePackName() { return null; }
    }
}