package io.github.angelclient.client.mod.hud;


import io.github.angelclient.client.util.data.Position;
import io.github.angelclient.client.util.data.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

public abstract class BaseHudElement implements HudElement {

	public abstract HudPosition getHudPosition();

	public abstract void setHudPosition(HudPosition position);

	public abstract float getHudScale();

	@Override
	public Position getPosition() {
		return getHudPosition().toAbsolute();
	}

	@Override
	public Position getDividedPosition() {
		return new Position((int) (getPosition().getX() / getHudScale()), (int) (getPosition().getY() / getHudScale()));
	}

	@Override
	public HudPosition getDefaultPosition() {
		return new HudPosition(0, 0);
	}

	@Override
	public void setPosition(Position position) {
		setHudPosition(HudPosition.fromAbsolute(position));
	}

	@Override
	public Rectangle getBounds() {
		return getBounds(getPosition());
	}

	@Override
	public float[] getHighPrecisionMultipliedBounds() {
		Rectangle rectangle = getBounds();

		if(rectangle == null) {
			return null;
		}

		return rectangle.highPrecisionMultiply(getHudScale());
	}

	@Override
	public Rectangle getMultipliedBounds() {
		Rectangle rectangle = getBounds();

		if(rectangle == null) {
			return null;
		}

		return rectangle.multiply(getHudScale());
	}

	@Override
	public void render(boolean editMode) {
		if(!isVisible()) return;

		if(!editMode && !isShownInReplay()) {
			return;
		}

		GlStateManager.pushMatrix();
		GlStateManager.scale(getHudScale(), getHudScale(), getHudScale());
		render(getDividedPosition(), editMode);
		GlStateManager.popMatrix();
	}

	@Override
	public boolean isSelected(int mouseX, int mouseY) {
		Rectangle bounds = getMultipliedBounds();
		return bounds != null && bounds.contains(mouseX, mouseY);
	}

}