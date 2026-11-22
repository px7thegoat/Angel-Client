package io.github.angelclient.client.util.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.angelclient.client.Client;
import io.github.angelclient.client.cosmetic.CosmeticAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AutoUpdater {

    private static final String RELEASES_API = System.getProperty(
        "io.github.angelclient.client.install.api",
        "https://api.github.com/repos/xistey/angel-client/releases"
    );
    private static boolean checked = false;
    private static volatile boolean updateAvailable = false;
    private static volatile String remoteVersion = null;
    private static volatile String downloadUrl = null;
    private static volatile boolean updateApplied = false;
    private static volatile String appliedVersion = null;

    public static void checkForUpdates() {
        if (checked) return;
        checked = true;

        if (Client.DEV) {
            System.out.println("[AutoUpdater] Ignorando verificacao em ambiente de desenvolvimento.");
            return;
        }

        String currentVersion = Client.VERSION;
        if (currentVersion == null || currentVersion.isEmpty() || "DEVELOPMENT TEST".equals(currentVersion)) {
            System.out.println("[AutoUpdater] Versao atual desconhecida, ignorando verificacao.");
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(RELEASES_API + "?per_page=30");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("Authorization", "token " + CosmeticAPI.GITHUB_TOKEN);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    System.out.println("[AutoUpdater] GitHub API respondeu: " + code);
                    return;
                }

                String json;
                try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    json = s.hasNext() ? s.next() : null;
                }
                if (json == null || json.isEmpty()) return;

                JsonArray releases = new JsonParser().parse(json).getAsJsonArray();

                String latestVersion = null;
                String latestDownloadUrl = null;

                for (int r = 0; r < releases.size(); r++) {
                    JsonObject release = releases.get(r).getAsJsonObject();

                    if (release.get("draft").getAsBoolean()) continue;
                    if (release.get("prerelease").getAsBoolean()) continue;

                    JsonArray assets = release.getAsJsonArray("assets");
                    if (assets == null || assets.size() == 0) continue;

                    String foundUrl = null;
                    for (int i = 0; i < assets.size(); i++) {
                        JsonObject a = assets.get(i).getAsJsonObject();
                        String assetName = a.get("name").getAsString();
                        if (assetName.startsWith("client-") && assetName.endsWith(".jar")) {
                            foundUrl = a.get("browser_download_url").getAsString();
                            break;
                        }
                    }
                    if (foundUrl == null) continue;

                    String ver = release.get("name").getAsString();
                    if (ver == null || ver.isEmpty()) continue;
                    ver = ver.replaceFirst("^[vV]", "");

                    if (latestVersion == null || isNewer(ver, latestVersion)) {
                        latestVersion = ver;
                        latestDownloadUrl = foundUrl;
                    }
                }

                if (latestVersion == null) {
                    System.out.println("[AutoUpdater] Nenhum release com client-.jar encontrado.");
                    return;
                }

                String normalizedCurrent = currentVersion.replaceFirst("^[vV]", "");

                if (latestVersion.equals(normalizedCurrent)) {
                    System.out.println("[AutoUpdater] Versao ja atualizada: " + currentVersion);
                    return;
                }

                if (!isNewer(latestVersion, normalizedCurrent)) {
                    System.out.println("[AutoUpdater] Versao local (" + currentVersion + ") e mais recente que a remota (" + latestVersion + ")");
                    return;
                }

                updateAvailable = true;
                remoteVersion = latestVersion;
                downloadUrl = latestDownloadUrl;
                System.out.println("[AutoUpdater] Nova versao disponivel: " + latestVersion + " (atual: " + currentVersion + ")");
                System.out.println("[AutoUpdater] Abra o menu do client para atualizar.");

            } catch (Exception e) {
                System.out.println("[AutoUpdater] Erro ao verificar atualizacao: " + e.getMessage());
            }
        }, "AutoUpdater").start();
    }

    private static boolean isNewer(String remote, String current) {
        if (!remote.matches("[0-9]+(\\.[0-9]+)*")) return false;
        if (!current.matches("[0-9]+(\\.[0-9]+)*")) return true;

        String[] rParts = remote.split("\\.");
        String[] cParts = current.split("\\.");
        int max = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < max; i++) {
            int r = i < rParts.length ? tryParseInt(rParts[i]) : 0;
            int c = i < cParts.length ? tryParseInt(cParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    private static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void performUpdate() {
        if (!updateAvailable || downloadUrl == null) {
            System.out.println("[AutoUpdater] Nenhuma atualizacao pendente.");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AutoUpdater] Baixando atualizacao " + remoteVersion + "...");

                File jarFile = new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());

                if (jarFile.isDirectory()) {
                    System.out.println("[AutoUpdater] Ambiente de desenvolvimento detectado (classes). Pulando substituicao.");
                    return;
                }

                File tempFile = new File(jarFile.getParent(), "client.jar.update");

                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "token " + CosmeticAPI.GITHUB_TOKEN);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);

                long totalBytes = conn.getContentLengthLong();
                long downloaded = 0;
                int lastPercent = -1;

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                        downloaded += len;
                        if (totalBytes > 0) {
                            int pct = (int) ((downloaded * 100) / totalBytes);
                            if (pct != lastPercent) {
                                lastPercent = pct;
                                System.out.println("[AutoUpdater] Download: " + pct + "%");
                            }
                        }
                    }
                }

                System.out.println("[AutoUpdater] Download concluido. Substituindo JAR...");

                File backup = new File(jarFile.getParent(), "client.jar.bak");
                if (backup.exists()) backup.delete();

                boolean replaced = false;
                if (jarFile.delete()) {
                    replaced = tempFile.renameTo(jarFile);
                }

                if (!replaced) {
                    try (FileInputStream fis = new FileInputStream(tempFile);
                         FileOutputStream fos = new FileOutputStream(jarFile)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = fis.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                        }
                    }
                    tempFile.delete();
                    replaced = jarFile.exists();
                }

                if (replaced) {
                    updateApplied = true;
                    appliedVersion = remoteVersion;
                    updateAvailable = false;
                    System.out.println("[AutoUpdater] Atualizacao para " + remoteVersion + " concluida com sucesso!");
                    System.out.println("[AutoUpdater] Reinicie o cliente para aplicar as mudancas.");
                } else {
                    System.out.println("[AutoUpdater] Falha ao substituir o JAR.");
                }

            } catch (Exception e) {
                System.out.println("[AutoUpdater] Erro ao baixar atualizacao: " + e.getMessage());
                e.printStackTrace();
            }
        }, "AutoUpdater-Download").start();
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getRemoteVersion() {
        return remoteVersion;
    }

    public static String getDownloadUrl() {
        return downloadUrl;
    }

    public static void setAppliedVersion(String version) {
        appliedVersion = version;
        updateApplied = true;
    }

    public static boolean isUpdateApplied() {
        return updateApplied;
    }

    public static String getAppliedVersion() {
        return appliedVersion;
    }
}
