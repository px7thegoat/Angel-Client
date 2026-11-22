package io.github.angelclient.client.event.impl;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;

public class RenderChunkPositionEvent {

	public final RenderChunk chunk;
	public final BlockPos position;


	public RenderChunkPositionEvent(RenderChunk chunk, BlockPos position) {
		this.chunk = chunk;
		this.position = position;
	}

}