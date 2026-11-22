package io.github.angelclient.client.mod.impl.hud;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;

import io.github.angelclient.client.event.EventHandler;
import io.github.angelclient.client.event.impl.PreGameOverlayRenderEvent;
import io.github.angelclient.client.event.impl.ScoreboardRenderEvent;
import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.mod.ModCategory;
import io.github.angelclient.client.mod.annotation.Option;
import io.github.angelclient.client.mod.annotation.Slider;
import io.github.angelclient.client.mod.hud.HudMod;
import io.github.angelclient.client.mod.hud.SimpleHudMod;
import io.github.angelclient.client.util.Utils;
import io.github.angelclient.client.util.data.Colour;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import io.github.angelclient.client.mod.hud.HudPosition;
import io.github.angelclient.client.util.data.Position;
import io.github.angelclient.client.util.data.Rectangle;

public class ScoreboardMod extends HudMod {

	public static ScoreboardMod instance;
	public static boolean enabled;

	private int lastWidth = 100;
	private int lastHeight = 100;
	@Expose
	@Option
	public boolean hide;
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY)
	public boolean background = true;
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY, applyToAllClass = Option.BACKGROUND_COLOUR_CLASS)
	public Colour backgroundColour = new Colour(1342177280);
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY)
	public boolean border = false;
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY, applyToAllClass = Option.BORDER_COLOUR_CLASS)
	public Colour borderColour = Colour.BLACK;
	@Expose
	@Option
	public Colour backgroundColourTop = new Colour(1610612736);
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY)
	public Colour textColour = Colour.WHITE;
	@Expose
	@Option(translationKey = SimpleHudMod.TRANSLATION_KEY)
	public boolean shadow = true;
	@Expose
	@Option
	public boolean numbers = true;
	@Expose
	@Option
	public Colour numbersColour = new Colour(-43691);

	@Override
	public String getId() {
		return "scoreboard";
	}

	@Override
	public ModCategory getCategory() {
		return ModCategory.HUD;
	}

	@Override
	public void onRegister() {
		super.onRegister();
		instance = this;
	}

	@Override
	protected void onEnable() {
		super.onEnable();
		enabled = true;
	}

	@Override
	protected void onDisable() {
		super.onDisable();
		enabled = false;
	}

	@EventHandler
	public void onScoreboardRender(ScoreboardRenderEvent event) {
		if (isEnabled() && !hide) {
			event.cancelled = true;
		}
	}

	@Override
	public void render(Position position, boolean editMode) {
		if(hide) {
			return;
		}

		Scoreboard scoreboard = null;
		net.minecraft.scoreboard.ScoreObjective objective = null;

		if (mc.theWorld != null) {
			scoreboard = mc.theWorld.getScoreboard();
			ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(mc.thePlayer.getName());
			if (scoreplayerteam != null) {
				int i1 = scoreplayerteam.getChatFormat().getColorIndex();
				if (i1 >= 0) {
					objective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
				}
			}
			if (objective == null) {
				objective = scoreboard.getObjectiveInDisplaySlot(1);
			}
		}

		if (objective == null && editMode) {
			scoreboard = new Scoreboard();
			objective = scoreboard.addScoreObjective("dummy", net.minecraft.scoreboard.IScoreObjectiveCriteria.DUMMY);
			objective.setDisplayName("Angel Client");
			scoreboard.getValueFromObjective("Player 1", objective).setScorePoints(3);
			scoreboard.getValueFromObjective("Player 2", objective).setScorePoints(2);
			scoreboard.getValueFromObjective("Player 3", objective).setScorePoints(1);
		}

		if (objective == null) return;

		Collection<Score> scores = scoreboard.getSortedScores(objective);
		List<Score> filteredScores = Lists.newArrayList(Iterables.filter(scores,
				p_apply_1_ -> p_apply_1_.getPlayerName() != null && !p_apply_1_.getPlayerName().startsWith("#")));
		Collections.reverse(filteredScores);

		if(filteredScores.size() > 15) {
			scores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - 15));
		}
		else {
			scores = filteredScores;
		}

		int i = mc.fontRendererObj.getStringWidth(objective.getDisplayName());

		for(Score score : scores) {
			ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
			String s = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
			if(numbers) {
				 s +=  ": " + EnumChatFormatting.RED + score.getScorePoints();
			}
			i = Math.max(i, mc.fontRendererObj.getStringWidth(s));
		}

		int scoresHeight = (scores.size() + 1) * mc.fontRendererObj.FONT_HEIGHT + 1;

		this.lastWidth = i + 4;
		this.lastHeight = scoresHeight + 2;

		GlStateManager.pushMatrix();
		GlStateManager.translate(position.getX(), position.getY(), 0);
		float sScale = getScale();
		GlStateManager.scale(sScale, sScale, sScale);

		int l1 = 2; // Offset for left side
		int l = l1 + i; // Right side

		int j = 0;

		for(Score score1 : scores) {
			++j;
			ScorePlayerTeam scoreplayerteam1 = scoreboard.getPlayersTeam(score1.getPlayerName());
			String s1 = ScorePlayerTeam.formatPlayerName(scoreplayerteam1, score1.getPlayerName());
			String s2 = "" + score1.getScorePoints();
			int k = (j * mc.fontRendererObj.FONT_HEIGHT) + 1;

			if(background) {
				Gui.drawRect(l1 - 2, k, l, k + mc.fontRendererObj.FONT_HEIGHT, backgroundColour.getValue());
			}

			mc.fontRendererObj.drawString(s1, l1, k, textColour.getValue(), shadow);

			if(numbers) {
				mc.fontRendererObj.drawString(s2, l - mc.fontRendererObj.getStringWidth(s2) - (border ? 1 : 0), k,
						numbersColour.getValue(), shadow);
			}

			if(j == scores.size()) {
				String s3 = objective.getDisplayName();
				if (background) {
					Gui.drawRect(l1 - 2, 0, l, mc.fontRendererObj.FONT_HEIGHT,
							backgroundColourTop.getValue());
					Gui.drawRect(l1 - 2, mc.fontRendererObj.FONT_HEIGHT, l, mc.fontRendererObj.FONT_HEIGHT + 1, backgroundColour.getValue());
				}
				mc.fontRendererObj.drawString(s3, l1 + i / 2 - mc.fontRendererObj.getStringWidth(s3) / 2,
						1, textColour.getValue(), shadow);
			}
		}

		if(border) {
			Utils.drawOutline(l1 - 3, 0, l + 1, scoresHeight + 1, borderColour.getValue());
		}

		GlStateManager.popMatrix();
	}

	@Override
	public Rectangle getBounds(Position position) {
		float sScale = getScale();
		return new Rectangle(position.getX(), position.getY(), (int)(lastWidth * sScale), (int)(lastHeight * sScale));
	}

	@Override
	public HudPosition getDefaultPosition() {
		return new HudPosition(0.8f, 0.35f);
	}

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

}
