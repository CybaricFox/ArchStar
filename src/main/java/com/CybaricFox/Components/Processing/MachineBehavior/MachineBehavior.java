package com.CybaricFox.Components.Processing.MachineBehavior;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.UI.Pages.CommonPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

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

    public boolean run(EssentialsContext context) {
        return false;
    };
}
