package io.github.angelclient.client.cosmetic;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CosmeticAPI {
    
    public static final String GITHUB_TOKEN = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    public static final String GITHUB_BASE_URL = "https://raw.githubusercontent.com/xistey/angel-client/master/AngelClient/cosm%C3%A9ticos/";
    public static final String GITHUB_ROOT_URL = "https://raw.githubusercontent.com/xistey/angel-client/master/";
    public static final String COSMETICS_JSON_URL = GITHUB_ROOT_URL + "cosmetics.json";
    public static final String BANDANAS_JSON_URL = GITHUB_BASE_URL + "bandanas.json";
    public static final String HATS_JSON_URL = GITHUB_BASE_URL + "hats.json";
    public static final String WINGS_JSON_URL = GITHUB_BASE_URL + "wings.json";
    public static final String NAMETAGS_JSON_URL = GITHUB_BASE_URL + "nametags.json";
    public static final String ITEMS_JSON_URL = GITHUB_BASE_URL + "itens.json";
    public static final String SHIELDS_JSON_URL = GITHUB_BASE_URL + "escudos.json";
    public static final String AURAS_JSON_URL = GITHUB_BASE_URL + "auras.json";

    private final Gson gson = new Gson();

    public void fetchCosmetics(Consumer<List<Map<String, Object>>> callback) {
        try {
            String jsonResponse = makeGetRequest(COSMETICS_JSON_URL);
            
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                callback.accept(new ArrayList<>());
                return;
            }
            
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);
            
            if (root == null) {
                callback.accept(new ArrayList<>());
                return;
            }
            
            if (!root.has("capes")) {
                callback.accept(new ArrayList<>());
                return;
            }
            
            JsonArray capesArray = root.getAsJsonArray("capes");
            
            if (capesArray == null) {
                callback.accept(new ArrayList<>());
                return;
            }
            
            List<Map<String, Object>> cosmetics = new ArrayList<>();
            
            for (JsonElement element : capesArray) {
    try {
        JsonObject cape = element.getAsJsonObject();

        Map<String, Object> cosmeticData = new HashMap<>();

        cosmeticData.put("id", cape.get("id").getAsString());
        cosmeticData.put("name", cape.get("name").getAsString());
        cosmeticData.put("file", cape.get("file").getAsString());
        cosmeticData.put("premium", cape.has("premium") && cape.get("premium").getAsBoolean());

        List<String> allowedPlayers = new ArrayList<>();

        if (cape.has("allowedPlayers")) {
            JsonArray players = cape.getAsJsonArray("allowedPlayers");

            for (JsonElement p : players) {
                allowedPlayers.add(p.getAsString());
            }
        }

        cosmeticData.put("allowedPlayers", allowedPlayers);

        if (cape.has("requiredTag")) {
            cosmeticData.put("requiredTag", cape.get("requiredTag").getAsString());
        }

        cosmetics.add(cosmeticData);

    } catch (Exception e) {
    }
}
            
            callback.accept(cosmetics);
            
        } catch (Exception e) {
            callback.accept(new ArrayList<>());
        }
    }

    public void fetchPlayerCosmetics(Consumer<Map<String, String>> callback) {
        try {
            String jsonResponse = readPlayersJson();
            if (jsonResponse == null) {
                callback.accept(new HashMap<>());
                return;
            }

            JsonObject playersObj = new Gson().fromJson(jsonResponse, JsonObject.class);
            if (playersObj == null) {
                callback.accept(new HashMap<>());
                return;
            }

            Map<String, String> playerCosmetics = new HashMap<>();

            for (Map.Entry<String, JsonElement> entry : playersObj.entrySet()) {
                try {
                    String playerName = entry.getKey();
                    JsonObject playerData = entry.getValue().getAsJsonObject();

                    String cosmeticId = null;
                    if (playerData.has("cape")) {
                        cosmeticId = playerData.get("cape").getAsString();
                    } else if (playerData.has("cosmetic")) {
                        cosmeticId = playerData.get("cosmetic").getAsString();
                    }
                    if (cosmeticId != null && !cosmeticId.isEmpty()) {
                        playerCosmetics.put(playerName, cosmeticId);
                    }
                } catch (Exception e) {
                }
            }

            callback.accept(playerCosmetics);

        } catch (Exception e) {
            callback.accept(new HashMap<>());
        }
    }

    public void updatePlayerCosmetic(String playerName, String cosmeticId) {
        try {
            String lower = playerName.toLowerCase();
            MySQLManager.getInstance().setCosmetic(lower, "cape", cosmeticId);
        } catch (Exception e) {
        }
    }

    public String getGithubBaseUrl() {
        return GITHUB_BASE_URL;
    }

    public static String makeGetRequest(String urlString) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        
        try {
            if (urlString.contains("?")) {
                urlString += "&_t=" + System.currentTimeMillis();
            } else {
                urlString += "?_t=" + System.currentTimeMillis();
            }
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "AngelClient/1.0");
            if (urlString.contains("raw.githubusercontent.com")) {
                connection.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode != 200) {
                return null;
            }
            
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            return response.toString();
            
        } catch (Exception e) {
            return null;
            
        } finally {
            try {
                if (reader != null) reader.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
            }
        }
    }

    public static byte[] downloadBytes(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "AngelClient/1.0");
            connection.setRequestProperty("Authorization", "token " + GITHUB_TOKEN);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
            in.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    public static void syncPlayerToMySQL() {
        new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.getSession() == null) return;
                String playerName = mc.getSession().getUsername();
                if (playerName == null || playerName.isEmpty()) return;

                String currentServer = mc.getCurrentServerData() != null
                    ? mc.getCurrentServerData().serverIP : null;

                String ip = getExternalIp();

                MySQLManager mysql = MySQLManager.getInstance();
                mysql.syncPlayerData(playerName, ip, currentServer);

                String lower = playerName.toLowerCase();
                LocalCosmeticManager lcm = LocalCosmeticManager.getInstance();
                saveCosmeticToMySQL(mysql, lower, "cape", lcm.getActiveCape(lower));
                saveCosmeticToMySQL(mysql, lower, "bandana", lcm.getActiveBandana(lower));
                saveCosmeticToMySQL(mysql, lower, "hat", lcm.getActiveHat(lower));
                saveCosmeticToMySQL(mysql, lower, "wing", lcm.getActiveWing(lower));
                saveCosmeticToMySQL(mysql, lower, "nametag_cosmetic", lcm.getActiveNametag(lower));
                saveCosmeticToMySQL(mysql, lower, "item", lcm.getActiveItem(lower));
                saveCosmeticToMySQL(mysql, lower, "shield", lcm.getActiveShield(lower));
                saveCosmeticToMySQL(mysql, lower, "aura", lcm.getActiveAura(lower));
            } catch (Exception e) {
            }
        }).start();
    }

    private static void saveCosmeticToMySQL(MySQLManager mysql, String player, String type, String id) {
        if (id != null && !id.isEmpty()) {
            mysql.setCosmetic(player, type, id);
        }
    }

    private static String getExternalIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "AngelClient/1.0");
            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String ip = reader.readLine();
                reader.close();
                return ip;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String readPlayerJson(String playerName) {
        if (playerName == null || playerName.isEmpty()) return null;
        try {
            return MySQLManager.getInstance().getPlayerDataJson(playerName);
        } catch (Exception e) {
            return null;
        }
    }

    public static String readPlayersJson() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.getNetHandler() == null) return "{}";

            Collection<?> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();
            if (playerInfoMap == null) return "{}";

            List<String> names = new ArrayList<>();
            for (Object obj : playerInfoMap) {
                try {
                    net.minecraft.client.network.NetworkPlayerInfo info =
                        (net.minecraft.client.network.NetworkPlayerInfo) obj;
                    String name = info.getGameProfile().getName();
                    if (name != null && !name.isEmpty()) names.add(name);
                } catch (Exception ignored) {}
            }

            Map<String, JsonObject> playersData = MySQLManager.getInstance().getAllPlayersData(names);

            JsonObject aggregate = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : playersData.entrySet()) {
                aggregate.add(entry.getKey(), entry.getValue());
            }

            return aggregate.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
