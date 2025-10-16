package io.github.altkat.BuffedItems.Menu;

import org.bukkit.entity.Player;


public class PlayerMenuUtility {

    private Player owner;

    private String itemToEditId;

    public PlayerMenuUtility(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    public String getItemToEditId() {
        return itemToEditId;
    }

    public void setItemToEditId(String itemToEditId) {
        this.itemToEditId = itemToEditId;
    }
}