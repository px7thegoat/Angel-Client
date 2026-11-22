package io.github.angelclient.client.event.impl;

import net.minecraft.entity.Entity;

public class HitboxRenderEvent {

	public final Entity entity;
	public final double x;
	public final double y;
	public final double z;
	public final float entityYaw;
	public final float partialTicks;
	public boolean cancelled;


	public HitboxRenderEvent(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
		this.entity = entity;
		this.x = x;
		this.y = y;
		this.z = z;
		this.entityYaw = entityYaw;
		this.partialTicks = partialTicks;
	}

}