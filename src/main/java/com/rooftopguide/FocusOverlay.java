package com.rooftopguide;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayLayer;

/** The cover above everything, interface included. Active in transparent mode unless the interface is revealed. */
class FocusOverlay extends FocusCover
{
	@Inject
	private FocusOverlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config,
		SpriteManager spriteManager, RooftopGuideOverlay obstacleOverlay, MarkOverlay markOverlay,
		ObstacleTimerOverlay timerOverlay)
	{
		super(client, plugin, config, spriteManager, obstacleOverlay, markOverlay, timerOverlay,
			OverlayLayer.ALWAYS_ON_TOP);
	}

	@Override
	boolean isActive(RooftopGuidePlugin plugin)
	{
		return plugin.isCoverOverInterface();
	}
}
