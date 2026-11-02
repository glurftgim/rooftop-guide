package com.rooftopguide;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Laps, XP and marks of grace for the current session. Starts on the first obstacle clicked.
 * The numbers are recalculated on an interval, not every frame, so the rates settle instead of jumping around.
 */
class SessionOverlay extends OverlayPanel
{
	/** Rates are shown only after this long, since a short session gives wild numbers. */
	private static final long MIN_SECONDS_FOR_RATES = 60;

	/** How often the numbers are recalculated. */
	private static final long UPDATE_SECONDS = 1;

	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;

	/** The text last calculated, and when and for which session. Redrawn each frame, recalculated on an interval. */
	private Instant cachedSession;
	private Instant cachedAt;
	private String timeText;
	private String lapsText;
	private String xpText;
	private String marksText;

	@Inject
	private SessionOverlay(RooftopGuidePlugin plugin, RooftopGuideConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset", "Rooftop session"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Instant start = plugin.getSessionStart();
		if (!config.showSession() || start == null)
		{
			cachedSession = null;
			return null;
		}

		Instant now = Instant.now();
		boolean newSession = !start.equals(cachedSession);
		boolean due = cachedAt == null || Duration.between(cachedAt, now).getSeconds() >= UPDATE_SECONDS;
		if (newSession || due)
		{
			recalculate(start, now);
		}

		panelComponent.getChildren().add(TitleComponent.builder().text("Rooftop agility").build());
		line("Time", timeText);
		line("Laps", lapsText);
		line("XP", xpText);
		line("Marks", marksText);
		if (plugin.getFalls() > 0)
		{
			line("Falls", Integer.toString(plugin.getFalls()));
		}

		return super.render(graphics);
	}

	private void recalculate(Instant start, Instant now)
	{
		Duration elapsed = Duration.between(start, now);
		boolean showRates = elapsed.getSeconds() >= MIN_SECONDS_FOR_RATES;
		double hours = elapsed.toMillis() / 3_600_000.0;

		timeText = formatDuration(elapsed);
		lapsText = plugin.getLaps() + rate(plugin.getLaps(), hours, showRates);
		xpText = String.format("%,d", plugin.getXpGained()) + rate(plugin.getXpGained(), hours, showRates);
		marksText = plugin.getMarksGained() + rate(plugin.getMarksGained(), hours, showRates);

		cachedSession = start;
		cachedAt = now;
	}

	private void line(String left, String right)
	{
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(right).build());
	}

	private static String rate(int amount, double hours, boolean showRates)
	{
		return showRates ? String.format(" (%,d/hr)", Math.round(amount / hours)) : "";
	}

	private static String formatDuration(Duration duration)
	{
		long seconds = duration.getSeconds();
		return String.format("%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
	}
}
