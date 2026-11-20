# 🐢 Floppy Turtles

**Autonomous, Code-Free Robotic Turtles for Minecraft 1.21.11 (Fabric)**

Love the legendary turtles from ComputerCraft, but don't want to write Lua code or troubleshoot scripting errors? **Floppy Turtles** brings automated robotics to modern Minecraft with zero programming required!

Simply craft a Turtle, fuel it with coal, equip it with your favorite tool, and pop in a pre-programmed **Floppy Disk** to get to work.

---

## 🚀 Quick Start in 4 Easy Steps

1. **Craft & Place**: Build an Autonomous Turtle and place it down in your world.
2. **Fuel It Up**: Open the GUI and feed it coal, charcoal, or any furnace fuel (holds up to 10,000 fuel units!).
3. **Equip a Tool**: Place a pickaxe, axe, hoe, shovel, or sword into the dedicated tool slot.
4. **Insert Floppy & Press [START]**: Insert an app disk, adjust your distance or width settings, and hit **START**!

---

## 💾 The 7 Floppy Disks

Each Floppy Disk is uniquely color-coded and loaded with dedicated autonomous software:

* ⛏️ **3×3 Tunnel Miner** *(Royal Blue)*:
  * Excavates a clean 3×3 tunnel ahead up to 128 blocks.
  * Clears falling sand and gravel automatically so it never gets buried.
  * Automatically places torches along tunnel walls every 7 blocks.
* 🕳️ **Quarry 3×3** *(Amethyst Purple)*:
  * Bores a 3×3 shaft straight down to bedrock layer-by-layer.
  * Safely navigates all the way back to the surface when finished.
* 🌉 **Bridge Builder** *(Construction Orange)*:
  * Paves sturdy bridges across ravines, oceans, and lava lakes using blocks from its storage.
  * Supports **1-wide**, **3-wide**, and **5-wide** paths.
  * In 5-wide mode, it automatically places guard rails and fences along both sides!
  * Automatically touches down upon hitting solid ground and carves a doorway into walls.
* 🌲 **Tree Feller** *(Forest Green)*:
  * 3D Breadth-First Search (BFS) fells connected trunks and branches on normal and large branching trees.
  * Collapses leaf canopies and triggers sapling drops.
  * Built-in vacuum aura collects all wood and saplings before climbing terrain.
  * Cave-safe: refuses to dig into stone hills or subterranean caverns.
* 🌾 **Crop Harvester** *(Golden Yellow)*:
  * Sweeps a 9-wide swath across farmland.
  * Detects mature wheat, carrots, potatoes, and beetroots, harvests them, and auto-replants seeds.
  * Intelligently scales stepped farmland terraces without damaging crops.
* 🚜 **Farm Creator** *(Aqua Cyan)*:
  * Automatically clears, tills, hydrates, and seeds **1, 2, or 3** full 9×9 crop plots in a row.
  * Places a center water source in each plot.
  * Hovers safely 1 block above ground level so planted crops are never trampled.
* ⚔️ **Mob Sentry** *(Crimson Red)*:
  * 360° perimeter defense turret that engages all hostile monsters within 3m–10m.
  * Attacks with equipped swords, scaling with material tier (Wood to Netherite).
  * Out-ranges Creeper detonation, neutralizing them safely without explosions.
  * Vacuums mob loot straight into its internal storage.

---

## 🖥️ Interactive Retro GUI

* **Green Phosphor LCD Display**: Live feedback showing robot coordinates, remaining fuel, active programs, and status badges (`Idle`, `Running`, `Paused`, `Out of Fuel`, `Tool Required`, `Path Blocked`, `Program Complete`).
* **Dynamic Range Controls**: Adjust working distance with `[-]` and `[+]` from 2m to 128m.
* **Contextual Buttons**: Toggle bridge widths (1W / 3W / 5W) and farm plot counts (1P / 2P / 3P) right from the interface.
* **Internal 9-Slot Grid**: Holds collected minerals, seeds, logs, and construction blocks.

---

## 🔨 Crafting Recipes

* **Autonomous Turtle**: 7 Iron Ingots + 1 Redstone Block + 1 Chest
* **Blank Floppy Disk**: 1 Iron Nugget + 3 Paper + 1 Redstone (yields 2)
* **3×3 Tunnel Miner**: Blank Disk + Iron Pickaxe
* **Quarry 3×3**: Blank Disk + Diamond Pickaxe
* **Bridge Builder**: Blank Disk + Stone Bricks
* **Tree Feller**: Blank Disk + Iron Axe
* **Crop Harvester**: Blank Disk + Iron Hoe
* **Farm Creator**: Blank Disk + Water Bucket
* **Mob Sentry**: Blank Disk + Iron Sword

---

## ⚙️ Installation & Requirements

* **Minecraft**: `1.21.11`
* **Fabric Loader**: `0.19.3+`
* **Fabric API**: Required
* **Java**: `21+`

Put `floppy-turtles-1.0.0.jar` into your `.minecraft/mods` folder and enjoy automated mining, building, and farming!
