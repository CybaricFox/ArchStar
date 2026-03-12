package com.CybaricFox.Components.Blocks;

import com.CybaricFox.API.Direction;
import com.CybaricFox.API.DirectionLibrary;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorImporter;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorInstance;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorRouter;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class ConveyorComponent implements Component<ChunkStore> {
    public static final BuilderCodec<ConveyorComponent> CODEC;
    //List of all items in this conveyor
    private ArrayList<ConveyorInstance> items = new ArrayList<>();
    //Forward direction of this conveyor
    private Direction forwardDirection = Direction.NOT_SET;
    //The block location of the target
    private Vector3i targetBlock;
    //Type of conveyor, controls how it functions
    private ConveyorType type = ConveyorType.NOT_SET;
    //A multiplier to the timer
    //Tier I = 1.0 (5s by default)
    //Tier II =
    //Tier III =
    private float speedMultiplier = 1.0f;
    //Is valid is true when the target block and forward direction are valid values.
    public boolean isValid = false;
    //Data related to the importer
    private ConveyorImporter importerData = null;
    //Data related to routers
    private ConveyorRouter routerData = null;
    //Max size of this conveyor DEFAULT: 100. Can be overriden in the json.
    private int maxSize = 100;

    public ConveyorComponent() {

    }

    public ConveyorComponent(ConveyorType type, float speedMultiplier, ArrayList<ConveyorInstance> items, Direction forwardDirection, Vector3i targetBlock, int maxSize) {
        this.type = type;
        this.speedMultiplier = speedMultiplier;
        this.items = new ArrayList<>(items);
        this.forwardDirection = forwardDirection;
        this.targetBlock = targetBlock;
        this.maxSize = maxSize;

        checkValidity();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ConveyorComponent(type, speedMultiplier, items, forwardDirection, targetBlock, maxSize);
    }

    public ConveyorType getType() {
        return type;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setTarget(Vector3i targetBlock) {
        this.targetBlock = targetBlock;
        checkValidity();
    }

    public void setForwardDirection(Direction direction) {
        forwardDirection = direction;
        checkValidity();
    }

    public void setImporterData(Vector3i pos, boolean reverse) {
        if(!isValid || pos == null) return;
        importerData = new ConveyorImporter(reverse ? DirectionLibrary.getBackwardDirection(forwardDirection) : forwardDirection, pos);
    }

    public void setRouterData(Vector3i pos, World world) {
        if(pos == null || world == null) return;
        routerData = new ConveyorRouter(pos, world);
    }

    public ConveyorImporter getImportData() {
        if(importerData == null) return null;
        return importerData;
    }

    public ConveyorRouter getRouterData() {
        if(routerData == null) return null;
        return routerData;
    }

    public Direction getForwardDirection() {
        return forwardDirection;
    }

    public void addItem(ConveyorInstance instance) {
        if(items.size() >= maxSize) return;

        instance.setCooldown(getTimer());
        items.add(instance);
    }

    private void checkValidity() {
        isValid = targetBlock != null && forwardDirection != Direction.NOT_SET;

        if(getType() == ConveyorType.ROUTER) {
            isValid = true;
        }
    }

    public boolean canAddItem() {
        return items.size() < maxSize;
    }

    public int getTimer() {
        //The base timer value. 150 = 5 seconds
        return Math.round(150 * speedMultiplier);
    }

    public Vector3i getTargetBlock() {
        return targetBlock;
    }

    public ArrayList<ConveyorInstance> getAllInstances() {
        return items;
    }

    public ArrayList<ConveyorInstance> decrementTimers() {
        ArrayList<ConveyorInstance> readyInstances = new ArrayList<>();
        for(ConveyorInstance instance : items) {
            instance.decrementTimer();
            if(instance.getCooldown() == 0) {
                readyInstances.add(instance);
            }
        }

        return readyInstances;
    }

    public void removeReadyItems() {
        ArrayList<ConveyorInstance> remaining = new ArrayList<>();

        for (ConveyorInstance item : items) {
            if (item.getCooldown() > 0) {
                remaining.add(item);
            }
        }

        clearItems();
        items = remaining;
    }

    public void clearItems() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    static {

        CODEC = (BuilderCodec.builder(ConveyorComponent.class, ConveyorComponent::new))
                //Required fields
                .append(new KeyedCodec<>("Type", Codec.STRING), (component, s) -> component.type = ConveyorType.valueOf(s.toUpperCase()), (component) -> component.type.toString()).add()
                .append(new KeyedCodec<>("SpeedMultiplier", Codec.FLOAT), (component, s) -> component.speedMultiplier = s, (component) -> component.speedMultiplier).add()

                //Optional overrides
                .append(new KeyedCodec<>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()

                //Save data
                .append(new KeyedCodec<>("ItemList", new ArrayCodec<>(ConveyorInstance.CODEC, ConveyorInstance[]::new)), (component, s) -> component.items = new ArrayList<>(Arrays.asList(s)), (component) -> component.items.toArray(ConveyorInstance[]::new)).add()
                .append(new KeyedCodec<>("ForwardDirection", Codec.STRING), (component, s) -> component.forwardDirection = Direction.valueOf(s.toUpperCase()), (component) -> component.forwardDirection.toString()).add()
                .append(new KeyedCodec<>("TargetBlock", Vector3i.CODEC), (component, s) -> component.targetBlock = s, (component) -> component.targetBlock).add()

                .build();
    }
}
