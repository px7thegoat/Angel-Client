package io.github.angelclient.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class DiscordApiHelper {

    public interface Callback<T> {
        void onResult(T result);
    }

    public static class AuthUrlResponse {
        public final boolean sucesso;
        public final String url;
        public final String state;
        public final String erro;
        public AuthUrlResponse(boolean sucesso, String url, String state, String erro) {
            this.sucesso = sucesso; this.url = url; this.state = state; this.erro = erro;
        }
    }

    public static class ApiResponse {
        public final boolean sucesso;
        public final int valor;
        public final String erro;
        public ApiResponse(boolean sucesso, int valor, String erro) {
            this.sucesso = sucesso; this.valor = valor; this.erro = erro;
        }
    }

    public static void getAuthUrlAsync(final String nick, final Callback<AuthUrlResponse> callback) {
        new Thread(() -> {
            final AuthUrlResponse result = getAuthUrlSync(nick);
            MinecraftRunOnMain(() -> callback.onResult(result));
        }, "AngelClient-AuthUrl").start();
    }

    private static AuthUrlResponse getAuthUrlSync(String nick) {
        try {
            String urlStr = DiscordConfig.getApiUrl() + "/auth/discord?nick=" + URLEncoder.encode(nick, "UTF-8");
            String body = get(urlStr);
            if (body == null) return new AuthUrlResponse(false, null, null, "Sem resposta da API.");
            if (body.startsWith("ERR:")) return new AuthUrlResponse(false, null, null, body.substring(4));

            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            if (!json.has("url")) return new AuthUrlResponse(false, null, null, "Campo url nao encontrado.");
            String url = json.get("url").getAsString();

            String state = null;
            int stateIdx = url.indexOf("state=");
            if (stateIdx != -1) {
                int stateStart = stateIdx + 6;
                int stateEnd = url.indexOf("&", stateStart);
                state = stateEnd == -1 ? url.substring(stateStart) : url.substring(stateStart, stateEnd);
            }

            return new AuthUrlResponse(true, url, state, null);
        } catch (Exception e) {
            return new AuthUrlResponse(false, null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public static String checkAuthStatusSync(String nick) {
        try {
            String urlStr = DiscordConfig.getApiUrl() + "/auth/status/" + URLEncoder.encode(nick, "UTF-8");
            String body = get(urlStr);
            if (body == null || body.startsWith("ERR:")) return null;
            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            if (!json.has("vinculado") || !json.get("vinculado").getAsBoolean()) return null;
            return json.has("discord_id") ? json.get("discord_id").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] fetchDiscordIdFromToken(String state) {
        try {
            String tokenUrl = DiscordConfig.getApiUrl() + "/auth/token/" + state;
            String tokenBody = get(tokenUrl);
            if (tokenBody == null || tokenBody.startsWith("ERR:")) return null;

            JsonObject tokenJson = new JsonParser().parse(tokenBody).getAsJsonObject();
            if (!tokenJson.has("access_token") || !tokenJson.has("nick")) return null;
            String accessToken = tokenJson.get("access_token").getAsString();
            String nick = tokenJson.get("nick").getAsString();

            URL url = new URL("https://discord.com/api/users/@me");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("User-Agent", "AngelClient/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status != 200) return null;

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String userBody = br.readLine();
            br.close();
            conn.disconnect();

            JsonObject userJson = new JsonParser().parse(userBody).getAsJsonObject();
            if (!userJson.has("id")) return null;
            String discordId = userJson.get("id").getAsString();
            String username = userJson.has("username") ? userJson.get("username").getAsString() : "";
            String avatar = userJson.has("avatar") && !userJson.get("avatar").isJsonNull()
                ? userJson.get("avatar").getAsString() : "";

            return new String[]{nick, discordId, state, accessToken, username, avatar};
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean confirmarVinculo(String nick, String discordId, String state) {
        try {
            String urlStr = DiscordConfig.getApiUrl() + "/auth/confirmar"
                + "?nick=" + URLEncoder.encode(nick, "UTF-8")
                + "&discord_id=" + discordId
                + "&state=" + URLEncoder.encode(state, "UTF-8");
            String body = get(urlStr);
            if (body == null || body.startsWith("ERR:")) {
                System.out.println("[DiscordApi] confirmarVinculo falhou: " + (body != null ? body : "sem resposta"));
                return false;
            }
            JsonObject json = new JsonParser().parse(body).getAsJsonObject();
            return json.has("status") && "ok".equals(json.get("status").getAsString());
        } catch (Exception e) {
            System.out.println("[DiscordApi] confirmarVinculo erro: " + e.getMessage());
            return false;
        }
    }

    public static void resgatarAsync(final String codigo, final Callback<ApiResponse> callback) {
        new Thread(() -> {
            final ApiResponse resp = resgatarSync(codigo);
            MinecraftRunOnMain(() -> callback.onResult(resp));
        }, "AngelClient-Resgatar").start();
    }

    private static ApiResponse resgatarSync(String codigo) {
        try {
            String urlStr = DiscordConfig.getApiUrl() + "/resgatar/" + codigo.trim().toUpperCase();
            String body = get(urlStr);
            if (body == null) return new ApiResponse(false, 0, "Sem resposta da API.");
            if (body.startsWith("ERR:")) return new ApiResponse(false, 0, body.substring(4));
            try {
                JsonObject json = new JsonParser().parse(body).getAsJsonObject();
                if (json.has("status") && "ok".equals(json.get("status").getAsString())) {
                    int valor = json.has("valor") ? json.get("valor").getAsInt() : 0;
                    return new ApiResponse(true, valor, null);
                }
            } catch (Exception ignored) {}
            if (body.contains("404") || body.contains("invalido") || body.contains("usado")) {
                return new ApiResponse(false, 0, "Codigo invalido ou ja utilizado.");
            }
            return new ApiResponse(false, 0, "Resposta inesperada: " + body);
        } catch (Exception e) {
            return new ApiResponse(false, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static String get(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", DiscordConfig.getApiKey());
            conn.setRequestProperty("ngrok-skip-browser-warning", "true");
            conn.setRequestProperty("localtonet-skip-warning", "true");
            conn.setRequestProperty("User-Agent", "AngelClient/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            BufferedReader br;
            if (status >= 200 && status < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } else {
                java.io.InputStream es = conn.getErrorStream();
                if (es == null) return "ERR:HTTP " + status + " (sem body)";
                br = new BufferedReader(new InputStreamReader(es, "UTF-8"));
            }
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return "ERR:" + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void MinecraftRunOnMain(Runnable r) {
        try {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(r);
        } catch (Exception e) {
            r.run();
        }
    }
}
