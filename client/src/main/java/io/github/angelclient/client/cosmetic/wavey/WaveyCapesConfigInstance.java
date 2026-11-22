package io.github.angelclient.client.cosmetic.wavey;

public class WaveyCapesConfigInstance {

    private static final WaveyCapesConfig INSTANCE = new WaveyCapesConfig();

    public static WaveyCapesConfig get() {
        return INSTANCE;
    }
}
