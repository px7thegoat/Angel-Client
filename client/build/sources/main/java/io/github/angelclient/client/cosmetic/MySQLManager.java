package io.github.angelclient.client.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MySQLManager {

    private static MySQLManager INSTANCE;

    private static final String HOST = "br-02.blackhosting.com.br";
    private static final int PORT = 30005;
    private static final String DB = "angelclient";
    private static final String USER = "xisteey";
    private static final String PASS = "xisteey0001";

    private static final long CACHE_TTL_MS = 10_000L;
    private final ConcurrentHashMap<String, PlayerCacheEntry> playerDataCache = new ConcurrentHashMap<>();

    private Connection connection;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MySQL-KeepAlive");
        t.setDaemon(true);
        return t;
    });

    private MySQLManager() {
        connect();
        createTables();
        scheduler.scheduleAtFixedRate(this::keepAlive, 30, 30, TimeUnit.SECONDS);
    }

    public static MySQLManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MySQLManager();
        return INSTANCE;
    }

    private synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    private synchronized void connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            connection = DriverManager.getConnection(url, USER, PASS);
        } catch (Exception e) {
        }
    }

    private void keepAlive() {
        try {
            Connection conn = getConnection();
            if (conn != null && !conn.isClosed()) {
                Statement stmt = conn.createStatement();
                stmt.execute("SELECT 1");
                stmt.close();
            }
        } catch (Exception ignored) {}
    }

    private void createTables() {
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS players ("
                + "player_name VARCHAR(32) PRIMARY KEY,"
                + "ip VARCHAR(45),"
                + "first_join DATETIME,"
                + "last_server VARCHAR(255),"
                + "last_update DATETIME,"
                + "extra_data TEXT,"
                + "special_tag VARCHAR(64),"
                + "cosmetics TEXT,"
                + "favorites TEXT,"
                + "profiles TEXT,"
                + "settings TEXT"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8");

            try { stmt.execute("ALTER TABLE players ADD COLUMN cosmetics TEXT"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE players ADD COLUMN favorites TEXT"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE players ADD COLUMN profiles TEXT"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE players ADD COLUMN settings TEXT"); } catch (Exception ignored) {}

            stmt.close();
        } catch (Exception e) {
        }
    }

    public void syncPlayerData(String playerName, String ip, String lastServer) {
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            String lower = playerName.toLowerCase();
            String now = Instant.now().toString().replace("T", " ").substring(0, 19);

            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT first_join FROM players WHERE player_name = ?");
            checkStmt.setString(1, lower);
            ResultSet rs = checkStmt.executeQuery();

            String firstJoin;
            if (rs.next()) {
                firstJoin = rs.getString("first_join");
            } else {
                firstJoin = now;
            }
            rs.close();
            checkStmt.close();

            PreparedStatement upsert = conn.prepareStatement(
                "INSERT INTO players (player_name, ip, first_join, last_server, last_update) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE ip = VALUES(ip), last_server = VALUES(last_server), last_update = VALUES(last_update)");
            upsert.setString(1, lower);
            upsert.setString(2, ip);
            upsert.setString(3, firstJoin);
            upsert.setString(4, lastServer);
            upsert.setString(5, now);
            upsert.executeUpdate();
            upsert.close();
        } catch (Exception e) {
        }
    }

    public void setCosmetic(String playerName, String type, String cosmeticId) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;

            PreparedStatement sel = conn.prepareStatement(
                "SELECT cosmetics FROM players WHERE player_name = ?");
            sel.setString(1, lower);
            ResultSet rs = sel.executeQuery();
            JsonObject cos = new JsonObject();
            if (rs.next()) {
                String raw = rs.getString("cosmetics");
                if (raw != null && !raw.isEmpty()) {
                    try { cos = new JsonParser().parse(raw).getAsJsonObject(); } catch (Exception ignored) {}
                }
            }
            rs.close();
            sel.close();

            if (cosmeticId == null || cosmeticId.isEmpty()) {
                cos.remove(type);
            } else {
                cos.addProperty(type, cosmeticId);
            }

            PreparedStatement upd = conn.prepareStatement(
                "UPDATE players SET cosmetics = ? WHERE player_name = ?");
            upd.setString(1, cos.toString());
            upd.setString(2, lower);
            upd.executeUpdate();
            upd.close();

            playerDataCache.remove(lower);
        } catch (Exception e) {
        }
    }

    public String getCosmetic(String playerName, String type) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return null;
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT cosmetics FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("cosmetics");
                if (raw != null && !raw.isEmpty()) {
                    try {
                        JsonObject cos = new JsonParser().parse(raw).getAsJsonObject();
                        if (cos.has(type)) return cos.get(type).getAsString();
                    } catch (Exception ignored) {}
                }
            }
            rs.close();
            stmt.close();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, String> getAllCosmeticsForPlayer(String playerName) {
        Map<String, String> map = new HashMap<>();
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return map;
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT cosmetics FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("cosmetics");
                if (raw != null && !raw.isEmpty()) {
                    try {
                        JsonObject cos = new JsonParser().parse(raw).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> e : cos.entrySet()) {
                            map.put(e.getKey(), e.getValue().getAsString());
                        }
                    } catch (Exception ignored) {}
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception ignored) {}
        return map;
    }

    public String getPlayerDataJson(String playerName) {
        String lower = playerName.toLowerCase();

        PlayerCacheEntry cached = playerDataCache.get(lower);
        if (cached != null && cached.isValid()) return cached.data;

        try {
            Connection conn = getConnection();
            if (conn == null) return null;

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT ip, first_join, last_server, last_update, extra_data, special_tag, cosmetics FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();

            JsonObject data = new JsonObject();
            if (rs.next()) {
                data.addProperty("has_angelclient", true);
                if (rs.getString("ip") != null) data.addProperty("ip", rs.getString("ip"));
                if (rs.getString("first_join") != null) data.addProperty("first_join", rs.getString("first_join"));
                if (rs.getString("last_server") != null) data.addProperty("last_server", rs.getString("last_server"));
                if (rs.getString("last_update") != null) data.addProperty("last_update", rs.getString("last_update"));
                if (rs.getString("special_tag") != null) data.addProperty("special_tag", rs.getString("special_tag"));
                String extra = rs.getString("extra_data");
                if (extra != null && !extra.isEmpty() && !extra.equals("{}")) {
                    try {
                        JsonObject extraObj = new JsonParser().parse(extra).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : extraObj.entrySet()) {
                            data.add(entry.getKey(), entry.getValue());
                        }
                    } catch (Exception ignored) {}
                }
                String cosRaw = rs.getString("cosmetics");
                if (cosRaw != null && !cosRaw.isEmpty() && !cosRaw.equals("{}")) {
                    try {
                        JsonObject cosObj = new JsonParser().parse(cosRaw).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : cosObj.entrySet()) {
                            data.add(entry.getKey(), entry.getValue());
                        }
                    } catch (Exception ignored) {}
                }
            }
            rs.close();
            stmt.close();

            String json = data.toString();
            playerDataCache.put(lower, new PlayerCacheEntry(json));
            return json;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, JsonObject> getAllPlayersData(Collection<String> playerNames) {
        Map<String, JsonObject> result = new HashMap<>();
        if (playerNames == null || playerNames.isEmpty()) return result;

        try {
            Connection conn = getConnection();
            if (conn == null) return result;

            for (String name : playerNames) {
                String json = getPlayerDataJson(name);
                if (json != null) {
                    try {
                        result.put(name.toLowerCase(), new JsonParser().parse(json).getAsJsonObject());
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    public void savePlayerJson(String playerName, String json) {
        if (json == null || json.isEmpty()) return;
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;

            JsonObject fullData;
            try {
                fullData = new JsonParser().parse(json).getAsJsonObject();
            } catch (Exception e) {
                return;
            }

            JsonElement specialTagEl = fullData.remove("special_tag");

            fullData.remove("has_angelclient");
            fullData.remove("ip");
            fullData.remove("first_join");
            fullData.remove("last_server");
            fullData.remove("last_update");
            fullData.remove("cape");
            fullData.remove("bandana");
            fullData.remove("hat");
            fullData.remove("wing");
            fullData.remove("nametag_cosmetic");
            fullData.remove("item");
            fullData.remove("shield");
            fullData.remove("aura");

            String extraData = fullData.toString();

            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET extra_data = ? WHERE player_name = ?");
            stmt.setString(1, extraData);
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();

            if (specialTagEl != null && !specialTagEl.isJsonNull()) {
                String tag = specialTagEl.getAsString();
                PreparedStatement tagStmt = conn.prepareStatement(
                    "UPDATE players SET special_tag = ? WHERE player_name = ?");
                tagStmt.setString(1, tag);
                tagStmt.setString(2, lower);
                tagStmt.executeUpdate();
                tagStmt.close();
            }

            playerDataCache.remove(lower);
        } catch (Exception e) {
        }
    }

    private static class PlayerCacheEntry {
        final String data;
        final long timestamp;
        PlayerCacheEntry(String data) { this.data = data; this.timestamp = System.currentTimeMillis(); }
        boolean isValid() { return System.currentTimeMillis() - timestamp < CACHE_TTL_MS; }
    }

    // ── Special Tag ─────────────────────────────────────────────────────────────

    public void setSpecialTag(String playerName, String tag) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET special_tag = ? WHERE player_name = ?");
            stmt.setString(1, tag);
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();
            playerDataCache.remove(lower);
        } catch (Exception ignored) {}
    }

    // ── Favorites ──────────────────────────────────────────────────────────────

    public Set<String> getFavorites(String playerName) {
        Set<String> set = new HashSet<>();
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return set;
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT favorites FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("favorites");
                if (raw != null && !raw.isEmpty()) {
                    try {
                        JsonArray arr = new JsonParser().parse(raw).getAsJsonArray();
                        for (JsonElement el : arr) set.add(el.getAsString());
                    } catch (Exception ignored) {}
                }
            }
            rs.close(); stmt.close();
        } catch (Exception ignored) {}
        return set;
    }

    public void addFavorite(String playerName, String cosmeticId) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;

            Set<String> favs = getFavorites(playerName);
            favs.add(cosmeticId);

            JsonArray arr = new JsonArray();
            for (String f : favs) arr.add(f);
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET favorites = ? WHERE player_name = ?");
            stmt.setString(1, arr.toString());
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception ignored) {}
    }

    public void removeFavorite(String playerName, String cosmeticId) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;

            Set<String> favs = getFavorites(playerName);
            favs.remove(cosmeticId);

            JsonArray arr = new JsonArray();
            for (String f : favs) arr.add(f);
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET favorites = ? WHERE player_name = ?");
            stmt.setString(1, arr.toString());
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception ignored) {}
    }

    // ── Profiles ───────────────────────────────────────────────────────────────

    private JsonObject loadProfilesRaw(String lower) {
        try {
            Connection conn = getConnection();
            if (conn == null) return new JsonObject();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT profiles FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String raw = rs.getString("profiles");
                if (raw != null && !raw.isEmpty()) {
                    try { return new JsonParser().parse(raw).getAsJsonObject(); } catch (Exception ignored) {}
                }
            }
            rs.close(); stmt.close();
        } catch (Exception ignored) {}
        return new JsonObject();
    }

    private void saveProfilesRaw(String lower, JsonObject profiles) {
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET profiles = ? WHERE player_name = ?");
            stmt.setString(1, profiles.toString());
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception ignored) {}
    }

    public void saveProfile(String playerName, String profileName, String profileData) {
        String lower = playerName.toLowerCase();
        JsonObject profiles = loadProfilesRaw(lower);
        profiles.addProperty(profileName, profileData);
        saveProfilesRaw(lower, profiles);
    }

    public String loadProfile(String playerName, String profileName) {
        String lower = playerName.toLowerCase();
        JsonObject profiles = loadProfilesRaw(lower);
        return profiles.has(profileName) ? profiles.get(profileName).getAsString() : null;
    }

    public List<String> getProfileNames(String playerName) {
        String lower = playerName.toLowerCase();
        JsonObject profiles = loadProfilesRaw(lower);
        List<String> names = new ArrayList<>(profiles.keySet());
        Collections.sort(names);
        return names;
    }

    public void deleteProfile(String playerName, String profileName) {
        String lower = playerName.toLowerCase();
        JsonObject profiles = loadProfilesRaw(lower);
        if (profiles.has(profileName)) {
            profiles.remove(profileName);
            saveProfilesRaw(lower, profiles);
        }
    }

    // ── Settings ───────────────────────────────────────────────────────────────

    public void saveSettings(String playerName, String settingsData) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return;
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE players SET settings = ? WHERE player_name = ?");
            stmt.setString(1, settingsData);
            stmt.setString(2, lower);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception ignored) {}
    }

    public String loadSettings(String playerName) {
        String lower = playerName.toLowerCase();
        try {
            Connection conn = getConnection();
            if (conn == null) return null;
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT settings FROM players WHERE player_name = ?");
            stmt.setString(1, lower);
            ResultSet rs = stmt.executeQuery();
            String result = rs.next() ? rs.getString("settings") : null;
            rs.close(); stmt.close();
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public void shutdown() {
        try {
            scheduler.shutdown();
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception ignored) {}
    }
}
