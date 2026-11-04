package com.rooftopguide;

import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayLayer;

/**
 * The cover under the interface panels. Active in transparent mode while the interface is revealed: the game stays
 * dark and see-through, but the inventory, spellbook, chat and the rest draw normally on top of it.
 */
class FocusUnderlay extends FocusCover
{
	@Inject
	private FocusUnderlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config,
		SpriteManager spriteManager, RooftopGuideOverlay obstacleOverlay, MarkOverlay markOverlay,
		ObstacleTimerOverlay timerOverlay, PredictedObstacleOverlay predictedOverlay)
	{
		super(client, plugin, config, spriteManager, obstacleOverlay, markOverlay, timerOverlay, predictedOverlay,
			OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	boolean isActive(RooftopGuidePlugin plugin)
	{
		return plugin.isCoverUnderInterface();
	}
}
