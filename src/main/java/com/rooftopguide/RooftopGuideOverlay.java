package com.rooftopguide;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
class RooftopGuideOverlay extends Overlay
{
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;

	@Inject
	private RooftopGuideOverlay(RooftopGuidePlugin plugin, RooftopGuideConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// in focus mode the FocusOverlay draws these above everything else
		if (!plugin.isFocusMode())
		{
			renderHighlights(graphics);
		}
		return null;
	}

	void renderHighlights(Graphics2D graphics)
	{
		TileObject target = plugin.getNextObstacle();
		boolean inProgress = plugin.isInProgress(target);
		if (target != null && (!inProgress || config.showInProgress()))
		{
			draw(graphics, target,
				inProgress ? config.inProgressColor() : config.highlightColor(),
				inProgress ? "in progress" : "next obstacle");
		}

		TileObject upcoming = plugin.getUpcomingObstacle();
		if (upcoming != null && upcoming != target)
		{
			draw(graphics, upcoming, config.upcomingColor(), "upcoming");
		}
	}

	private void draw(Graphics2D graphics, TileObject obstacle, Color color, String text)
	{
		Shape clickbox = obstacle.getClickbox();
		if (clickbox != null)
		{
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(2));
			graphics.draw(clickbox);

			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
			graphics.fill(clickbox);
			graphics.setComposite(AlphaComposite.SrcOver);
		}

		if (config.showLabel())
		{
			RooftopCourse course = RooftopCourse.forObstacle(obstacle.getId());
			String label = (course == null ? "" : course.getDisplayName() + ": ") + text;
			Point textLocation = obstacle.getCanvasTextLocation(graphics, label, 0);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, label, color);
			}
		}
	}
}
