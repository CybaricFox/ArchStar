package com.CybaricFox.Modules.ArchMachines.Systems;

import com.CybaricFox.Modules.ArchEnergy.Components.EnergyCableComponent;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.DirectionLibrary;
import com.CybaricFox.Modules.ArchMachines.UI.HUDs.MultimeterHUD;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.UUID;

public class MultimeterSystem extends TickingSystem<EntityStore> {
    private static final ArrayList<UUID> players = new ArrayList<>();

    public static void handlePlayer(UUID uuid) {
        if(players.contains(uuid)) {
            removePlayer(uuid);
        }
        else {
            addPlayer(uuid);
        }
    }

    private static void removePlayer(UUID uuid) {
        players.remove(uuid);
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if(playerRef == null) return;

        Player player = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        player.getHudManager().removeCustomHud(playerRef, "Multimeter");
    }

    private static void addPlayer(UUID uuid) {
        players.add(uuid);
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if(playerRef == null) return;

        Player player = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        player.getHudManager().addCustomHud(playerRef, new MultimeterHUD(playerRef, "Multimeter"));
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<EntityStore> store) {
        if(players.isEmpty()) return;

        ArrayList<UUID> toRemove = new ArrayList<>();

        for(UUID uuid : players) {
            PlayerRef playerRef = Universe.get().getPlayer(uuid);
            if(playerRef == null) {
                toRemove.add(uuid);
                continue;
            }

            InventoryComponent hotbar = store.getComponent(playerRef.getReference(), InventoryComponent.getComponentTypeById(-1));
            byte activeSlot = InventoryUtils.getActiveSlot(playerRef.getReference(), -1, store);

            ItemStack item = hotbar.getInventory().getItemStack(activeSlot);

            if(item == null || !item.getItemId().equals("Multimeter")) {
                toRemove.add(uuid);
                continue;
            }

            UUID worldUUID = playerRef.getWorldUuid();
            if(worldUUID == null) continue;

            World world = Universe.get().getWorld(worldUUID);

            Vector3i targetBlock = DirectionLibrary.raycastBlockFromPlayer(playerRef.getReference(), world);
            if(targetBlock == null) continue;

            MultimeterHUD multimeterHUD = getHud(store, playerRef);
            if(multimeterHUD == null) {
                toRemove.add(uuid);
                continue;
            }

            Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, targetBlock);
            if(blockRef == null) {
                multimeterHUD.clearInfo();
                continue;
            }

            EnergyComponent energyComponent = blockRef.getStore().getComponent(blockRef, EnergyComponent.getComponentType());
            EnergyCableComponent energyCableComponent = blockRef.getStore().getComponent(blockRef, EnergyCableComponent.getComponentType());

            if(energyComponent == null && energyCableComponent == null) {
                multimeterHUD.clearInfo();
                continue;
            }

            BlockType blockType = world.getBlockType(targetBlock);
            if(blockType == null) continue;

            multimeterHUD.updateInfo(blockType.getItem().getId(), energyComponent, energyCableComponent);
        }

        for(UUID uuid : toRemove) {
            removePlayer(uuid);
        }
    }

    private MultimeterHUD getHud(Store<EntityStore> store, PlayerRef playerRef) {
        Player player = store.getComponent(playerRef.getReference(), Player.getComponentType());
        if(player == null) return null;
        CustomUIHud hud = player.getHudManager().getCustomHud("Multimeter");
        if(hud == null) return null;

        if(hud instanceof MultimeterHUD multimeterHUD) return multimeterHUD;

        return null;
    }
}
