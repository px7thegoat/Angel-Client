package io.github.angelclient.client.mod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.angelclient.client.Client;
import net.minecraft.client.Minecraft;

public class ProfileManager {

    private static ProfileManager instance;
    private final File profilesDir;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProfileManager() {
        profilesDir = new File(Minecraft.getMinecraft().mcDataDir, "angel_profiles");
        profilesDir.mkdirs();
    }

    public static ProfileManager getInstance() {
        if (instance == null) instance = new ProfileManager();
        return instance;
    }

    public List<String> getProfileNames() {
        List<String> names = new ArrayList<>();
        File[] files = profilesDir.listFiles();
        if (files == null) return names;
        for (File f : files) {
            if (f.getName().endsWith(".json")) {
                names.add(f.getName().replace(".json", ""));
            }
        }
        return names;
    }

    public boolean saveProfile(String name) {
        try {
            Client.INSTANCE.save();
            File src = new File(Minecraft.getMinecraft().mcDataDir, "angel_client_mods.json");
            File dst = new File(profilesDir, name + ".json");
            FileUtils.copyFile(src, dst);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadProfile(String name) {
        try {
            File src = new File(profilesDir, name + ".json");
            if (!src.exists()) return false;
            File dst = new File(Minecraft.getMinecraft().mcDataDir, "angel_client_mods.json");
            FileUtils.copyFile(src, dst);
            // Lê o JSON no campo data
            Client.INSTANCE.load();
            // Aplica imediatamente em todos os mods sem precisar relogar
            Client.INSTANCE.applyToMods();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteProfile(String name) {
        File f = new File(profilesDir, name + ".json");
        return f.exists() && f.delete();
    }

    public boolean profileExists(String name) {
        return new File(profilesDir, name + ".json").exists();
    }
}
