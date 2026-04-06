package com.CybaricFox.Modules.ArchMachines.UI.Pages.Common;

import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchLibrary.IDataPanel;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;
import java.util.logging.Level;

public class UIComponentContext {
    public String name;
    public ItemContainer container;
    public float progress = -1.0f;
    public boolean canInsert;
    public int sectionID = 0;
    private IMachineUIComponent component;
    private IDataPanel data;

    //Note that sectionID 0-99 are free. 100+ are dynamically assigned to components.
    public UIComponentContext(String name, ItemContainer container, boolean canInsert, int sectionID) {
        this.name = name;
        this.container = container;
        this.canInsert = canInsert;
        this.sectionID = sectionID;
    }

    public UIComponentContext(String name, IMachineUIComponent component, IDataPanel data) {
        this.component = component;
        this.name = name;
        if(component != null) {
            container = component.getContainer();
            progress = component.getProgress();
            canInsert = component.canInsert();
            sectionID = component.getSectionID();
        }
        this.data = data;
    }

    public float updateProgress() {
        if(progress < 0) return -1.0f;
        if(component == null) {
            ArchStar.LOGGER.at(Level.WARNING).log("A UI Component Context is attempting to update its progress but no component was ever assigned." +
                    " Components that need progress updates must use the constructor with an IMachineUIComponent argument!");
            return -1.0f;
        }

        progress = component.getProgress();
        return progress;
    }

    public void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity) {
        if(component == null) return;
        component.onDrop(sender, receiver, senderSlot, receiverSlot, quantity);
    }

    public ArrayList<String> getData() {
        if(data == null) return null;
        return data.getData();
    }
}
