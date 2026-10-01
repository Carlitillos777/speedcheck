# SpeedCheck

A [Scarpet](https://github.com/gnembon/fabric-carpet) app for measuring how fast a contraption processes items.

## Info

SpeedCheck does two things:

- **Counts item sets.** Select an area and it counts every item inside containers (chests, barrels, hoppers, droppers, furnaces, placed shulker boxes...). Shulker boxes are opened and their contents counted, but the boxes themselves are not. Each count is saved under a name, split into 64-stackables, 16-stackables and unstackables.
- **Times contraptions.** Starts the game with `/tick sprint`, counts game ticks until a redstone signal block of your choice switches into the state you chose, then freezes the game and reports the speed in items/hour and as a multiple of hopper speed (1x = 9000 items/h).

Timing uses game ticks, so the result doesn't depend on how fast your computer sprints.

## How to install

Requires Carpet mod. Tested on Minecraft 1.21.4 and 1.21.11.

1. `/carpet scriptsAppStore Carlitillos777/speedcheck/contents/programs`
2. `/script download speedcheck.sc`
3. `/carpet setDefault commandScriptACE 3`

Step 3 is needed because `/tick` requires permission level 3 and Carpet runs app commands at the level set by `commandScriptACE` (default 2). Without it the app can't unfreeze or sprint the game, and it tells you so.

You can also download [`programs/speedcheck.sc`](programs/speedcheck.sc) and place it in `<world>/scripts/`, then run `/script load speedcheck`.

## Commands

- `/speedcheck save <from> <to> <name>` - Counts the items in containers between `<from>` and `<to>` and saves the count as `<name>`
- `/speedcheck list` - Lists the saved sets and their totals
- `/speedcheck info <name>` - Shows the full breakdown of a set
- `/speedcheck delete <name>` - Deletes a set
- `/speedcheck test <pos> <name> <on|off>` - Unfreezes and sprints the game, then ends the test when the block at `<pos>` switches into `on` (powered) or `off` (unpowered). Speed is calculated using the item total of set `<name>`
- `/speedcheck stop` - Aborts the running test and freezes the game

## Usage

1. Fill the input of your contraption, e.g. a double chest of shulker boxes, and freeze the game with `/tick freeze`.
2. Save the input: `/speedcheck save ~ ~ ~ ~1 ~ ~ Test_Set_1`
   ```
   Test_Set_1: 93312 items (64x: 93312 | 16x: 0 | 1x: 0)
     dirt 93312
   ```
3. Start the test, pointing at the block that powers when the contraption is done: `/speedcheck test <x y z> Test_Set_1 on`
   ```
   Test_Set_1 finished
     Speed: 64745 items/h (7.19x hopper)
     Items: 93312
     Game time: 103768 gt | 5188.40 s | 1.4412 h
     Real time: 412.36 s
   ```

The test only ends on a switch **into** the chosen state. Switching away from it is ignored, so an "is working" signal that starts ON, turns OFF while processing and turns ON again when done works with `on`.

## Notes

- Saved sets are stored per world in `<world>/scripts/speedcheck.data/sets.json`.
- A set is a snapshot of the item count. Refill the contraption with the same items before each test, or save the set again.
- Keep the contraption loaded during the test (stay nearby or use `/forceload`), otherwise ticks are counted while nothing runs.
- If the signal never reaches the chosen state, the test runs until `/speedcheck stop`.
- Only block containers are counted. Minecarts, dropped items and bundle contents are not.

## Changelog

### V1
- Initial release
