package com.CybaricFox.Modules.ArchMachines.MachineBehavior;

import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.CommonPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
//import org.joml.Vector3i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public abstract class MachineBehavior {
    private Class<? extends CommonPage> pageRef;

    public void setPageRef(Class<? extends CommonPage> pageRef) {
        this.pageRef = pageRef;
    }

    public CommonPage getPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonPage.CommonData> eventDataCodec, Vector3i pos){
        try {
            return pageRef.getDeclaredConstructor(PlayerRef.class, BuilderCodec.class, Vector3i.class).newInstance(playerRef, eventDataCodec, pos);
        } catch (Exception e) {
            throw new RuntimeException("Attempted to create a new Common UI Page but something went wrong: " + e);
        }
    }

    public abstract boolean run(EssentialsContext context);
}
