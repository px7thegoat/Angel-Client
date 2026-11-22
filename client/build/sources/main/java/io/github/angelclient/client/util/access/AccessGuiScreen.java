package io.github.angelclient.client.util.access;

public interface AccessGuiScreen {

	default boolean canBeForceClosed() {
    return true;

	}
}
