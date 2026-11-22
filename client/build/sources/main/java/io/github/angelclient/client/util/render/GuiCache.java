package io.github.angelclient.client.util.render;

import java.util.HashMap;
import java.util.Map;

public class GuiCache {

    private static final Map<String, Integer> cache = new HashMap<>();

    public static boolean has(String id) {
        return cache.containsKey(id);
    }

    public static void put(String id, int value) {
        cache.put(id, value);
    }

    public static int get(String id) {
        return cache.getOrDefault(id, 0);
    }

}