package io.github.angelclient.client.cosmetic;

import net.minecraft.util.ResourceLocation;
import java.util.List;

public class LocalCosmetic {

    private final String id;
    private final String name;
    private final CosmeticType type;
    private final String file;
    private final String texture;
    private ResourceLocation resourceLocation;
    private ResourceLocation textureLocation;
    private boolean premium;
    private String color;
    private List<String> allowedPlayers;

    public LocalCosmetic(String id, String name, CosmeticType type, String file, String texture) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.file = file;
        this.texture = texture;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public CosmeticType getType() { return type; }
    public String getFile() { return file; }
    public String getTexture() { return texture; }

    public ResourceLocation getResourceLocation() {
        if (resourceLocation == null) {
            resourceLocation = new ResourceLocation("angelcosmetics", file);
        }
        return resourceLocation;
    }

    public ResourceLocation getTextureLocation() {
        if (texture == null || texture.isEmpty()) return null;
        if (textureLocation == null) {
            textureLocation = new ResourceLocation("angelcosmetics", texture);
        }
        return textureLocation;
    }

    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getAllowedPlayers() { return allowedPlayers; }
    public void setAllowedPlayers(List<String> allowedPlayers) { this.allowedPlayers = allowedPlayers; }

    public boolean hasAccess(String playerName) {
        if (!premium) return true;
        if (allowedPlayers != null && !allowedPlayers.isEmpty()) {
            return allowedPlayers.contains(playerName);
        }
        return false;
    }
}
