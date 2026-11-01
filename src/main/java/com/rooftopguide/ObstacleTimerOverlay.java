package com.rooftopguide;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A bar at the top of the screen while an obstacle is being crossed, filling toward how long that obstacle usually
 * takes (averaged from earlier laps). Handy on the long obstacles, to know when to look.
 */
@Singleton
class ObstacleTimerOverlay extends Overlay
{
	private static final int BAR_WIDTH = 240;
	private static final int BAR_HEIGHT = 16;
	private static final int TOP_MARGIN = 40;

	private final Client client;
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;

	@Inject
	private ObstacleTimerOverlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// in transparent mode the FocusOverlay draws this above the cover
		if (!plugin.isFocusMode())
		{
			renderTimer(graphics);
		}
		return null;
	}

	void renderTimer(Graphics2D graphics)
	{
		int obstacleId = plugin.getPendingId();
		// the bar only runs for an uninterrupted click straight to the obstacle
		if (!config.showCrossingTimer() || obstacleId == -1 || plugin.isPendingInterrupted())
		{
			return;
		}

		long elapsed = plugin.getPendingElapsedMillis();
		Long expected = plugin.getExpectedMillis(obstacleId);

		// the bar stays empty until this obstacle's usual time has been learned from a completed lap
		double fraction = expected == null ? 0 : Math.min(1.0, elapsed / (double) expected);
		Color fill = expected != null && elapsed >= expected ? config.highlightColor() : config.inProgressColor();

		int x = (client.getCanvasWidth() - BAR_WIDTH) / 2;
		int y = TOP_MARGIN;

		graphics.setColor(new Color(0, 0, 0, 170));
		graphics.fillRoundRect(x, y, BAR_WIDTH, BAR_HEIGHT, 8, 8);

		graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 200));
		graphics.fillRoundRect(x, y, (int) Math.round(BAR_WIDTH * fraction), BAR_HEIGHT, 8, 8);

		graphics.setColor(fill);
		graphics.setStroke(new BasicStroke(1));
		graphics.drawRoundRect(x, y, BAR_WIDTH, BAR_HEIGHT, 8, 8);
	}
}
