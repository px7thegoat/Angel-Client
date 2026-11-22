package io.github.angelclient.client.mushstats;

public interface PlayerStatsCallback {
    void onResult(PlayerStats playerStats);

    void onError(String str, String str2);
}
