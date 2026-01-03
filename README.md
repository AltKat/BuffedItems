<p align="center">
  <img src="https://altkat.github.io/buffeditems/banner-main.jpg" alt="BuffedItems Banner"/>
  <br/>
  <img src="https://img.shields.io/badge/MC-1.21+-green?style=for-the-badge&logo=minecraft" alt="Minecraft 1.21+" />
  <img src="https://img.shields.io/badge/Java-17+-blueviolet?style=for-the-badge&logo=openjdk" alt="Java 17+" />
  <a href="https://github.com/altkat/BuffedItems/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License: MIT" />
  </a>
  <a href="https://bstats.org/plugin/bukkit/BuffedItems/27592">
    <img src="https://img.shields.io/bstats/servers/27592?label=bStats&style=for-the-badge" alt="bStats Servers" />
  </a>
  <br>
  <a href="https://discord.gg/nxY3fc7xz9">
    <img src="https://img.shields.io/badge/Discord-Support-5865F2?style=for-the-badge&logo=discord" alt="Discord Server" />
  </a>
  <a href="https://github.com/AltKat/BuffedItems/wiki">
    <img src="https://img.shields.io/badge/Wiki-Documentation-orange?style=for-the-badge&logo=bookstack" alt="Wiki Documentation" />
  </a>
</p>

<h1 align="center">⚔️ BuffedItems - Item Engine 🛡️</h1>

> **Transform your server with Living Items, Passive Stats, and Dynamic Abilities.**

**BuffedItems** is a powerful item engine that allows you to create unique custom items with persistent attributes, passive effects, and scripted active abilities—all managed through a comprehensive **in-game GUI editor**.

***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/features.jpg" alt="BuffedItems Features Banner"/>
</p>

### 🛠️ Core Features
* **🖥️ Full In-Game GUI Editor:** Create, edit, and manage items without touching a single config file. Just type `/bi menu`.
---
* **🎨 Advanced Visuals Engine (v1.7.0):**
    * **Particles:** Attach stunning shapes like **Helix, Vortex, Wings, Spheres, and Bursts** to your items.
    * **Trigger Modes:** Play visuals **Passive** (while wearing/holding) or **Active** (on ability cast).
    * **Priority System:** Smart handling for multiple continuous effects.
---
* **🛡️ Passive Effects:** Grant permanent stats based on where the item is held/worn (`MAIN_HAND`, `OFF_HAND`, `ARMOR`, `INVENTORY`).
    * **Potions:** Speed, Night Vision, Jump Boost, etc.
    * **Attributes:** +Max Health, +Attack Damage, +Movement Speed, +Armor Toughness, etc.
---
* **⚡ Active Abilities & Scripting:**
    * Trigger complex command chains with a **Right-Click**.
    * **Logic System:** Use prefixes like `[chance:50]`, `[delay:20]`, and `[else]` to create RNG-based mechanics.
    * **Internal Tags:** Use `[message]`, `[actionbar]`, `[title]`, and `[sound]` for faster, color-supported feedback.
---
* **🔄 Live Item Updates:** Change an item's stats or visuals in the editor, and all existing items in players' inventories will **automatically update** the moment they are used.
---
* **🔋 Usage Limits (Custom Durability):** Create items with a specific number of uses (independent of vanilla durability).
    * **Actions:** Choose what happens when depleted: `DESTROY`, `DISABLE` (Mark as depleted), or `TRANSFORM` (into another item).
    * **Feedback:** Custom sounds and messages for depletion events.
---
* **🆕 Item Set Bonuses:** Create RPG-style armor sets!
    * Combine specific items (Helmet, Chestplate, Sword, etc.) to unlock **Tiered Bonuses**.
    * **Example:** "Wear 2 pieces for Speed I, wear 4 pieces for +10 Health."
    * Manage sets entirely via the **In-Game GUI** (`/bi menu` -> Item Sets).
