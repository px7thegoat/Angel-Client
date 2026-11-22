package io.github.angelclient.client.cosmetic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import java.io.File;
import java.io.FileOutputStream;
import java.awt.image.BufferedImage;

public class CapeCosmetic extends Cosmetic {
    private boolean downloaded = false;

    public CapeCosmetic(String id, String name, String filePath, boolean premium, String baseUrl) {
        super(id, name, filePath, premium);

        this.texture = new ResourceLocation("angelclient/capes/" + id.toLowerCase());

        File cache = new File(Minecraft.getMinecraft().mcDataDir, "angelclient/cache/" + id + ".png");

        // Pré-baixa com autenticação para repositório privado
        try {
            if (!cache.exists()) {
                cache.getParentFile().mkdirs();
                byte[] data = CosmeticAPI.downloadBytes(baseUrl + filePath);
                if (data != null) {
                    FileOutputStream fos = new FileOutputStream(cache);
                    fos.write(data);
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ThreadDownloadImageData image = new ThreadDownloadImageData(cache, baseUrl + filePath, null, new net.minecraft.client.renderer.IImageBuffer() {
            @Override
            public BufferedImage parseUserSkin(BufferedImage img) {
                return img;
            }
            @Override
            public void skinAvailable() {
                downloaded = true;
            }
        });

        Minecraft.getMinecraft().getTextureManager().loadTexture(texture, image);
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    @Override
    public void render(float x, float y, float z) {
    }
}