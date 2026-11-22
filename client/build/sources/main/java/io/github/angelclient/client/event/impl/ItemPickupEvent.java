package io.github.angelclient.client.event.impl;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;

public class ItemPickupEvent {

	public final EntityPlayer player;
	public final EntityItem pickedUp;


	public ItemPickupEvent(EntityPlayer player, EntityItem pickedUp) {
		this.player = player;
		this.pickedUp = pickedUp;
	}

}