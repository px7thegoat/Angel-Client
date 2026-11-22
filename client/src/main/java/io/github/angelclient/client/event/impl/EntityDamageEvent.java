package io.github.angelclient.client.event.impl;

import net.minecraft.entity.Entity;

public class EntityDamageEvent {

	public final Entity entity;


	public EntityDamageEvent(Entity entity) {
		this.entity = entity;
	}

}