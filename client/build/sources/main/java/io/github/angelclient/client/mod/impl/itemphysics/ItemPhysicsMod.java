/*
 * Original code by TheKodeToad,
 * but was modified to be more similar to CreativeMD's original mod.
 */

package io.github.angelclient.client.mod.impl.itemphysics;

import java.util.Map;
import java.util.WeakHashMap;

import com.google.gson.annotations.Expose;
import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.ItemEntityRenderEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.PrimaryIntegerSettingMod;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.util.access.AccessEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemPhysicsMod extends Mod implements PrimaryIntegerSettingMod {

	@Expose
	@Option
	@Slider(min = 0, max = 100, step = 1, format = "angel_client.slider.percent")
	private float rotationSpeed = 100;

	private final Map<EntityItem, ItemData> dataMap = new WeakHashMap<>();

	// Usado pelo MixinRenderEntityItem para aplicar as transformações
	public static float pendingRotation = 0;
	public static boolean hasPendingTransform = false;

	@Override
	public String getId() {
		return "item_physics";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.VISUAL;
	}

	@EventHandler
	public void onItemEntityRenderEvent(ItemEntityRenderEvent event) {
		// Não cancela — deixa o vanilla renderizar
		// Só calcula e armazena a rotação para o mixin aplicar
		ItemStack itemstack = event.entity.getEntityItem();
		if (itemstack.getItem() == null) {
			hasPendingTransform = false;
			return;
		}

		long now = System.nanoTime();
		ItemData data = dataMap.computeIfAbsent(event.entity, (e) -> new ItemData(System.nanoTime()));
		long since = now - data.getLastUpdate();

		if (!Minecraft.getMinecraft().isGamePaused()) {
			if (!event.entity.onGround) {
				int divisor = 2500000;
				if (((AccessEntity) event.entity).getIsInWeb()) {
					divisor *= 10;
				}
				data.setRotation(data.getRotation() + (((float) since) / ((float) divisor) * (rotationSpeed / 100F)));
			} else if (data.getRotation() != 0) {
				data.setRotation(0);
			}
		}

		data.setLastUpdate(now);

		pendingRotation = data.getRotation();
		hasPendingTransform = true;
	}

	@Override
	public void decrement() {
		rotationSpeed = Math.max(0, rotationSpeed - 10);
	}

	@Override
	public void increment() {
		rotationSpeed = Math.min(100, rotationSpeed + 10);
	}

	private int getClumpSize(int size) {
		if (size > 48) return 5;
		else if (size > 32) return 4;
		else if (size > 16) return 3;
		else if (size > 1) return 2;
		return 1;
	}
}