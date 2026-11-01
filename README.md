# Miscellania Monitor

A read-only RuneLite panel for live Managing Miscellania upkeep status and player guidance. It does not interact with the game or change assignments, coffer balance, or collected resources.

## Supported features

- Reads live popularity (approval) and coffer values from the client after *Throne of Miscellania* is complete. Values are refreshed on game ticks, relevant var changes, and the player's entry into the kingdom region. It accepts values in a kingdom region, or outside it only when both values are positive; otherwise it shows an unavailable state.
- Shows live popularity, coffer, and estimated depletion in its sidebar panel. The top-left in-game overlay has been removed. It produces no transient alerts, sound, flashing, or notifications.
- Shows the appropriate capped wage target: 500,000 gp at a 50,000 gp daily wage cap, or 750,000 gp at a 75,000 gp cap after *Royal Trouble*.
- Shows approval as low at or below the configurable warning threshold (default 50%).
- Counts down to the next midnight UTC collection and coffer update.
- Calculates how many capped daily withdrawals remain before the observed coffer balance falls below its wage target. This projection assumes that daily upkeep continues without a pause; it is not a game-reported timer and does not project beyond the threshold.
- When the game's collection reward interface opens, records the visible haul's item names and quantities with a local timestamp in the account's **Collection Log**. The log can be reset. It records only collections observed after the plugin is enabled and cannot reconstruct earlier hauls.
- Includes a “How it works” tab covering quest unlocks, worker counts, upkeep, popularity, assignments, collection, and known limits.

The status view uses a vertically stacked ledger that fills the sidebar viewport; text fields wrap and cards expand horizontally with their parent. RuneLite's `PluginPanel` API fixes each plugin panel's preferred and minimum width at 225 pixels (the standard scroll wrapper adds scrollbar space), so this plugin cannot add an independent width-resize handle. The layout follows any width the host gives it, while the actual dock width remains controlled by RuneLite.

## Mechanics shown in the guide

- *Throne of Miscellania* unlocks up to 10 workers. *Royal Trouble* raises this to 15 and increases productivity by 50%.
- The coffer pays 10% of its current balance each day, up to 50,000 gp or 75,000 gp after *Royal Trouble*. Keeping 500,000 gp or 750,000 gp respectively supports the full capped wage.
- Popularity falls by 2.5 percentage points per day before *Royal Trouble* and 1 point per day afterward. Island tasks can raise it to a maximum of 100%.
- Six categories are available: herbs, wood, mining, fishing, hardwood, and farming. Each allows up to 10 workers; idle workers still incur wages.
- Assignments can be changed before collection. The distribution in place when collecting determines the resource mix for the pending haul. Collection deposits resources into the bank.
- Gathering and coffer upkeep pause after 30 days away and resume after returning.

## Limits

- Exact assignment counts are not currently shown. The management interface exposes bars but no semantic numeric count field, and this plugin does not interpret their visual state. If displayed collection item quantities are absent, those remain unknown too. A future yield calculator should label its output as an estimate unless exact values become available from client data.
- **Estimated coffer depletion** shows the number of capped daily withdrawals until the balance falls below its wage target. It assumes uninterrupted upkeep and does not account for the 30-day away pause. It shows 0 when the balance is already below the target.
- Default or unloaded zero vars outside the kingdom are treated as unavailable, not as a real coffer snapshot.
- The plugin does not send notifications, write game state, or provide controls for the game's management interface. It stores the Collection Log in the active RuneLite account profile and does not store coffer history.
- RuneLite’s built-in **Kingdom of Miscellania** plugin already tracks approval/coffer changes and local estimates. This plugin focuses on a broader upkeep view and does not duplicate its estimate model.

## What appears on entering the island

Entering the kingdom region does not itself open the in-game management or collection screen. The sidebar refreshes `MISC_APPROVAL` and `MISC_COFFERS` on game ticks and when those vars change. RuneLite's built-in plugin reads those same vars and uses those regions.

