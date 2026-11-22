package io.github.angelclient.client.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalCosmeticManager {

    private static LocalCosmeticManager INSTANCE;

    private final Map<String, LocalCosmetic> bandanas = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> hats = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> wings = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> capes = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> nametags = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> items = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> shields = new ConcurrentHashMap<>();
    private final Map<String, LocalCosmetic> auras = new ConcurrentHashMap<>();

    private final Map<String, String> activeBandanas = new ConcurrentHashMap<>();
    private final Map<String, String> activeHats = new ConcurrentHashMap<>();
    private final Map<String, String> activeWings = new ConcurrentHashMap<>();
    private final Map<String, String> activeNametags = new ConcurrentHashMap<>();
    private final Map<String, String> activeItems = new ConcurrentHashMap<>();
    private final Map<String, String> activeCapes = new ConcurrentHashMap<>();
    private final Map<String, String> activeShields = new ConcurrentHashMap<>();
    private final Map<String, String> activeAuras = new ConcurrentHashMap<>();

    private final Map<String, String> remoteBandanas = new ConcurrentHashMap<>();
    private final Map<String, String> remoteHats = new ConcurrentHashMap<>();
    private final Map<String, String> remoteWings = new ConcurrentHashMap<>();
    private final Map<String, String> remoteNametags = new ConcurrentHashMap<>();
    private final Map<String, String> remoteItems = new ConcurrentHashMap<>();
    private final Map<String, String> remoteCapes = new ConcurrentHashMap<>();
    private final Map<String, String> remoteShields = new ConcurrentHashMap<>();
    private final Map<String, String> remoteAuras = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LocalCosmetic-Sync");
        t.setDaemon(true);
        return t;
    });

    private LocalCosmeticManager() {
        loadAll();
        scheduler.scheduleAtFixedRate(this::fetchRemoteCosmetics, 10, 20, TimeUnit.SECONDS);
    }

    public static LocalCosmeticManager getInstance() {
        if (INSTANCE == null) INSTANCE = new LocalCosmeticManager();
        return INSTANCE;
    }

    private void loadAll() {
        loadBandanas();
        loadHats();
        loadWings();
        loadCapes();
        loadNametags();
        loadItems();
        loadShields();
        loadAuras();
    }

    private String readLocalJson(String path) {
        try {
            ResourceLocation loc = new ResourceLocation("angelcosmetics", path);
            InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
            Scanner s = new Scanner(in).useDelimiter("\\A");
            return s.hasNext() ? s.next() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readJson(String path, String remoteUrl) {
        if (remoteUrl != null) {
            String json = CosmeticAPI.makeGetRequest(remoteUrl);
            if (json != null && !json.isEmpty()) return json;
        }
        return readLocalJson(path);
    }

    private void loadBandanas() {
        String json = readJson("bandanas.json", CosmeticAPI.BANDANAS_JSON_URL);
        if (json != null && !json.isEmpty()) {
            try {
                JsonObject root = new JsonParser().parse(json).getAsJsonObject();
                JsonArray arr = root.getAsJsonArray("bandanas");
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    String name = obj.get("name").getAsString();
                    String file = obj.get("file").getAsString();
                    LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.BANDANA, file, null);
                    if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                    if (obj.has("allowedPlayers")) {
                        List<String> players = new ArrayList<>();
                        for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                            players.add(p.getAsString());
                        c.setAllowedPlayers(players);
                    }
                    bandanas.put(id.toLowerCase(), c);
                }
                return;
            } catch (Exception e) {
            }
        }
        String[] names = {"bandana", "bandana1", "bandana2", "bandana3", "bandana4", "bandana5", "bandana6",
                "bandana7", "bandana8", "bandana9", "bandana10", "bandana11", "bandana12", "bandana13",
                "bandana14", "bandana15", "bandana16", "bandana17", "bandana18", "bandana19", "bandana20", "bandana21"};
        for (String n : names) {
            String texPath = "bandana/bandana/" + n + ".png";
            try {
                ResourceLocation loc = new ResourceLocation("angelcosmetics", texPath);
                Minecraft.getMinecraft().getResourceManager().getResource(loc);
                LocalCosmetic c = new LocalCosmetic(n.toLowerCase(), n, CosmeticType.BANDANA, texPath, null);
                bandanas.put(n.toLowerCase(), c);
            } catch (Exception ignored) {}
        }
    }

    private void loadHats() {
        String json = readJson("hats.json", CosmeticAPI.HATS_JSON_URL);
        if (json == null || json.isEmpty()) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("hats");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                String tex = obj.has("texture") ? obj.get("texture").getAsString() : null;
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.HAT, f, tex);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                hats.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadWings() {
        String json = readJson("wings.json", CosmeticAPI.WINGS_JSON_URL);
        if (json == null || json.isEmpty()) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("wings");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.WING, f, null);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                wings.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadCapes() {
        String json = readJson("cosmetics.json", CosmeticAPI.COSMETICS_JSON_URL);
        if (json == null) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("capes");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                String tex = obj.has("texture") ? obj.get("texture").getAsString() : null;
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.CAPE, f, tex);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                capes.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadNametags() {
        String json = readJson("nametags.json", CosmeticAPI.NAMETAGS_JSON_URL);
        if (json == null) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("nametags");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String dn = obj.get("displayName").getAsString();
                String f = obj.get("file").getAsString();
                LocalCosmetic c = new LocalCosmetic(id, dn, CosmeticType.NAMETAG, f, null);
                if (obj.has("color")) c.setColor(obj.get("color").getAsString());
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                nametags.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadItems() {
        String json = readJson("itens.json", CosmeticAPI.ITEMS_JSON_URL);
        if (json == null || json.isEmpty()) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("itens");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                String tex = obj.has("texture") ? obj.get("texture").getAsString() : null;
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.ITEM, f, tex);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                items.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadShields() {
        String json = readJson("escudos.json", CosmeticAPI.SHIELDS_JSON_URL);
        if (json == null || json.isEmpty()) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("escudos");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                String tex = obj.has("texture") ? obj.get("texture").getAsString() : null;
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.SHIELD, f, tex);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                shields.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    private void loadAuras() {
        String json = readJson("auras.json", CosmeticAPI.AURAS_JSON_URL);
        if (json == null || json.isEmpty()) return;
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("auras");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("id").getAsString();
                String name = obj.get("name").getAsString();
                String f = obj.get("file").getAsString();
                String tex = obj.has("texture") ? obj.get("texture").getAsString() : null;
                LocalCosmetic c = new LocalCosmetic(id, name, CosmeticType.EFFECT, f, tex);
                if (obj.has("premium")) c.setPremium(obj.get("premium").getAsBoolean());
                if (obj.has("allowedPlayers")) {
                    List<String> players = new ArrayList<>();
                    for (JsonElement p : obj.getAsJsonArray("allowedPlayers"))
                        players.add(p.getAsString());
                    c.setAllowedPlayers(players);
                }
                auras.put(id.toLowerCase(), c);
            }
        } catch (Exception e) {
        }
    }

    public void setDisplayName(String id, String dn) {}

    public Collection<LocalCosmetic> getBandanas() { return bandanas.values(); }
    public Collection<LocalCosmetic> getHats() { return hats.values(); }
    public Collection<LocalCosmetic> getWings() { return wings.values(); }
    public Collection<LocalCosmetic> getCapes() { return capes.values(); }
    public Collection<LocalCosmetic> getNametags() { return nametags.values(); }
    public Collection<LocalCosmetic> getItems() { return items.values(); }
    public Collection<LocalCosmetic> getShields() { return shields.values(); }
    public Collection<LocalCosmetic> getAuras() { return auras.values(); }

    public LocalCosmetic getBandana(String id) { return bandanas.get(id.toLowerCase()); }
    public LocalCosmetic getHat(String id) { return hats.get(id.toLowerCase()); }
    public LocalCosmetic getWing(String id) { return wings.get(id.toLowerCase()); }
    public LocalCosmetic getNametag(String id) { return nametags.get(id.toLowerCase()); }
    public LocalCosmetic getItem(String id) { return items.get(id.toLowerCase()); }
    public LocalCosmetic getShield(String id) { return shields.get(id.toLowerCase()); }
    public LocalCosmetic getAura(String id) { return auras.get(id.toLowerCase()); }
    public LocalCosmetic getCape(String id) { return capes.get(id.toLowerCase()); }

    public void setActiveBandana(String player, String id) {
        if (id == null) {
            activeBandanas.remove(player);
            remoteBandanas.remove(player);
        } else {
            activeBandanas.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "bandana", id);
    }

    public void setActiveHat(String player, String id) {
        if (id == null) {
            activeHats.remove(player);
            remoteHats.remove(player);
        } else {
            activeHats.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "hat", id);
    }

    public void setActiveWing(String player, String id) {
        if (id == null) {
            activeWings.remove(player);
            remoteWings.remove(player);
        } else {
            activeWings.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "wing", id);
    }

    public String getActiveBandana(String player) {
        String local = activeBandanas.get(player);
        if (local != null) return local;
        return remoteBandanas.get(player);
    }

    public String getActiveHat(String player) {
        String local = activeHats.get(player);
        if (local != null) return local;
        return remoteHats.get(player);
    }

    public String getActiveWing(String player) {
        String local = activeWings.get(player);
        if (local != null) return local;
        return remoteWings.get(player);
    }

    public void setActiveNametag(String player, String id) {
        if (id == null) {
            activeNametags.remove(player);
            remoteNametags.remove(player);
        } else {
            activeNametags.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "nametag_cosmetic", id);
    }

    public String getActiveNametag(String player) {
        String local = activeNametags.get(player);
        if (local != null) return local;
        return remoteNametags.get(player);
    }

    public void setActiveItem(String player, String id) {
        if (id == null) {
            activeItems.remove(player);
            remoteItems.remove(player);
        } else {
            activeItems.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "item", id);
    }

    public void setActiveShield(String player, String id) {
        if (id == null) {
            activeShields.remove(player);
            remoteShields.remove(player);
        } else {
            activeShields.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "shield", id);
    }

    public void setActiveCape(String player, String id) {
        if (id == null) {
            activeCapes.remove(player);
            remoteCapes.remove(player);
        } else {
            activeCapes.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "cape", id);
    }

    public String getActiveItem(String player) {
        String local = activeItems.get(player);
        if (local != null) return local;
        return remoteItems.get(player);
    }

    public String getActiveShield(String player) {
        String local = activeShields.get(player);
        if (local != null) return local;
        return remoteShields.get(player);
    }

    public String getActiveCape(String player) {
        String local = activeCapes.get(player);
        if (local != null) return local;
        return remoteCapes.get(player);
    }

    // ── Favorites ────────────────────────────────────────────────────────────────

    private Set<String> favorites = new HashSet<>();
    private boolean favoritesLoaded = false;

    public Set<String> getFavorites() {
        if (!favoritesLoaded) {
            String player = Minecraft.getMinecraft().getSession().getUsername();
            if (player != null && !player.isEmpty()) {
                favorites = MySQLManager.getInstance().getFavorites(player);
                favoritesLoaded = true;
            }
        }
        return favorites;
    }

    public boolean isFavorite(String cosmeticId) {
        return getFavorites().contains(cosmeticId.toLowerCase());
    }

    public void toggleFavorite(String cosmeticId) {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        if (player == null || player.isEmpty()) return;
        String id = cosmeticId.toLowerCase();
        if (favorites.contains(id)) {
            favorites.remove(id);
            MySQLManager.getInstance().removeFavorite(player, id);
        } else {
            favorites.add(id);
            MySQLManager.getInstance().addFavorite(player, id);
        }
    }

    // ── Profiles ─────────────────────────────────────────────────────────────────

    public void saveProfile(String profileName) {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        if (player == null || player.isEmpty()) return;

        JsonObject data = new JsonObject();
        String lower = player.toLowerCase();

        String cape = getActiveCape(lower);
        if (cape != null) data.addProperty("cape", cape);
        String bandana = getActiveBandana(lower);
        if (bandana != null) data.addProperty("bandana", bandana);
        String hat = getActiveHat(lower);
        if (hat != null) data.addProperty("hat", hat);
        String wing = getActiveWing(lower);
        if (wing != null) data.addProperty("wing", wing);
        String nametag = getActiveNametag(lower);
        if (nametag != null) data.addProperty("nametag_cosmetic", nametag);
        String item = getActiveItem(lower);
        if (item != null) data.addProperty("item", item);
        String shield = getActiveShield(lower);
        if (shield != null) data.addProperty("shield", shield);
        String aura = getActiveAura(lower);
        if (aura != null) data.addProperty("aura", aura);

        JsonArray favArray = new JsonArray();
        for (String f : getFavorites()) favArray.add(f);
        data.add("favorites", favArray);

        MySQLManager.getInstance().saveProfile(player, profileName, data.toString());
    }

    public boolean loadProfile(String profileName) {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        if (player == null || player.isEmpty()) return false;

        String data = MySQLManager.getInstance().loadProfile(player, profileName);
        if (data == null) return false;

        try {
            JsonObject root = new JsonParser().parse(data).getAsJsonObject();
            String lower = player.toLowerCase();

            if (root.has("cape") && !root.get("cape").isJsonNull())
                setActiveCape(player, root.get("cape").getAsString());
            else setActiveCape(player, null);
            if (root.has("bandana") && !root.get("bandana").isJsonNull())
                setActiveBandana(player, root.get("bandana").getAsString());
            else setActiveBandana(player, null);
            if (root.has("hat") && !root.get("hat").isJsonNull())
                setActiveHat(player, root.get("hat").getAsString());
            else setActiveHat(player, null);
            if (root.has("wing") && !root.get("wing").isJsonNull())
                setActiveWing(player, root.get("wing").getAsString());
            else setActiveWing(player, null);
            if (root.has("nametag_cosmetic") && !root.get("nametag_cosmetic").isJsonNull())
                setActiveNametag(player, root.get("nametag_cosmetic").getAsString());
            else setActiveNametag(player, null);
            if (root.has("item") && !root.get("item").isJsonNull())
                setActiveItem(player, root.get("item").getAsString());
            else setActiveItem(player, null);
            if (root.has("shield") && !root.get("shield").isJsonNull())
                setActiveShield(player, root.get("shield").getAsString());
            else setActiveShield(player, null);
            if (root.has("aura") && !root.get("aura").isJsonNull())
                setActiveAura(player, root.get("aura").getAsString());
            else setActiveAura(player, null);

            if (root.has("favorites")) {
                favorites.clear();
                for (JsonElement el : root.getAsJsonArray("favorites")) {
                    favorites.add(el.getAsString());
                }
                for (String fav : favorites) {
                    MySQLManager.getInstance().addFavorite(player, fav);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getProfileNames() {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        if (player == null || player.isEmpty()) return new ArrayList<>();
        return MySQLManager.getInstance().getProfileNames(player);
    }

    public void deleteProfile(String profileName) {
        String player = Minecraft.getMinecraft().getSession().getUsername();
        if (player == null || player.isEmpty()) return;
        MySQLManager.getInstance().deleteProfile(player, profileName);
    }

    // ── Preview Override (for GUI preview without equipping) ──────────────────

    private final Map<CosmeticType, String> previewOverrides = new ConcurrentHashMap<>();

    public void setPreviewOverride(CosmeticType type, String id) {
        if (id == null) previewOverrides.remove(type);
        else previewOverrides.put(type, id);
    }

    public void clearPreviewOverrides() {
        previewOverrides.clear();
    }

    public String getActiveWithPreview(String player, CosmeticType type, String activeId) {
        String preview = previewOverrides.get(type);
        return preview != null ? preview : activeId;
    }

    public boolean hasLargeCosmetic(String player) {
        String hat = getActiveHat(player);
        if (hat != null) {
            String lower = hat.toLowerCase();
            if (lower.contains("urso") || lower.contains("minime")
                || lower.contains("raposa") || lower.contains("bear")
                || lower.contains("fox")) return true;
        }
        return false;
    }

    public void setActiveAura(String player, String id) {
        if (id == null) {
            activeAuras.remove(player);
            remoteAuras.remove(player);
        } else {
            activeAuras.put(player, id);
        }
        CosmeticAPI.syncPlayerToMySQL();
        MySQLManager.getInstance().setCosmetic(player, "aura", id);
    }

    public String getActiveAura(String player) {
        String local = activeAuras.get(player);
        if (local != null) return local;
        return remoteAuras.get(player);
    }

    public boolean playerHasAnyActive(String player) {
        if (activeCapes.containsKey(player) || remoteCapes.containsKey(player)) return true;
        if (activeBandanas.containsKey(player) || remoteBandanas.containsKey(player)) return true;
        if (activeHats.containsKey(player) || remoteHats.containsKey(player)) return true;
        if (activeWings.containsKey(player) || remoteWings.containsKey(player)) return true;
        if (activeNametags.containsKey(player) || remoteNametags.containsKey(player)) return true;
        if (activeItems.containsKey(player) || remoteItems.containsKey(player)) return true;
        if (activeShields.containsKey(player) || remoteShields.containsKey(player)) return true;
        if (activeAuras.containsKey(player) || remoteAuras.containsKey(player)) return true;
        return false;
    }

    public void forceRefresh() {
        scheduler.execute(this::fetchRemoteCosmetics);
    }

    public void clearActiveAndRemote() {
        activeBandanas.clear();
        activeHats.clear();
        activeWings.clear();
        activeCapes.clear();
        activeNametags.clear();
        activeItems.clear();
        activeShields.clear();
        activeAuras.clear();
        remoteBandanas.clear();
        remoteHats.clear();
        remoteWings.clear();
        remoteCapes.clear();
        remoteNametags.clear();
        remoteItems.clear();
        remoteShields.clear();
        remoteAuras.clear();
    }

    public List<LocalCosmetic> getAll() {
        List<LocalCosmetic> all = new ArrayList<>();
        all.addAll(bandanas.values());
        all.addAll(hats.values());
        all.addAll(wings.values());
        all.addAll(capes.values());
        all.addAll(nametags.values());
        return all;
    }

    private void fetchRemoteCosmetics() {
        try {
            String json = CosmeticAPI.readPlayersJson();
            if (json == null) return;
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            String localPlayer = Minecraft.getMinecraft().getSession().getUsername().toLowerCase();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                try {
                    String player = entry.getKey();
                    JsonObject data = entry.getValue().getAsJsonObject();

                    if (data.has("cape") && !data.get("cape").isJsonNull()) {
                        String val = data.get("cape").getAsString();
                        remoteCapes.put(player, val);
                        if (player.equals(localPlayer) && !activeCapes.containsKey(player))
                            activeCapes.put(player, val);
                    }
                    if (data.has("bandana") && !data.get("bandana").isJsonNull()) {
                        String val = data.get("bandana").getAsString();
                        remoteBandanas.put(player, val);
                        if (player.equals(localPlayer) && !activeBandanas.containsKey(player))
                            activeBandanas.put(player, val);
                    }
                    if (data.has("hat") && !data.get("hat").isJsonNull()) {
                        String val = data.get("hat").getAsString();
                        remoteHats.put(player, val);
                        if (player.equals(localPlayer) && !activeHats.containsKey(player))
                            activeHats.put(player, val);
                    }
                    if (data.has("wing") && !data.get("wing").isJsonNull()) {
                        String val = data.get("wing").getAsString();
                        remoteWings.put(player, val);
                        if (player.equals(localPlayer) && !activeWings.containsKey(player))
                            activeWings.put(player, val);
                    }
                    if (data.has("nametag_cosmetic") && !data.get("nametag_cosmetic").isJsonNull()) {
                        String val = data.get("nametag_cosmetic").getAsString();
                        remoteNametags.put(player, val);
                        if (player.equals(localPlayer) && !activeNametags.containsKey(player))
                            activeNametags.put(player, val);
                    }
                    if (data.has("item") && !data.get("item").isJsonNull()) {
                        String val = data.get("item").getAsString();
                        remoteItems.put(player, val);
                        if (player.equals(localPlayer) && !activeItems.containsKey(player))
                            activeItems.put(player, val);
                    }
                    if (data.has("shield") && !data.get("shield").isJsonNull()) {
                        String val = data.get("shield").getAsString();
                        remoteShields.put(player, val);
                        if (player.equals(localPlayer) && !activeShields.containsKey(player))
                            activeShields.put(player, val);
                    }
                    if (data.has("aura") && !data.get("aura").isJsonNull()) {
                        String val = data.get("aura").getAsString();
                        remoteAuras.put(player, val);
                        if (player.equals(localPlayer) && !activeAuras.containsKey(player))
                            activeAuras.put(player, val);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        }
    }
}
