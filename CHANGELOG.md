# Changelog

What changed in each version of Rooftop Guide, newest first.

## 2026-11-04

### Added
- **Predictable Obstacle Mode** (off by default): while you cross an obstacle, a ghost of the next obstacle shows where
  it will appear on screen once you land, so the mouse can already be there. It has its own settings section with the
  on/off checkbox, a color and a fill opacity. It also shows in transparent mode, above the dark cover.
- Landing tiles for all nine rooftop courses, and a `THIRD_PARTY_NOTICES.md` crediting burkeg's
  rooftop-obstacle-preview, whose camera method and landing tiles this uses.

### Changed
- The settings sections were renumbered to make room for the new one.

## 2026-11-03

### Added
- **Bar direction** setting for the timer bar: horizontal (the default) or vertical, which fills upward. Switching
  swaps the bar's width and height, so a resized bar turns with it.
- Preview pictures for the timer bar, an annotated screenshot with a legend, and labels on the transparent mode
  screenshot. The Plugin Hub page shows GIFs only as links, so the page uses still pictures with a link to each GIF.

### Changed
- The README is shorter: the courses list, timer bar and transparent mode stay, and the how-it-picks, marks of grace
  and rules sections are gone.
- The earlier "Vertical bar" checkbox is replaced by the dropdown. A saved choice resets to horizontal.

## 2026-11-02

### Added
- A plugin icon, and `glurftgim` as the author name.
- A first version of the vertical timer bar, as a checkbox.

## 2026-11-01

### First release on the Plugin Hub
- Highlights the next obstacle on all nine rooftop courses, plus the one being crossed and the one after it. Ardougne,
  Rellekka, Pollnivneach and Seers' Village are confirmed in game.
- **Timer bar** that fills toward your own usual time for each obstacle, learned per obstacle and per tile, turning
  green when the XP drop says you can move on. Hold Alt to move or resize it.
- **Marks of grace**: a glow and beam only when you can walk to the mark, and a count under the timer bar.
- **Session box** with time, laps, XP, marks and falls, with per-hour rates.
- **Falls** are noticed: the timer stops, no lap is counted, and the highlight returns to the course start.
- **Transparent mode**: darkens the game and makes the window see-through so a video can show through. Clicks on the
  covered interface are ignored, and the interface is shown after a Seers' Village lap so you can teleport back.

### Fixed
- Varrock's sixth obstacle is accepted even though its object sits far from where you stand.
- Pollnivneach's highlight no longer blinks out between roofs.
- The window no longer goes solid black after you drag the timer bar in transparent mode.

### Removed
- The two window-opacity hotkeys, and the "only marks I can reach" setting, which is now always on.
