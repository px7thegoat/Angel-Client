package io.github.angelclient.client.event.impl;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;

public class ScoreboardRenderEvent {

	public final ScoreObjective objective;
	public final ScaledResolution scaledRes;
	public boolean cancelled;


	public ScoreboardRenderEvent(ScoreObjective objective, ScaledResolution scaledRes) {
		this.objective = objective;
		this.scaledRes = scaledRes;
	}

}