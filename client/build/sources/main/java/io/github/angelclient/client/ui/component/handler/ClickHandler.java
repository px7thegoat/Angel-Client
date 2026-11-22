package io.github.angelclient.client.ui.component.handler;

import io.github.angelclient.client.ui.component.ComponentRenderInfo;

@FunctionalInterface
public interface ClickHandler {

	boolean onClick(ComponentRenderInfo info, int button);

}
