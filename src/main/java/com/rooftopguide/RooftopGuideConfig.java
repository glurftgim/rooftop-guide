package com.rooftopguide;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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

	@ConfigSection(name = "Marks of grace", description = "Marks of grace highlight", position = 2)
	String marksSection = "marks";

	@ConfigSection(name = "Session", description = "Laps and rate overlay", position = 3)
	String sessionSection = "session";

	@ConfigSection(name = "Transparent mode", description = "Hide the game and make the window see-through, leaving only the highlights", position = 4)
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
		return new Keybind(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK);
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
		description = "Also cover the game interface (inventory, chat, minimap). Turn off to darken only the game view "
			+ "and keep the interface visible",
		section = focusSection,
		position = 3
	)
	default boolean hideInterface()
	{
		return true;
	}

	@ConfigItem(
		keyName = "interfaceAfterLap",
		name = "Show interface after a lap",
		description = "When you finish the last obstacle of a course, show the game interface (for example to teleport) "
			+ "while the game stays dark and transparent. It hides again when you start the next lap",
		section = focusSection,
		position = 7
	)
	default InterfaceAfterLap interfaceAfterLap()
	{
		return InterfaceAfterLap.SEERS;
	}

	/** Which courses show the interface again after the last obstacle. */
	enum InterfaceAfterLap
	{
		NEVER("Never"),
		SEERS("Seers' Village only"),
		ALL("All courses");

		private final String label;

		InterfaceAfterLap(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "opacityUpHotkey",
		name = "More opaque hotkey",
		description = "While transparent mode is on, raises the window opacity by 5 percent",
		section = focusSection,
		position = 5
	)
	default Keybind opacityUpHotkey()
	{
		return new Keybind(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "opacityDownHotkey",
		name = "More transparent hotkey",
		description = "While transparent mode is on, lowers the window opacity by 5 percent",
		section = focusSection,
		position = 6
	)
	default Keybind opacityDownHotkey()
	{
		return new Keybind(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK);
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
		section = obstacleSection,
		position = 6
	)
	default boolean showCrossingTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLabel",
		name = "Show label",
		description = "Show a short label above the highlighted obstacles",
		section = obstacleSection,
		position = 7
	)
	default boolean showLabel()
	{
		return true;
	}

	@Range(min = 3, max = 30)
	@ConfigItem(
		keyName = "maxDistance",
		name = "Max distance",
		description = "Only consider obstacles within this many tiles of you",
		section = obstacleSection,
		position = 8
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
