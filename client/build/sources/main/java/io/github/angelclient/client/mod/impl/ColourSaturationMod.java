package io.github.angelclient.client.mod.impl;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;

import io.github.angelclient.client.Client;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PostProcessingEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.PrimaryIntegerSettingMod;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.util.access.AccessShaderGroup;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class ColourSaturationMod extends Mod implements PrimaryIntegerSettingMod {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(
            "minecraft:shaders/post/" + "color_convolve.json");

    @Expose
    @Option
    @Slider(min = 0, max = 2F, step = 0.1F)
    private float saturation = 1f;
    private ShaderGroup group;
    private float groupSaturation;

    public ShaderGroup getGroup() {
        return group;
    }

    @Override
    public String getId() {
        return "colour_saturation";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    @Override
    public void onRegister() {
        super.onRegister();
        Client.INSTANCE.addResource(RESOURCE_LOCATION, new SaturationShader());
    }

    public void update() {
        if(group == null) {
            groupSaturation = saturation;
            try {
                group = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(), mc.getFramebuffer(), RESOURCE_LOCATION);
                group.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
            }
            catch(JsonSyntaxException | IOException error) {
                logger.error("Could not load saturation shader", error);
            }
        }

        if(groupSaturation != saturation) {
            ((AccessShaderGroup) group).getListShaders().forEach((shader) -> {
                ShaderUniform saturationUniform = shader.getShaderManager().getShaderUniform("Saturation");
                if(saturationUniform != null) {
                    saturationUniform.set(saturation);
                }
            });
            groupSaturation = saturation;
        }
    }

    // ✅ FIX BUG 3: Verifica se o Fast Render do OptiFine está ativo 
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
        // Bloqueia e limpa o shader se estiver com o Fast Render ligado
        if (isFastRenderEnabled()) {
            if (group != null) {
                group = null;
                groupSaturation = -1f;
            }
            return;
        }

        update();
        if (group != null) {
            event.groups.add(getGroup());
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        group = null;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (group != null) {
            group = null;
            groupSaturation = -1f;
        }
    }

    @Override
    public void decrement() {
        saturation = Math.max(0, saturation - 0.1F);
        group = null;
    }

    @Override
    public void increment() {
        saturation = Math.min(2, saturation + 0.1F);
        group = null;
    }

    public class SaturationShader implements IResource {
        @Override
        public ResourceLocation getResourceLocation() { return null; }

        @Override
        public InputStream getInputStream() {
            return IOUtils.toInputStream(String.format("{" +
                    "    \"targets\": [" +
                    "        \"swap\"," +
                    "        \"previous\"" +
                    "    ]," +
                    "    \"passes\": [" +
                    "        {" +
                    "            \"name\": \"color_convolve\"," +
                    "            \"intarget\": \"minecraft:main\"," +
                    "            \"outtarget\": \"swap\"," +
                    "            \"auxtargets\": [" +
                    "                {" +
                    "                    \"name\": \"PrevSampler\"," +
                    "                    \"id\": \"previous\"" +
                    "                }" +
                    "            ]," +
                    "            \"uniforms\": [" +
                    "                {" +
                    "                    \"name\": \"Saturation\"," +
                    "                    \"values\": [ %s ]" +
                    "                }" +
                    "            ]" +
                    "        }," +
                    "        {" +
                    "            \"name\": \"blit\"," +
                    "            \"intarget\": \"swap\"," +
                    "            \"outtarget\": \"previous\"" +
                    "        }," +
                    "        {" +
                    "            \"name\": \"blit\"," +
                    "            \"intarget\": \"swap\"," +
                    "            \"outtarget\": \"minecraft:main\"" +
                    "        }" +
                    "    ]" +
                    "}", saturation, saturation, saturation));
        }

        @Override
        public boolean hasMetadata() { return false; }

        @Override
        public <T extends IMetadataSection> T getMetadata(String p_110526_1_) { return null; }

        @Override
        public String getResourcePackName() { return null; }
    }
}