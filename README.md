# Rooftop Guide (RuneLite plugin)

Highlights the next obstacle on the nine rooftop agility courses (Draynor, Al Kharid, Varrock, Canifis, Falador, Seers,
Pollnivneach, Rellekka, Ardougne). It only draws an overlay; it never clicks or moves for you.

## Setup

1. Install a JDK 11+ (e.g. Temurin 17) and IntelliJ IDEA Community.
2. Open this folder in IntelliJ as a Gradle project. The Gradle wrapper (`gradlew`) is included.
3. Run `RooftopGuidePluginTest.main` (src/test) to launch RuneLite with the
   plugin loaded. Use a run configuration with the program argument `--developer-mode` and the VM option `-ea`.
4. Log in with RuneLite's Jagex auth panel in the sidebar, then enable Rooftop Guide in RuneLite's settings.

To build the jar: `./gradlew build` (output in `build/libs`).

## How it picks the obstacle

The next one in the course's order after the one you just finished (detected by an agility XP gain after clicking it).
When your position in the course isn't known yet, it falls back to the nearest obstacle on your current level.
Turn off the built-in Agility plugin's obstacle highlighting to avoid two overlapping highlights.

## Features

All rooftop courses; Ardougne's order is confirmed in game, the others start from RuneLite's list order and correct
themselves after the real order is seen twice.

- Next obstacle in one color, the obstacle you're crossing in another, and the one after it in a third.
- Crossing timer: a bar at the top of the screen while you cross an obstacle, filling toward how long that obstacle
  usually takes you (learned from your own laps and remembered between sessions), and changing color once that time
  has passed. It also shows in transparent mode. Only the first click counts, so spam-clicking the obstacle doesn't
  reset it. Clicking anywhere else interrupts it: the bar disappears and that run isn't learned from. Clicking the
  obstacle again after an interruption starts a fresh timer.
- Marks of grace: a pulsing glow on the tile, a light beam rising from it, and the mark's icon floating on top.
- Session box: time, laps, XP and marks of grace with per-hour rates. Starts on your first click; right-click it to reset.
- Course order: an obstacle clicked out of order isn't tracked and no lap is counted for it; the highlight
  stays on the obstacle you should do next. A click also has to be on an obstacle near you (the Max distance setting),
  except the start of the course, so clicking a far part of the course changes nothing.
- Transparent mode (hotkey, default Ctrl+H): darkens the game so only the highlights show, and lowers the window
  opacity so a video can show through. The game's own click cross sprites (yellow walking, red interacting) are drawn
  over the cover. The Darkness setting goes from solid black down to a light dim. Window opacity changes apply live
  while it's on, and Ctrl+= / Ctrl+- raise or lower it in steps of 5. The "Hide interface" setting (on by default)
  also covers the interface; turn it off to darken only the game view and keep the interface visible. The window
  opacity is set on the window directly and nothing is saved in RuneLite's settings, so it can't stay stuck after a
  crash. It needs RuneLite's custom window chrome on; use it with Always on top. Covered interface parts can still be
  clicked.
  After the last obstacle of a course, the interface is shown (for example to teleport) while the game stays dark and
  transparent, and it hides again when the next lap starts. The "Show interface after a lap" setting picks Seers'
  Village only (default), all courses, or never. Pressing the hotkey yourself ends it early.

## Config

Transparent mode hotkey, darkness and window opacity, colors for next / in progress / upcoming, on/off for the
in-progress highlight, the upcoming highlight and the timer bar, label, max distance, marks highlight and color,
session box.

## Jagex rules

This plugin only draws overlays and changes how the window looks. It does not click, move, or press keys for you, add
menu entries, change menu options, move or resize click zones, unhide interface parts, or show anything about boss
mechanics, other players, or loot. As I read Jagex's third-party client guidelines, none of their listed prohibitions
apply, but that list says it isn't exhaustive, so the final call is Jagex's and the Plugin Hub reviewers'.
