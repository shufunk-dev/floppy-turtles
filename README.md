# Turtle Mod (Minecraft 1.21.11 - Fabric)

An autonomous programmable robotic turtle mod inspired by ComputerCraft, built from the ground up for **Minecraft 1.21.11** using **Fabric Loader 0.19.3**.

Instead of writing Lua code in an in-game editor, you insert **Floppy Disks** with pre-programmed apps! Equip your turtle with tools, keep it fueled with coal, insert an app disk, and configure its parameters right in its interactive GUI.

---

## Features

### 1. Autonomous Block-Aligned Turtle
- Grid-aligned autonomous robot with 6-axis orientation and full movement capabilities.
- Retains internal state, inventory, equipment, and fuel seamlessly across movements.
- Automatically breaks when mined by a player, cleanly dropping all contained items and remaining fuel.

### 2. Fuel System
- Requires fuel to operate (primarily **Coal** and **Charcoal**).
- Compatible with all furnace fuels via vanilla `FuelRegistry` (e.g., Blaze Rods, Lava Buckets, Wood).
- Consumes **1 fuel unit per movement/action**.
- Max fuel capacity of **10,000 units**.
- Visual fuel bar and live numerical fuel meter inside the Turtle GUI.

### 3. Equipment & Tool Support
- Dedicated **Tool Slot** accepting pickaxes, shovels, axes, hoes, and swords.
- Automatically uses equipped tool types to mine blocks with proper loot drops and tool durability damage.

### 4. Floppy Disk App System
Pre-programmed Floppy Disks allow players to plug-and-play diverse automated behaviors:

| Floppy Disk | App Name | Function | Recommended Tool |
| :--- | :--- | :--- | :--- |
| **Blank Disk** | *Unprogrammed* | Base crafting material for software disks | None |
| **3x3 Tunnel Miner**| `MINER` | Excavates a 3x3 tunnel ahead and places torches every 7 blocks | Pickaxe / Shovel |
| **Quarry 3x3** | `QUARRY` | Excavates a 3x3 shaft straight down layer-by-layer down to bedrock | Pickaxe |
| **Bridge Builder**| `BUILDER` | Paves solid bridges across ravines, chasms, oceans, and lava lakes using blocks from its storage | Building Blocks |
| **Tree Feller** | `LUMBERJACK` | Detects tree trunks, fells trees vertically from base to canopy, collecting wood and saplings | Axe |
| **Crop Harvester**| `FARMER` | Detects mature crops (wheat, carrots, potatoes, beetroot), harvests them, and auto-replants seeds | Hoe |
| **Mob Sentry** | `SENTRY` | Scans surrounding area for hostile monsters, attacks with equipped weapon, and vacuums mob drops | Sword |

### 5. Interactive GUI
- **Fuel Slot** (with coal silhouette).
- **Disk Slot** (with floppy disk silhouette).
- **Tool Slot** (with tool silhouette).
- **3x3 Storage Grid** (9 item slots for mined resources and building materials).
- **Controls**:
  - `[START]`: Launches the active floppy disk program.
  - `[STOP]`: Pauses or halts operation.
  - `[-]` and `[+]`: Dynamically adjusts operating range / distance (2m to 128m).
- **Live Status Badges**:
  - `Idle` (Gray)
  - `Running` (Green)
  - `Paused` (Yellow)
  - `Out of Fuel` (Red)
  - `Tool Required` (Orange)
  - `Path Blocked` (Red)
  - `Program Complete` (Cyan)

---

## Crafting Recipes

### Autonomous Turtle
```
[Iron Ingot]   [Iron Ingot]        [Iron Ingot]
[Iron Ingot]   [Redstone Block]    [Iron Ingot]
[Iron Ingot]   [Chest]             [Iron Ingot]
```
*(Yields 1 Autonomous Turtle)*

### Blank Floppy Disk
```
               [Iron Nugget]
[Paper]        [Redstone]          [Paper]
               [Paper]
```
*(Yields 2 Blank Floppy Disks)*

### Software Disks (Shapeless)
- **Tunnel Miner**: `Blank Disk` + `Iron Pickaxe`
- **Quarry 3x3**: `Blank Disk` + `Diamond Pickaxe`
- **Bridge Builder**: `Blank Disk` + `Stone Bricks`
- **Tree Feller**: `Blank Disk` + `Iron Axe`
- **Crop Harvester**: `Blank Disk` + `Iron Hoe`
- **Mob Sentry**: `Blank Disk` + `Iron Sword`

---

## Building & Running

### Build the Mod Jar:
```bash
./gradlew build
```
The compiled jar will be located at:
```
build/libs/turtle-1.0.0.jar
```

### Launch Minecraft Client Dev Environment:
```bash
./gradlew runClient
```
