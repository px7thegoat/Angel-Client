package io.github.angelclient.client.event.impl;

import net.minecraft.item.ItemStack;

public class TransformFirstPersonItemEvent {

	public final ItemStack itemToRender;
	public final float equipProgress;
	public final float swingProgress;


	public TransformFirstPersonItemEvent(ItemStack itemToRender, float equipProgress, float swingProgress) {
		this.itemToRender = itemToRender;
		this.equipProgress = equipProgress;
		this.swingProgress = swingProgress;
	}

}