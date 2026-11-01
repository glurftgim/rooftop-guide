package com.rooftopguide;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Launches RuneLite with the plugin loaded, for development. */
public class RooftopGuidePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RooftopGuidePlugin.class);
		RuneLite.main(args);
	}
}
