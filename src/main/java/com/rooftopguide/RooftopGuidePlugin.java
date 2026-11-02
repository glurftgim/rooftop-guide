package com.rooftopguide;

import com.google.inject.Provides;
import java.awt.Canvas;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.time.Instant;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

/**
 * Highlights the next rooftop obstacle. Purely visual: it never clicks or moves for the player.
 *
 * Clicking an obstacle marks it as in progress, and it stays highlighted until an agility XP gain confirms it was
 * finished. The next obstacle is then the following one in course order where that order is known, otherwise the
 * closest rooftop obstacle on the player's plane that isn't the one just finished.
 */
@PluginDescriptor(
	name = "Rooftop Guide",
	description = "Highlights the next obstacle on rooftop agility courses",
	tags = {"agility", "rooftop", "obstacle", "highlight"}
)
public class RooftopGuidePlugin extends Plugin
{
	/** Safety net so an obstacle that is never finished doesn't stay highlighted forever (about 90 seconds). */
	private static final int PENDING_TIMEOUT_TICKS = 150;

	/** Config key for the learned course-order corrections, and how many sightings make one trusted. */
	private static final String LEARNED_NEXT_KEY = "learnedNext";
	private static final int SUCCESSOR_CONFIRMATIONS = 2;

	/** Window opacity limits and step, matching the Window opacity setting's range. */
	private static final int MIN_OPACITY = 15;
	private static final int MAX_OPACITY = 95;
	private static final int OPACITY_STEP = 5;

	/** Config key (in the plugin's config group) that stores the learned obstacle times. */
	private static final String LEARNED_KEY = "learnedDurationsV3";
	private static final int SAMPLES_KEPT = 5;
	private static final long READY_HOLD_MILLIS = 3_000;

	/** Measured times outside this range are ignored: too short is a mis-detection, too long was interrupted. */
	private static final long MIN_LEARNED_MILLIS = 1_000;
	private static final long MAX_LEARNED_MILLIS = 120_000;

	/** How soon after an out-of-order click an XP gain counts as having completed it. */
	private static final long OUT_OF_ORDER_SYNC_MILLIS = 15_000;

	/** Search radius for an obstacle in a known course order; effectively the whole loaded scene. */
	private static final int ORDERED_SEARCH_RADIUS = 100;

	@Inject
	private Client client;

	@Inject
	private RooftopGuideConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RooftopGuideOverlay overlay;

	@Inject
	private MarkOverlay markOverlay;

	@Inject
	private SessionOverlay sessionOverlay;

	@Inject
	private FocusOverlay focusOverlay;

	@Inject
	private FocusUnderlay focusUnderlay;

	@Inject
	private ObstacleTimerOverlay timerOverlay;

	@Inject
	private KeyManager keyManager;

	/** Focus mode hides the game and shows only the highlights. Toggled by the hotkey. */
	private boolean focusMode;

	@Inject
	private ConfigManager configManager;

	/** Transparent mode lowers the window opacity and puts it back when toggled off. */
	private boolean transparent;
	private float opacityBeforeTransparent = 1f;

