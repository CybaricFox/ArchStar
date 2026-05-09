package com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors;

import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.UpgradeStationPage;


public class UpgradeStation extends MachineBehavior {

    public UpgradeStation(String id) {
        super(id);
        setPageRef(UpgradeStationPage.class);
    }

    @Override
    public boolean run(EssentialsContext context) {
        return true;
    }

    @Override
    public MachineBehavior createInstance() {
        return new UpgradeStation(getId());
    }
}
