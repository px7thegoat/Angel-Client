package io.github.angelclient.client.coins;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Sistema de Moedas do Angel Client
 * 
 * Gerencia moedas do jogador com sincronização via API
 */
public class CoinSystem {
    
    private static CoinSystem instance;
    
    private int coins = 0;
    private boolean loading = false;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 60 * 1000; // 60 segundos
    
    // TODO: Configurar sua API
    private static final String API_URL = "https://sua-api.com";
    private static final String API_KEY =
            System.getenv("ANGEL_API_KEY") == null ? "" : System.getenv("ANGEL_API_KEY");
    
    private CoinSystem() {
    }
    
    public static CoinSystem getInstance() {
        if (instance == null) {
            instance = new CoinSystem();
        }
        return instance;
    }
    
    /**
     * Carrega moedas do servidor
     * 
     * @param username Nome do jogador
     */
    public void loadCoins(String username) {
        if (loading) return;
        
        loading = true;
        lastUpdate = System.currentTimeMillis();
        
        new Thread(() -> {
            try {
                String url = API_URL + "/coins/" + username;
                String response = fetchUrl(url);
                
                if (response != null) {
                    JsonParser parser = new JsonParser();
                    JsonObject json = parser.parse(response).getAsJsonObject();
                    
                    if (json.has("coins")) {
                        coins = json.get("coins").getAsInt();
                        System.out.println("[CoinSystem] Moedas carregadas: " + coins);
                    }
                }
            } catch (Exception e) {
                System.err.println("[CoinSystem] Erro ao carregar moedas: " + e.getMessage());
            } finally {
                loading = false;
            }
        }, "CoinSystem-Load").start();
    }
    
    /**
     * Atualiza moedas se passou tempo suficiente
     */
    public void updateIfNeeded(String username) {
        long now = System.currentTimeMillis();
        
        if (!loading && now - lastUpdate > UPDATE_INTERVAL) {
            loadCoins(username);
        }
    }
    
    /**
     * Adiciona moedas localmente (sincronizar depois)
     */
    public void addCoins(int amount) {
        this.coins += amount;
    }
    
    /**
     * Remove moedas localmente (sincronizar depois)
     */
    public boolean removeCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false; // Não tem moedas suficientes
    }
    
    /**
     * Define moedas diretamente
     */
    public void setCoins(int coins) {
        this.coins = coins;
    }
    
    /**
     * Retorna quantidade de moedas
     */
    public int getCoins() {
        return coins;
    }
    
    /**
     * Verifica se está carregando
     */
    public boolean isLoading() {
        return loading;
    }
    
    /**
     * Sincroniza moedas com servidor
     */
    public void syncCoins(String username, int coins) {
        new Thread(() -> {
            try {
                String url = API_URL + "/coins/" + username;
                
                JsonObject data = new JsonObject();
                data.addProperty("coins", coins);
                
                postJson(url, data.toString());
                
                System.out.println("[CoinSystem] Moedas sincronizadas: " + coins);
            } catch (Exception e) {
                System.err.println("[CoinSystem] Erro ao sincronizar: " + e.getMessage());
            }
        }, "CoinSystem-Sync").start();
    }
    
    /**
     * Faz requisição GET
     */
    private String fetchUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() != 200) {
                return null;
            }
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            reader.close();
            conn.disconnect();
            
            return response.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Faz requisição POST com JSON
     */
    private void postJson(String urlString, String json) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", API_KEY);
            conn.setDoOutput(true);
            
            conn.getOutputStream().write(json.getBytes());
            conn.getOutputStream().flush();
            
            conn.getResponseCode(); // Trigger request
            conn.disconnect();
        } catch (Exception e) {
            // Silencioso
        }
    }
    
    /**
     * Reseta sistema (para logout)
     */
    public void reset() {
        coins = 0;
        loading = false;
        lastUpdate = 0;
    }
}
