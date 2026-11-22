package io.github.angelclient.client.discord;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.Properties;

public class DiscordConfig {

    private static Properties props = null;

    private static File getFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "angelclient_discord.properties");
    }

    private static Properties getProps() {
        if (props != null) return props;
        props = new Properties();
        try {
            File f = getFile();
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                props.load(fis);
                fis.close();
            }
        } catch (Exception ignored) {}
        return props;
    }

    private static void salvar() {
        try {
            FileOutputStream fos = new FileOutputStream(getFile());
            getProps().store(fos, "AngelClient Discord Config");
            fos.close();
        } catch (Exception ignored) {}
    }

    public static String getDiscordId() {
        return getProps().getProperty("discord_id");
    }

    public static void setDiscordId(String id) {
        getProps().setProperty("discord_id", id.trim());
        salvar();
    }

    public static boolean isVinculado() {
        String id = getDiscordId();
        return id != null && !id.isEmpty();
    }

    public static String getOAuthToken() {
        String t = getProps().getProperty("oauth_token");
        return (t != null && !t.isEmpty()) ? t : null;
    }

    public static void setOAuthToken(String token) {
        if (token == null) {
            getProps().remove("oauth_token");
        } else {
            getProps().setProperty("oauth_token", token.trim());
        }
        salvar();
    }

    public static String getApiUrl() {
        return getProps().getProperty("api_url", "https://vhwcydciz2.localto.net");
    }

    public static String getApiKey() {
        String apiKey = System.getenv("ANGEL_API_KEY");
        return apiKey == null ? "" : apiKey;
    }

    public static String getDiscordUsername() {
        return getProps().getProperty("discord_username");
    }

    public static void setDiscordUsername(String username) {
        if (username == null) {
            getProps().remove("discord_username");
        } else {
            getProps().setProperty("discord_username", username);
        }
        salvar();
    }

    public static String getDiscordAvatar() {
        return getProps().getProperty("discord_avatar");
    }

    public static void setDiscordAvatar(String avatar) {
        if (avatar == null) {
            getProps().remove("discord_avatar");
        } else {
            getProps().setProperty("discord_avatar", avatar);
        }
        salvar();
    }

    public static boolean isAndroid() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void limparVinculo() {
        setDiscordId(null);
        setOAuthToken(null);
        setDiscordUsername(null);
        setDiscordAvatar(null);
    }
}
