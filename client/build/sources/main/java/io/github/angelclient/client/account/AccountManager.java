package io.github.angelclient.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountManager {

    private static final int MAX_ACCOUNTS = 3;
    private static AccountManager INSTANCE;
    private final List<Account> accounts = new ArrayList<>();
    private Account currentAccount;
    private final Gson gson;

    private AccountManager() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public static AccountManager getInstance() {
        if (INSTANCE == null) INSTANCE = new AccountManager();
        return INSTANCE;
    }

    public List<Account> getAccounts() { return accounts; }
    public Account getCurrentAccount() { return currentAccount; }
    public int getMaxAccounts() { return MAX_ACCOUNTS; }

    public void load() {
        accounts.clear();
        try {
            Minecraft mc = Minecraft.getMinecraft();
            String playerName = mc.getSession().getUsername();
            if (playerName == null || playerName.isEmpty()) return;
            String json = io.github.angelclient.client.cosmetic.CosmeticAPI.readPlayerJson(playerName);
            if (json == null || json.isEmpty()) return;
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (!root.has("accounts")) return;
            JsonArray arr = root.getAsJsonArray("accounts");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String typeStr = obj.get("type").getAsString();
                String username = obj.get("username").getAsString();
                String uuid = obj.get("uuid").getAsString();
                Account.Type type = "microsoft".equals(typeStr) ? Account.Type.MICROSOFT : Account.Type.OFFLINE;
                accounts.add(new Account(type, username, uuid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAccount(Account account) {
        if (accounts.size() >= MAX_ACCOUNTS) return;
        accounts.removeIf(a -> a.getUsername().equalsIgnoreCase(account.getUsername()));
        accounts.add(account);
        save();
    }

    public void removeAccount(String username) {
        accounts.removeIf(a -> a.getUsername().equalsIgnoreCase(username));
        save();
    }

    public void switchTo(Account account) {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            Session session = new Session(
                account.getUsername(),
                account.getUuid(),
                "",
                account.getType() == Account.Type.MICROSOFT ? "mojang" : "legacy"
            );
            Field sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, session);
            currentAccount = account;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            String playerName = mc.getSession().getUsername();
            if (playerName == null || playerName.isEmpty()) return;

            String existingJson = io.github.angelclient.client.cosmetic.CosmeticAPI.readPlayerJson(playerName);
            JsonObject root;
            if (existingJson != null && !existingJson.isEmpty()) {
                root = new JsonParser().parse(existingJson).getAsJsonObject();
            } else {
                root = new JsonObject();
            }

            JsonArray arr = new JsonArray();
            for (Account acc : accounts) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", acc.getType() == Account.Type.MICROSOFT ? "microsoft" : "offline");
                obj.addProperty("username", acc.getUsername());
                obj.addProperty("uuid", acc.getUuid());
                arr.add(obj);
            }
            root.add("accounts", arr);

            String newJson = gson.toJson(root);
            io.github.angelclient.client.cosmetic.MySQLManager.getInstance().savePlayerJson(playerName, newJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String generateOfflineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static boolean isPremiumName(String username) {
        try {
            java.net.URL url = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
