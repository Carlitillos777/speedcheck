# SpeedCheck

A [Scarpet](https://github.com/gnembon/fabric-carpet) app for measuring how fast a contraption processes items.

## Info

SpeedCheck does three things:

- **Counts item sets.** Select an area and it counts every item inside containers (chests, barrels, hoppers, droppers, furnaces, placed shulker boxes...). Shulker boxes are opened and their contents counted, but the boxes themselves are not. Each count is saved under a name, split into 64-stackables, 16-stackables and unstackables.
- **Times contraptions.** Starts the game with `/tick sprint`, counts game ticks until a redstone signal block of your choice switches into the state you chose, then freezes the game and reports the speed in items/hour and as a multiple of hopper speed (1x = 9000 items/h).

- **Checks output packing.** Scans an area of chests holding single-item shulker boxes and verifies every item uses the minimum number of boxes with at most one partial box, e.g. 3 boxes worth of cobblestone must come out as 3 boxes, not 4.

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
- `/speedcheck info <name>` - Shows a set's totals and the count of every item in it
- `/speedcheck delete <name>` - Deletes a set
- `/speedcheck test <pos> <name> <on|off>` - Unfreezes and sprints the game, then ends the test when the block at `<pos>` switches into `on` (powered) or `off` (unpowered). Speed is calculated using the item total of set `<name>`
- `/speedcheck stop` - Aborts the running test and freezes the game
- `/speedcheck check <from> <to>` - Checks that the shulker boxes in containers between `<from>` and `<to>` are ideally packed: per item, boxes = ceil(total / box capacity) and at most one partial box. The percentage is the share of boxes that match the ideal output (full boxes plus at most one partial per item). Mixed and empty boxes count as failures, loose items are listed and ignored

## Usage

1. Fill the input of your contraption, e.g. a double chest of shulker boxes, and freeze the game with `/tick freeze`.
2. Save the input: `/speedcheck save ~ ~ ~ ~1 ~ ~ Test_Set_1`
   ```
   Test_Set_1: 93312 items, 1 types (64x: 93312 | 16x: 0 | 1x: 0)
   ```
3. Start the test, pointing at the block that powers when the contraption is done: `/speedcheck test <x y z> Test_Set_1 on`
   ```
   Test_Set_1 finished
     Speed: 64745 items/h (7.19x hopper)
     Items: 93312
     Game time: 103768 gt | 5188.40 s | 86.47 min | 1.4412 h
     Real time: 412.36 s
   ```

4. Check the output boxes: `/speedcheck check <from> <to>`
   ```
   Output check: FAIL - 2 types, 8 boxes (3 partial), ideal 5 (1 partial), 50.0%
     cobblestone: 4 boxes (2 partial), ideal 3 (0 partial), 50.0%
     1 mixed boxes
     1 empty boxes
     loose items (ignored): stone 5
   ```

The test only ends on a switch **into** the chosen state. Switching away from it is ignored, so an "is working" signal that starts ON, turns OFF while processing and turns ON again when done works with `on`.

## Notes

- Saved sets are stored per world in `<world>/scripts/speedcheck.data/sets.json`.
- A set is a snapshot of the item count. Refill the contraption with the same items before each test, or save the set again.
- Keep the contraption loaded during the test (stay nearby or use `/forceload`), otherwise ticks are counted while nothing runs.
- If the signal never reaches the chosen state, the test runs until `/speedcheck stop`.
- Only block containers are counted. Minecarts, dropped items and bundle contents are not.

## Changelog

### V1.1
- Added `/speedcheck check` to verify ideal output packing

### V1
- Initial release
