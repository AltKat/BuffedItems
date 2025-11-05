

<p align="center">
  <img src="https://altkat.github.io/buffeditems/banner-main.jpg" alt="BuffedItems Banner"/>



  <br/>
    <img src="https://img.shields.io/badge/MC-1.21+-green?style=for-the-badge" alt="Minecraft 1.21+" />
  <img src="https://img.shields.io/badge/Java-17+-blueviolet?style=for-the-badge" alt="Java 17+" />
  <a href="https://github.com/altkat/BuffedItems/blob/master/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge" alt="License: MIT" />
  </a>
  <a href="https://bstats.org/plugin/bukkit/BuffedItems/27592">
    <img src="https://img.shields.io/bstats/players/27592?label=bStats&style=for-the-badge" alt="bStats Players" />
  </a>
  <br>
  <a href="https://discordapp.com/users/247441109888925697">
    <img src="https://img.shields.io/badge/Discord-Profile-5865F2?style=for-the-badge&logo=discord" alt="Discord Profile" />
  </a>
</p>

**BuffedItems** lets you create unique, custom items with persistent potion effects, attributes, and special properties, all from a comprehensive **in-game GUI editor**.


***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/features.jpg" alt="BuffedItems Features Banner"/>
</p>

* **Full In-Game GUI Editor**: Access a powerful menu with `/bi menu` to manage every aspect of your items.
* **Persistent Effects**: Grant permanent potion effects (like Speed, Haste) or attribute modifiers (like +Max Health, +Movement Speed).
* **Slot-Based Application**: Apply effects only when an item is in a specific slot:
    * `MAIN_HAND` or `OFF_HAND`
    * `HELMET`, `CHESTPLATE`, `LEGGINGS`, `BOOTS`
    * `INVENTORY` (applies if the item is anywhere in the player's inventory)
* **Deep Customization**:
    * **Display Name & Lore**: Full support for Hex codes (`&#RRGGBB`) and standard color codes (`&c`).
    * **Material**: Choose any item material from a paginated in-game selector.
    * **Enchantments**: Add any enchantment with any level (e.g., Sharpness 10).
    * **Glow**: Toggle the enchantment glow effect with one click.
    * **Custom Model Data**: Supports direct integers and integration with **ItemsAdder** and **Nexo** (e.g., `itemsadder:my_sword`).
* **Powerful Item Flags (Protections)**:
    * `PREVENT_DEATH_DROP`: The item stays in the player's inventory on death.
    * `PREVENT_DROP`: Prevents dropping the item or storing it in containers/item frames.
    * `UNBREAKABLE`: The item never loses durability.
    * `PREVENT_ANVIL_USE`: Blocks the item from being used in anvils.
    * `PREVENT_SMITHING_USE`: Blocks the item from being used in smithing tables.
    * ...and many more, like `PREVENT_CRAFTING`, `PREVENT_PLACEMENT`, and `HIDE_ATTRIBUTES`.
* **Permission-Based Effects**: Optionally require players to have a specific permission node for an item's effects to apply.
* **Integrations**:
    * **PlaceholderAPI**: Use placeholders in your item's display name and lore.
    * **ItemsAdder**: Get custom model data from an ItemsAdder item (e.g., `itemsadder:my_sword`).
    * **Nexo**: Get custom model data from a Nexo item (e.g., `nexo:my_sword`).
* **Performance**: Features an intelligent caching and task-scheduling system to apply effects with minimal impact on server performance.

***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/images.jpg" alt="BuffedItems Images Section Banner"/>
</p>

**Click any image to view full size or visit the [Gallery](https://modrinth.com/plugin/buffeditems/gallery) section.**

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

| | | |
|:---:|:---:|:---:|
| <a href="https://altkat.github.io/buffeditems/images/warrior_talisman.gif" target="_blank"><img src="https://altkat.github.io/buffeditems/images/warrior_talisman.gif" width="240" alt="Warrior's Talisman"/></a> | <a href="https://altkat.github.io/buffeditems/images/swift.gif" target="_blank"><img src="https://altkat.github.io/buffeditems/images/swift.gif" width="240" alt="Swiftness Boots"/></a> | |
| **Warrior's Talisman** | **Swiftness Boots** | |
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/how-it-works.jpg" alt="BuffedItems How it works Banner"/>
</p>

1.  **Create**: Use `/bi menu` to create a new item and give it a unique ID (e.g., `warriors_talisman`).
2.  **Configure**: Use the GUI to set the item's name, material, and add effects. For example, add `STRENGTH 1` and `GENERIC_MAX_HEALTH +4.0` to the `INVENTORY` slot.
3.  **Give**: Give the item to a player using `/bi give <player> warriors_talisman`.
4.  **Apply**: The plugin's core task detects that the player has the item in their inventory and automatically applies the `STRENGTH 1` and `+2 Hearts` effects. If the player drops the item, the effects are instantly removed.
5. **Save**: Changes are held in memory. Use `/bi save` or wait for the auto-save to write them to `config.yml`.

***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/important-notes.jpg" alt="BuffedItems Important Notes Banner"/>
</p>

* **PAPI Placeholders**: PlaceholderAPI placeholders (e.g., `%player_name%`) are only parsed when the item is given using the `/bi give` command. They will **not** update dynamically while the item is already in a player's inventory.
* **Custom Armor Textures**: This plugin only sets the `CustomModelData` tag. It does **not** manage resource packs or custom armor models. Wearable items (helmets, armor) using ItemsAdder/Nexo IDs will show the custom texture in the inventory, but will render as the **default material** (e.g., Diamond Helmet) when equipped on the player.
* **Configuration**: **Do Not Edit `config.yml` Manually!** Always use the `/bi menu`. Changes made in the menu are saved to memory and only written to the file on `/bi save` or auto-save. Using `/bi reload` will **overwrite** your unsaved menu changes with the contents of the file.
***

<p align="center">
  <img src="https://altkat.github.io/buffeditems/commands-permissions.jpg" alt="BuffedItems Commands & Permissions Banner"/>
</p>

The main command is `/buffeditems` (Aliases: `/bi`, `/buffitems`).

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/bi menu` | `buffeditems.command.menu` | Opens the main GUI editor to create, edit, and delete items. |
| `/bi give <player> <item_id> [amount]` | `buffeditems.command.give` | Gives a player the specified custom item. |
| `/bi save` | `buffeditems.command.reload` | Manually saves all changes made in the GUI to the `config.yml`. |
| `/bi reload` | `buffeditems.command.reload` | Reloads the `config.yml` from disk, discarding any unsaved changes from the GUI. |
| `/bi list` | `buffeditems.command.list` | Lists all created items and shows if any have configuration errors. |

**Admin Permission:**
* `buffeditems.admin`: Grants access to all BuffedItems commands.

**Item Permissions:**
* You can define a custom permission (e.g., `myitems.warrior_perk`) inside the item editor. If set, a player must have this permission to receive the item's effects.

<p align="center">
  <img src="https://altkat.github.io/buffeditems/requirements.jpg" alt="BuffedItems Requirements Banner"/>
</p>

This plugin requires the **Paper API** and uses Paper-exclusive events (like `AsyncChatEvent` and `PlayerArmorChangeEvent`). It **will not work on Spigot**.

* **Java 17** or newer
* **Minecraft 1.21** or newer
* One of the following server software:
    * **Paper** (Target platform)
    * **Pufferfish** (Paper fork)
    * **Purpur** (Paper fork)