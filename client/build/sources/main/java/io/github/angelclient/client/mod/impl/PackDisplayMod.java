package io.github.angelclient.client.mod.impl;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.mod.hud.HudMod;
import io.github.angelclient.client.mod.hud.HudPosition;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.util.data.Colour;
import io.github.angelclient.client.util.data.Position;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

public class PackDisplayMod extends HudMod {

    @Expose
    @Option
    public boolean showIcon = true;

    // float obrigatório para @Slider funcionar.
    // format usa chave de lang: "angel_client.slider.pixels" → "%spx"
    // Adicione no en_US.lang: angel_client.slider.pixels=%spx
    @Expose
    @Option
    @Slider(min = 16, max = 64, step = 4, format = "angel_client.slider.pixels")
    public float iconSize = 32;

    @Expose
    @Option
    public boolean showName = true;

    @Expose
    @Option
    public boolean shadow = true;

    // Colour é a classe interna do Angel Client — única reconhecida pelo color picker
    @Expose
    @Option
    public Colour textColor = Colour.WHITE;

    @Expose
    @Option
    public boolean chroma = false;

    private ResourceLocation packIconLocation = null;
    private String cachedPackName = null;
    private int cachedIconSize = -1;

    @Override
    public String getId() {
        return "packdisplay";
    }

    @Override
    public ModCategory getCategory() {
        return ModCategory.VISUAL;
    }

    @Override
    public HudPosition getDefaultPosition() {
        return new HudPosition(10, 10);
    }

    @Override
    public void render(Position position, boolean editMode) {
        if (!isEnabled()) return;

        String packName = getActivePackName();
        int size = (int) iconSize;

        if (!packName.equals(cachedPackName) || size != cachedIconSize) {
            loadPackIcon(size);
            cachedPackName = packName;
            cachedIconSize = size;
        }

        FontRenderer fr = mc.fontRendererObj;
        float scale = getScale();

        GlStateManager.pushMatrix();
        GlStateManager.translate(position.getX(), position.getY(), 0);
        GlStateManager.scale(scale, scale, 1);

        int xOffset = 0;

        if (showIcon && packIconLocation != null) {
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(packIconLocation);
            drawIcon(0, 0, size, size);
            xOffset = size + 4;
        }

        if (showName && packName != null && !packName.isEmpty()) {
            int color = chroma ? getChromaColor() : textColor.getValue();
            int textY = (showIcon ? size : fr.FONT_HEIGHT) / 2 - fr.FONT_HEIGHT / 2;

            if (shadow) {
                fr.drawStringWithShadow(packName, xOffset, textY, color);
            } else {
                fr.drawString(packName, xOffset, textY, color);
            }
        }

        GlStateManager.popMatrix();
    }

    @Override
    public Rectangle getBounds(Position position) {
        FontRenderer fr = mc.fontRendererObj;
        String packName = getActivePackName();
        float scale = getScale();
        int size = (int) iconSize;

        int width = 0;
        int height = 0;

        if (showIcon) {
            int scaledIcon = (int) (size * scale);
            width += scaledIcon;
            height = scaledIcon;
            if (showName) width += (int) (4 * scale);
        }

        if (showName && packName != null) {
            width += (int) (fr.getStringWidth(packName) * scale);
            int textHeight = (int) (fr.FONT_HEIGHT * scale);
            if (textHeight > height) height = textHeight;
        }

        if (width == 0) width = 80;
        if (height == 0) height = 16;

        return new Rectangle((int) position.getX(), (int) position.getY(), width, height);
    }

    private String getActivePackName() {
        List<ResourcePackRepository.Entry> entries =
                mc.getResourcePackRepository().getRepositoryEntries();

        if (entries == null || entries.isEmpty()) {
            return "Default";
        }

        return entries.get(entries.size() - 1).getResourcePackName();
    }

    private void loadPackIcon(int size) {
        packIconLocation = null;

        try {
            List<ResourcePackRepository.Entry> entries =
                    mc.getResourcePackRepository().getRepositoryEntries();

            if (entries == null || entries.isEmpty()) {
                packIconLocation = getDefaultIcon();
                return;
            }

            ResourcePackRepository.Entry top = entries.get(entries.size() - 1);
            IResourcePack resourcePack = top.getResourcePack();

            if (resourcePack == null) {
                packIconLocation = getDefaultIcon();
                return;
            }

            BufferedImage image = null;
            try {
                image = resourcePack.getPackImage();
            } catch (Exception ignored) {}

            if (image != null) {
                BufferedImage resized = resizeImage(image, size, size);
                DynamicTexture texture = new DynamicTexture(resized);

                String safeName = top.getResourcePackName()
                        .toLowerCase()
                        .replaceAll("[^a-z0-9._-]", "_");

                packIconLocation = mc.getTextureManager().getDynamicTextureLocation(
                        "packdisplay/icon_" + safeName + "_" + size, texture
                );
            } else {
                packIconLocation = getDefaultIcon();
            }

        } catch (Exception e) {
            packIconLocation = getDefaultIcon();
        }
    }

    private ResourceLocation getDefaultIcon() {
        return new ResourceLocation("textures/misc/unknown_pack.png");
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    private void drawIcon(int x, int y, int width, int height) {
        net.minecraft.client.renderer.Tessellator tess =
                net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer wr = tess.getWorldRenderer();

        wr.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        wr.pos(x,         y + height, 0).tex(0, 1).endVertex();
        wr.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        wr.pos(x + width, y,          0).tex(1, 0).endVertex();
        wr.pos(x,         y,          0).tex(0, 0).endVertex();
        tess.draw();
    }

    private int getChromaColor() {
        float hue = (System.currentTimeMillis() % 2000) / 2000f;
        return Color.HSBtoRGB(hue, 0.8f, 1f) | 0xFF000000;
    }
}