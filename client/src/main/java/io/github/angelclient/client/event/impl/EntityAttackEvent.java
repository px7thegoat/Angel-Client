package io.github.angelclient.client.event.impl;

import net.minecraft.entity.Entity;

public class EntityAttackEvent {

	public final Entity victim;


	public EntityAttackEvent(Entity victim) {
		this.victim = victim;
	}

}