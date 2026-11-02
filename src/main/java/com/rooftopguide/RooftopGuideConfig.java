package com.rooftopguide;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(RooftopGuideConfig.GROUP)
public interface RooftopGuideConfig extends Config
{
	String GROUP = "rooftopguide";

	@ConfigSection(name = "Obstacles", description = "How obstacles are highlighted", position = 1)
	String obstacleSection = "obstacles";

	@ConfigSection(name = "Timer bar", description = "The crossing timer bar. Hold Alt and drag it to move it", position = 2)
	String timerSection = "timer";

	@ConfigSection(name = "Marks of grace", description = "Marks of grace highlight", position = 3)
	String marksSection = "marks";

	@ConfigSection(name = "Session", description = "Laps and rate overlay", position = 4)
	String sessionSection = "session";

	@ConfigSection(name = "Transparent mode", description = "Hide the game and make the window see-through, leaving only the highlights", position = 5)
	String focusSection = "focus";

	@ConfigItem(
		keyName = "focusHotkey",
		name = "Hotkey",
		description = "Turns transparent mode on and off: hides the game and makes the window see-through, leaving only the highlights",
		section = focusSection,
		position = 1
	)
	default Keybind focusHotkey()
	{
		return Keybind.NOT_SET;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "focusDarkness",
		name = "Darkness",
		description = "100 is solid black. Lower values leave the game faintly visible underneath",
		section = focusSection,
		position = 2
	)
	default int focusDarkness()
	{
		return 100;
	}

	@Range(min = 15, max = 95)
	@ConfigItem(
		keyName = "transparentOpacity",
		name = "Window opacity",
		description = "How opaque the window is while transparent mode is on, in percent. Lower is more see-through. "
			+ "Changes apply right away while transparent mode is on",
		section = focusSection,
		position = 4
	)
	default int transparentOpacity()
	{
		return 40;
	}

	@ConfigItem(
		keyName = "hideInterface",
		name = "Hide interface",
		description = "Also cover the game interface (inventory, chat, minimap), and ignore clicks on it so they can't be "
			+ "hit by accident. Off darkens only the game view and leaves the interface visible and clickable",
		section = focusSection,
		position = 3
	)
	default boolean hideInterface()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInterfaceAfterSeersLap",
		name = "Show interface after Seers' lap",
		description = "When you finish the last obstacle of the Seers' Village course, show the game interface so you "
			+ "can teleport back, while the game stays dark and transparent. It hides again when you start the next lap",
		section = focusSection,
		position = 7
	)
	default boolean showInterfaceAfterSeersLap()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Next obstacle color",
		description = "Color of the obstacle to do next, before you click it",
		section = obstacleSection,
		position = 1
	)
	default Color highlightColor()
	{
		return new Color(0, 255, 120, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "inProgressColor",
		name = "In progress color",
		description = "Color of the obstacle you clicked and are crossing",
		section = obstacleSection,
		position = 2
	)
	default Color inProgressColor()
	{
		return new Color(255, 170, 0, 255);
	}

	@ConfigItem(
		keyName = "showInProgress",
		name = "Show in-progress obstacle",
		description = "Highlight the obstacle you clicked while you cross it. Turn off to hide it",
		section = obstacleSection,
		position = 3
	)
	default boolean showInProgress()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showUpcoming",
		name = "Show upcoming obstacle",
		description = "While crossing an obstacle, also highlight the one after it",
		section = obstacleSection,
		position = 4
	)
	default boolean showUpcoming()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "upcomingColor",
		name = "Upcoming color",
		description = "Color of the obstacle after the one you are crossing",
		section = obstacleSection,
		position = 5
	)
	default Color upcomingColor()
	{
		return new Color(0, 200, 255, 255);
	}

	@ConfigItem(
		keyName = "showCrossingTimer",
		name = "Show timer bar",
		description = "Show a bar at the top of the screen while crossing an obstacle, filling toward how long "
			+ "it usually takes you. Turn off to hide it",
		section = timerSection,
		position = 1
	)
	default boolean showCrossingTimer()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "timerBackground",
		name = "Background opacity",
		description = "How dark the empty part of the bar is, in percent. 0 shows only the fill and outline",
		section = timerSection,
		position = 4
	)
	default int timerBackground()
	{
		return 67;
	}

	@Range(min = 0, max = 10)
	@ConfigItem(
		keyName = "timerWarnSeconds",
		name = "Warn early (seconds)",
		description = "The bar changes color this many seconds before the obstacle usually ends, so you can look back "
			+ "in time. 0 changes it exactly at the usual time",
		section = timerSection,
		position = 5
	)
	default int timerWarnSeconds()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "showLabel",
		name = "Show label",
		description = "Show a short label above the highlighted obstacles",
		section = obstacleSection,
		position = 6
	)
	default boolean showLabel()
	{
		return false;
	}

	@Range(min = 3, max = 30)
	@ConfigItem(
		keyName = "maxDistance",
		name = "Max distance",
		description = "Only consider obstacles within this many tiles of you",
		section = obstacleSection,
		position = 7
	)
	default int maxDistance()
	{
		return 15;
	}

	@ConfigItem(
		keyName = "showMarks",
		name = "Highlight marks of grace",
		description = "Outline marks of grace lying on the roof",
		section = marksSection,
		position = 1
	)
	default boolean showMarks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMarkBadge",
		name = "Show count by timer bar",
		description = "Show a mark of grace icon under the timer bar whenever a mark is lying nearby, on any roof, "
			+ "with x2, x3... for several",
		section = marksSection,
		position = 4
	)
	default boolean showMarkBadge()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "markColor",
		name = "Mark color",
		description = "Color of the marks of grace outline",
		section = marksSection,
		position = 2
	)
	default Color markColor()
	{
		return new Color(255, 230, 0, 255);
	}

	@ConfigItem(
		keyName = "showSession",
		name = "Show session overlay",
		description = "Show laps, XP and marks per hour. Right-click the box to reset it",
		section = sessionSection,
		position = 1
	)
	default boolean showSession()
	{
		return true;
	}
}
