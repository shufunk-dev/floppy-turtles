# 🐢 Floppy Turtles (Minecraft 1.21.11 - Fabric)

An autonomous robotic turtle mod inspired by ComputerCraft, built from the ground up for **Minecraft 1.21.11** using **Fabric Loader 0.19.3** and **Java 21**.

Instead of writing Lua code in an in-game terminal, **Floppy Turtles** gives you code-free automation! Simply equip your turtle with a tool, fuel it with coal, insert a pre-programmed **Floppy Disk**, and let it work. Configure operating ranges, bridge widths, and farm plot sizes directly inside an interactive retro-styled GUI.

---

## ✨ Features

### 1. Autonomous Block-Aligned Turtle
- Grid-aligned autonomous robot with full 6-axis spatial movement.
- Preserves internal inventory, tool equipment, fuel level, and active task state across movements and world reloads.
- Safe to mine: cleanly drops all contained items, equipment, and unspent fuel when broken by a player.

### 2. Universal Fuel System
- Powered by vanilla furnace fuels registered in `FuelRegistry` (Coal, Charcoal, Blaze Rods, Lava Buckets, Wood, etc.).
- Consumes **1 fuel unit per movement or tool action**.
- High-capacity internal fuel tank holding up to **10,000 fuel units**.
- Features an animated burning fuel ember gauge and live numerical readout on the Turtle display.

### 3. Equipment & Tool Support
- Dedicated **Tool Slot** supporting Pickaxes, Axes, Shovels, Hoes, and Swords.
- Uses equipped tools to harvest blocks with authentic drop tables and properly tracks tool durability wear.
- Sword attack power dynamically scales based on weapon material tier (Wood through Netherite).

### 4. Interactive Retro GUI
- **Fuel Slot**: Dedicated slot with coal silhouette.
- **Disk Slot**: Accepts any Floppy Disk with custom silhouette.
- **Tool Slot**: Houses your mining, felling, farming, or combat tools.
- **3×3 Internal Storage (9 Slots)**: Holds gathered loot, seeds, and construction blocks.
- **CRT / LCD Green Digital Display**: Live coordinate readout, operation state, fuel percentage, and active program details.
- **Dynamic Controls**:
  - `[START]`: Launches or resumes active program.
  - `[STOP]`: Halts or pauses the turtle mid-routine.
  - `[-]` and `[+]`: Adjusts operational distance/range (2m to 128m).
  - Mode Toggles: Contextual buttons for specialized disks (e.g., 1-Wide / 3-Wide / 5-Wide Bridge Builder; 1 / 2 / 3 Plot Farm Creator).
- **Status Badges**:
  - `Idle` (Gray)
  - `Running` (Green)
  - `Paused` (Yellow)
  - `Out of Fuel` (Red)
  - `Tool Required` (Orange)
  - `Path Blocked` (Red)
  - `Program Complete` (Cyan)

---

## 💾 The Floppy Disks

Each software disk is color-coded with a distinct rainbow palette and programmed for specialized automated tasks:

| Floppy Disk | App ID | Color Label | Required Tool / Items | Function & Description |
| :--- | :--- | :--- | :--- | :--- |
| **Blank Disk** | — | Neutral Gray | — | Blank magnetic storage disk used to craft software disks. |
| **3×3 Tunnel Miner** | `MINER` | 🟦 Royal Blue | Pickaxe / Shovel, Torches | Excavates a 3×3 tunnel ahead, auto-clears falling gravel/sand, and places torches every 7 blocks. |
| **Quarry 3×3** | `QUARRY` | 🟪 Amethyst Purple | Pickaxe | Bores a 3×3 shaft straight down to bedrock layer-by-layer, then returns safely to the surface. |
| **Bridge Builder** | `BUILDER` | 🟧 Construction Orange | Solid Blocks, Fences/Walls | Paves bridges across chasms, water, and lava. Supports **1-wide**, **3-wide**, and **5-wide** (with outer guard rails). Clears landing doorway on far bank. |
| **Tree Feller** | `LUMBERJACK` | 🟩 Forest Green | Axe | Performs 3D BFS trunk & branch felling on trees, collapses leaf canopies, auto-vacuums logs/saplings, and scales terrain without cave digging. |
| **Crop Harvester** | `FARMER` | 🟨 Golden Yellow | Hoe | Sweeps a 9-wide crop swath, harvests mature crops (wheat, carrots, potatoes, beetroot), auto-replants seeds, and scales elevated farmland terraces. |
| **Farm Creator** | `FARM_CREATOR` | 🩵 Aqua / Water Cyan | Hoe, Water Bucket, Seeds | Automatically constructs **1 to 3** fully hydrated, tilled, and seeded 9×9 crop plots with center water reservoirs; hovers safely over planted crops. |
| **Mob Sentry** | `SENTRY` | 🟥 Crimson Red | Sword | 360° perimeter defense turret that engages all hostile monsters within 3m–10m, applies sword tier damage, and vacuums mob drops. |

---

### Disk Highlights & Operational Details

#### ⛏️ 3×3 Tunnel Miner (`MINER`)
- Excavates a clean 3-block wide by 3-block tall horizontal tunnel forward to the configured distance (2m–128m).
- Actively detects and dislodges falling blocks (sand, gravel) so it never gets buried or trapped.
- Automatically places torches from its storage along the tunnel wall every 7 blocks.

