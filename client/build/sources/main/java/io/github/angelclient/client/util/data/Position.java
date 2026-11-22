package io.github.angelclient.client.util.data;

public class Position {

	private final int x;
	private final int y;

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() { return x; }
	public int getY() { return y; }

	public Position offset(int x, int y) {
		return new Position(this.x + x, this.y - y);
	}

	public Rectangle rectangle(int width, int height) {
		return new Rectangle(x, y, width, height);
	}

}
