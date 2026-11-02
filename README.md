# Rooftop Guide (RuneLite plugin)

Highlights the next obstacle on the nine rooftop agility courses (Draynor, Al Kharid, Varrock, Canifis, Falador, Seers,
Pollnivneach, Rellekka, Ardougne). It only draws an overlay; it never clicks or moves for you. Transparent mode and timer bar allow easy "afk" while doing other tasks on the PC.

## How it picks the obstacle

The next one in the course's order after the one you just finished (detected by an agility XP gain after clicking it).
When your position in the course isn't known yet, it falls back to the nearest obstacle on your current level.
Turn off the built-in Agility plugin's obstacle highlighting to avoid two overlapping highlights.

## Features

All rooftop courses. Ardougne, Rellekka, Pollnivneach and Seers' Village are confirmed in game; the others
start from RuneLite's list order and correct themselves after the real order is seen twice.

- **Obstacle highlights:** the next obstacle in one color, the obstacle you're crossing in another, and the one after it
  in a third.
- **Course order:** an obstacle clicked out of order isn't tracked and no lap is counted for it; the highlight stays on
  the obstacle you should do next. A click also has to be on an obstacle near you (the Max distance setting), except
  the start of the course and the obstacle that is next in order, so clicking a far part of the course changes nothing.
- **Falls:** failing an obstacle ("You slip and fall...") stops its timer, counts no lap, and sends the highlight back
  to the start of the course. The session box shows how many falls you've had.
- **Crossing timer:** a bar that fills from your first click on an obstacle toward how long it usually takes you, and
  turns to the "next obstacle" color once that time has passed (the Warn early setting shifts that earlier). When the
  agility XP drop arrives, the moment you're free to move on, it jumps to full and stays for a few seconds. Times are
  learned per obstacle and per tile you clicked from (the course is the same tiles every lap) as the middle of your
  last five runs, and are remembered between sessions. The bar is empty for an obstacle you haven't finished yet. Only
  the first click counts, so spam-clicking doesn't reset it. Clicking anywhere else interrupts it: the bar disappears
  and that run isn't learned from. Clicking the obstacle again starts a fresh timer.
- **Moving and resizing the bar:** hold Alt and drag it, or drag its edges to resize it. It can be made very thin.
  Right-click it with Alt held and choose Reset to put it back.
- **Marks of grace:** a pulsing glow on the tile, a light beam rising from it, and the mark's icon floating on top. By
  default only marks you can walk to from where you stand are shown, so a mark on another roof stays hidden until it
  can be picked up.
- **Mark count:** the mark of grace icon appears under the left end of the timer bar whenever a mark is lying nearby,
  on any roof, with x2, x3... for several.
- **Session box:** time, laps, XP, marks of grace and falls, with per-hour rates, refreshed every second. Starts on your
  first click; right-click it to reset. Hold Alt to move it.
- **Transparent mode** (hotkey, none by default: set one in the plugin's settings): darkens the game so only the
  highlights show, and lowers the window opacity so a video can show through. The game's own click cross sprites (yellow walking, red interacting) are drawn
  over the cover. The Darkness setting goes from solid black down to a light dim. Window opacity changes apply live
  while it's on, and two optional hotkeys (unset by default) raise or lower it in steps of 5. The "Hide interface"
  setting (on by default) also covers the interface, and while it is covered, clicks on the inventory and interface
  panels do nothing, so they can't be hit by accident; clicks in the game world still work. That changes what a click
  does, which makes it the part of this mode closest to Jagex's rules about menu actions. Turn Hide interface off to
  darken only the game view and keep the interface visible and clickable. The window opacity is set on the window
  directly and nothing is saved in RuneLite's settings, so it can't stay stuck after a crash. It needs RuneLite's custom
  window chrome on; use it with Always on top. After the last obstacle of the Seers' Village course, the interface is
  shown and clickable so you can teleport back while the game stays dark and transparent, and it hides again when the
  next lap starts. The "Show interface after Seers' lap" setting (on by default) turns that off. Pressing the hotkey
  yourself ends it early.

## Config

- **Obstacles:** colors for next / in progress / upcoming, on/off for the in-progress and upcoming highlights, label,
  max distance.
- **Timer bar:** on/off, background opacity, how many seconds before the usual time it changes color.
- **Marks of grace:** highlight on/off, only marks you can reach, count by the timer bar, color.
- **Session:** the session box on/off.
- **Transparent mode:** hotkey, darkness, window opacity and its hotkeys, hide interface, show interface after a Seers' lap.

## Jagex rules

This plugin only draws overlays and changes how the window looks. It does not click, move, or press keys for you, add
menu entries, change menu options, move or resize click zones, unhide interface parts, or show anything about boss
mechanics, other players, or loot. As I read Jagex's third-party client guidelines, none of their listed prohibitions
apply, but that list says it isn't exhaustive, so the final call is Jagex's and the Plugin Hub reviewers'.
