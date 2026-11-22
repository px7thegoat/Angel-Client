package io.github.angelclient.client.nametag;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.angelclient.client.cosmetic.CosmeticAPI;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class NametagManager {

    private static NametagManager instance;
    private static final String DEFAULT_MEMBER_TAG_ID = "member";
    private static final long REFRESH_INTERVAL_SECONDS = 30L;

    private volatile List<Nametag>        allNametags    = new ArrayList<>();
    private final Map<String, Nametag>    playerNametags = new ConcurrentHashMap<>();
    private final Set<String>             angelClientUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, String>     specialTags = new ConcurrentHashMap<>();
    private final Map<String, Integer>    borderColors = new ConcurrentHashMap<>();
    private final Set<String>             gradientBorderPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer>    borderGradientColor2  = new ConcurrentHashMap<>();
    private final Map<String, String>     nametagMessages = new ConcurrentHashMap<>();
    private String equippedNametagId = null;
    private String defaultNametagId = DEFAULT_MEMBER_TAG_ID;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Nametag-Refresh");
                t.setDaemon(true);
                return t;
            });

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private NametagManager() {
        scheduler.execute(this::doLoadNametags);
        scheduler.execute(this::doRefreshAll);
        scheduler.scheduleAtFixedRate(this::doRefreshAll,
                REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static NametagManager getInstance() {
        if (instance == null) instance = new NametagManager();
        return instance;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    public List<Nametag> getAllNametags() {
        return allNametags;
    }

    public Nametag getPlayerNametag(String playerName) {
        if (playerName == null) return null;
        String lowerName = playerName.toLowerCase();
        if (specialTags.containsKey(lowerName)) {
            return findNametagById(specialTags.get(lowerName));
        }
        if (playerNametags.containsKey(lowerName))
            return playerNametags.get(lowerName);
        if (hasAngelClient(lowerName) && defaultNametagId != null)
            return findNametagById(defaultNametagId);
        return null;
    }

    public boolean hasAngelClient(String playerName) {
        if (playerName == null) return false;
        String lowerName = playerName.toLowerCase();
        return lowerName.contains("angel") || angelClientUsers.contains(lowerName);
    }

    public boolean hasSpecialTag(String playerName) {
        if (playerName == null) return false;
        return specialTags.containsKey(playerName.toLowerCase());
    }

    public String getSpecialTagType(String playerName) {
        if (playerName == null) return null;
        return specialTags.get(playerName.toLowerCase());
    }

    /**
     * Retorna o Nametag correspondente ao special_tag do jogador (ex: "owner", "mod").
     * A textura é baixada do GitHub via Nametag.getTexture().
     */
    public Nametag getTagNametag(String playerName) {
        if (playerName == null) return null;
        String tagType = specialTags.get(playerName.toLowerCase());
        if (tagType == null) return null;
        return findNametagById(tagType);
    }

    /**
     * Retorna apenas a nametag pessoal do jogador (ignorando special_tag).
     */
    public Nametag getPersonalNametag(String playerName) {
        if (playerName == null) return null;
        String lowerName = playerName.toLowerCase();
        if (playerNametags.containsKey(lowerName))
            return playerNametags.get(lowerName);
        if (hasAngelClient(lowerName) && defaultNametagId != null)
            return findNametagById(defaultNametagId);
        return null;
    }

    public String getEquippedNametagId() {
        if (equippedNametagId == null) {
            String playerName = Minecraft.getMinecraft().getSession().getUsername();
            Nametag nametag = getPlayerNametag(playerName);
            if (nametag != null) equippedNametagId = nametag.getId();
        }
        return equippedNametagId;
    }

    public void setDefaultNametagId(String nametagId) {
        this.defaultNametagId = nametagId;
    }

    public String getDefaultNametagId() {
        return defaultNametagId;
    }

    public void equipNametag(String nametagId) {
        equippedNametagId = nametagId;
        CosmeticAPI.syncPlayerToMySQL();
    }

    public void unequipNametag() { equipNametag("none"); }

    public void uploadBorderColor(int argb, boolean gradient, int argb2) {
        String p = Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
        borderColors.put(p, argb);
        if (gradient) {
            gradientBorderPlayers.add(p);
            borderGradientColor2.put(p, argb2);
        } else {
            gradientBorderPlayers.remove(p);
            borderGradientColor2.remove(p);
        }
        CosmeticAPI.syncPlayerToMySQL();
    }

    public void uploadBorderColor(int argb) { uploadBorderColor(argb, false, 0); }
    public void uploadBorderColor(int argb, boolean gradient) { uploadBorderColor(argb, gradient, 0); }

    public void removeBorderColor() {
        borderColors.remove(Minecraft.getMinecraft().getSession().getUsername().toLowerCase());
        CosmeticAPI.syncPlayerToMySQL();
    }

    public int getPlayerBorderColor(String playerName) {
        if (playerName == null) return -1;
        Integer color = borderColors.get(playerName.toLowerCase());
        return color != null ? color : -1;
    }

    public boolean isPlayerBorderGradient(String playerName) {
        if (playerName == null) return false;
        return gradientBorderPlayers.contains(playerName.toLowerCase());
    }

    public int getPlayerBorderGradientColor2(String playerName) {
        if (playerName == null) return -1;
        Integer color = borderGradientColor2.get(playerName.toLowerCase());
        return color != null ? color : -1;
    }

    public String getPlayerNametagMessage(String playerName) {
        if (playerName == null) return null;
        return nametagMessages.get(playerName.toLowerCase());
    }

    public void uploadNametagMessage(String message) {
        String p = Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
        if (message == null || message.trim().isEmpty())
            nametagMessages.remove(p);
        else
            nametagMessages.put(p, message.trim());
        CosmeticAPI.syncPlayerToMySQL();
    }

    public void forceRefresh() { scheduler.execute(this::doRefreshAll); }

    // ── Carga única de players.json ──────────────────────────────────────────

    private void doRefreshAll() {
        if (!refreshing.compareAndSet(false, true)) return;
        try {
            String json = CosmeticAPI.readPlayersJson();
            if (json == null) return;

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            Set<String> newAngel = ConcurrentHashMap.newKeySet();
            Map<String, Nametag> newNametags = new HashMap<>();
            Map<String, String> newSpecial = new HashMap<>();
            Map<String, Integer> newColors = new HashMap<>();
            Set<String> newGrad = new HashSet<>();
            Map<String, Integer> newGrad2 = new HashMap<>();
            Map<String, String> newMsgs = new HashMap<>();

            for (String playerName : root.keySet()) {
                try {
                    JsonObject pd = root.getAsJsonObject(playerName);
                    String lower = playerName.toLowerCase();

                    boolean isAngel = pd.has("has_angelclient") && pd.get("has_angelclient").getAsBoolean();
                    if (isAngel) newAngel.add(lower);

                    if (pd.has("nametag")) {
                        String ntId = pd.get("nametag").getAsString();
                        if (!ntId.equals("none")) {
                            Nametag nt = findNametagById(ntId);
                            if (nt != null) newNametags.put(lower, nt);
                        }
                    }

                    if (pd.has("special_tag"))
                        newSpecial.put(lower, pd.get("special_tag").getAsString());

                    if (pd.has("border_color")) {
                        int c = parseHexColor(pd.get("border_color").getAsString());
                        if (c != -1) newColors.put(lower, c);
                        if (pd.has("border_gradient") && pd.get("border_gradient").getAsBoolean()) {
                            newGrad.add(lower);
                            if (pd.has("border_color2")) {
                                int c2 = parseHexColor(pd.get("border_color2").getAsString());
                                if (c2 != -1) newGrad2.put(lower, c2);
                            }
                        }
                    }

                    if (pd.has("nametag_message")) {
                        String msg = pd.get("nametag_message").getAsString().trim();
                        if (!msg.isEmpty()) newMsgs.put(lower, msg);
                    }
                } catch (Exception ignored) {}
            }

            angelClientUsers.clear(); angelClientUsers.addAll(newAngel);
            playerNametags.clear();   playerNametags.putAll(newNametags);
            specialTags.clear();      specialTags.putAll(newSpecial);
            borderColors.clear();     borderColors.putAll(newColors);
            gradientBorderPlayers.clear(); gradientBorderPlayers.addAll(newGrad);
            borderGradientColor2.clear();  borderGradientColor2.putAll(newGrad2);
            nametagMessages.clear();  nametagMessages.putAll(newMsgs);

            System.out.println("[NametagManager] players.json loaded: " + root.size() + " players");
        } catch (Exception e) {
            System.err.println("[NametagManager] Erro no refresh: " + e.getMessage());
        } finally {
            refreshing.set(false);
        }
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

    private void doLoadNametags() {
        List<Nametag> loaded = NametagAPI.fetchNametags();
        if (loaded != null) {
            allNametags = loaded;
            System.out.println("[NametagManager] Nametags disponíveis: " + loaded.size());
        }
    }

    private String toHexColor(int argb) {
        return String.format("#%02X%02X%02X%02X",
                (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >> 24) & 0xFF);
    }

    private int parseHexColor(String hex) {
        if (hex == null) return -1;
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            if (hex.length() == 6) return 0xFF000000 | (int) Long.parseLong(hex, 16);
            if (hex.length() == 8) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                int a = Integer.parseInt(hex.substring(6, 8), 16);
                return (a << 24) | (r << 16) | (g << 8) | b;
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    private Nametag findNametagById(String id) {
        for (Nametag n : allNametags) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }
}