---
* **🔨 Custom Crafting System:** 
  * Create custom shaped recipes (3x3) for your items directly in-game.
  * **Advanced Ingredients:** Supports Vanilla materials, other BuffedItems, and **Exact NBT** items.
  * **Recipe Book:** Automatically registers your custom recipes to the vanilla green Recipe Book for easy crafting.
---
* **💎 Advanced Cost System:**
    * Set requirements for using items or upgrading them.
    * **Supports:** Money (Vault), CoinsEngine, AuraSkills Mana, XP, Levels, Health, Hunger, Vanilla Items, and Custom BuffedItems.
---
* **🆙 Upgrade Station:**
    * Allow players to evolve their items (e.g., *Rusty Sword* -> *Excalibur*).
    * Configure **Success Rates**, **Risk Factors** (Lose item on fail?), and **Failure Actions**.
---
* **🚩 Item Flags & Protections**:
    * **PREVENT_DEATH_DROP (Soulbound):** Item stays with player on death.
    * **PREVENT_DROP:** Prevents dropping, storing in chests, or placing in item frames.
    * **UNBREAKABLE:** Item never loses durability.
    * **PREVENT_ANVIL_USE / PREVENT_SMITHING_USE:** Restricts item usage in various crafting tables.
    * **PREVENT_CONSUME / PREVENT_INTERACT:** Blocks eating/drinking or general right-click actions.
    * and many more...
---
  
* **🎨 Visuals & Customization:**
    * Full **HEX Color** support (`&#RRGGBB`).
    * **PAPI Placeholders:** Placeholders are accepted everywhere.
    * **Leather Armor Dyeing:** Set exact colors using hex codes.
    * **Custom Model Data** support (ItemsAdder/Nexo compatible).
