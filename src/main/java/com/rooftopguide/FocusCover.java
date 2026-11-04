package com.rooftopguide;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The dark cover used by transparent mode, with the highlights, timer bar and the game's click cross redrawn on top.
 * There are two of these: one above everything, which also hides the interface, and one under the interface panels,
 * used to leave the interface visible (e.g. to teleport after a lap) while the game stays dark.
 */
abstract class FocusCover extends Overlay
{
	/** The game's click animation: 4 frames, in the CROSS sprite group (yellow frames 0-3, red frames 4-7). */
	private static final int CROSS_FRAMES = 4;
	private static final int RED_FRAME_OFFSET = 4;
	private static final long CROSS_MILLIS = 450;

	private final Client client;
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;
	private final SpriteManager spriteManager;
	private final RooftopGuideOverlay obstacleOverlay;
	private final MarkOverlay markOverlay;
	private final ObstacleTimerOverlay timerOverlay;
	private final PredictedObstacleOverlay predictedOverlay;

	FocusCover(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config, SpriteManager spriteManager,
		RooftopGuideOverlay obstacleOverlay, MarkOverlay markOverlay, ObstacleTimerOverlay timerOverlay,
		PredictedObstacleOverlay predictedOverlay, OverlayLayer layer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		this.obstacleOverlay = obstacleOverlay;
		this.markOverlay = markOverlay;
		this.timerOverlay = timerOverlay;
		this.predictedOverlay = predictedOverlay;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(layer);
	}

	/** @return true if this cover is the one that should be drawing right now */
	abstract boolean isActive(RooftopGuidePlugin plugin);

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!isActive(plugin))
		{
			return null;
		}

		int alpha = Math.round(255 * config.focusDarkness() / 100f);
		graphics.setColor(new Color(0, 0, 0, alpha));
		graphics.fillRect(0, 0, client.getCanvasWidth(), client.getCanvasHeight());

		markOverlay.renderHighlights(graphics);
		obstacleOverlay.renderHighlights(graphics);
		timerOverlay.renderOnCover(graphics);
		predictedOverlay.renderGhost(graphics);
		drawClickCross(graphics);
		return null;
	}

	/** Draws the game's own click-cross frames over the cover, since the cover hides the one the game draws. */
	private void drawClickCross(Graphics2D graphics)
	{
		long age = System.currentTimeMillis() - plugin.getClickTimeMillis();
		if (plugin.getClickX() < 0 || age < 0 || age >= CROSS_MILLIS)
		{
			return;
		}

		int frame = (int) (age * CROSS_FRAMES / CROSS_MILLIS) + (plugin.isClickInteraction() ? RED_FRAME_OFFSET : 0);
		BufferedImage sprite = spriteManager.getSprite(SpriteID.CROSS, frame);
		if (sprite != null)
		{
			graphics.drawImage(sprite,
				plugin.getClickX() - sprite.getWidth() / 2, plugin.getClickY() - sprite.getHeight() / 2, null);
		}
	}
}
