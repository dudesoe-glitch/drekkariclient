# Drekkari Client

A custom Haven & Hearth client built on top of the official vanilla client, with extensive automation, quality-of-life features, and UI improvements.

## Building & Running

**Requirements:** Java 15-21, Apache Ant

```bash
ant run       # Build and launch
ant bin       # Build only
ant clean     # Clean build artifacts
```

Or just run `Play.bat` from a release.

## Features

### Bots & Automation
- **FarmingBot** - Harvest mature crops, replant with highest quality seeds
- **CleanupBot** - Chop bushes/trees, chip rocks, destroy stumps
- **FishingBot** - Automated fishing with full inventory handling
- **ButcherBot** - Batch animal butchering with pathfinding and auto-drink
- **OreSmeltingBot** - Full smelting workflow (7 ore types, fuel config)
- **MiningBot** - Map-click targeted mining with safe mode (support radius validation)
- **ForagingBot** - Auto-pick herbs and optional ground items
- **ClayDiggingBot** - Pathfind to clay, dig, auto-drink
- **CellarDiggingBot** - Automated cellar expansion
- **RoastingSpitBot** - Automated cooking on spit
- **TarKilnCleanerBot** - Automated tar kiln management
- **GrubGrubBot** - Automated foraging
- **OceanScoutBot** - Ocean exploration automation

### Combat
- **CombatDistanceTool** - Auto-respace to optimal weapon range, weapon auto-detect (sword/spear/axe/bow), auto-re-attack peaced targets
- **CombatRotationBot** - Configurable skill sequence executor with loop/cooldown support
- **Weapon distance table** - Ring of Brodgar wiki multipliers for 10+ weapon types

### Inventory & Items
- **Multi-criteria sorting** - Sort by item name, then quality within groups
- **Container sorting** - Uses player inventory as staging area for reliable sorting
- **Multi-slot item sorting** - Large items sorted by area for better bin packing
- **Item filter search** - `q>10`, `q<50`, `q:10-50`, `fep:str>1`, `armor:hard>1`, `has:water`, `lp>100`, `wear<50`
- **Extended inventory panel** - Grouping by name/type, filter bar, collapsible groups, click-to-highlight
- **Inventory list view** - Text-based summary with icons, sort, quality coloring
- **Per-item auto-drop** - JSON-config auto-drop with quality thresholds
- **Category auto-drop** - Quality threshold by mining category (stones, coals, ores, etc.)
- **Stack/Unstack all** - One-click inventory organization
- **Quality display modes** - Default, Mean, Average, Min, Max aggregation
- **Item category badges** - Colored dots (food, armor, curio, tool, seed, container, material)
- **Armor & durability overlays** - Hard/soft armor text, durability numbers and bars

### Pathfinding
- **A* pathfinding** - Click-to-pathfind around obstacles
- **Long-distance pathfinding** - Auto-chains hops for far destinations
- **Minimap pathfinding** - Pathfind on minimap click
- **Continuous walking + pathfinding** - Pathfind while holding movement
- **PathQueue** - Shift-click waypoint queuing, auto-walk through queue, visual gold lines on map
- **Fallback hitboxes** - Unknown gobs get default obstacles instead of being invisible
- **Horse/mount support** - Expanded hitbox detection when mounted

### Map & Navigation
- **Custom map marker icons** - 38 selectable icons for map markers
- **CheckpointManager** - Route/waypoint automation with save/load (SQLite), area scanning, ETA display
- **Minimap waypoint rendering** - Gold lines and waypoint dots

### UI & Quality of Life
- **Equipment quick-swap hotkeys** - 17 keybinds for weapons, tools, utility items from belt
- **Flower menu auto-select** - Auto-pick right-click menu options
- **Flower menu repeat** - One-click repeat last action across items
- **Batch crafting "Craft N"** - Craft multiple items with stop button
- **Interact with nearest** - Smart nearest-object interaction (gates, herbs, critters, doors)
- **Enter nearest vehicle** - One-key mount horses, boats, wagons, skis, coracles
- **Wagon nearest liftable** - Auto lift-and-load for wagons
- **Toggle nearest gate** - Quick gate open/close
- **Pickup nearest** - Quick item pickup
- **NotepadWindow** - In-game line-based notepad
- **Refill water containers** - Auto-refill from water tiles or barrels
- **Alarm system** - Configurable audio alerts for threats

### Settings & Configuration
- **Modular options window** - 18 extracted panel categories
- **Extensive keybinding system** - Customizable hotkeys for all features
- **UI themes** - Multiple visual themes

## Credits

Forked from [Hurricane](https://github.com/Nightdawg/Hurricane) by Nightdawg — the foundation this client is built on.

- Official Haven & Hearth client source: `git://sh.seatribe.se/hafen-client`
- Web map support: [Cediner's hnh-map-vuetify](https://github.com/Cediner/hnh-map-vuetify)