---
* 📚 For detailed guides, visit the [BuffedItems Wiki](https://github.com/AltKat/BuffedItems/wiki).
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/images.jpg" alt="BuffedItems Images Section Banner"/>
  <img src="https://altkat.github.io/buffeditems/images/warrior_talisman.gif" width="720" alt="Warrior's Talisman"/>
</p>
<details>
<summary><b>📸 Click to view In-Game Screenshots</b></summary>

**Click any image to view full size.**

| | | |
|:---:|:---:|:---:|
| <a href="https://altkat.github.io/buffeditems/images/main_menu.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/main_menu.png" width="240" alt="Main Menu"/></a> | <a href="https://altkat.github.io/buffeditems/images/edit_menu.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/edit_menu.png" width="240" alt="Edit Menu"/></a> | <a href="https://altkat.github.io/buffeditems/images/preview_slot.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/preview_slot.png" width="240" alt="Preview Item"/></a> |
| **Main Menu** | **Edit Menu** | **Item Preview** |

| | | |
|:---:|:---:|:---:|
| <a href="https://altkat.github.io/buffeditems/images/orb_of_elements.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/orb_of_elements.png" width="240" alt="Orb of Elements"/></a> | <a href="https://altkat.github.io/buffeditems/images/axe_of_the_berserker.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/axe_of_the_berserker.png" width="240" alt="Axe of The Berserker"/></a> | <a href="https://altkat.github.io/buffeditems/images/boots_of_swiftness.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/boots_of_swiftness.png" width="240" alt="Boots of Swiftness"/></a> |
| **Orb of Elements** | **Axe of The Berserker** | **Boots of Swiftness** |

| | | |
|:---:|:---:|:---:|
| <a href="https://altkat.github.io/buffeditems/images/miners_dream.png" target="_blank"><img src="https://altkat.github.io/buffeditems/images/miners_dream.png" width="240" alt="Miner's Dream"/></a> | <a href="https://altkat.github.io/buffeditems/images/bunny_hope.gif" target="_blank"><img src="https://altkat.github.io/buffeditems/images/bunny_hope.gif" width="240" alt="Bunny Charm"/></a> | <a href="https://altkat.github.io/buffeditems/images/minersdream.gif" target="_blank"><img src="https://altkat.github.io/buffeditems/images/minersdream.gif" width="240" alt="Miner's Dream Effect"/></a> |
| **Miner's Dream** | **Bunny Charm** | **Miner's Dream (GIF)** |

| |
|:---:|
| <a href="https://altkat.github.io/buffeditems/images/swift.gif" target="_blank"><img src="https://altkat.github.io/buffeditems/images/swift.gif" width="240" alt="Swiftness Boots"/></a> |
| **Swiftness Boots** |
</details>

***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/how-it-works.jpg" alt="BuffedItems How it works Banner"/>
</p>

1.  **Design in-Game**: Use `/bi menu` to create items. No config editing needed.
2.  **Passive Engine**: Automatically applies Potion Effects and Attributes based on the item's slot.
3.  **Visual Engine**: Spawns complex particle shapes and manages BossBars/ActionBars based on item state.
4.  **Active Triggers**: Executes scripted logic when an item is used, handling Cooldowns, Costs, and RNG.
5.  **Live Updates**: Any changes made in the editor are pushed to existing items in real-time.
   
* 📚 For detailed setup instructions and advanced configuration, see the [Getting Started section in the Wiki](https://github.com/AltKat/BuffedItems/wiki).
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/important-notes.jpg" alt="BuffedItems Important Notes Banner"/>
</p>

* **Custom Armor Textures**: This plugin only sets the `CustomModelData` tag. It does **not** manage resource packs or custom armor models. Wearable items (helmets, armor) using ItemsAdder/Nexo IDs will show the custom texture in the inventory, but will render as the **default material** (e.g., Diamond Helmet) when equipped on the player.
* **Configuration Files**:
    * **Editing**: We recommend using the in-game GUI (`/bi menu`) as it handles everything for you instantly. However, you CAN edit yml files manually if you prefer! Just run `/bi reload` afterwards to apply your manual changes.
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/commands-permissions.jpg" alt="BuffedItems Commands & Permissions Banner"/>
</p>

The main command is `/buffeditems` (Aliases: `/bi`, `/buffitems`).

| Command                                | Permission                    | Description                                                         |
|:---------------------------------------|:------------------------------|:--------------------------------------------------------------------|
| `/bi menu`                             | `buffeditems.command.menu`    | Opens the main GUI editor to create, edit, and delete items.        |
| `/bi upgrade`                          | `buffeditems.command.upgrade` | Opens the Item Upgrade Station.                                     |
| `/bi recipes`                          | `buffeditems.command.recipes` | Opens a menu showing all custom crafting recipes.                   |
| `/bi sets`                             | `buffeditems.command.sets`    | Opens a menu listing all Item Sets and their bonuses.               |                                                                        |
| `/bi give <player> <item_id> [amount]` | `buffeditems.command.give`    | Gives a player the specified custom item.                           |
| `/bi reload`                           | `buffeditems.command.reload`  | Safely reloads all configuration files from disk.                   |
| `/bi list`                             | `buffeditems.command.list`    | Lists all created items and shows if any have configuration errors. |
| `/bi wiki`                             | `buffeditems.command.wiki`    | Opens wiki page.                                                    |
| `/bi update`                           | `buffeditems.command.update`  | Checks for the latest update.                                       |

**Admin Permission:**
* `buffeditems.admin`: Grants access to **ALL** BuffedItems commands listed above.

**Item Permissions:**
You can define custom permissions inside the Item Editor:
* **Main Permission:** Required to use/hold the item.
* **Active Permission:** Required specifically for the Right-Click ability.
* **Passive Permission:** Required specifically for the passive stats.
* *Example:* `my.server.vip.sword`
* See the [Wiki for permission details](https://github.com/AltKat/BuffedItems/wiki).
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/requirements.jpg" alt="BuffedItems Requirements Banner"/>
</p>

* **Java 17** or newer
* **Minecraft 1.21** or newer
* **Paper** or any Paper-based fork (Purpur, Pufferfish).