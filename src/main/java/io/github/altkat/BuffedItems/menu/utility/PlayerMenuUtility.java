package io.github.altkat.BuffedItems.menu.utility;

import org.bukkit.Material;
import org.bukkit.entity.Player;


public class PlayerMenuUtility {

    private final Player owner;
    private String itemToEditId;
    private String targetSlot;
    private int editIndex = -1;

    private boolean waitingForChatInput = false;
    private String chatInputPath;
    private String attributeToEdit;
    private boolean showPreviewDetails = false;
    private String tempId;
    private Material tempMaterial;
    private String tempSetId;
    private int tempBonusCount;
    private MaterialSelectionContext materialContext = MaterialSelectionContext.ICON;

    public enum MaterialSelectionContext {
        ICON,
        COST,
        INGREDIENT
    }


    public PlayerMenuUtility(Player owner) {
        this.owner = owner;
    }

    public Material getTempMaterial() {
        return tempMaterial;
    }

    public void setTempMaterial(Material tempMaterial) {
        this.tempMaterial = tempMaterial;
    }

    public MaterialSelectionContext getMaterialContext() {
        return materialContext;
    }

    public void setMaterialContext(MaterialSelectionContext materialContext) {
        this.materialContext = materialContext;
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

    public String getTargetSlot() {
        return targetSlot;
    }

    public void setTargetSlot(String targetSlot) {
        this.targetSlot = targetSlot;
    }

    public int getEditIndex() {
        return editIndex;
    }

    public void setEditIndex(int editIndex) {
        this.editIndex = editIndex;
    }

    public boolean isShowPreviewDetails() {
        return showPreviewDetails;
    }

    public void toggleShowPreviewDetails() {
        this.showPreviewDetails = !this.showPreviewDetails;
    }

    public String getTempId() {
        return tempId;
    }

    public void setTempId(String tempId) {
        this.tempId = tempId;
    }

    public String getTempSetId() { return tempSetId; }
    public void setTempSetId(String tempSetId) { this.tempSetId = tempSetId; }

    public int getTempBonusCount() { return tempBonusCount; }
    public void setTempBonusCount(int tempBonusCount) { this.tempBonusCount = tempBonusCount; }
}