package io.github.angelclient.client.util.video;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import static org.lwjgl.opengl.GL11.*;

public class VideoBackgroundPlayer {

    private static final String[] FFMPEG_CANDIDATES = {
        "ffmpeg",
        "/system/bin/ffmpeg",
        "/system/xbin/ffmpeg",
        "/data/data/com.movtery.zalithlauncher/files/ffmpeg",
        "/data/app/com.movtery.zalithlauncher/lib/arm64/ffmpeg",
    };

    private int texId = -1;
    private int vidW = 1280, vidH = 720;
    private Process process;
    private Thread reader;
    private ByteBuffer directBuf;
    private volatile boolean running;
    private volatile boolean hasFrame;
    private final Object lock = new Object();

    public boolean start(String filePath) {
        detectSize(filePath);
        int frameSize = vidW * vidH * 4;
        directBuf = ByteBuffer.allocateDirect(frameSize);

        texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, vidW, vidH, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        return spawnFFmpeg(filePath);
    }

    private boolean spawnFFmpeg(String path) {
        try {
            String ffmpeg = findFFmpeg();
            ProcessBuilder pb = new ProcessBuilder(
                ffmpeg,
                "-re",
                "-stream_loop", "-1",
                "-i", path,
                "-f", "rawvideo",
                "-pix_fmt", "rgba",
                "-s", vidW + "x" + vidH,
                "-"
            );
            process = pb.start();
            running = true;
            reader = new Thread(() -> readLoop(process.getInputStream()), "video-reader");
            reader.setDaemon(true);
            reader.start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void readLoop(InputStream in) {
        int frameSize = vidW * vidH * 4;
        byte[] buf = new byte[frameSize];
        int off = 0;
        try {
            while (running) {
                int n = in.read(buf, off, frameSize - off);
                if (n < 0) break;
                off += n;
                if (off >= frameSize) {
                    synchronized (lock) {
                        directBuf.clear();
                        directBuf.put(buf, 0, frameSize);
                        directBuf.flip();
                        hasFrame = true;
                    }
                    off = 0;
                }
            }
        } catch (IOException ignored) {}
    }

    public void render(int screenW, int screenH) {
        if (texId < 0) return;

        if (hasFrame) {
            synchronized (lock) {
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, vidW, vidH,
                    GL_RGBA, GL_UNSIGNED_BYTE, directBuf);
                hasFrame = false;
            }
        }

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, screenW, screenH, 0.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);

        glBindTexture(GL_TEXTURE_2D, texId);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(0, screenH, 0).tex(0, 1).endVertex();
        wr.pos(screenW, screenH, 0).tex(1, 1).endVertex();
        wr.pos(screenW, 0, 0).tex(1, 0).endVertex();
        wr.pos(0, 0, 0).tex(0, 0).endVertex();
        tess.draw();

        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    public void stop() {
        running = false;
        if (reader != null) {
            reader.interrupt();
            reader = null;
        }
        if (process != null) {
            process.destroy();
            try { process.waitFor(500, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {}
            process.destroyForcibly();
            process = null;
        }
        if (texId >= 0) {
            glDeleteTextures(texId);
            texId = -1;
        }
    }

    private String findFFmpeg() {
        for (String c : FFMPEG_CANDIDATES) {
            try {
                Process p = new ProcessBuilder(c, "-version")
                    .redirectErrorStream(true).start();
                if (p.waitFor(1, TimeUnit.SECONDS)) return c;
            } catch (Exception ignored) {}
        }
        return "ffmpeg";
    }

    private void detectSize(String path) {
        Pattern resPat = Pattern.compile("(\\d+)x(\\d+)");
        try {
            Process p = new ProcessBuilder("ffprobe", "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=p=0", path).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (p.waitFor(1, TimeUnit.SECONDS) && line != null) {
                    String[] parts = line.split(",");
                    vidW = Integer.parseInt(parts[0]);
                    vidH = Integer.parseInt(parts[1]);
                    return;
                }
            }
        } catch (Exception ignored) {}

        try {
            Process p = new ProcessBuilder(findFFmpeg(), "-i", path, "-f", "null", "-")
                .start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = resPat.matcher(line);
                    if (m.find()) {
                        vidW = Integer.parseInt(m.group(1));
                        vidH = Integer.parseInt(m.group(2));
                        return;
                    }
                }
            }
            p.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }
}
