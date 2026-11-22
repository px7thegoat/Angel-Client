package io.github.angelclient.client.cosmetic;

import net.minecraft.util.ResourceLocation;
import java.util.List;
import java.util.ArrayList;

public abstract class Cosmetic {
    
    protected String id;
    protected String name;
    protected String filePath;
    protected boolean premium;
    protected ResourceLocation texture;

    // NOVO
    protected List<String> allowedPlayers = new ArrayList<>();
    protected String requiredTag;
    
    public Cosmetic(String id, String name, String filePath, boolean premium) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.premium = premium;
    }

    // NOVO construtor (não quebra o antigo)
    public Cosmetic(String id, String name, String filePath, boolean premium, List<String> allowedPlayers, String requiredTag) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.premium = premium;

        if (allowedPlayers != null) {
            this.allowedPlayers = allowedPlayers;
        }

        this.requiredTag = requiredTag;
    }
    
    public abstract void render(float x, float y, float z);
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isPremium() {
        return premium;
    }
    
    public ResourceLocation getTexture() {
        return texture;
    }

    // NOVO
    public boolean canPlayerUse(String playerName, String tag) {

    if (allowedPlayers != null && !allowedPlayers.isEmpty()) {
        return allowedPlayers.contains(playerName);
    }

    if (requiredTag != null && tag != null) {
        if (requiredTag.equalsIgnoreCase(tag)) {
            return true;
        }
        return false;
    }

    return true;
}

    // NOVO
    public void setAllowedPlayers(List<String> players) {
        this.allowedPlayers = players;
    }

    // NOVO
    public void setRequiredTag(String tag) {
        this.requiredTag = tag;
    }
}