#### 🕳️ Quarry 3×3 (`QUARRY`)
- Digs straight down beneath itself in 3×3 horizontal slices until it reaches bedrock or configured depth.
- Collects all ores and stone in its internal 9-slot storage.
- Climbs safely back to the starting surface level when finished.

#### 🌉 Bridge Builder (`BUILDER`)
- Bridges over chasms, ravines, oceans, and lava lakes.
- **Configurable Widths**:
  - `1-Wide`: Single-line walkway.
  - `3-Wide`: 3-block wide path.
  - `5-Wide`: 3-block wide road flanked by outer guard rails.
- **Auto Railings**: Prioritizes wooden fences and cobblestone walls from inventory for outer edges in 5-wide mode.
- **Bank Touchdown**: Automatically lands on solid ground when reaching the far bank and carves a 2-block high entryway into walls.

#### 🌲 Tree Feller (`LUMBERJACK`)
- Identifies tree trunks and employs a 3D Breadth-First Search (BFS) to fell all connected logs and branches (fully compatible with large branching oaks).
- Breaks surrounding leaf canopies to trigger sapling and apple drops.
- Integrates a drop-suction aura to vacuum up all fallen wood and saplings before climbing or returning.
- Hill-climbing AI navigates natural slopes while refusing to dig blindly into stone or underground caves.

#### 🚜 Farm Creator (`FARM_CREATOR`)
- Builds **1, 2, or 3** fully functional, self-hydrated 9×9 crop plots in a row.
- Places a central water source block, tills all 80 surrounding dirt/grass blocks into farmland, and seeds them from inventory.
- Smart seed sorting: uses seeds left-to-right from slots 3–11 (wheat seeds, carrot, potato, beetroot).
- Safely hovers 1 block above crop level when moving forward so tilled land and planted crops are never crushed or scraped.

#### 🌾 Crop Harvester (`FARMER`)
- Automatically hovers into position and sweeps a 9-wide swath across farmland.
- Inspects crop maturity (wheat, carrots, potatoes, beetroots) at dual-depth check levels (1 and 2 blocks below).
- Harvests mature crops, immediately replants with seeds from storage, and preserves immature crops.
- **Terrace Navigation**: Seamlessly detects and scales stepped farmland hills and terraced farms.
- Touches down and powers off upon completing the route.

#### ⚔️ Mob Sentry (`SENTRY`)
- Stationed perimeter defense turret that attacks all hostile `Monster` entities (zombies, creepers, skeletons, spiders, witches, slimes, phantoms, wither skeletons, etc.).
- Attacks with equipped sword, factoring in weapon tier and enchantments.
- Configurable engagement radius from **3m to 10m**.
- **Creeper Safe**: Sentry engagement distance comfortably out-ranges the Creeper 3-block panic detonation radius, neutralizing them before they can ignite.
- Automatically vacuums up all mob drops into its 9-slot inventory.

---

## 🔨 Crafting Recipes

### 1. Autonomous Turtle
```
[Iron Ingot]      [Iron Ingot]       [Iron Ingot]
[Iron Ingot]    [Redstone Block]     [Iron Ingot]
[Iron Ingot]        [Chest]          [Iron Ingot]
```
*(Yields 1 Autonomous Turtle)*

### 2. Blank Floppy Disk
```
                  [Iron Nugget]
[Paper]             [Redstone]          [Paper]
                     [Paper]
```
*(Yields 2 Blank Floppy Disks)*

### 3. Software Disks (Shapeless Crafting)
Combine **1 Blank Floppy Disk** with the corresponding item in any crafting grid:

| Resulting Floppy Disk | Ingredient 1 | Ingredient 2 |
| :--- | :--- | :--- |
| **3×3 Tunnel Miner** | Blank Floppy Disk | Iron Pickaxe |
| **Quarry 3×3** | Blank Floppy Disk | Diamond Pickaxe |
| **Bridge Builder** | Blank Floppy Disk | Stone Bricks |
| **Tree Feller** | Blank Floppy Disk | Iron Axe |
| **Crop Harvester** | Blank Floppy Disk | Iron Hoe |
| **Farm Creator** | Blank Floppy Disk | Water Bucket |
| **Mob Sentry** | Blank Floppy Disk | Iron Sword |

---

## 🛠️ Building & Installation

### Requirements
- **Minecraft**: `1.21.11`
- **Fabric Loader**: `0.19.3` or newer
- **Fabric API**: Required for 1.21.11
- **Java**: `21` or newer

### Building from Source
Clone the repository and build the jar using the Gradle wrapper:

```bash
# Clone the repository
git clone https://github.com/your-username/floppy-turtles.git
cd floppy-turtles

# Build the mod jar
./gradlew build
```

The compiled mod jar will be created at:
```
build/libs/floppy-turtles-1.0.0.jar
```

### Launch Development Environment
```bash
./gradlew runClient
```

---

## 📜 Credits & License
- Inspired by the legendary **ComputerCraft** turtles by Dan200.
- Built for modern Fabric Minecraft by the **Floppy Turtles** contributors.
- Licensed under the [MIT License](LICENSE).
