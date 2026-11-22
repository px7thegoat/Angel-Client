package io.github.angelclient.client.nametag;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * API para carregar nametags do GitHub.
 *
 * CORREÇÃO DE TRAVAMENTOS:
 *  - downloadTexture agora aceita um Runnable onReady que é chamado
 *    DEPOIS que a textura foi registrada no TextureManager (na render thread).
 *  - Isso permite que Nametag.getTexture() só retorne a ResourceLocation
 *    quando a textura realmente existir, evitando FileNotFoundException
 *    e o micro-freeze causado pelo tratamento de erro do TextureManager.
 */
public class NametagAPI {

    private static final String NAMETAGS_URL =
            io.github.angelclient.client.cosmetic.CosmeticAPI.NAMETAGS_JSON_URL;

    // ── Lista de nametags disponíveis ─────────────────────────────────────────

    public static List<Nametag> fetchNametags() {
        List<Nametag> nametags = new ArrayList<>();

        try {
            String json = fetchUrl(NAMETAGS_URL);
            if (json == null) return nametags;

            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();
            JsonArray nametagsArray = root.getAsJsonArray("nametags");

            for (int i = 0; i < nametagsArray.size(); i++) {
                try {
                    JsonObject tagObj = nametagsArray.get(i).getAsJsonObject();

                    String id = tagObj.get("id").getAsString();
                    String displayName = tagObj.has("displayName")
                            ? tagObj.get("displayName").getAsString()
                            : (tagObj.has("name") ? tagObj.get("name").getAsString() : id.toUpperCase());
                    String file = tagObj.get("file").getAsString();
                    int color = tagObj.has("color")
                            ? Integer.parseInt(tagObj.get("color").getAsString().replace("#", ""), 16)
                            : 0xFFFFFF;
                    boolean premium = tagObj.has("premium") && tagObj.get("premium").getAsBoolean();

                    nametags.add(new Nametag(id, displayName, file, color, premium));
                } catch (Exception e) {
                    System.err.println("[NametagAPI] Erro ao parsear nametag: " + e.getMessage());
                }
            }

            System.out.println("[NametagAPI] " + nametags.size() + " nametags carregadas.");
        } catch (Exception e) {
            System.err.println("[NametagAPI] Erro ao buscar nametags: " + e.getMessage());
        }

        return nametags;
    }

    // ── Download de textura ───────────────────────────────────────────────────

    /**
     * Baixa a imagem da URL em background e registra no TextureManager na render thread.
     * Chama {@code onReady} (na render thread) após o registro bem-sucedido.
     *
     * @param urlString       URL da imagem
     * @param resourceLocation ResourceLocation onde a textura será registrada
     * @param onReady         Callback chamado depois que a textura está disponível (pode ser null)
     */
    public static void downloadTexture(String urlString, ResourceLocation resourceLocation,
                                       Runnable onReady) {
        Thread t = new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "AngelClient/1.0");
                conn.setRequestProperty("Authorization", "token " + io.github.angelclient.client.cosmetic.CosmeticAPI.GITHUB_TOKEN);

                if (conn.getResponseCode() != 200) {
                    conn.disconnect();
                    System.err.println("[NametagAPI] HTTP " + conn.getResponseCode() + " ao baixar " + urlString);
                    return;
                }

                InputStream inputStream = conn.getInputStream();
                BufferedImage image = ImageIO.read(inputStream);
                inputStream.close();
                conn.disconnect();

                if (image == null) {
                    System.err.println("[NametagAPI] Imagem null após download: " + urlString);
                    return;
                }

                final BufferedImage finalImage = image;

                // Registra na render thread
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        DynamicTexture dynamicTexture = new DynamicTexture(finalImage);
                        Minecraft.getMinecraft().getTextureManager()
                                .loadTexture(resourceLocation, dynamicTexture);

                        System.out.println("[NametagAPI] Textura registrada: " + resourceLocation);

                        // Notifica que a textura está pronta
                        if (onReady != null) {
                            onReady.run();
                        }
                    } catch (Exception e) {
                        System.err.println("[NametagAPI] Erro ao registrar textura: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                System.err.println("[NametagAPI] Erro ao baixar textura " + urlString + ": " + e.getMessage());
            }
        }, "Nametag-Download-" + resourceLocation.getResourcePath());
        t.setDaemon(true);
        t.start();
    }

    /**
     * Sobrecarga sem callback para compatibilidade.
     */
    public static void downloadTexture(String urlString, ResourceLocation resourceLocation) {
        downloadTexture(urlString, resourceLocation, null);
    }

    // ── HTTP utilitário ───────────────────────────────────────────────────────

    private static String fetchUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "AngelClient/1.0");
            conn.setRequestProperty("Authorization", "token " + io.github.angelclient.client.cosmetic.CosmeticAPI.GITHUB_TOKEN);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[NametagAPI] Erro HTTP: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
