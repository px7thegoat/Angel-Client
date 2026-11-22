package io.github.angelclient.client.mixin;

import org.spongepowered.asm.mixin.Mixin;

import io.github.angelclient.client.culling.Cullable;
import net.minecraft.tileentity.TileEntity;

@Mixin(TileEntity.class)
public class MixinTileEntity implements Cullable {

	private boolean culled;

	@Override
	public void setCulled(boolean culled) { this.culled = culled; }
	@Override
	public boolean isCulled() { return culled; }

}