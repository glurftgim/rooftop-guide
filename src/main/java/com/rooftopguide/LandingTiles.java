package com.rooftopguide;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;

/**
 * The tile the player stands on when each rooftop obstacle's XP drops, so Predictable Obstacle Mode can work out how
 * the next obstacle will look from there. The tiles are from burkeg's rooftop-obstacle-preview (BSD 2-Clause, see
 * THIRD_PARTY_NOTICES.md) and match the tiles seen in this plugin's own laps.
 */
final class LandingTiles
{
	private static final Map<Integer, WorldPoint> LANDINGS = new HashMap<>();

	static
	{
		// Draynor
		put(3102, 3279, 3, ObjectID.ROOFTOPS_DRAYNOR_WALLCLIMB);
		put(3090, 3276, 3, ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_1);
		put(3092, 3266, 3, ObjectID.ROOFTOPS_DRAYNOR_TIGHTROPE_2);
		put(3088, 3261, 3, ObjectID.ROOFTOPS_DRAYNOR_WALLCROSSING);
		put(3088, 3255, 3, ObjectID.ROOFTOPS_DRAYNOR_WALLSCRAMBLE);
		put(3096, 3256, 3, ObjectID.ROOFTOPS_DRAYNOR_LEAPDOWN);
		put(3103, 3261, 0, ObjectID.ROOFTOPS_DRAYNOR_CRATE);

		// Al Kharid
		put(3273, 3192, 3, ObjectID.ROOFTOPS_KHARID_WALLCLIMB);
		put(3272, 3172, 3, ObjectID.ROOFTOPS_KHARID_TIGHTROPE_1);
		put(3284, 3166, 3, ObjectID.ROOFTOPS_KHARID_ROPE_SWING);
		put(3315, 3163, 1, ObjectID.ROOFTOPS_KHARID_SLIDE_SIDE);
		put(3317, 3174, 2, ObjectID.ROOFTOPS_KHARID_BAMBOO_TREE_TOP);
		put(3316, 3180, 3, ObjectID.ROOFTOPS_KHARID_WALLCLIMB_2);
		put(3302, 3186, 3, ObjectID.ROOFTOPS_KHARID_TIGHTROPE_4);
		put(3299, 3194, 0, ObjectID.ROOFTOPS_KHARID_LEAPDOWN);

		// Varrock
		put(3219, 3414, 3, ObjectID.ROOFTOPS_VARROCK_WALLCLIMB);
		put(3208, 3414, 3, ObjectID.ROOFTOPS_VARROCK_CLOTHESLINE);
		put(3197, 3416, 1, ObjectID.ROOFTOPS_VARROCK_LEAPTORUINS);
		put(3192, 3406, 3, ObjectID.ROOFTOPS_VARROCK_WALLSWING);
		put(3193, 3398, 3, ObjectID.ROOFTOPS_VARROCK_WALLSCRAMBLE);
		put(3218, 3399, 3, ObjectID.ROOFTOPS_VARROCK_LEAPTOBALCONY);
		put(3236, 3403, 3, ObjectID.ROOFTOPS_VARROCK_LEAPDOWN);
		put(3236, 3410, 3, ObjectID.ROOFTOPS_VARROCK_STEPUPROOF);
		put(3236, 3417, 0, ObjectID.ROOFTOPS_VARROCK_FINISH);

		// Canifis
		put(3506, 3492, 2, ObjectID.ROOFTOPS_CANIFIS_START_TREE);
		put(3502, 3504, 2, ObjectID.ROOFTOPS_CANIFIS_JUMP);
		put(3492, 3504, 2, ObjectID.ROOFTOPS_CANIFIS_JUMP_2);
		put(3479, 3499, 3, ObjectID.ROOFTOPS_CANIFIS_JUMP_5);
		put(3478, 3486, 2, ObjectID.ROOFTOPS_CANIFIS_JUMP_3);
		put(3489, 3476, 3, ObjectID.ROOFTOPS_CANIFIS_POLEVAULT);
		put(3510, 3476, 2, ObjectID.ROOFTOPS_CANIFIS_JUMP_4);
		put(3510, 3485, 0, ObjectID.ROOFTOPS_CANIFIS_LEAPDOWN);

		// Falador (ledge 3 has two variants that land on the same tile)
		put(3036, 3342, 3, ObjectID.ROOFTOPS_FALADOR_WALLCLIMB);
		put(3047, 3344, 3, ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_1);
		put(3050, 3357, 3, ObjectID.ROOFTOPS_FALADOR_HANDHOLDS_START);
		put(3048, 3361, 3, ObjectID.ROOFTOPS_FALADOR_GAP_1);
		put(3041, 3361, 3, ObjectID.ROOFTOPS_FALADOR_GAP_2);
		put(3028, 3354, 3, ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_2);
		put(3020, 3353, 3, ObjectID.ROOFTOPS_FALADOR_TIGHTROPE_3);
		put(3018, 3349, 3, ObjectID.ROOFTOPS_FALADOR_GAP_3);
		put(3014, 3346, 3, ObjectID.ROOFTOPS_FALADOR_LEDGE_1);
		put(3013, 3342, 3, ObjectID.ROOFTOPS_FALADOR_LEDGE_2);
		put(3015, 3334, 3, ObjectID.ROOFTOPS_FALADOR_LEDGE_3A, ObjectID.ROOFTOPS_FALADOR_LEDGE_3B);
		put(3019, 3334, 3, ObjectID.ROOFTOPS_FALADOR_LEDGE_4);
		put(3029, 3334, 0, ObjectID.ROOFTOPS_FALADOR_EDGE);

		// Seers' Village
		put(2729, 3491, 3, ObjectID.ROOFTOPS_SEERS_WALLCLIMB);
		put(2713, 3494, 2, ObjectID.ROOFTOPS_SEERS_JUMP);
		put(2710, 3480, 2, ObjectID.ROOFTOPS_SEERS_TIGHTROPE);
		put(2710, 3472, 3, ObjectID.ROOFTOPS_SEERS_JUMP_1);
		put(2702, 3465, 2, ObjectID.ROOFTOPS_SEERS_JUMP_2);
		put(2704, 3464, 0, ObjectID.ROOFTOPS_SEERS_LEAPDOWN);

		// Pollnivneach
		put(3351, 2964, 1, ObjectID.ROOFTOPS_POLLNIVNEACH_BASKET);
		put(3352, 2973, 1, ObjectID.ROOFTOPS_POLLNIVNEACH_MARKETSTALL);
		put(3360, 2977, 1, ObjectID.ROOFTOPS_POLLNIVNEACH_HANGINGBANNER);
		put(3366, 2976, 1, ObjectID.ROOFTOPS_POLLNIVNEACH_GAP);
		put(3368, 2982, 1, ObjectID.ROOFTOPS_POLLNIVNEACH_TREE);
		put(3365, 2983, 2, ObjectID.ROOFTOPS_POLLNIVNEACH_WALLCLIMB);
		put(3358, 2991, 2, ObjectID.ROOFTOPS_POLLNIVNEACH_MONKEYBARS_START);
		put(3359, 3000, 2, ObjectID.ROOFTOPS_POLLNIVNEACH_TREETOP);
		put(3363, 2998, 0, ObjectID.ROOFTOPS_POLLNIVNEACH_LINE);

		// Rellekka
		put(2625, 3676, 3, ObjectID.ROOFTOPS_RELLEKKA_WALLCLIMB);
		put(2622, 3668, 3, ObjectID.ROOFTOPS_RELLEKKA_GAP_1);
		put(2627, 3654, 3, ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_1);
		put(2639, 3653, 3, ObjectID.ROOFTOPS_RELLEKKA_GAP_2);
		put(2643, 3657, 3, ObjectID.ROOFTOPS_RELLEKKA_GAP_3);
		put(2655, 3670, 3, ObjectID.ROOFTOPS_RELLEKKA_TIGHTROPE_3);
		put(2653, 3676, 0, ObjectID.ROOFTOPS_RELLEKKA_DROPOFF);

		// Ardougne
		put(2671, 3299, 3, ObjectID.ROOFTOPS_ARDY_WALLCLIMB);
		put(2665, 3318, 3, ObjectID.ROOFTOPS_ARDY_JUMP);
		put(2656, 3318, 3, ObjectID.ROOFTOPS_ARDY_PLANK);
		put(2653, 3314, 3, ObjectID.ROOFTOPS_ARDY_JUMP_2);
		put(2651, 3309, 3, ObjectID.ROOFTOPS_ARDY_JUMP_3);
		put(2656, 3297, 3, ObjectID.ROOFTOPS_ARDY_WALLCROSSING);
		put(2668, 3297, 0, ObjectID.ROOFTOPS_ARDY_JUMP_4);
	}

	private LandingTiles()
	{
	}

	private static void put(int x, int y, int plane, int... objectIds)
	{
		for (int objectId : objectIds)
		{
			LANDINGS.put(objectId, new WorldPoint(x, y, plane));
		}
	}

	/** @return where the player stands when this obstacle's XP drops, or null if it isn't known */
	static WorldPoint of(int objectId)
	{
		return LANDINGS.get(objectId);
	}
}
