package com.rooftopguide;

import java.util.ArrayDeque;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Constants;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Works out which tiles the player can walk to from where they stand, using the game's own collision data. A mark of
 * grace on another roof (or across a gap or fence) isn't reachable, so it can't be picked up from here.
 */
@Singleton
class MarkReach
{
	private static final int SIZE = Constants.SCENE_SIZE;
	private static final int BLOCKED = CollisionDataFlag.BLOCK_MOVEMENT_FULL
		| CollisionDataFlag.BLOCK_MOVEMENT_OBJECT
		| CollisionDataFlag.BLOCK_MOVEMENT_FLOOR
		| CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION;

	private final Client client;

	private final boolean[][] reachable = new boolean[SIZE][SIZE];
	private final ArrayDeque<int[]> queue = new ArrayDeque<>();
	// what the cached result was computed for; it is redone when the player steps, or the map or level changes
	private int cachedX = -1;
	private int cachedY = -1;
	private int cachedPlane = -1;
	private int cachedBaseX = -1;
	private int cachedBaseY = -1;

	@Inject
	private MarkReach(Client client)
	{
		this.client = client;
	}

	/** @return true if the tile is on the player's level and connected to the player's tile by walkable ground */
	boolean canReach(Tile tile)
	{
		Player player = client.getLocalPlayer();
		WorldView worldView = client.getTopLevelWorldView();
		CollisionData[] maps = worldView == null ? null : worldView.getCollisionMaps();
		int plane = worldView == null ? -1 : worldView.getPlane();
		if (player == null || maps == null || plane < 0 || tile.getPlane() != plane || maps[plane] == null)
		{
			return false;
		}

		int[][] flags = maps[plane].getFlags();
		LocalPoint here = player.getLocalLocation();
		int x = here.getSceneX();
		int y = here.getSceneY();
		if (x != cachedX || y != cachedY || plane != cachedPlane
			|| worldView.getBaseX() != cachedBaseX || worldView.getBaseY() != cachedBaseY)
		{
			flood(flags, x, y);
			cachedX = x;
			cachedY = y;
			cachedPlane = plane;
			cachedBaseX = worldView.getBaseX();
			cachedBaseY = worldView.getBaseY();
		}

		net.runelite.api.Point target = tile.getSceneLocation();
		int tx = target.getX();
		int ty = target.getY();
		return inScene(tx, ty) && (reachable[tx][ty] || touchesReachable(tx, ty));
	}

	/** The mark's own tile may be flagged blocked (it can sit against an obstacle), so a reached neighbour is enough. */
	private boolean touchesReachable(int x, int y)
	{
		for (int dx = -1; dx <= 1; dx++)
		{
			for (int dy = -1; dy <= 1; dy++)
			{
				if (inScene(x + dx, y + dy) && reachable[x + dx][y + dy])
				{
					return true;
				}
			}
		}
		return false;
	}

	private void flood(int[][] flags, int startX, int startY)
	{
		for (boolean[] column : reachable)
		{
			Arrays.fill(column, false);
		}
		queue.clear();
		if (!inScene(startX, startY))
		{
			return;
		}

		reachable[startX][startY] = true;
		queue.add(new int[]{startX, startY});
		while (!queue.isEmpty())
		{
			int[] at = queue.poll();
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int nx = at[0] + dx;
					int ny = at[1] + dy;
					if ((dx != 0 || dy != 0) && inScene(nx, ny) && !reachable[nx][ny]
						&& canStep(flags, at[0], at[1], dx, dy))
					{
						reachable[nx][ny] = true;
						queue.add(new int[]{nx, ny});
					}
				}
			}
		}
	}

	/** A step needs a walkable tile with no wall in the way; a diagonal also needs both tiles it cuts past. */
	private static boolean canStep(int[][] flags, int x, int y, int dx, int dy)
	{
		if (dx != 0 && dy != 0)
		{
			return canStep(flags, x, y, dx, 0) && canStep(flags, x, y, 0, dy)
				&& canStep(flags, x + dx, y, 0, dy) && canStep(flags, x, y + dy, dx, 0);
		}

		int nx = x + dx;
		int ny = y + dy;
		if (!inScene(nx, ny) || (flags[nx][ny] & BLOCKED) != 0)
		{
			return false;
		}
		int leaving = dy > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_NORTH
			: dy < 0 ? CollisionDataFlag.BLOCK_MOVEMENT_SOUTH
			: dx > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_EAST : CollisionDataFlag.BLOCK_MOVEMENT_WEST;
		int entering = dy > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_SOUTH
			: dy < 0 ? CollisionDataFlag.BLOCK_MOVEMENT_NORTH
			: dx > 0 ? CollisionDataFlag.BLOCK_MOVEMENT_WEST : CollisionDataFlag.BLOCK_MOVEMENT_EAST;
		return (flags[x][y] & leaving) == 0 && (flags[nx][ny] & entering) == 0;
	}

	private static boolean inScene(int x, int y)
	{
		return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
	}
}
