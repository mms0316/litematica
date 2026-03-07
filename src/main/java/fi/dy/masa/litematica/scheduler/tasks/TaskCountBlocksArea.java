package fi.dy.masa.litematica.scheduler.tasks;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.selection.AreaSelection;

//Custom Additions (easier to resolve future merge conflicts)
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
    protected void countAtBox(net.minecraft.util.math.Box box)
    {
        List<Entity> entities = this.clientWorld.getOtherEntities(null, box);
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
        Identifier id = EntityType.getId(entityType);
        Item item = Registries.ITEM.get(id);
        if (item != null)
        {
            // Check for entity itself
            this.addItemStackToCount(new ItemStack(item), this.itemTypesTotal);

            // Check for items inside the entity

            // Minecarts with Chests / Hopper, Boats with Chests
            // - clientWorld has empty Inventory, so it's not possible to count Block Entities' inventories

            // Item Frames
            if (clientEntity instanceof ItemFrameEntity clientItemFrameEntity)
            {
                ItemStack clientHeldItem = clientItemFrameEntity.getHeldItemStack();

                this.addItemStackToCount(clientHeldItem, this.itemTypesTotal);
            }
            // Armor Stands
            else if (clientEntity instanceof ArmorStandEntity clientArmorStandEntity) {
                List<ItemStack> clientEquipments = EquipmentSlot.VALUES.stream()
                    .map(slot -> clientArmorStandEntity.getEquippedStack(slot))
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .toList();

                clientEquipments.forEach(equippedStack -> {
                    this.addItemStackToCount(equippedStack, this.itemTypesTotal);
                });
            }
        }
    }
}
