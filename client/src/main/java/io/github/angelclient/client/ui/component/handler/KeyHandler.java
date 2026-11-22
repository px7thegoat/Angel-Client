package io.github.angelclient.client.ui.component.handler;

import io.github.angelclient.client.ui.component.ComponentRenderInfo;

@FunctionalInterface
public interface KeyHandler {

	boolean keyPressed(ComponentRenderInfo info, int keyCode, char character);

}
