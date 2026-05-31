package com.CybaricFox.Modules.ArchTransport;

import com.CybaricFox.Modules.ArchLibrary.Direction;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3i;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class ConveyorInstance {
    public static final BuilderCodec<ConveyorInstance> CODEC;

    public ItemStack item;
    public Direction from = Direction.NOT_SET;
    public Direction to = Direction.NOT_SET;
    private int transferCooldown = 0;
    private int initialCooldown = 0;
    public UUID uuid;

    public ConveyorInstance(){

    }
    public ConveyorInstance(ItemStack item, Direction to) {
        this.item = item;
        this.to = to;
    }
    public ConveyorInstance(ItemStack item, Direction from, Direction to, int transferCooldown, UUID uuid) {
        this.item = item;
        this.from = from;
        this.to = to;
        this.transferCooldown = transferCooldown;
        this.uuid = uuid;
    }


    public int getCooldown() {
        return transferCooldown;
    }

    public void setCooldown(int timer) {
        transferCooldown = timer;
        initialCooldown = timer;
    }

    public ItemStack getItem() {
        return item;
    }

    public void decrementTimer() {
        if(transferCooldown > 0) {
            transferCooldown--;
        }
    }

    public void updateItemLocation(Vector3i pos, Vector3i target, World world) {
        if(uuid == null) return;

        double x = target.x - pos.x;
        double y = target.y - pos.y;
        double z = target.z - pos.z;

        double percent = 1 - ((double) transferCooldown / initialCooldown);

        x = x * percent;
        y = y * percent;
        z = z * percent;

        final Vector3d finalPos = new Vector3d(pos.x + x + 0.5, pos.y + y + 0.1, pos.z + z + 0.5);

        world.execute(() -> {
            WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(finalPos.x, finalPos.z));
            if(chunk == null) return;

            Ref<EntityStore> item = world.getEntityRef(uuid);
            if(item == null) {
                return;
            }

            TransformComponent transformComponent = item.getStore().getComponent(item, TransformComponent.getComponentType());
            transformComponent.setPosition(finalPos);

            ItemComponent itemComponent = item.getStore().getComponent(item, ItemComponent.getComponentType());
            itemComponent.setPickupDelay(999);
        });
    }

    public void deleteItemEntity(World world) {
        world.execute(() -> {
            Ref<EntityStore> entityStoreRef = world.getEntityRef(uuid);
            if(entityStoreRef == null) return;

            entityStoreRef.getStore().removeEntity(entityStoreRef, RemoveReason.REMOVE);

            uuid = null;
        });
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public ConveyorInstance clone() {
        return new ConveyorInstance(item, from, to, transferCooldown, uuid);
    }

    static {
        CODEC = (BuilderCodec.builder(ConveyorInstance.class, ConveyorInstance::new))
                //Common fields
                .append(new KeyedCodec<>("Item", ItemStack.CODEC), (component, s) -> component.item = s, (component) -> component.item).add()
                .append(new KeyedCodec<>("From", Codec.STRING), (component, s) -> component.from = Direction.valueOf(s.toUpperCase()), (component) -> component.from.toString()).add()
                .append(new KeyedCodec<>("To", Codec.STRING), (component, s) -> component.to = Direction.valueOf(s.toUpperCase()), (component) -> component.to.toString()).add()
                .append(new KeyedCodec<>("TransferCooldown", Codec.INTEGER), (component, s) -> component.transferCooldown = s, (component) -> component.transferCooldown).add()
                .append(new KeyedCodec<>("ItemUUID", Codec.UUID_BINARY), (component, s) -> component.uuid = s, (component) -> component.uuid).add()

                .build();
    }
}
