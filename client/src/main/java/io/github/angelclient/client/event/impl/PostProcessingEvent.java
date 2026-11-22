package io.github.angelclient.client.event.impl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.shader.ShaderGroup;

public class PostProcessingEvent {

	public final Type type;
	public final List<ShaderGroup> groups = new ArrayList<>();

	public enum Type {
		RENDER,
		UPDATE
	}


	public PostProcessingEvent(Type type) {
		this.type = type;
	}

}