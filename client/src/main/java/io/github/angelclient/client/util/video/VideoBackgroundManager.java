package io.github.angelclient.client.util.video;

import java.io.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class VideoBackgroundManager {

    private static VideoBackgroundPlayer player;

    public static void start() {
        stop();

        String path = findVideo();
        if (path == null) {
            path = extractAsset("angel_client", "background.mp4");
        }
        if (path == null) {
            path = extractAsset("angel_client", "videos/background.mp4");
        }
        if (path == null) {
            path = extractAsset("angel_client", "textures/angelclient/background.mp4");
        }
        if (path == null) return;

        player = new VideoBackgroundPlayer();
        if (!player.start(path)) {
            player = null;
        }
    }

    public static void stop() {
        if (player != null) {
            player.stop();
            player = null;
        }
    }

    public static void render(int width, int height) {
        if (player != null) {
            player.render(width, height);
        }
    }

    public static boolean isPlaying() {
        return player != null;
    }

    private static String findVideo() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.mcDataDir == null) return null;
        File mcDir = mc.mcDataDir;

        String[] candidates = {
            new File(mcDir, "background.mp4").getAbsolutePath(),
            new File(mcDir, "angelclient/background.mp4").getAbsolutePath(),
            new File(mcDir, "AngelClient/background.mp4").getAbsolutePath(),
            "background.mp4",
        };

        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    private static String extractAsset(String domain, String path) {
        try {
            ResourceLocation loc = new ResourceLocation(domain, path);
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(loc);
            if (resource == null) return null;

            String suffix = path.contains(".") ? path.substring(path.lastIndexOf('.')) : ".mp4";
            File temp = File.createTempFile("bg_", suffix);
            temp.deleteOnExit();

            try (InputStream in = resource.getInputStream();
                 FileOutputStream out = new FileOutputStream(temp)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }

            return temp.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
