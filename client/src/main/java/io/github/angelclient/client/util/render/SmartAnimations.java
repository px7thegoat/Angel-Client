package io.github.angelclient.client.util.render;

import java.util.HashSet;
import java.util.Set;

public class SmartAnimations {

    private static final Set<String> rendered = new HashSet<>();

    public static void reset() {
        rendered.clear();
    }

    public static boolean isRendered(String texture) {
        return rendered.contains(texture);
    }

    public static void markRendered(String texture) {
        rendered.add(texture);
    }

}