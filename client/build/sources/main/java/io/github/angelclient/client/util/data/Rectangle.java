package io.github.angelclient.client.util.data;

import io.github.angelclient.client.util.Utils;

public class Rectangle {

	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public static final Rectangle ZERO = ofDimensions(0, 0);

	public Rectangle(float x, float y, float lineW2, float lineW) {
		this.x = (int) x;
		this.y = (int) y;
		this.width = (int) lineW2;
		this.height = (int) lineW;
	}

	public Rectangle(int x, int y) {
		this(x, y, 1, 1);
	}

	public int getX() { return x; }
	public int getY() { return y; }
	public int getWidth() { return width; }
	public int getHeight() { return height; }

	public static Rectangle ofDimensions(int width, int height) {
		return new Rectangle(0, 0, width, height);
	}

	public Rectangle offset(int x, int y) {
		return new Rectangle(this.x + x, this.y + y, width, height);
	}

	public boolean contains(Position position) {
		return contains(position.getX(), position.getY());
	}

	public boolean contains(int x, int y) {
		return x > this.x && x < this.x + width
				&& y > this.y && y < this.y + height;
	}

	public void fill(Colour colour) {
		Utils.drawRectangle(this, colour);
	}

	public void stroke(Colour colour) {
		Utils.drawOutline(this, colour);
	}

	public Rectangle multiply(float scale) {
		return multiply(scale, scale);
	}

	public Rectangle multiply(float width, float height) {
		return new Rectangle(x, y, (int) (this.width * width), (int) (this.height * height));
	}

	public float[] highPrecisionMultiply(float scale) {
		return highPrecisionMultiply(scale, scale);
	}

	public float[] highPrecisionMultiply(float width, float height) {
		return new float[] { x, y, this.width * width, this.height * height };
	}

	public int getEndX() {
		return x + width;
	}

	public int getEndY() {
		return y + height;
	}

	@Override
	public String toString() {
		return "Rectangle(x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Rectangle)) return false;
		Rectangle r = (Rectangle) o;
		return x == r.x && y == r.y && width == r.width && height == r.height;
	}

	@Override
	public int hashCode() {
		int result = x;
		result = 31 * result + y;
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}

}
