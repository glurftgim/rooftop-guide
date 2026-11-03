package com.rooftopguide;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A bar while an obstacle is being crossed, filling toward how long that obstacle usually takes (averaged from earlier
 * laps). Handy on the long obstacles, to know when to look. Hold Alt to drag it, or drag its edges to resize it.
 * A mark of grace icon hangs under its left end, with a count, while any marks are lying nearby, on any roof.
 */
@Singleton
class ObstacleTimerOverlay extends Overlay
{
	private static final Dimension DEFAULT_SIZE = new Dimension(240, 16);
	private static final Dimension DEFAULT_SIZE_VERTICAL = new Dimension(16, 240);
	private static final int MAX_ARC = 14;
	private static final int MIN_ICON_HEIGHT = 16;
	private static final int MAX_ICON_HEIGHT = 32;

	private final Client client;
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;
	private final MarkOverlay markOverlay;
	private final ItemManager itemManager;

	@Inject
	private ObstacleTimerOverlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config,
		MarkOverlay markOverlay, ItemManager itemManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.markOverlay = markOverlay;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		// Alt-drag on the edges resizes it; the size is kept by RuneLite and "Reset" in its menu restores the default
		setResizable(true);
		setMinimumSize(4);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean bar = isShown();
		int marks = markCount();
		// while Alt is held an empty bar shows so there is something to grab, even between obstacles
		boolean placeholder = !bar && marks == 0 && client.isKeyPressed(KeyCode.KC_ALT);
		if (!bar && marks == 0 && !placeholder)
		{
			return null;
		}
		Dimension size = getPreferredSize() != null ? getPreferredSize()
			: config.timerDirection() == RooftopGuideConfig.TimerDirection.VERTICAL ? DEFAULT_SIZE_VERTICAL : DEFAULT_SIZE;
		// in transparent mode the FocusOverlay draws this above the cover, at this overlay's current spot; returning
		// the size either way keeps the bounds current so it can still be dragged
		if (!plugin.isFocusMode())
		{
			draw(graphics, size, bar, placeholder, marks);
		}
		return size;
	}

	/** Called by the transparent mode cover to draw this above it, where the user has put it. */
	void renderOnCover(Graphics2D graphics)
	{
		boolean bar = isShown();
		int marks = markCount();
		boolean placeholder = !bar && marks == 0 && client.isKeyPressed(KeyCode.KC_ALT);
		Rectangle bounds = getBounds();
		if ((!bar && marks == 0 && !placeholder) || bounds == null || bounds.isEmpty())
		{
			return;
		}
		graphics.translate(bounds.x, bounds.y);
		draw(graphics, bounds.getSize(), bar, placeholder, marks);
		graphics.translate(-bounds.x, -bounds.y);
	}

	private int markCount()
	{
		return config.showMarkBadge() ? markOverlay.countMarks() : 0;
	}

	// the bar shows after the XP drop too, full and green, for the moment the character is free to move on
	private boolean isShown()
	{
		return isRunning() || (config.showCrossingTimer() && plugin.isReadyForNext());
	}

	// the bar only runs for an uninterrupted click straight to the obstacle
	private boolean isRunning()
	{
		return config.showCrossingTimer() && plugin.getPendingId() != -1 && !plugin.isPendingInterrupted();
	}

	private void draw(Graphics2D graphics, Dimension size, boolean bar, boolean placeholder, int marks)
	{
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (bar || placeholder)
		{
			drawBar(graphics, size, placeholder);
		}
		if (marks > 0)
		{
			drawMarkBadge(graphics, size, marks);
		}
	}

	/**
	 * Swaps the bar's width and height when the orientation setting is switched, so a bar you have resized turns with
	 * it. Does nothing if the bar still has its default size.
	 */
	void flipSize()
	{
		Dimension size = getPreferredSize();
		if (size != null)
		{
			setPreferredSize(new Dimension(size.height, size.width));
		}
	}

	/** Draws the bar in the size given, filling to the right, or upward if the bar is set to be vertical. */
	private void drawBar(Graphics2D graphics, Dimension size, boolean placeholder)
	{
		if (config.timerDirection() != RooftopGuideConfig.TimerDirection.VERTICAL)
		{
			drawBarAcross(graphics, size, placeholder);
			return;
		}

		// A vertical bar is the same bar turned a quarter, so the fill starts at the bottom and rises. Draw it as a
		// horizontal bar as long as this one is tall, then turn it.
		AffineTransform saved = graphics.getTransform();
		graphics.translate(0, size.height);
		graphics.rotate(-Math.PI / 2);
		drawBarAcross(graphics, new Dimension(size.height, size.width), placeholder);
		graphics.setTransform(saved);
	}

	private void drawBarAcross(Graphics2D graphics, Dimension size, boolean placeholder)
	{
		int width = size.width;
		int height = size.height;
		int arc = Math.min(MAX_ARC, height);
		Color fill = config.inProgressColor();
		double fraction = 0;
		if (!placeholder && !isRunning())
		{
			// ready: the XP drop landed
			fraction = 1;
			fill = config.highlightColor();
		}
		else if (!placeholder)
		{
			long elapsed = plugin.getPendingElapsedMillis();
			Long expected = plugin.getPendingExpectedMillis();

			// the bar stays empty until this obstacle has been completed once
			fraction = expected == null ? 0 : Math.min(1.0, elapsed / (double) expected);
			if (expected != null && elapsed >= expected - config.timerWarnSeconds() * 1000L)
			{
				fill = config.highlightColor();
			}
		}

		Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, arc, arc);
		int background = Math.round(config.timerBackground() * 2.55f);

		// soft drop shadow so the bar lifts off any scenery behind it
		graphics.setColor(new Color(0, 0, 0, 70));
		graphics.fill(new RoundRectangle2D.Float(0.5f, 2f, width - 1f, height - 1f, arc, arc));

		// dark glass track, a touch lighter at the top
		graphics.setPaint(new GradientPaint(0, 0, new Color(34, 40, 52, background),
			0, height, new Color(12, 15, 20, background)));
		graphics.fill(shape);

		// the fill, clipped to the bar's shape so both ends stay rounded
		int fillWidth = (int) Math.round(width * fraction);
		if (fillWidth > 0)
		{
			Shape oldClip = graphics.getClip();
			graphics.clip(shape);

			graphics.setPaint(new GradientPaint(0, 0, shade(fill, 1.25f, 235), 0, height, shade(fill, 0.72f, 235)));
			graphics.fillRect(0, 0, fillWidth, height);

			// glossy highlight across the top half
			graphics.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 70),
				0, height / 2f, new Color(255, 255, 255, 0)));
			graphics.fillRect(0, 0, fillWidth, Math.max(1, height / 2));

			// a bright edge where the bar has got to
			if (fraction < 1)
			{
				graphics.setColor(new Color(255, 255, 255, 150));
				graphics.fillRect(Math.max(0, fillWidth - 2), 0, 2, height);
			}
			graphics.setClip(oldClip);
		}

		// crisp border in the bar's color, with a faint inner line
		graphics.setStroke(new BasicStroke(1.5f));
		graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 230));
		graphics.draw(shape);
		if (height >= 10)
		{
			graphics.setStroke(new BasicStroke(1f));
			graphics.setColor(new Color(255, 255, 255, 35));
			graphics.draw(new RoundRectangle2D.Float(2f, 2f, width - 4f, height - 4f,
				Math.max(0, arc - 3), Math.max(0, arc - 3)));
		}
	}

	/**
	 * The mark of grace icon with "x2", "x3"... beside it when there is more than one, hanging just under the bar's
	 * left end. No box around it: the icon and text scale with the bar, within limits that keep them readable.
	 */
	private void drawMarkBadge(Graphics2D graphics, Dimension size, int marks)
	{
		BufferedImage icon = itemManager.getImage(MarkOverlay.MARK_OF_GRACE);
		int iconHeight = Math.max(MIN_ICON_HEIGHT, Math.min(MAX_ICON_HEIGHT, Math.round(size.height * 0.6f)));
		// item icons are a little wider than tall; keep their proportions
		int iconWidth = icon == null ? iconHeight : Math.round(iconHeight * icon.getWidth() / (float) icon.getHeight());

		int x = 2;
		int y = size.height + 2;
		if (icon != null)
		{
			// a soft shadow under the icon so it reads over any scenery
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
			graphics.drawImage(icon, x + 1, y + 1, iconWidth, iconHeight, null);
			graphics.setComposite(AlphaComposite.SrcOver);
			graphics.drawImage(icon, x, y, iconWidth, iconHeight, null);
		}

		if (marks > 1)
		{
			graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, Math.max(11f, iconHeight * 0.6f)));
			FontMetrics metrics = graphics.getFontMetrics();
			int textX = x + iconWidth + 4;
			int baseline = y + (iconHeight + metrics.getAscent() - metrics.getDescent()) / 2;
			String text = "x" + marks;
			graphics.setColor(new Color(0, 0, 0, 210));
			graphics.drawString(text, textX + 1, baseline + 1);
			graphics.setColor(Color.WHITE);
			graphics.drawString(text, textX, baseline);
		}
	}

	/** The color lightened (factor above 1) or darkened (below 1), with the given alpha. */
	private static Color shade(Color color, float factor, int alpha)
	{
		return new Color(
			Math.min(255, Math.round(color.getRed() * factor)),
			Math.min(255, Math.round(color.getGreen() * factor)),
			Math.min(255, Math.round(color.getBlue() * factor)),
			alpha);
	}
}
