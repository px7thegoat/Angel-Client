package io.github.angelclient.client.mod.impl.hud.timers;

import net.minecraft.item.ItemStack;

/**
 * TODO: Move to ClientApi.
 */
public class Timer {

	private final String name;
	private final ItemStack renderItem;
	private long id;
	private long time;

	public Timer(String name, ItemStack renderItem, long startTime) {
		this(name, renderItem);
		time = startTime;
	}

	public void tick() {
		if(time > 0) {
			time--;
		}
	}

	public Timer(String name, ItemStack renderItem) { this.name=name; this.renderItem=renderItem; }

	public String getName() { return name; }

	public net.minecraft.item.ItemStack getRenderItem() { return renderItem; }

	public long getTime() { return time; }

	public long getId() { return id; }

	public void setTime(long time) { this.time=time; }

	public void setId(long id) { this.id=id; }

}