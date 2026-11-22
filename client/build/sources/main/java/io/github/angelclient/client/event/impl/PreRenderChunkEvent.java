package io.github.angelclient.client.event.impl;

import net.minecraft.client.renderer.chunk.RenderChunk;

public class PreRenderChunkEvent {

	public final RenderChunk chunk;


	public PreRenderChunkEvent(RenderChunk chunk) {
		this.chunk = chunk;
	}

}