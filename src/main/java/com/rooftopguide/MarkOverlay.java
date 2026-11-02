package com.rooftopguide;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Marks of grace on the player's roof level: a softly pulsing tile, a light beam rising from it, and the mark's own
 * icon floating at the top of the beam, so they stand out from a distance without cluttering the screen.
 */
@Singleton
class MarkOverlay extends Overlay
{
	/** Item ID of a mark of grace, used to draw its icon. */
	static final int MARK_OF_GRACE = 11849;

	private static final int BEAM_HEIGHT = 70;
	private static final double PULSE_MILLIS = 900;

	private final Client client;
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;
	private final ItemManager itemManager;
	private final MarkReach reach;

	@Inject
	private MarkOverlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config, ItemManager itemManager,
		MarkReach reach)
	{
		this.reach = reach;
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// in transparent mode the FocusOverlay draws these above everything else
		if (!plugin.isFocusMode())
		{
			renderHighlights(graphics);
		}
		return null;
	}

	/** A mark on the player's level and, if that setting is on, one the player can walk to. */
	private boolean isVisible(Tile tile, int plane)
	{
		return tile.getPlane() == plane && tile.getLocalLocation() != null
			&& (!config.onlyLootableMarks() || reach.canReach(tile));
	}

	/** @return how many marks of grace are lying anywhere nearby, on any roof or level, counting stacks */
	int countMarks()
	{
		int count = 0;
		for (TileItem mark : plugin.getMarks().keySet())
		{
			count += Math.max(1, mark.getQuantity());
		}
		return count;
	}

	void renderHighlights(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (!config.showMarks() || player == null)
		{
			return;
		}

		int plane = player.getWorldLocation().getPlane();
		double pulse = 0.5 + 0.5 * Math.sin(2 * Math.PI * (System.currentTimeMillis() % (long) PULSE_MILLIS) / PULSE_MILLIS);
		BufferedImage icon = itemManager.getImage(MARK_OF_GRACE);
		Color color = config.markColor();

		for (Tile tile : plugin.getMarks().values())
		{
			if (!isVisible(tile, plane))
			{
				continue;
			}

			Polygon outline = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
			if (outline == null)
			{
				continue;
			}

			// pulsing glow on the tile, with a crisp edge
			graphics.setColor(withAlpha(color, (int) (50 + 90 * pulse)));
			graphics.fill(outline);
			graphics.setColor(withAlpha(color, 230));
			graphics.setStroke(new BasicStroke(2));
			graphics.draw(outline);

			Point base = Perspective.localToCanvas(client, tile.getLocalLocation(), tile.getPlane());
			if (base == null)
			{
				continue;
			}

			// light beam fading upward, then the icon bobbing at the top
			graphics.setPaint(new GradientPaint(
				base.getX(), base.getY(), withAlpha(color, 170),
				base.getX(), base.getY() - BEAM_HEIGHT, withAlpha(color, 0)));
			graphics.setStroke(new BasicStroke(3));
			graphics.drawLine(base.getX(), base.getY(), base.getX(), base.getY() - BEAM_HEIGHT);

			if (icon != null)
			{
				int bob = (int) Math.round(3 * pulse);
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.75 + 0.25 * pulse)));
				graphics.drawImage(icon,
					base.getX() - icon.getWidth() / 2, base.getY() - BEAM_HEIGHT - icon.getHeight() - bob, null);
				graphics.setComposite(AlphaComposite.SrcOver);
			}
		}
	}

	private static Color withAlpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
	}
}
