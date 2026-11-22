package io.github.angelclient.client.mushstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MushApiClient {
    private static final int CACHE_LIMIT = 800;
    private static final long CACHE_TTL = 120000;
    private static final Map<String, PlayerStats> statsCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final String BASE_URL = "https://mush.com.br/api";

    public interface LeaderboardCallback {
        void onResult(String str);
        void onError(String str);
    }

    public static void getPlayerStatsAsync(String username, PlayerStatsCallback callback) {
        getPlayerStatsAsync(username, callback, false);
    }

    public static void fetchStats(String username) {
    getPlayerStatsAsync(username, new PlayerStatsCallback() {
        @Override
        public void onResult(PlayerStats stats) {
            // Pode deixar vazio (cache já é atualizado automaticamente)
        }

        @Override
        public void onError(String username, String error) {
            // Também pode deixar vazio ou logar se quiser debug
            System.out.println("[MushApiClient] Erro ao buscar stats de " + username + ": " + error);
        }
    });
}

    public static void getPlayerStatsAsync(String username, final PlayerStatsCallback callback, boolean forceRefresh) {
        Long timestamp;
        final String sanitizedUsername = username.replaceAll("[^A-Za-z0-9_]", "");
        if (!forceRefresh && statsCache.containsKey(sanitizedUsername) && (timestamp = cacheTimestamps.get(sanitizedUsername)) != null && System.currentTimeMillis() - timestamp < CACHE_TTL) {
            callback.onResult(statsCache.get(sanitizedUsername));
        } else {
            new Thread(() -> {
                HttpURLConnection conn = null;
                try {
                    String urlString = BASE_URL + "/player/" + sanitizedUsername;
                    URL url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "MushMcStatsMod/3.0");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 429) {
                        callback.onError(sanitizedUsername, "RATE_LIMIT");
                    } else if (responseCode == 404) {
                        callback.onError(sanitizedUsername, "Jogador não encontrado.");
                    } else if (responseCode == 200) {
                        String json = readStream(conn.getInputStream());
                        PlayerStats stats = parseJson(sanitizedUsername, json);
                        if (stats.isDataAvailable()) {
                            statsCache.put(sanitizedUsername, stats);
                            cacheTimestamps.put(sanitizedUsername, System.currentTimeMillis());
                        }
                        trimCache();
                        callback.onResult(stats);
                    } else {
                        callback.onError(sanitizedUsername, "Erro HTTP: " + responseCode);
                    }
                } catch (SocketTimeoutException e) {
                    callback.onError(sanitizedUsername, "Timeout na conexão");
                } catch (IOException e) {
                    callback.onError(sanitizedUsername, "Erro de rede: " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }).start();
        }
    }

    public static void getLeaderboardAsync(final String minigame, final LeaderboardCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String urlString = BASE_URL + "/leaderboard/" + minigame;
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "MushMcStatsMod/3.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    callback.onResult(readStream(conn.getInputStream()));
                } else {
                    callback.onError("Erro: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String getJsonObject(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";
        int start = json.indexOf("{", keyIndex + searchKey.length());
        if (start == -1) return "";
        int braceCount = 0;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            if (braceCount == 0) return json.substring(start, i + 1);
        }
        return "";
    }

    public static PlayerStats parseJson(String username, String json) {
        String bedwarsJson = getJsonObject(json, "bedwars");
        String context = bedwarsJson.isEmpty() ? json : bedwarsJson;

        int wins = getJsonIntValue(context, "wins");
        int losses = getJsonIntValue(context, "losses");
        int finalKills = getJsonIntValue(context, "final_kills");
        int fkdr = (int) getJsonFloatValue(context, "fkdr"); 
        int bedsBroken = getJsonIntValue(context, "beds_broken");
        int level = getJsonIntValue(context, "level");
        long xp = getJsonLongValue(context, "xp");

        String badgeJson = getJsonObject(context, "level_badge");
        String levelFormatString = getJsonStringValue(badgeJson, "format");
        String hexColor = getJsonStringValue(badgeJson, "hex_color");
        int levelColorInt = parseHexColor(hexColor);

        if (levelColorInt == 11184810 && level >= 200 && level <= 299) {
            if (levelFormatString != null && !levelFormatString.isEmpty()) {
                levelColorInt = getColorFromFormatString(levelFormatString, 11184810);
            }
            if (levelColorInt == 11184810) levelColorInt = -22016;
        }

        int winstreak = getJsonIntValue(context, "winstreak");
        String accountJson = getJsonObject(json, "account");
        String accountType = getJsonStringValue(accountJson, "type");
        long firstLogin = getJsonLongValue(json, "first_login");

        boolean isDataAvailable = level > 0 || wins > 0 || losses > 0;
        return new PlayerStats(username, wins, losses, finalKills, fkdr, bedsBroken, level, xp, levelFormatString, levelColorInt, winstreak, accountType, firstLogin, isDataAvailable);
    }

    private static int getJsonIntValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return 0;
            int start = keyIndex + searchKey.length();
            int end = findJsonValueEnd(json, start);
            String val = json.substring(start, end).trim().split("\\.")[0].replaceAll("[^0-9-]", "");
            return val.isEmpty() ? 0 : Integer.parseInt(val);
        } catch (Exception e) { return 0; }
    }

    private static float getJsonFloatValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return 0.0f;
            int start = keyIndex + searchKey.length();
            int end = findJsonValueEnd(json, start);
            return Float.parseFloat(json.substring(start, end).trim());
        } catch (Exception e) { return 0.0f; }
    }

    private static long getJsonLongValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return 0L;
            int start = keyIndex + searchKey.length();
            int end = findJsonValueEnd(json, start);
            String val = json.substring(start, end).trim().replaceAll("[^0-9-]", "");
            return val.isEmpty() ? 0L : Long.parseLong(val);
        } catch (Exception e) { return 0L; }
    }

    private static int findJsonValueEnd(String json, int start) {
        int comma = json.indexOf(',', start);
        int brace = json.indexOf('}', start);
        if (comma != -1 && brace != -1) return Math.min(comma, brace);
        return (comma != -1) ? comma : brace;
    }

    private static String getJsonStringValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return "";
            int start = json.indexOf("\"", keyIndex + searchKey.length()) + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) { return ""; }
    }

    public static String readStream(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private static int parseHexColor(String hex) {
        if (hex == null || hex.isEmpty()) return 11184810;
        try {
            return Integer.parseInt(hex.replaceAll("[#'\"]", ""), 16) | 0xFF000000;
        } catch (Exception e) { return 11184810; }
    }

    private static int getColorFromFormatString(String format, int defaultColor) {
        if (format == null || !format.contains("§")) return defaultColor;
        char code = format.charAt(format.indexOf("§") + 1);
        switch (code) {
            case '1': return -16777046; case '2': return -16733696; case '3': return -16733526;
            case '4': return -5636096; case '5': return -5635926; case '6': return -22016;
            case 'a': return -11141291; case 'b': return -11141121; case 'c': return -43691;
            case 'd': return -43521; case 'e': return -171; case 'f': return -1;
            default: return defaultColor;
        }
    }

    public static void removeCachedPlayer(String username) {
        String sanitized = username.replaceAll("[^A-Za-z0-9_]", "");
        statsCache.remove(sanitized);
        cacheTimestamps.remove(sanitized);
    }

    public static void clearCache() {
        statsCache.clear();
        cacheTimestamps.clear();
    }

    public static PlayerStats getCachedPlayer(String username) {
    String sanitized = username.replaceAll("[^A-Za-z0-9_]", "");
    return statsCache.get(sanitized);
}

    private static void trimCache() {
        if (statsCache.size() > CACHE_LIMIT) {
            String oldestKey = statsCache.keySet().iterator().next();
            statsCache.remove(oldestKey);
            cacheTimestamps.remove(oldestKey);
        }
    }
}