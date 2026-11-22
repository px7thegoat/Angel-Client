package io.github.angelclient.client.event.impl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;

public class PlayerSleepEvent {

	public final EntityPlayer entityPlayer;
	public final BlockPos pos;


	public PlayerSleepEvent(EntityPlayer entityPlayer, BlockPos pos) {
		this.entityPlayer = entityPlayer;
		this.pos = pos;
	}

}