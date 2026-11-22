package io.github.angelclient.client.mod.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;

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

public class MotionBlurMod extends Mod implements PrimaryIntegerSettingMod {

    public static final ResourceLocation RESOURCE_LOCATION =
            new ResourceLocation("minecraft:shaders/post/motion_blur.json");

    @Expose @Option
    @Slider(min = 0, max = 0.99f, step = 0.01f)
    private float blur = 0.5f;

    @Expose @Option
    public boolean adaptive = true;

    private ShaderGroup group;
    private float groupBlur = -1f;

    public ShaderGroup getGroup() {
        return group;
    }

    @Override
    public String getId() {
        return "motion_blur";
    }

    @Override
    public String getName() {
        return "Motion Blur";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    @Override
    public void onRegister() {
        super.onRegister();
        Client.INSTANCE.addResource(RESOURCE_LOCATION, new MotionBlurShader());
    }

    private float getEffectiveBlur() {
        if (!adaptive) return blur;

        int fps = net.minecraft.client.Minecraft.getDebugFPS();
        float multiplier;
        if      (fps >= 75) multiplier = 1.0f;
        else if (fps >= 60) multiplier = 0.8f;
        else if (fps >= 45) multiplier = 0.6f;
        else if (fps >= 30) multiplier = 0.4f;
        else if (fps >= 15) multiplier = 0.2f;
        else                multiplier = 0.1f;

        return Math.min(0.99f, blur * multiplier);
    }

    public void update() {
        float effective = getEffectiveBlur();

        if (group == null) {
            groupBlur = effective;
            try {
                group = new ShaderGroup(mc.getTextureManager(), mc.getResourceManager(),
                        mc.getFramebuffer(), RESOURCE_LOCATION);
                group.createBindFramebuffers(mc.displayWidth, mc.displayHeight);
            } catch (JsonSyntaxException | IOException error) {
                logger.error("Could not load motion blur", error);
                return;
            }
        }

        if (groupBlur != effective) {
            ((AccessShaderGroup) group).getListShaders().forEach(shader -> {
                ShaderUniform blendFactor = shader.getShaderManager().getShaderUniform("BlendFactor");
                if (blendFactor != null) {
                    blendFactor.set(effective);
                }
            });
            groupBlur = effective;
        }
    }

    // ✅ FIX BUG 4: Verifica se Fast Render está ativo usando Reflection para não dar erro de compilação
    private boolean isFastRenderEnabled() {
        try {
            // Tenta acessar a variável ofFastRender criada pelo OptiFine
            Field fastRenderField = mc.gameSettings.getClass().getDeclaredField("ofFastRender");
            return fastRenderField.getBoolean(mc.gameSettings);
        } catch (Exception e) {
            // OptiFine não está presente ou não tem essa variável
        }
        
        // Verifica Fast Render no TweaksMod (se existir)
        try {
            TweaksMod tweaks = (TweaksMod) Client.INSTANCE.getModById("tweaks");
            if (tweaks != null && tweaks.isEnabled()) {
                // Aqui você pode verificar o fastRender do seu client caso tenha criado um customizado
            }
        } catch (Exception e) {
            // Mod tweaks não encontrado
        }
        
        return false;
    }

    @EventHandler
    public void onPostProcessing(PostProcessingEvent event) {
        // ✅ FIX BUG 4: NÃO aplica Motion Blur se Fast Render estiver ativo
        if (isFastRenderEnabled()) {
            // Limpa o shader group se existir
            if (group != null) {
                group = null;
                groupBlur = -1f;
            }
            return; // Não adiciona o shader
        }
        
        update();
        if (group != null) {
            event.groups.add(group);
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        group = null;
        groupBlur = -1f;
    }
    
    @Override
    protected void onDisable() {
        super.onDisable();
        // ✅ Limpa o shader ao desativar
        if (group != null) {
            group = null;
            groupBlur = -1f;
        }
    }

    @Override
    public void decrement() {
        blur = Math.max(0, blur - 0.1f);
        group = null;
        groupBlur = -1f;
    }

    @Override
    public void increment() {
        blur = Math.min(0.99f, blur + 0.1f);
        group = null;
        groupBlur = -1f;
    }

    public class MotionBlurShader implements IResource {

        @Override
        public ResourceLocation getResourceLocation() {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            return IOUtils.toInputStream(
                "{" +
                "    \"targets\": [" +
                "        \"swap\"," +
                "        \"previous\"" +
                "    ]," +
                "    \"passes\": [" +
                "        {" +
                "            \"name\": \"motion_blur\"," +
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
                "                    \"name\": \"BlendFactor\"," +
                "                    \"values\": [ 0.5 ]" +
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
                "}"
            );
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Override
        public <T extends IMetadataSection> T getMetadata(String p_110526_1_) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return null;
        }
    }
}