RuneLite's generated interface API identifies separate `MISC_BOTH_MANAGE` and `MISC_COLLECTION` widget groups. The management group exposes separate category labels and up to ten bar widgets per category, but no semantic numeric assignment-count field. Counting or interpreting bar sprite/visibility state would be a visual heuristic, so exact assignment counts are not currently shown. The collection group has a named `ITEMS` widget; when the reward interface is visible, the plugin reads positive item IDs and quantities from its visible widget tree using RuneLite's `Widget` API and appends that haul to the local Collection Log. The log cannot backfill hauls from before the plugin was enabled.

These interfaces are separate from the region state and are loaded on demand. Open the relevant interface through normal in-game interaction (Advisor Ghrim, or Astral Contact for the options the spell supports) if you want to view it in-game. The plugin does not invoke those interactions. Approval and coffer are available on entering the region; collection log entries are recorded when the game loads the reward interface.

## Client behavior

The plugin reads values exposed by the client and presents them in the sidebar. It does not submit menu operations, packets, dialogue, clicks, or client-script calls to the game, and exposes no control for changing kingdom settings. The Collection Log reset clears only the plugin's local history.

## Build/install status

Build the plugin JAR with `gradlew.bat clean jar` on Windows or `./gradlew clean jar` on macOS/Linux. The project compiles to Java 11 bytecode and resolves the current RuneLite client API through `latest.release`. The output is `build/libs/miscellania-monitor-0.1.0-SNAPSHOT.jar`. No automated tests are included in the build.
The current local build uses Gradle 9.8.2 and RuneLite client/API 1.13.4. A runnable copy is available at `MiscellaniaMonitor.jar` in this folder. The latest changes require a RuneLite restart before they appear in an already-running client.


For local developer loading, copy the JAR into RuneLite's `~/.runelite/sideloaded-plugins` directory and launch a developer-mode RuneLite client with `-ea` as a JVM argument and `--developer-mode` as a program argument. The standard RuneLite launcher does not load sideloaded developer plugins; use a direct developer/client launch. After the client starts, enable **Miscellania Monitor** in the plugin list if it is not already enabled. The plugin is optional and disabled by default. Plugin Hub publication is separate and requires a public repository and manifest entry; acceptance of a custom sidebar panel remains subject to RuneLite review.

## API assumptions

- `VarbitID.MISC_APPROVAL` is represented on a 0–127 scale and is converted to integer percent.
- `VarbitID.MISC_COFFERS` exposes the current coffer balance.
- `VarPlayerID.MISC_QUEST` indicates that *Throne of Miscellania* is unlocked, and `Quest.ROYAL_TROUBLE` identifies the higher upkeep tier.
- `WorldPoint.getRegionID()` reports whether the player is in one of the two kingdom regions used by RuneLite's built-in plugin.
- The generated `InterfaceID.MiscBothManage` and `InterfaceID.MiscCollection` constants identify the management and collection interface groups. Collection quantities are read with `Widget.getItemId()` and `Widget.getItemQuantity()` only for visible widgets; visibility and item reads occur on the client thread. `ConfigManager` stores the timestamped Collection Log in the active RuneScape account profile.
- RuneLite event handlers read client data on the client thread; Swing UI changes are queued on the EDT.

These varbit/varplayer names, region checks, and quest checks match RuneLite's current built-in [Kingdom of Miscellania plugin](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/kingdomofmiscellania/KingdomPlugin.java). Widget-reading capabilities are exposed by RuneLite's [Widget API](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/widgets/Widget.java), and the groups are named in its generated [InterfaceID API](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/InterfaceID.java). RuneScape rule scope follows the official [plugin guidelines](https://developer.runescape.com/rs/submission/guidelines.html). Mechanics are summarized from the [OSRS Wiki Managing Miscellania page](https://oldschool.runescape.wiki/w/Managing_Miscellania). Confirm API compatibility against the intended RuneLite release before building.
