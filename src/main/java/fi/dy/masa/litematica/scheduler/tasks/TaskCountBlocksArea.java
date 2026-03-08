package fi.dy.masa.litematica.scheduler.tasks;

import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
//Custom Additions (easier to resolve future merge conflicts)
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.List;

public class TaskCountBlocksArea extends TaskCountBlocksBase
{
    public TaskCountBlocksArea(AreaSelection selection, IMaterialList materialList)
    {
        super(materialList, "litematica.gui.label.task_name.area_analyzer");

        this.addPerChunkBoxes(selection.getAllSubRegionBoxes());
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateClient = this.clientWorld.getBlockState(pos);
        this.countsTotal.addTo(stateClient, 1);

        // clientWorld has empty Inventory, so it's not possible to count Block Entities' inventories
    }

    //Custom Additions (easier to resolve future merge conflicts)
    @Override
    protected void countAtBox(AABB box)
    {
        List<Entity> entities = this.clientWorld.getEntities(null, box);
        if (entities != null && !entities.isEmpty())
        {
            for (Entity entity : entities)
            {
                countEntity(entity);
            }
        }
    }

    private void countEntity(Entity clientEntity)
    {
        EntityType<?> entityType = clientEntity.getType();
        Identifier id = EntityType.getKey(entityType);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item != null)
        {
            // Check for entity itself
            this.addItemStackToCount(new ItemStack(item), this.itemTypesTotal);

            // Check for items inside the entity

            // Minecarts with Chests / Hopper, Boats with Chests
            // - clientWorld has empty Inventory, so it's not possible to count Block Entities' inventories

            // Item Frames
            if (clientEntity instanceof ItemFrame clientItemFrameEntity)
            {
                ItemStack clientHeldItem = clientItemFrameEntity.getItem();

                this.addItemStackToCount(clientHeldItem, this.itemTypesTotal);
            }
            // Armor Stands
            else if (clientEntity instanceof ArmorStand clientArmorStandEntity) {
                List<ItemStack> clientEquipments = EquipmentSlot.VALUES.stream()
                    .map(slot -> clientArmorStandEntity.getItemBySlot(slot))
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .toList();

                clientEquipments.forEach(equippedStack -> {
                    this.addItemStackToCount(equippedStack, this.itemTypesTotal);
                });
            }
        }
    }
}
