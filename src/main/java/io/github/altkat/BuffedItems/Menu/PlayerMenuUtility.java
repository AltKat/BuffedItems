package io.github.altkat.BuffedItems.Menu;

import org.bukkit.entity.Player;


public class PlayerMenuUtility {

    private Player owner;

    private String itemToEditId;

    private boolean waitingForChatInput = false;
    private String chatInputPath;
    private String attributeToEdit;

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

    public boolean isWaitingForChatInput() {
        return waitingForChatInput;
    }

    public void setWaitingForChatInput(boolean waitingForChatInput) {
        this.waitingForChatInput = waitingForChatInput;
    }

    public String getChatInputPath() {
        return chatInputPath;
    }

    public void setChatInputPath(String chatInputPath) {
        this.chatInputPath = chatInputPath;
    }

    public String getAttributeToEdit() {
        return attributeToEdit;
    }

    public void setAttributeToEdit(String attributeToEdit) {
        this.attributeToEdit = attributeToEdit;
    }
}