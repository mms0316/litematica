package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.InventoryUtils;

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

            BlockEntity schematicBlockEntity = this.schematicWorld.getBlockEntity(pos);
            if (schematicBlockEntity instanceof Inventory schematicInventory)
            {
                schematicInventory.forEach(itemStack -> addItemStackToCount(itemStack, this.itemTypesTotal));
                BlockEntity clientBlockEntity = this.clientWorld.getBlockEntity(pos);
                if (!(clientBlockEntity instanceof Inventory))
                {
                    schematicInventory.forEach(itemStack -> addItemStackToCount(itemStack, this.itemTypesMissing));
                }
                // clientWorld has empty Inventory, so it's not possible to compare
            }
        }
    }

    @Override
    protected void countAtBox(net.minecraft.util.math.Box box)
    {
        Map<UUID, Entity> schematicEntities = new HashMap<>();

        List<Entity> entities = this.schematicWorld.getOtherEntities(null, box);
        if (entities != null && !entities.isEmpty())
        {
            for (Entity entity : entities)
            {
                // Mark entities processed, because they may reside in multiple chunks
                if (entity.getUuid() != null && schematicEntities.containsKey(entity.getUuid()))
                    continue;
                schematicEntities.put(entity.getUuid(), entity);

                this.countEntity(entity);
            }
        }
    }

    protected void countEntity(Entity schematicEntity)
    {
        EntityType<?> entityType = schematicEntity.getType();
        Identifier id = EntityType.getId(entityType);
        Item item = Registries.ITEM.get(id);
        if (item != null)
        {
            // Check for entity itself
            ItemStack entityItemStack = new ItemStack(item);

            this.addItemStackToCount(entityItemStack, this.itemTypesTotal);

            boolean entityMissing = false;

            List<Entity> clientEntities = this.clientWorld.getOtherEntities(null, schematicEntity.getBoundingBox().expand(0.1));
            Stream<Entity> clientMatchingEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType);
            if (clientMatchingEntities.count() == 0)
            {
                entityMissing = true;

                this.addItemStackToCount(entityItemStack, this.itemTypesMissing);
            }

            // Check for items inside the entity

            // Minecarts with Chests / Hopper, Boats with Chests
            if (schematicEntity instanceof Inventory schematicInventory) {
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

                List<Entity> clientInventoryEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof Inventory).toList();

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
            else if (schematicEntity instanceof ItemFrameEntity schematicItemFrameEntity)
            {
                ItemStack schematicHeldItem = schematicItemFrameEntity.getHeldItemStack();

                this.addItemStackToCount(schematicHeldItem, this.itemTypesTotal);
                if (entityMissing)
                {
                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMissing);
                    return;
                }

                List<Entity> clientItemFrameEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof ItemFrameEntity).toList();

                if (clientItemFrameEntities.size() != 1)
                {
                    // No matching inventory found in client world, or cannot compare with multiple matches
                    this.addItemStackToCount(schematicHeldItem, this.itemTypesMissing);
                }
                else if (clientItemFrameEntities.get(0) instanceof ItemFrameEntity clientItemFrameEntity) {
                    ItemStack clientHeldItem = clientItemFrameEntity.getHeldItemStack();

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
            else if (schematicEntity instanceof ArmorStandEntity schematicArmorStandEntity) {
                List<ItemStack> schematicEquipments = EquipmentSlot.VALUES.stream()
                    .map(slot -> schematicArmorStandEntity.getEquippedStack(slot))
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

                List<Entity> clientArmorStandEntities = clientEntities.stream().filter(entity -> entity.getType() == entityType && entity instanceof ArmorStandEntity).toList();
                if (clientArmorStandEntities.size() != 1)
                {
                    // No matching inventory found in client world, or cannot compare with multiple matches
                    schematicEquipments.forEach(equippedStack -> {
                        this.addItemStackToCount(equippedStack, this.itemTypesMissing);
                    });
                }
                else if (clientArmorStandEntities.get(0) instanceof ArmorStandEntity clientArmorStandEntity)
                {
                    for (EquipmentSlot slot : EquipmentSlot.VALUES) {
                        ItemStack stackSchematic = schematicArmorStandEntity.getEquippedStack(slot);
                        ItemStack stackClient = clientArmorStandEntity.getEquippedStack(slot);

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
