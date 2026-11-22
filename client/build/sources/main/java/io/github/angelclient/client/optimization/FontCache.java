package io.github.angelclient.client.optimization;

import java.util.HashMap;
import java.util.Map;

public class FontCache {

    private static final Map<String,Integer> widthCache = new HashMap<>();

    public static int getWidth(String text, net.minecraft.client.gui.FontRenderer fr){

        if(widthCache.containsKey(text))
            return widthCache.get(text);

        int w = fr.getStringWidth(text);

        widthCache.put(text,w);

        return w;
    }

}