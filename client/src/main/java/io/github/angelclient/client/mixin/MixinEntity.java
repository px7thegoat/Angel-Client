package io.github.angelclient.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.github.angelclient.client.culling.Cullable;
import io.github.angelclient.client.util.access.AccessEntity;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntity implements Cullable, AccessEntity {

	private boolean culled;

	@Override
	public void setCulled(boolean culled) {
		this.culled = culled;
	}

	@Override
	public boolean isCulled() {
		return culled;
	}

	@Accessor("isInWeb")
	public abstract boolean getIsInWeb();

}