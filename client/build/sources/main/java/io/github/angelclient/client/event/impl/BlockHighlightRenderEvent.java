package io.github.angelclient.client.event.impl;

import net.minecraft.util.MovingObjectPosition;

public class BlockHighlightRenderEvent {

	public final MovingObjectPosition movingObjectPosition;
	public final float partialTicks;
	public boolean cancelled;


	public BlockHighlightRenderEvent(MovingObjectPosition movingObjectPosition, float partialTicks) {
		this.movingObjectPosition = movingObjectPosition;
		this.partialTicks = partialTicks;
	}

}