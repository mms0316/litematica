package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

//Custom Additions (easier to resolve future merge conflicts)
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import fi.dy.masa.malilib.util.ItemType;

public abstract class TaskCountBlocksBase extends TaskProcessChunkBase
{
    protected final Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMismatch = new Object2IntOpenHashMap<>();
    protected final IMaterialList materialList;
    protected final LayerRange layerRange;

    //Custom Additions (easier to resolve future merge conflicts)
    protected final Object2IntOpenHashMap<ItemType> itemTypesTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<ItemType> itemTypesMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<ItemType> itemTypesMismatch = new Object2IntOpenHashMap<>();

    protected TaskCountBlocksBase(IMaterialList materialList, String nameOnHud)
    {
        super(nameOnHud);

        this.materialList = materialList;

        if (materialList.getMaterialListType() == BlockInfoListType.ALL)
        {
            this.layerRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
        }
        else
        {
            this.layerRange = DataManager.getRenderLayerRange();
        }
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.clientWorld, 0);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        //Custom Additions (easier to resolve future merge conflicts)
        //Merged with countBlocksInChunk()

        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == Direction.Axis.X ? Math.max(bb.minX(), range.getLayerMin()) : bb.minX();
            final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY(), range.getLayerMin()) : bb.minY();
            final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ(), range.getLayerMin()) : bb.minZ();
            final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX(), range.getLayerMax()) : bb.maxX();
            final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY(), range.getLayerMax()) : bb.maxY();
            final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ(), range.getLayerMax()) : bb.maxZ();

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }

            //Custom Additions (easier to resolve future merge conflicts)
            this.countAtBox(new AABB(startX, startY, startZ, endX + 1, endY + 1, endZ + 1));
        }

        return true;
    }

    protected abstract void countAtPosition(BlockPos pos);

    //Custom Additions (easier to resolve future merge conflicts)
    protected abstract void countAtBox(AABB box);

    @Override
    protected void onStop()
    {
        if (this.finished && this.isInWorld())
        {
            //Custom Additions (easier to resolve future merge conflicts)
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    MaterialListUtils.fromBlockStateCount(this.countsTotal, this.itemTypesTotal),
                    MaterialListUtils.fromBlockStateCount(this.countsMissing, this.itemTypesMissing),
                    MaterialListUtils.fromBlockStateCount(this.countsMismatch, this.itemTypesMismatch),
                    this.mc.player);
            this.materialList.setMaterialListEntries(list);
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }

    //Custom Additions (easier to resolve future merge conflicts)
    protected void addItemStackToCount(ItemStack itemStack, Object2IntOpenHashMap<ItemType> itemTypeCountMap)
    {
        if (itemStack == null || itemStack.isEmpty()) return;

        ItemType itemType = new ItemType(itemStack, false, false);
        itemTypeCountMap.addTo(itemType, itemStack.getCount());
    }

}
