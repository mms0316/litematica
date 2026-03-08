package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
//Custom Additions (easier to resolve future merge conflicts)
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TaskCountBlocksPlacement extends TaskCountBlocksBase
{
    protected final SchematicPlacement schematicPlacement;
    protected final boolean ignoreState;

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(schematicPlacement, materialList, false);
    }

    public TaskCountBlocksPlacement(SchematicPlacement schematicPlacement, IMaterialList materialList, boolean ignoreState)
    {
        super(materialList, "litematica.gui.label.task_name.material_list");

        this.schematicPlacement = schematicPlacement;
        this.ignoreState = ignoreState;
        Collection<Box> boxes = schematicPlacement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values();

        // Filter/clamp the boxes to intersect with the render layer
        if (materialList.getMaterialListType() == BlockInfoListType.RENDER_LAYERS)
        {
            this.addPerChunkBoxes(boxes, DataManager.getRenderLayerRange());
        }
        else
        {
            this.addPerChunkBoxes(boxes);
        }

    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.schematicWorld.getBlockState(pos);

        if (stateSchematic.isAir() == false)
        {

            //Custom Additions (easier to resolve future merge conflicts)
            if (Configs.Generic.MATERIAL_LIST_AVOID_BEACONS.getBooleanValue() &&
                    DataManager.getBeaconManager().checkIfObstructs(pos, stateSchematic))
                return;

            BlockState stateClient = this.clientWorld.getBlockState(pos);

            this.countsTotal.addTo(stateSchematic, 1);

            if (stateClient.isAir())
            {
                this.countsMissing.addTo(stateSchematic, 1);
            }
            else if (stateClient != stateSchematic &&
                    (this.ignoreState == false || stateClient.getBlock() != stateSchematic.getBlock()))
            {
                this.countsMissing.addTo(stateSchematic, 1);
                this.countsMismatch.addTo(stateSchematic, 1);
            }

            //Custom Additions (easier to resolve future merge conflicts)
            BlockEntity schematicBlockEntity = this.schematicWorld.getBlockEntity(pos);
            if (schematicBlockEntity instanceof Container schematicInventory)
            {
                schematicInventory.forEach(itemStack -> addItemStackToCount(itemStack, this.itemTypesTotal));
                BlockEntity clientBlockEntity = this.clientWorld.getBlockEntity(pos);
                if (!(clientBlockEntity instanceof Container))
                {
                    schematicInventory.forEach(itemStack -> addItemStackToCount(itemStack, this.itemTypesMissing));
                }
                // clientWorld has empty Container, so it's not possible to compare
            }
        }
    }

    //Custom Additions (easier to resolve future merge conflicts)
    @Override
    protected void countAtBox(AABB box)
    {
        Map<UUID, Entity> schematicEntities = new HashMap<>();

        List<Entity> entities = this.schematicWorld.getEntities(null, box);
        if (entities != null && !entities.isEmpty())
        {
            for (Entity entity : entities)
            {
                // Mark entities processed, because they may reside in multiple chunks
                if (entity.getUUID() != null && schematicEntities.containsKey(entity.getUUID()))
                    continue;
                schematicEntities.put(entity.getUUID(), entity);

                this.countEntity(entity);
            }
        }
    }

    protected void countEntity(Entity schematicEntity)
    {
        EntityType<?> entityType = schematicEntity.getType();
        Identifier id = EntityType.getKey(entityType);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item != null)
        {
            // Check for entity itself
            ItemStack entityItemStack = new ItemStack(item);

            this.addItemStackToCount(entityItemStack, this.itemTypesTotal);

            boolean entityMissing = false;

            List<Entity> clientEntities = this.clientWorld.getEntities(null, schematicEntity.getBoundingBox().inflate(0.1));
            Stream<Entity> clientMatchingEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType);
            if (clientMatchingEntities.count() == 0)
            {
                entityMissing = true;

                this.addItemStackToCount(entityItemStack, this.itemTypesMissing);
            }

            // Check for items inside the entity

            // Minecarts with Chests / Hopper, Boats with Chests
            if (schematicEntity instanceof Container schematicInventory) {
                schematicInventory.forEach(itemStack -> {
                    this.addItemStackToCount(itemStack, this.itemTypesTotal);
                });

                if (entityMissing)
                {
                    schematicInventory.forEach(itemStack -> {
                        this.addItemStackToCount(itemStack, this.itemTypesMissing);
                    });
                    return;
                }

                List<Entity> clientInventoryEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof Container).toList();

                if (clientInventoryEntities.size() != 1)
                {
                    // No matching inventory found in client world, or cannot compare with multiple matches
                    schematicInventory.forEach(itemStack -> {
                        this.addItemStackToCount(itemStack, this.itemTypesMissing);
                    });
                }
                // clientWorld has empty Inventory, so it's not possible to compare
            }
            // Item Frames
            else if (schematicEntity instanceof ItemFrame schematicItemFrameEntity)
            {
                ItemStack schematicHeldItem = schematicItemFrameEntity.getItem();

                this.addItemStackToCount(schematicHeldItem, this.itemTypesTotal);
                if (entityMissing)
                {
                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMissing);
                    return;
                }

                List<Entity> clientItemFrameEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof ItemFrame).toList();

                if (clientItemFrameEntities.size() != 1)
                {
                    // No matching inventory found in client world, or cannot compare with multiple matches
                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMissing);
                }
                else if (clientItemFrameEntities.get(0) instanceof ItemFrame clientItemFrameEntity) {
                    ItemStack clientHeldItem = clientItemFrameEntity.getItem();

                    if (this.ignoreState)
                    {
                        if (InventoryUtils.areStacksEqualIgnoreNbt(schematicHeldItem, clientHeldItem))
                            return;
                    }
                    else
                    {
                        if (InventoryUtils.areStacksAndNbtEqual(schematicHeldItem, clientHeldItem))
                            return;
                    }

                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMissing);
                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMismatch);
                }
            }
            // Armor Stands
            else if (schematicEntity instanceof ArmorStand schematicArmorStandEntity) {
                List<ItemStack> schematicEquipments = EquipmentSlot.VALUES.stream()
                    .map(slot -> schematicArmorStandEntity.getItemBySlot(slot))
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .toList();

                schematicEquipments.forEach(equippedStack -> {
                    this.addItemStackToCount(equippedStack, this.itemTypesTotal);
                });

                if (entityMissing)
                {
                    schematicEquipments.forEach(equippedStack -> {
                        this.addItemStackToCount(equippedStack, this.itemTypesMissing);
                    });
                    return;
                }

                List<Entity> clientArmorStandEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof ArmorStand).toList();
                if (clientArmorStandEntities.size() != 1)
                {
                    // No matching inventory found in client world, or cannot compare with multiple matches
                    schematicEquipments.forEach(equippedStack -> {
                        this.addItemStackToCount(equippedStack, this.itemTypesMissing);
                    });
                }
                else if (clientArmorStandEntities.get(0) instanceof ArmorStand clientArmorStandEntity)
                {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        ItemStack stackSchematic = schematicArmorStandEntity.getItemBySlot(slot);
                        ItemStack stackClient = clientArmorStandEntity.getItemBySlot(slot);

                        if (this.ignoreState)
                        {
                            if (InventoryUtils.areStacksEqualIgnoreNbt(stackSchematic, stackClient))
                                continue;
                        }
                        else
                        {
                            if (InventoryUtils.areStacksAndNbtEqual(stackSchematic, stackClient))
                                continue;
                        }

                        this.addItemStackToCount(stackSchematic, this.itemTypesMissing);
                        this.addItemStackToCount(stackSchematic, this.itemTypesMismatch);
                    }
                }
            }
        }
    }
}
