package io.github.angelclient.client.cosmetic.render;

import io.github.angelclient.client.cosmetic.CosmeticAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class TextureLoader {

    private static final Map<String, ResourceLocation> cache = new HashMap<>();

    public static ResourceLocation loadTexture(String relativePath) {
        if (cache.containsKey(relativePath)) {
            return cache.get(relativePath);
        }

        String url = CosmeticAPI.GITHUB_BASE_URL + relativePath;
        byte[] data = CosmeticAPI.downloadBytes(url);
        if (data == null) {
            System.err.println("[AngelClient] Failed to download texture: " + url);
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                System.err.println("[AngelClient] Failed to decode image: " + url);
                return null;
            }

            DynamicTexture dynamicTexture = new DynamicTexture(image);
            String safeName = "cosmetic_" + relativePath.replaceAll("[^a-zA-Z0-9]", "_");
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation(safeName, dynamicTexture);

            cache.put(relativePath, location);
            return location;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void clearCache() {
        cache.clear();
    }
}
