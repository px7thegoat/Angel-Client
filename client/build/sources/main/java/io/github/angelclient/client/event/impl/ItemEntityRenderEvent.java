package io.github.angelclient.client.event.impl;

import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;

public class ItemEntityRenderEvent {
	
	public boolean cancelled;
	public final EntityItem entity;
	public final double x;
	public final double y;
	public final double z;
	public final float partialTicks;
	public final IBakedModel model;
	public int result = -1;


	public ItemEntityRenderEvent(EntityItem entity, double x, double y, double z, float partialTicks, IBakedModel model) {
		this.entity = entity;
		this.x = x;
		this.y = y;
		this.z = z;
		this.partialTicks = partialTicks;
		this.model = model;
	}

}