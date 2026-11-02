package com.rooftopguide;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.gameval.ObjectID;

/**
 * Obstacle object IDs for each rooftop course, in route order. The IDs mirror the groups in RuneLite's own agility
 * plugin. Ardougne's order is confirmed in game; for the others it is a starting guess that the plugin corrects
 * itself when the real next obstacle differs (see RooftopGuidePlugin's learned successors).
 */
enum RooftopCourse
{
	DRAYNOR("Draynor",
		ObjectID.ROOFTOPS_DRAYNOR_WALLCLIMB, ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_1, ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_2,
		ObjectID.ROOFTOPS_DRAYNOR_WALLCROSSING, ObjectID.ROOFTOPS_DRAYNOR_WALLSCRAMBLE, ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN,
		ObjectID.ROOFTOPS_DRAYNOR_CRATE),
	AL_KHARID("Al Kharid",
		ObjectID.ROOFTOPS_KHARID_WALLCLIMB, ObjectID.ROOFTOPS_KHARID_TIGHTROPE_1, ObjectID.ROOFTOPS_KHARID_ROPE_SWING,
		ObjectID.ROOFTOPS_KHARID_SLIDE_SIDE, ObjectID.ROOFTOPS_KHARID_BAMBOO_TREE_TOP, ObjectID.ROOFTOPS_KHARID_WALLCLIMB_2,
		ObjectID.ROOFTOPS_KHARID_TIGHTROPE_4, ObjectID.ROOFTOPS_KHARID_LEAPDOWN),
	VARROCK("Varrock",
		ObjectID.ROOFTOPS_VARROCK_WALLCLIMB, ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE, ObjectID.ROOFTOPS_VARROCK_LEAPTORUINS,
		ObjectID.ROOFTOPS_VARROCK_WALLSWING, ObjectID.ROOFTOPS_VARROCK_WALLSCRAMBLE, ObjectID.ROOFTOPS_VARROCK_LEAPTOBALCONY,
		ObjectID.ROOFTOPS_VARROCK_LEAPDOWN, ObjectID.ROOFTOPS_VARROCK_STEPUPROOF, ObjectID.ROOFTOPS_VARROCK_FINISH),
	CANIFIS("Canifis",
		ObjectID.ROOFTOPS_CANIFIS_START_TREE, ObjectID.ROOFTOPS_CANIFIS_JUMP, ObjectID.ROOFTOPS_CANIFIS_JUMP_2,
		ObjectID.ROOFTOPS_CANIFIS_JUMP_5, ObjectID.ROOFTOPS_CANIFIS_JUMP_3, ObjectID.ROOFTOPS_CANIFIS_POLEVAULT,
		ObjectID.ROOFTOPS_CANIFIS_JUMP_4, ObjectID.ROOFTOPS_CANIFIS_LEAPDOWN),
	FALADOR("Falador",
		ObjectID.ROOFTOPS_FALADOR_WALLCLIMB, ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_1, ObjectID.ROOFTOPS_FALADOR_HANDHOLDS_START,
		ObjectID.ROOFTOPS_FALADOR_GAP_1, ObjectID.ROOFTOPS_FALADOR_GAP_2, ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_2,
		ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_3, ObjectID.ROOFTOPS_FALADOR_GAP_3, ObjectID.ROOFTOPS_FALADOR_LEDGE_1,
		ObjectID.ROOFTOPS_FALADOR_LEDGE_2, ObjectID.ROOFTOPS_FALADOR_LEDGE_3A, ObjectID.ROOFTOPS_FALADOR_LEDGE_3B,
		ObjectID.ROOFTOPS_FALADOR_LEDGE_4, ObjectID.ROOFTOPS_FALADOR_EDGE),
	SEERS("Seers' Village",
		ObjectID.ROOFTOPS_SEERS_WALLCLIMB, ObjectID.ROOFTOPS_SEERS_JUMP, ObjectID.ROOFTOPS_SEERS_TIGHTROPE,
		ObjectID.ROOFTOPS_SEERS_JUMP_1, ObjectID.ROOFTOPS_SEERS_JUMP_2, ObjectID.ROOFTOPS_SEERS_LEAPDOWN),
	POLLNIVNEACH("Pollnivneach",
		ObjectID.ROOFTOPS_POLLNIVNEACH_BASKET, ObjectID.ROOFTOPS_POLLNIVNEACH_MARKETSTALL, ObjectID.ROOFTOPS_POLLNIVNEACH_HANGINGBANNER,
		ObjectID.ROOFTOPS_POLLNIVNEACH_GAP, ObjectID.ROOFTOPS_POLLNIVNEACH_TREE, ObjectID.ROOFTOPS_POLLNIVNEACH_WALLCLIMB,
		ObjectID.ROOFTOPS_POLLNIVNEACH_MONKEYBARS_START, ObjectID.ROOFTOPS_POLLNIVNEACH_TREETOP, ObjectID.ROOFTOPS_POLLNIVNEACH_LINE),
	RELLEKKA("Rellekka",
		ObjectID.ROOFTOPS_RELLEKKA_WALLCLIMB, ObjectID.ROOFTOPS_RELLEKKA_GAP_1, ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_1,
		ObjectID.ROOFTOPS_RELLEKKA_GAP_2, ObjectID.ROOFTOPS_RELLEKKA_GAP_3, ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_3,
		ObjectID.ROOFTOPS_RELLEKKA_DROPOFF),
	ARDOUGNE("Ardougne",
		ObjectID.ROOFTOPS_ARDY_WALLCLIMB, ObjectID.ROOFTOPS_ARDY_JUMP, ObjectID.ROOFTOPS_ARDY_PLANK,
		ObjectID.ROOFTOPS_ARDY_JUMP_2, ObjectID.ROOFTOPS_ARDY_JUMP_3, ObjectID.ROOFTOPS_ARDY_WALLCROSSING,
		ObjectID.ROOFTOPS_ARDY_JUMP_4);

	private static final Map<Integer, RooftopCourse> BY_OBSTACLE_ID = new HashMap<>();

	static
	{
		for (RooftopCourse course : values())
		{
			for (int id : course.obstacleIds)
			{
				BY_OBSTACLE_ID.put(id, course);
			}
		}
	}

	private final String displayName;
	private final int[] obstacleIds;

	RooftopCourse(String displayName, int... obstacleIds)
	{
		this.displayName = displayName;
		this.obstacleIds = obstacleIds;
	}

	String getDisplayName()
	{
		return displayName;
	}

	/** @return the ID of the obstacle that ends a lap */
	int lastObstacleId()
	{
		return obstacleIds[obstacleIds.length - 1];
	}

	/** @return the ID of the obstacle that starts the course */
	int firstObstacleId()
	{
		return obstacleIds[0];
	}

	/** @return the ID of the obstacle after the given one, wrapping from the last obstacle back to the first */
	int nextAfter(int objectId)
	{
		for (int i = 0; i < obstacleIds.length; i++)
		{
			if (obstacleIds[i] == objectId)
			{
				return obstacleIds[(i + 1) % obstacleIds.length];
			}
		}
		return -1;
	}

	/** @return the rooftop course this object belongs to, or null if it is not a rooftop obstacle */
	static RooftopCourse forObstacle(int objectId)
	{
		return BY_OBSTACLE_ID.get(objectId);
	}
}
