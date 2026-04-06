package com.CybaricFox.Modules.ArchMachines.UI.Pages.Common;

import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

public interface IMachineUIComponent {
    ItemContainer getContainer();

    float getProgress();

    int getSectionID();

    boolean canInsert();

    void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity);
}
