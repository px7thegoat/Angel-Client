package io.github.angelclient.client.util.render;

public class GuiBatchRenderer {

    private static long lastFrame;

    public static boolean shouldRender() {

        long now = System.nanoTime();

        if (now - lastFrame < 1000000)
            return false;

        lastFrame = now;
        return true;
    }

}