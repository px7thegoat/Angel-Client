package io.github.angelclient.client.ui.component.impl;


public enum ButtonType {
	SMALL(20),
	NORMAL(100),
	LARGE(200);

	private final int width;

	ButtonType(int width) { this.width = width; }
	public int getWidth() { return width; }

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

}
