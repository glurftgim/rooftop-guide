package com.rooftopguide;

/*
 * The camera model below (a focal point ahead of the camera that the camera follows, measured while the player and
 * camera are at rest) and the way the next obstacle's click area is shifted to where it will appear come from burkeg's
 * rooftop-obstacle-preview, BSD 2-Clause, Copyright (c) 2026, burkeg. See THIRD_PARTY_NOTICES.md.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Predictable Obstacle Mode: while an obstacle is being crossed, draws a ghost of the next obstacle's click area where
 * it will appear on screen once the player lands, so the mouse can already be there.
 *
 * The camera follows the player around a focal point. The view after landing is therefore today's camera looking at
 * the next obstacle's model moved by (focal point now minus focal point after landing).
 */
@Singleton
class PredictedObstacleOverlay extends Overlay
{
	/** The camera counts as at rest after this many frames without moving. */
	private static final int STEADY_FRAMES = 15;
	/** ...and the player must also have stood still this long, so the camera has caught up with them. */
	private static final long PLAYER_STILL_MILLIS = 1200;
	/** How far off the camera's axis the player may be for a calibration to be trusted. */
	private static final int MAX_OFF_AXIS = 16;
	/** The focal point's height above the ground grows with the log of the zoom (fitted from zoom 242 to 1604). */
	private static final double FOLLOW_PER_LOG_SCALE = 35.0;

	private final Client client;
	private final RooftopGuidePlugin plugin;
	private final RooftopGuideConfig config;

	// The focal point is distanceRatio * (600 + 3 * pitch) ahead of the camera, and FOLLOW_PER_LOG_SCALE * ln(zoom) +
	// followOffset above the player's ground. Both are measured whenever the player and camera are at rest.
	private double distanceRatio = 1.25;
	private double followOffset = -174.7;
	private double[] lastCamera = new double[3];
	private int steadyFrames;
	private boolean measured;
	private LocalPoint lastPlayerPoint;
	private long playerMovedAtMillis;

