package com.CybaricFox.Modules.ArchMachines.UI.HUDs;

import com.CybaricFox.Modules.ArchEnergy.Components.EnergyCableComponent;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class MultimeterHUD extends CustomUIHud {
    private String blockName = " ";
    private final String DEFAULT_MESSAGE = "Hover A Block To View Energy Data";

    public MultimeterHUD(@Nonnull PlayerRef playerRef, @Nonnull String key) {
        super(playerRef, key);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("Hud/MultimeterHUD.ui");
        update(false, uiCommandBuilder);
    }

    public void updateInfo(String blockName, EnergyComponent energyComponent, EnergyCableComponent energyCableComponent) {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();

        if(this.blockName.equals(DEFAULT_MESSAGE) && blockName.equals(DEFAULT_MESSAGE)) return;
        this.blockName = blockName;

        uiCommandBuilder.set("#TitleContainer.Text", this.blockName);

        String dataString = "";
        if(energyComponent != null) {
            ArrayList<String> data = energyComponent.getData();

            dataString = dataString.concat("Stored Energy: " + energyComponent.getCurrentEnergy() + " / " + energyComponent.getMaxEnergy() + "\n");

            for(String line : data) {
                dataString = dataString.concat(line + "\n");
            }

            uiCommandBuilder.set("#DataLabel.Text", dataString);
            update(false, uiCommandBuilder);
            return;
        }
        if(energyCableComponent != null) {
            dataString =  dataString.concat("Whoops!\n");
            dataString =  dataString.concat("It seems cables aren't fully implemented yet!\n\n");
            dataString =  dataString.concat("Try again in another update.");
            uiCommandBuilder.set("#DataLabel.Text", dataString);
            update(false, uiCommandBuilder);
            return;
        }

        uiCommandBuilder.set("#DataLabel.Text", " ");
        update(false, uiCommandBuilder);
    }

    public void clearInfo() {
        updateInfo(DEFAULT_MESSAGE, null, null);
    }
}
