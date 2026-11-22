package io.github.angelclient.client.mod.impl.replay.fix;

import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PreTickEvent;

public class TickListener {
	private final Runnable runnable;
	public TickListener(Runnable runnable) { this.runnable = runnable; }
	@EventHandler
	public void onTick(PreTickEvent event) { runnable.run(); }
}