	private final HotkeyListener opacityUpListener = new HotkeyListener(() -> config.opacityUpHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			adjustOpacity(OPACITY_STEP);
		}
	};

	private final HotkeyListener opacityDownListener = new HotkeyListener(() -> config.opacityDownHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			adjustOpacity(-OPACITY_STEP);
		}
	};

	/** The last click in the game world, so focus mode can leave a gap in the cover for the game's click cross. */
	private int clickX = -1;
	private int clickY = -1;
	private long clickTimeMillis;
	private boolean clickIsInteraction;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.focusHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			setFocus(!focusMode);
		}
	};

	/**
	 * Set after the last obstacle of a course: transparent mode stays on, but its cover moves under the interface so
	 * the interface is visible (e.g. to teleport). Cleared when the next lap starts or the hotkey is pressed.
	 */
	private boolean interfaceRevealed;

	private final Set<TileObject> obstacles = new HashSet<>();

	/** Marks of grace on the ground, with the tile each lies on. */
	private final Map<TileItem, Tile> marks = new HashMap<>();

	/** Session stats. sessionStart is null until the first obstacle is clicked. */
	private Instant sessionStart;
	private int laps;
	private int xpGained;
	private int marksGained;
	private int falls;
	private int lastMarkCount = -1;

	/**
	 * The obstacle the player clicked and is crossing. pendingId and pendingWorld outlive the object itself, which is
	 * lost when the map reloads mid-course; the object is found again when it respawns. pendingId is -1 if none.
	 */
	private TileObject pending;
	private int pendingId = -1;
	private WorldPoint pendingWorld;
	private int pendingTicks;
	private int pendingStartDistance;

	/**
	 * An obstacle clicked out of course order. It isn't tracked as in progress, but if agility XP follows soon after,
	 * the player really did it and the course position is re-synced to it. -1 if none.
	 */
	private int outOfOrderId = -1;
	private long outOfOrderMillis;

	/**
	 * When the obstacle in progress was clicked, and (below) how long each obstacle takes you, learned from your laps.
	 */
	private long pendingStartMillis;
	private long finishedAtMillis;
	// the last few click-to-XP times per obstacle and starting tile, the course being the same tiles every lap
	private final Map<String, List<Long>> learnedSamples = new HashMap<>();
	private String pendingKey = "";

	/**
	 * Corrections to the course order, learned from play: the obstacle that really follows another when it differs
	 * from the list order. successorSightings counts how often a different successor was seen, since a correction
	 * is only trusted once it has been seen more than once.
	 */
	private final Map<Integer, Integer> learnedNext = new HashMap<>();
	private final Map<Long, Integer> successorSightings = new HashMap<>();

	/** True once the player clicked elsewhere in the world during the obstacle: the timer stops and isn't learned. */
	private boolean pendingInterrupted;

	/** Obstacle most recently finished, and its ID (the object can despawn). -1 if none. */
	private TileObject lastCompleted;
	private int lastCompletedId = -1;

	private int lastAgilityXp = -1;

	@Provides
	RooftopGuideConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RooftopGuideConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(markOverlay);
		overlayManager.add(sessionOverlay);
		overlayManager.add(timerOverlay);
		overlayManager.add(focusOverlay);
		overlayManager.add(focusUnderlay);
		loadLearnedDurations();
		loadLearnedNext();
		keyManager.registerKeyListener(hotkeyListener);
		keyManager.registerKeyListener(opacityUpListener);
		keyManager.registerKeyListener(opacityDownListener);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(markOverlay);
		overlayManager.remove(sessionOverlay);
		overlayManager.remove(timerOverlay);
		overlayManager.remove(focusOverlay);
		overlayManager.remove(focusUnderlay);
		keyManager.unregisterKeyListener(hotkeyListener);
		keyManager.unregisterKeyListener(opacityUpListener);
		keyManager.unregisterKeyListener(opacityDownListener);
		setTransparent(false);
		focusMode = false;
		interfaceRevealed = false;
		obstacles.clear();
		marks.clear();
		resetSession();
		lastMarkCount = -1;
		clearPending();
		lastCompleted = null;
		lastCompletedId = -1;
		lastAgilityXp = -1;
	}

	/** @return the obstacle to highlight, or null if there isn't one */
	TileObject getNextObstacle()
	{
		return chooseObstacle();
	}

	/** @return true while transparent mode is on, whichever cover is drawing */
	boolean isFocusMode()
	{
		return focusMode;
	}

	/** @return true if the cover should be drawn above everything, hiding the interface too */
	boolean isCoverOverInterface()
	{
		return focusMode && config.hideInterface() && !interfaceRevealed;
	}

	/**
	 * @return true if the cover should be drawn under the interface panels, so the interface stays visible: when the
	 * Hide interface setting is off, or after the last obstacle of a course
	 */
	boolean isCoverUnderInterface()
	{
		return focusMode && (!config.hideInterface() || interfaceRevealed);
	}

	/** Only Seers' Village needs it: its lap ends with a teleport back to the start. */
	private boolean showsInterfaceAfterLap(RooftopCourse course)
	{
		return course == RooftopCourse.SEERS && config.showInterfaceAfterSeersLap();
	}

	/** Transparent mode: covers the game so only the highlights show, and makes the window see-through. */
	private void setFocus(boolean on)
	{
		focusMode = on;
		interfaceRevealed = false;
		setTransparent(on);
	}

	/** Changes to the Window opacity setting apply straight away while transparent mode is on. */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!transparent)
		{
			return;
		}

		if (RooftopGuideConfig.GROUP.equals(event.getGroup()) && "transparentOpacity".equals(event.getKey()))
		{
			applyWindowOpacity(config.transparentOpacity() / 100f);
		}
		else if ("runelite".equals(event.getGroup()))
		{
			// Any RuneLite setting change (for example saving a dragged overlay's position) makes RuneLite put its own
			// window opacity back. Queue ours behind that so transparent mode keeps its opacity.
			Window frame = findWindow();
			if (frame != null)
			{
				SwingUtilities.invokeLater(() -> applyWindowOpacity(config.transparentOpacity() / 100f));
			}
		}
	}

	/** Raises or lowers the opacity in steps while transparent mode is on, and remembers it in the setting. */
	private void adjustOpacity(int delta)
	{
		if (!transparent)
		{
			return;
		}

		int next = Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, config.transparentOpacity() + delta));
		configManager.setConfiguration(RooftopGuideConfig.GROUP, "transparentOpacity", next);
		applyWindowOpacity(next / 100f);
	}

	/** @return the RuneLite window, found as the window the game canvas sits in, or null if there isn't one yet */
	private Window findWindow()
	{
		Canvas canvas = client.getCanvas();
		return canvas == null ? null : SwingUtilities.getWindowAncestor(canvas);
	}

	/**
	 * Sets the opacity of the RuneLite window directly. Nothing is saved in RuneLite's settings, so a crash can't
	 * leave the window see-through. It needs RuneLite's custom window chrome (an undecorated window); without it the
	 * window can't be made transparent and this does nothing.
	 */
	private void applyWindowOpacity(float opacity)
	{
		Window frame = findWindow();
		if (frame == null)
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			try
			{
				frame.setOpacity(opacity);
			}
			catch (IllegalComponentStateException | UnsupportedOperationException e)
			{
				// decorated window or unsupported platform: transparency isn't available
			}
		});
	}

	private void setTransparent(boolean on)
	{
		if (on == transparent)
		{
			return;
		}

		if (on)
		{
			Window frame = findWindow();
			opacityBeforeTransparent = frame == null ? 1f : frame.getOpacity();
			applyWindowOpacity(config.transparentOpacity() / 100f);
		}
		else
		{
			applyWindowOpacity(opacityBeforeTransparent);
		}

		transparent = on;
	}

	/** @return true if the obstacle is the one the player clicked and is crossing */
	boolean isInProgress(TileObject obstacle)
	{
		return obstacle != null && obstacle == pending;
	}

	/** @return the obstacle after the one being crossed, so it can be shown ahead of time, or null */
	TileObject getUpcomingObstacle()
	{
		Player player = client.getLocalPlayer();
		if (player == null || pending == null || !config.showUpcoming())
		{
			return null;
		}

		WorldPoint here = player.getWorldLocation();
		RooftopCourse course = RooftopCourse.forObstacle(pendingId);
		if (pending.getPlane() != here.getPlane() || course == null)
		{
			return null;
		}

		int nextId = nextAfter(course, pendingId);
		return nearest(here, obstacle -> obstacle.getId() == nextId, ORDERED_SEARCH_RADIUS);
	}

	Map<TileItem, Tile> getMarks()
	{
		return marks;
	}

	/** @return the ID of the obstacle being crossed, or -1 */
	int getPendingId()
	{
		return pendingId;
	}

	/** @return true if the obstacle in progress was interrupted by another click, so it shouldn't be timed */
	boolean isPendingInterrupted()
	{
		return pendingInterrupted;
	}

	/**
	 * @return true for a moment after the XP drop that ends an obstacle, until the next one is clicked: the tick the
	 * character is free to move on, which the timer bar shows as full and green
	 */
	boolean isReadyForNext()
	{
		return pendingId == -1 && finishedAtMillis != 0 && System.currentTimeMillis() - finishedAtMillis < READY_HOLD_MILLIS;
	}

	/** @return milliseconds since the obstacle in progress was clicked */
	long getPendingElapsedMillis()
	{
		return System.currentTimeMillis() - pendingStartMillis;
	}

	/**
	 * @return how long the obstacle in progress usually takes from the tile it was clicked from, in milliseconds. The
	 * course is the same tiles every lap, so that is the exact case; if this tile hasn't been seen, the obstacle's
	 * times from other tiles stand in. Null if the obstacle hasn't been completed yet.
	 */
	Long getPendingExpectedMillis()
	{
		List<Long> samples = learnedSamples.get(pendingKey);
		if (samples == null || samples.isEmpty())
		{
			String prefix = pendingId + "@";
			samples = new ArrayList<>();
			for (Map.Entry<String, List<Long>> entry : learnedSamples.entrySet())
			{
				if (entry.getKey().startsWith(prefix))
				{
					samples.addAll(entry.getValue());
				}
			}
		}
		return samples.isEmpty() ? null : median(samples);
	}

	/** The middle value, so a one-off slow or fast run doesn't move it. */
	private static long median(List<Long> values)
	{
		List<Long> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		return sorted.get(sorted.size() / 2);
	}

	/** Keeps this run's time among the last few for the obstacle and tile it started from. Interrupted runs are skipped. */
	private void learnDuration(long millis)
	{
		if (millis < MIN_LEARNED_MILLIS || millis > MAX_LEARNED_MILLIS)
		{
			return;
		}

		List<Long> samples = learnedSamples.computeIfAbsent(pendingKey, key -> new ArrayList<>());
		samples.add(millis);
		while (samples.size() > SAMPLES_KEPT)
		{
			samples.remove(0);
		}
		saveLearnedDurations();
	}

	/** @return the obstacle that follows this one: a learned correction if there is one, else the list order */
	private int nextAfter(RooftopCourse course, int obstacleId)
	{
		Integer learned = learnedNext.get(obstacleId);
		return learned != null ? learned : course.nextAfter(obstacleId);
	}

	/** Records that the player did "to" straight after "from". Once seen enough times it overrides the list order. */
	private void noteSuccessor(int from, int to)
	{
		RooftopCourse course = RooftopCourse.forObstacle(from);
		if (course == null || course != RooftopCourse.forObstacle(to) || from == to)
		{
			return;
		}

		long key = ((long) from << 32) | (to & 0xffffffffL);
		if (successorSightings.merge(key, 1, Integer::sum) >= SUCCESSOR_CONFIRMATIONS)
		{
			learnedNext.put(from, to);
			saveLearnedNext();
		}
	}

	private void saveLearnedNext()
	{
		StringBuilder encoded = new StringBuilder();
		learnedNext.forEach((from, to) -> encoded.append(from).append('>').append(to).append(';'));
		configManager.setConfiguration(RooftopGuideConfig.GROUP, LEARNED_NEXT_KEY, encoded.toString());
	}

	private void loadLearnedNext()
	{
		learnedNext.clear();
		String encoded = configManager.getConfiguration(RooftopGuideConfig.GROUP, LEARNED_NEXT_KEY);
		if (encoded == null)
		{
			return;
		}

		for (String entry : encoded.split(";"))
		{
			String[] parts = entry.split(">");
			if (parts.length != 2)
			{
				continue;
			}
			try
			{
				learnedNext.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
			}
			catch (NumberFormatException e)
			{
				// skip a damaged entry
			}
		}
	}

	private void saveLearnedDurations()
	{
		StringBuilder encoded = new StringBuilder();
		learnedSamples.forEach((key, samples) ->
		{
			encoded.append(key).append('=');
			samples.forEach(millis -> encoded.append(millis).append(','));
			encoded.append(';');
		});
		configManager.setConfiguration(RooftopGuideConfig.GROUP, LEARNED_KEY, encoded.toString());
	}

	private void loadLearnedDurations()
	{
		learnedSamples.clear();
		String encoded = configManager.getConfiguration(RooftopGuideConfig.GROUP, LEARNED_KEY);
		if (encoded == null)
		{
			return;
		}

		for (String entry : encoded.split(";"))
		{
			String[] parts = entry.split("=");
			if (parts.length != 2)
			{
				continue;
			}
			try
			{
				List<Long> samples = new ArrayList<>();
				for (String millis : parts[1].split(","))
				{
					if (!millis.isEmpty())
					{
						samples.add(Long.parseLong(millis));
					}
				}
				learnedSamples.put(parts[0], samples);
			}
			catch (NumberFormatException e)
			{
				// skip a damaged entry
			}
		}
	}

	Instant getSessionStart()
	{
		return sessionStart;
	}

	int getLaps()
	{
		return laps;
	}

	int getXpGained()
	{
		return xpGained;
	}

	int getMarksGained()
	{
		return marksGained;
	}

	int getFalls()
	{
		return falls;
	}

	void resetSession()
	{
		sessionStart = null;
		falls = 0;
		laps = 0;
		xpGained = 0;
		marksGained = 0;
	}

	private TileObject chooseObstacle()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		WorldPoint here = player.getWorldLocation();

		// stay on the obstacle the player clicked until the XP drop confirms it was finished
		if (pending != null)
		{
			if (pending.getPlane() == here.getPlane())
			{
				return pending;
			}
			// Jumping down off the last obstacle: the start of the course is next, so show it already. A fall is
			// handled separately (the fall message ends the obstacle).
			RooftopCourse pendingCourse = RooftopCourse.forObstacle(pendingId);
			boolean isLast = pendingCourse != null && pendingId == pendingCourse.lastObstacleId();
			if (isLast && here.getPlane() < pending.getPlane())
			{
				return startOfCourse(here);
			}
			// Mid-obstacle the player can be on another level than the obstacle (Pollnivneach's gaps between roofs):
			// keep showing it until the XP drop instead of letting it blink out
			return pending;
		}
		if (pendingId != -1)
		{
			// crossing, but the object is gone for now (map reload): show nothing rather than a wrong obstacle
			return null;
		}

		RooftopCourse course = lastCompletedId == -1 ? null : RooftopCourse.forObstacle(lastCompletedId);
		if (course != null)
		{
			int nextId = nextAfter(course, lastCompletedId);
			TileObject next = nearest(here, obstacle -> obstacle.getId() == nextId, ORDERED_SEARCH_RADIUS);
			if (next != null)
			{
				return next;
			}
		}

		// no known order, or the next obstacle isn't on this level (e.g. after a fall): the closest other one
		return nearest(here,
			obstacle -> obstacle != lastCompleted && obstacle.getId() != lastCompletedId,
			config.maxDistance());
	}

	/** The obstacle that starts the course the pending obstacle belongs to, or the closest other one if unknown. */
	private TileObject startOfCourse(WorldPoint here)
	{
		RooftopCourse course = RooftopCourse.forObstacle(pendingId);
		if (course != null)
		{
			return nearest(here, obstacle -> obstacle.getId() == course.firstObstacleId(), ORDERED_SEARCH_RADIUS);
		}
		return nearest(here, obstacle -> obstacle.getId() != pendingId, config.maxDistance());
	}

	private TileObject nearest(WorldPoint here, Predicate<TileObject> filter, int radius)
	{
		TileObject best = null;
		int bestDistance = radius + 1;
		for (TileObject obstacle : obstacles)
		{
			if (!filter.test(obstacle))
			{
				continue;
			}
			// distanceTo is Integer.MAX_VALUE across planes, which also filters other roof levels
			int distance = here.distanceTo(obstacle.getWorldLocation());
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = obstacle;
			}
		}
		return best;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (pendingId == -1 || player == null)
		{
			return;
		}

		boolean expired = ++pendingTicks > PENDING_TIMEOUT_TICKS;
		boolean leftCourse = false;
		if (pending != null)
		{
			// walking off the course ends it; an obstacle that sits far from where it's used starts out far away
			int distance = player.getWorldLocation().distanceTo(pending.getWorldLocation());
			leftCourse = distance != Integer.MAX_VALUE && distance > config.maxDistance() + pendingStartDistance;
		}

		if (leftCourse || expired)
		{
			clearPending();
		}
	}

	/**
	 * Failing an obstacle ("You slip and fall...") drops you off the roof. The obstacle wasn't finished, so its timer
	 * stops and no lap is counted; the course starts over, so the first obstacle is the next one.
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage()).toLowerCase();
		if (!message.contains("you slip and fall") && !message.startsWith("you fall"))
		{
			return;
		}

		int courseObstacleId = pendingId != -1 ? pendingId : lastCompletedId;
		RooftopCourse course = courseObstacleId == -1 ? null : RooftopCourse.forObstacle(courseObstacleId);
		if (course == null)
		{
			return;
		}

		falls++;
		clearPending();
		outOfOrderId = -1;
		// the next obstacle after the last one is the first one
		lastCompleted = null;
		lastCompletedId = course.lastObstacleId();
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();

		// with the interface covered, clicks on it (inventory, panels, orbs) do nothing, so a stray click can't hit them
		if (isCoverOverInterface() && isInterfaceAction(action))
		{
			event.consume();
			return;
		}

		boolean worldClick = recordClick(action);

		// Any other click in the world (walking, or interacting with something else) interrupts the obstacle in
		// progress. Clicking the obstacle itself again doesn't, and clicking a new one replaces it (see below).
		if (action.ordinal() < MenuAction.GAME_OBJECT_FIRST_OPTION.ordinal()
			|| action.ordinal() > MenuAction.GAME_OBJECT_FIFTH_OPTION.ordinal()
			|| RooftopCourse.forObstacle(event.getId()) == null)
		{
			if (worldClick)
			{
				interruptPending();
			}
			return;
		}

		// the clicked object is the tracked one with this ID nearest the click (scene coordinates ignore the plane)
		TileObject clicked = null;
		int bestDistance = Integer.MAX_VALUE;
		for (TileObject obstacle : obstacles)
		{
			if (obstacle.getId() != event.getId())
			{
				continue;
			}
			int dx = obstacle.getLocalLocation().getSceneX() - event.getParam0();
			int dy = obstacle.getLocalLocation().getSceneY() - event.getParam1();
			int distance = Math.max(Math.abs(dx), Math.abs(dy));
			if (distance < bestDistance)
			{
				bestDistance = distance;
				clicked = obstacle;
			}
		}

		if (clicked == null)
		{
			interruptPending();
			return;
		}

		// Spam-clicking the obstacle already in progress: the first click is the one that counts, so the timer keeps
		// running. If it was interrupted, clicking it again falls through and starts a fresh timer.
		if (pendingId != -1 && clicked.getId() == pendingId && !pendingInterrupted)
		{
			return;
		}

		// Only obstacles near the player count, so a click on a far part of the course changes nothing. The start of
		// the course is exempt, since after a fall the player may be a long way from it.
		RooftopCourse clickedCourse = RooftopCourse.forObstacle(clicked.getId());
		boolean isStart = clickedCourse != null && clicked.getId() == clickedCourse.firstObstacleId();
		// the obstacle that comes next is always fine to click, however far its object sits from the player: some
		// (like Varrock's leap to the balcony) are placed well away from where you stand to use them
		boolean isExpectedNext = clickedCourse != null && lastCompletedId != -1
			&& RooftopCourse.forObstacle(lastCompletedId) == clickedCourse
			&& clicked.getId() == nextAfter(clickedCourse, lastCompletedId);
		if (!isStart && !isExpectedNext && !isNearPlayer(clicked))
		{
			interruptPending();
			return;
		}

		if (!isInCourseOrder(clicked.getId()))
		{
			outOfOrderId = clicked.getId();
			outOfOrderMillis = System.currentTimeMillis();
			interruptPending();
			return;
		}
		outOfOrderId = -1;

		pending = clicked;
		pendingId = clicked.getId();
		pendingWorld = clicked.getWorldLocation();
		pendingTicks = 0;
		Player clicker = client.getLocalPlayer();
		int startDistance = clicker == null ? 0 : clicker.getWorldLocation().distanceTo(clicked.getWorldLocation());
		pendingStartDistance = startDistance == Integer.MAX_VALUE ? 0 : startDistance;
		pendingStartMillis = System.currentTimeMillis();
		pendingKey = startKey(clicked.getId());
		pendingInterrupted = false;
		interfaceRevealed = false;
		if (sessionStart == null)
		{
			sessionStart = Instant.now();
		}
	}

	/**
	 * @return true for a click on an interface panel or an item in it, as opposed to a click in the game world. The
	 * ITEM_* actions are deprecated in favour of CC_OP but are kept in case the client still sends them.
	 */
	@SuppressWarnings("deprecation")
	private static boolean isInterfaceAction(MenuAction action)
	{
		return action == MenuAction.CC_OP
			|| action == MenuAction.CC_OP_LOW_PRIORITY
			|| action == MenuAction.WIDGET_TARGET
			|| action == MenuAction.WIDGET_CONTINUE
			|| action == MenuAction.ITEM_USE
			|| isBetween(action, MenuAction.ITEM_FIRST_OPTION, MenuAction.ITEM_FIFTH_OPTION);
	}

	/** Identifies an obstacle plus the tile the player clicked it from, e.g. "15609@2670@3297@3". */
	private String startKey(int obstacleId)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return obstacleId + "@";
		}
		WorldPoint at = player.getWorldLocation();
		return obstacleId + "@" + at.getX() + "@" + at.getY() + "@" + at.getPlane();
	}

	/** The obstacle in progress, if any, was interrupted: its timer stops and the run isn't learned from. */
	private void interruptPending()
	{
		if (pendingId != -1)
		{
			pendingInterrupted = true;
		}
	}

	/** Remember the last world click (walking = yellow cross, interacting = red), the clicks the game draws a cross for. */
	private boolean recordClick(MenuAction action)
	{
		boolean interaction = isBetween(action, MenuAction.GAME_OBJECT_FIRST_OPTION, MenuAction.GAME_OBJECT_FIFTH_OPTION)
			|| isBetween(action, MenuAction.NPC_FIRST_OPTION, MenuAction.NPC_FIFTH_OPTION)
			|| isBetween(action, MenuAction.PLAYER_FIRST_OPTION, MenuAction.PLAYER_EIGHTH_OPTION)
			|| isBetween(action, MenuAction.GROUND_ITEM_FIRST_OPTION, MenuAction.GROUND_ITEM_FIFTH_OPTION);
		if (action != MenuAction.WALK && !interaction)
		{
			return false;
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		if (mouse != null && mouse.getX() >= 0)
		{
			clickX = mouse.getX();
			clickY = mouse.getY();
			clickTimeMillis = System.currentTimeMillis();
			clickIsInteraction = interaction;
		}
		return true;
	}

	private static boolean isBetween(MenuAction action, MenuAction first, MenuAction last)
	{
		return action.ordinal() >= first.ordinal() && action.ordinal() <= last.ordinal();
	}

	int getClickX()
	{
		return clickX;
	}

	int getClickY()
	{
		return clickY;
	}

	long getClickTimeMillis()
	{
		return clickTimeMillis;
	}

	boolean isClickInteraction()
	{
		return clickIsInteraction;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.AGILITY)
		{
			return;
		}

		int xp = event.getXp();
		int previousXp = lastAgilityXp;
		boolean gained = previousXp >= 0 && xp > previousXp;
		lastAgilityXp = xp;
		if (!gained)
		{
			return;
		}

		if (sessionStart != null)
		{
			xpGained += xp - previousXp;
		}

		if (pendingId != -1)
		{
			// a lap only counts if the last obstacle followed the one before it
			RooftopCourse course = RooftopCourse.forObstacle(pendingId);
			if (course != null && pendingId == course.lastObstacleId()
				&& lastCompletedId != -1 && nextAfter(course, lastCompletedId) == pendingId)
			{
				laps++;
			}

			// After the last obstacle, show the interface (e.g. to teleport) until the next lap starts
			if (course != null && pendingId == course.lastObstacleId() && focusMode && showsInterfaceAfterLap(course))
			{
				interfaceRevealed = true;
			}
			if (!pendingInterrupted)
			{
				learnDuration(System.currentTimeMillis() - pendingStartMillis);
			}
			lastCompleted = pending;
			lastCompletedId = pendingId;
			outOfOrderId = -1;
			finishedAtMillis = System.currentTimeMillis();
			clearPending();
		}
		else if (outOfOrderId != -1 && System.currentTimeMillis() - outOfOrderMillis < OUT_OF_ORDER_SYNC_MILLIS)
		{
			// the player really did an obstacle out of order: follow them, but don't count a lap
			noteSuccessor(lastCompletedId, outOfOrderId);
			lastCompleted = null;
			lastCompletedId = outOfOrderId;
			outOfOrderId = -1;
		}
	}

	/** @return true if the obstacle is on the player's level and within the max distance setting */
	private boolean isNearPlayer(TileObject obstacle)
	{
		Player player = client.getLocalPlayer();
		// distanceTo is Integer.MAX_VALUE across planes
		return player != null && player.getWorldLocation().distanceTo(obstacle.getWorldLocation()) <= config.maxDistance();
	}

	/**
	 * @return false if the obstacle is neither the next one in its course nor the start of the course, so it
	 * would skip ahead of the last obstacle finished. Always true when the position in the course isn't known.
	 */
	private boolean isInCourseOrder(int obstacleId)
	{
		RooftopCourse course = lastCompletedId == -1 ? null : RooftopCourse.forObstacle(lastCompletedId);
		if (course == null || RooftopCourse.forObstacle(obstacleId) != course)
		{
			return true;
		}
		return obstacleId == nextAfter(course, lastCompletedId) || obstacleId == course.firstObstacleId();
	}

	private void clearPending()
	{
		pending = null;
		pendingId = -1;
		pendingWorld = null;
		pendingInterrupted = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			// the map reloads as you move along a course; keep pendingId so the obstacle in progress isn't forgotten
			obstacles.clear();
			marks.clear();
			pending = null;
			lastCompleted = null;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			clearPending();
			lastCompletedId = -1;
			lastAgilityXp = -1;
			lastMarkCount = -1;
			setFocus(false);
		}
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		if (event.getItem().getId() == MarkOverlay.MARK_OF_GRACE)
		{
			marks.put(event.getItem(), event.getTile());
		}
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		marks.remove(event.getItem());
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		int count = event.getItemContainer().count(MarkOverlay.MARK_OF_GRACE);
		if (lastMarkCount >= 0 && count > lastMarkCount && sessionStart != null)
		{
			marksGained += count - lastMarkCount;
		}
		lastMarkCount = count;
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (event.getOverlay() == sessionOverlay && "Reset".equals(event.getEntry().getOption()))
		{
			resetSession();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		track(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		untrack(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		track(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		untrack(event.getGroundObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		track(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		untrack(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		track(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		untrack(event.getDecorativeObject());
	}

	private void track(TileObject object)
	{
		if (RooftopCourse.forObstacle(object.getId()) == null)
		{
			return;
		}

		obstacles.add(object);

		// the obstacle in progress respawned (map reload): pick it up again
		if (pending == null && pendingId == object.getId() && object.getWorldLocation().equals(pendingWorld))
		{
			pending = object;
		}
	}

	private void untrack(TileObject object)
	{
		obstacles.remove(object);
		if (object == pending)
		{
			pending = null;
		}
		if (object == lastCompleted)
		{
			lastCompleted = null;
		}
	}
}