	@Inject
	private PredictedObstacleOverlay(Client client, RooftopGuidePlugin plugin, RooftopGuideConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		// above the other highlights, so the ghost is never hidden under them
		setPriority(OverlayPriority.HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// in transparent mode the cover draws this above everything else
		if (!plugin.isFocusMode())
		{
			renderGhost(graphics);
		}
		return null;
	}

	/** Draws the ghost of the next obstacle, if the mode is on and one can be worked out. */
	void renderGhost(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (!config.predictableMode() || player == null)
		{
			return;
		}

		double[] focus = followCamera(player);
		Area ghost = predictedClickbox(focus);
		if (ghost == null || ghost.isEmpty())
		{
			return;
		}

		Color color = config.predictableColor();
		int fillAlpha = Math.round(255 * config.predictableOpacity() / 100f);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
		graphics.fill(ghost);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 230));
		graphics.setStroke(new BasicStroke(2f));
		graphics.draw(ghost);
	}

	/** The next obstacle's click area as it will look after landing, or null if there is nothing to show. */
	private Area predictedClickbox(double[] focus)
	{
		int crossingId = plugin.getPendingId();
		// an obstacle that was interrupted may not be finished, so there is nothing to look ahead from
		if (crossingId == -1 || plugin.isPendingInterrupted())
		{
			return null;
		}

		WorldPoint landing = LandingTiles.of(crossingId);
		int nextId = plugin.getFollowingObstacleId();
		if (landing == null || nextId == -1)
		{
			return null;
		}

		TileObject target = null;
		int best = Integer.MAX_VALUE;
		for (TileObject object : plugin.getObstacles())
		{
			if (object.getId() != nextId)
			{
				continue;
			}
			WorldPoint at = object.getWorldLocation();
			int distance = Math.max(Math.abs(at.getX() - landing.getX()), Math.abs(at.getY() - landing.getY()));
			if (distance < best)
			{
				best = distance;
				target = object;
			}
		}

		double[] then = focalPoint(landing);
		if (target == null || then == null)
		{
			return null;
		}

		Area area = new Area();
		addClickbox(area, target, (int) Math.round(focus[0] - then[0]), (int) Math.round(focus[1] - then[1]),
			(int) Math.round(focus[2] - then[2]));
		return area;
	}

	/** Tracks the camera, measures it when it is at rest, and returns the focal point it is looking at now. */
	private double[] followCamera(Player player)
	{
		double pitch = client.getCameraFpPitch();
		double yaw = client.getCameraFpYaw();
		double[] forward = {-Math.sin(yaw) * Math.cos(pitch), Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch)};
		double[] camera = {client.getCameraFpX(), client.getCameraFpY(), client.getCameraFpZ()};
		double formulaDistance = 600 + 3 * pitch / Perspective.UNIT;

		long now = System.currentTimeMillis();
		LocalPoint here = player.getLocalLocation();
		if (here == null || !here.equals(lastPlayerPoint))
		{
			playerMovedAtMillis = now;
			lastPlayerPoint = here;
		}

		boolean atRest = cameraAtRest(camera) && now - playerMovedAtMillis >= PLAYER_STILL_MILLIS;
		if (atRest && !measured && here != null)
		{
			measureCamera(camera, forward, formulaDistance, here);
		}
		measured = atRest;

		double distance = distanceRatio * formulaDistance;
		double[] focus = new double[3];
		Arrays.setAll(focus, axis -> camera[axis] + distance * forward[axis]);
		return focus;
	}

	private boolean cameraAtRest(double[] camera)
	{
		boolean moved = false;
		for (int axis = 0; axis < 3; axis++)
		{
			moved |= Math.abs(camera[axis] - lastCamera[axis]) > 0.01;
		}
		lastCamera = camera;
		steadyFrames = moved ? 0 : steadyFrames + 1;
		return steadyFrames >= STEADY_FRAMES;
	}

	/**
	 * At rest the camera's axis passes over the player: how far along it gives the camera distance, and the height
	 * there gives the follow height.
	 */
	private void measureCamera(double[] camera, double[] forward, double formulaDistance, LocalPoint at)
	{
		double flat = forward[0] * forward[0] + forward[1] * forward[1];
		if (client.getTopLevelWorldView() == null || flat < 0.00001)
		{
			return;
		}

		int ground = groundHeight(at, client.getTopLevelWorldView().getPlane());
		double along = ((at.getX() - camera[0]) * forward[0] + (at.getY() - camera[1]) * forward[1]) / flat;
		double offAxis = Math.hypot(camera[0] + along * forward[0] - at.getX(),
			camera[1] + along * forward[1] - at.getY());
		double ratio = along / formulaDistance;
		double follow = ground - (camera[2] + along * forward[2]);
		if (offAxis <= MAX_OFF_AXIS && ratio > 0.5 && ratio < 3 && follow > 0 && follow < 300)
		{
			distanceRatio = ratio;
			followOffset += 0.5 * (follow - followHeight());
		}
	}

	/** Where the camera's focal point will be with the player standing on the tile, or null if it is off the map. */
	private double[] focalPoint(WorldPoint tile)
	{
		if (client.getTopLevelWorldView() == null)
		{
			return null;
		}
		// Not the WorldPoint overload, which gives null for a tile on another level than the player's.
		LocalPoint local = LocalPoint.fromWorld(client.getTopLevelWorldView(), tile.getX(), tile.getY());
		return local == null ? null
			: new double[]{local.getX(), local.getY(), groundHeight(local, tile.getPlane()) - followHeight()};
	}

	/**
	 * The camera follows the highest ground under the player's footprint (the centre and corners), which on a slope
	 * is above the tile's centre.
	 */
	private int groundHeight(LocalPoint point, int plane)
	{
		Player player = client.getLocalPlayer();
		int half = player == null ? 0 : player.getFootprintSize() / 2;
		int height = Perspective.getTileHeight(client, point, plane);
		for (int dx = -half; dx <= half; dx += 2 * Math.max(half, 1))
		{
			for (int dy = -half; dy <= half; dy += 2 * Math.max(half, 1))
			{
				height = Math.min(height, Perspective.getTileHeight(client, point.plus(dx, dy), plane));
			}
		}
		return height;
	}

	/** How far above the player's ground the camera's focal point is at today's zoom. */
	private double followHeight()
	{
		return FOLLOW_PER_LOG_SCALE * Math.log(client.getScale()) + followOffset;
	}

	/** Adds the object's click area as if it stood (dx, dy, dz) local units from where it is. */
	private void addClickbox(Area area, TileObject object, int dx, int dy, int dz)
	{
		int x = object.getX() + dx;
		int y = object.getY() + dy;
		int z = object.getZ() + dz;
		if (object instanceof GameObject)
		{
			GameObject gameObject = (GameObject) object;
			add(area, object, gameObject.getRenderable(), gameObject.getModelOrientation(), x, y, z);
		}
		else if (object instanceof WallObject)
		{
			add(area, object, ((WallObject) object).getRenderable1(), 0, x, y, z);
			add(area, object, ((WallObject) object).getRenderable2(), 0, x, y, z);
		}
		else if (object instanceof DecorativeObject)
		{
			DecorativeObject decoration = (DecorativeObject) object;
			add(area, object, decoration.getRenderable(), 0, x + decoration.getXOffset(), y + decoration.getYOffset(), z);
			add(area, object, decoration.getRenderable2(), 0, x, y, z);
		}
		else if (object instanceof GroundObject)
		{
			add(area, object, ((GroundObject) object).getRenderable(), 0, x, y, z);
		}
	}

	private void add(Area area, TileObject object, Renderable renderable, int orientation, int x, int y, int z)
	{
		if (renderable == null)
		{
			return;
		}
		Model model = renderable instanceof Model ? (Model) renderable : renderable.getModel();
		if (model == null)
		{
			return;
		}
		Shape clickbox = Perspective.getClickbox(client, object.getWorldView(), model, orientation, x, y, z);
		if (clickbox != null)
		{
			area.add(new Area(clickbox));
		}
	}
}
