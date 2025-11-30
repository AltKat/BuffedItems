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

**BuffedItems** is a powerful item engine that allows you to create unique custom items with persistent attributes, passive effects, and scripted active abilities all managed through a comprehensive **in-game GUI editor**.

***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/features.jpg" alt="BuffedItems Features Banner"/>
</p>

### 🛠️ Core Features
* **🖥️ Full In-Game GUI Editor:** Create, edit, and manage items without touching a single config file. Just type `/bi menu`.
* **🛡️ Passive Effects:** Grant permanent stats based on where the item is held/worn (`MAIN_HAND`, `OFF_HAND`, `ARMOR`, `INVENTORY`).
    * **Potions:** Speed, Night Vision, Jump Boost, etc.
    * **Attributes:** +Max Health, +Attack Damage, +Movement Speed, +Armor Toughness, etc.
<br><br>
* **⚡ Active Abilities & Scripting:**
    * Trigger complex command chains with a **Right-Click**.
    * **Logic System:** Use prefixes like `[chance:50]`, `[delay:20]`, and `[else]` to create RNG-based mechanics.
    * **Actions:** Send messages, titles, play sounds, or execute console commands.
<br><br>
* **🔄 Live Item Updates:** Change an item's damage, lore, or name in the config, and players' existing items will **automatically update** the moment they are used or clicked.
    * *Data Safety:* Custom Enchants, Anvil Names, and Usage Stats are **preserved** during updates.
<br><br>
* **🔋 Usage Limits (Custom Durability):** Create items with a specific number of uses (independent of vanilla durability).
    * **Actions:** Choose what happens when depleted: `DESTROY`, `DISABLE` (Mark as depleted), or `TRANSFORM` (into another item).
    * **Feedback:** Custom sounds and messages for depletion events.
<br><br>
* **💎 Advanced Cost System:**
    * Set requirements for using items or upgrading them.
    * **Supports:** Money (Vault), CoinsEngine, XP, Levels, Health, Hunger, Vanilla Items, and Custom BuffedItems.
<br><br>
* **🆙 Upgrade Station:**
    * Allow players to evolve their items (e.g., *Rusty Sword* -> *Excalibur*).
    * Configure **Success Rates**, **Risk Factors** (Lose item on fail?), and **Failure Actions**.
<br><br>
* **🚩 Item Flags & Protections**:
    * **PREVENT_DEATH_DROP (Soulbound):** Item stays with player on death.
    * **PREVENT_DROP:** Prevents dropping, storing in chests, or placing in item frames.
    * **UNBREAKABLE:** Item never loses durability.
    * **PREVENT_ANVIL_USE / PREVENT_SMITHING_USE:** Restricts item usage in various crafting tables.
    * **PREVENT_CONSUME / PREVENT_INTERACT:** Blocks eating/drinking or general right-click actions.
    * and many more...
<br><br>
  
* **🎨 Visuals & Customization:**
    * Full **HEX Color** support (`&#RRGGBB`).
    * **Custom Model Data** support for resource packs (ItemsAdder/Nexo friendly).
    * **Cooldown Visuals:** BossBar, ActionBar, and Title countdowns.
    * **PAPI Placeholders:** Placeholders are accepted everywhere.
<br><br>
* 📚 For detailed guides on all features, visit the [BuffedItems Wiki](https://github.com/AltKat/BuffedItems/wiki).
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

1.  **Create**: Use `/bi menu` to create a new item and give it a unique ID (e.g., `warriors_talisman`).
2.  **Configure**: Use the GUI to set the item's properties.
3.  **Give**: Give the item to a player using `/bi give <player> warriors_talisman`.
4.  **Apply**: The plugin's core task detects that the player has the item in their inventory and automatically applies the `STRENGTH 1` and `+2 Hearts` effects. If the player drops the item, the effects are instantly removed.
* 📚 For detailed setup instructions and advanced configuration, see the [Getting Started section in the Wiki](https://github.com/AltKat/BuffedItems/wiki).
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/important-notes.jpg" alt="BuffedItems Important Notes Banner"/>
</p>

* **Custom Armor Textures**: This plugin only sets the `CustomModelData` tag. It does **not** manage resource packs or custom armor models. Wearable items (helmets, armor) using ItemsAdder/Nexo IDs will show the custom texture in the inventory, but will render as the **default material** (e.g., Diamond Helmet) when equipped on the player.
* **Configuration Files**:
    * `config.yml`: Contains general plugin settings (messages, debug level, etc.).
    * `items.yml`: Stores all your custom items.
    * `upgrades.yml`: Stores all your upgrade recipes.
    * **Editing**: We recommend using the in-game GUI (`/bi menu`) as it handles everything for you instantly. However, you CAN edit these files manually if you prefer! Just run `/bi reload` afterwards to apply your manual changes.
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/commands-permissions.jpg" alt="BuffedItems Commands & Permissions Banner"/>
</p>

The main command is `/buffeditems` (Aliases: `/bi`, `/buffitems`).

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/bi menu` | `buffeditems.command.menu` | Opens the main GUI editor to create, edit, and delete items. |
| `/bi upgrade` | `buffeditems.command.upgrade` | Opens the Item Upgrade Station. |
| `/bi give <player> <item_id> [amount]` | `buffeditems.command.give` | Gives a player the specified custom item. |
| `/bi reload` | `buffeditems.command.reload` | Safely reloads `config.yml`, `items.yml`, and `upgrades.yml` from disk. |
| `/bi list` | `buffeditems.command.list` | Lists all created items and shows if any have configuration errors. |
| `/bi update` | `buffeditems.command.update` | Checks for the latest update. |

**Admin Permission:**
* `buffeditems.admin`: Grants access to all BuffedItems commands.

**Item Permissions:**
* You can define a custom permission (e.g., `myitems.warrior_perk`) inside the item editor. If set, a player must have this permission to receive the item's effects. See the [Wiki for permission details](https://github.com/AltKat/BuffedItems/wiki).

<p align="center">
  <img src="https://altkat.github.io/buffeditems/requirements.jpg" alt="BuffedItems Requirements Banner"/>
</p>

This plugin requires the **Paper API** and uses Paper-exclusive events (like `AsyncChatEvent` and `PlayerArmorChangeEvent`). It **will not work on Spigot**.

* **Java 17** or newer
* **Minecraft 1.21** or newer
* Paper-based server software:
    * **Paper** (Target platform)
    * **Pufferfish** (Paper fork)
    * **Purpur** (Paper fork)
    * and others.
