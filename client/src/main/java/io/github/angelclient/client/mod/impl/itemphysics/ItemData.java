package io.github.angelclient.client.mod.impl.itemphysics;


public class ItemData {

	private long lastUpdate;
	private float rotation;

	public ItemData(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public long getLastUpdate() { return lastUpdate; }

	public float getRotation() { return rotation; }

	public void setLastUpdate(long lastUpdate) { this.lastUpdate=lastUpdate; }

	public void setRotation(float rotation) { this.rotation=rotation; }

}