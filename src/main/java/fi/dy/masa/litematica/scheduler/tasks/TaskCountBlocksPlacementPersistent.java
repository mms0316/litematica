//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.util.profiling.ProfilerFiller;

public class TaskCountBlocksPlacementPersistent extends TaskCountBlocksPlacement
{
    private final Map<BlockPos, BlockState> schematicWorldView = new HashMap<>();
    private final Map<BlockPos, BlockState> clientWorldView = new HashMap<>();
    private final Map<UUID, Entity> schematicEntities = new HashMap<>();
    private ArrayList<ChunkPos> schematicChunkPos;

    private long lastUpdateTime;

    public TaskCountBlocksPlacementPersistent(SchematicPlacement schematicPlacement, IMaterialList materialList)
    {
        this(schematicPlacement, materialList, false);
    }

    public TaskCountBlocksPlacementPersistent(SchematicPlacement schematicPlacement, IMaterialList materialList, boolean ignoreState)
    {
        super(schematicPlacement, materialList, ignoreState);

        // Keep a backup to filter chunks when receiving chunk data packets
        this.schematicChunkPos = new ArrayList<>(this.pendingChunks);
    }

    @Override
    protected void countAtPosition(BlockPos pos)
    {
        BlockState stateSchematic = this.schematicWorldView.get(pos);
        if (stateSchematic == null) return;

        BlockState stateClient = this.clientWorld.getBlockState(pos);
        if (stateClient == null || stateClient.isAir()) return;

        this.clientWorldView.put(new BlockPos(pos), stateClient);
    }

    @Override
    protected void countAtBox(AABB box)
    {
        // Done in execute()
    }

    @Override
    public boolean execute(ProfilerFiller profiler)
    {
        // Process the schematic world
        // This can't be done once in the constructor because the schematic world may not be loaded yet
        // (e.g. when the placement is far from the player when running the material list)
        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();

        var chunkManager = this.schematicWorld.getChunkSource();
        var schematicPlacementManager = DataManager.getSchematicPlacementManager();

        for (int chunkIndex = 0; chunkIndex < this.pendingChunks.size(); ++chunkIndex)
        {
            ChunkPos chunkPos = this.pendingChunks.get(chunkIndex);

            if (!chunkManager.hasChunk(chunkPos.x, chunkPos.z))
            {
                schematicPlacementManager.markChunkForRebuild(chunkPos);
                continue;
            }

            for (IntBoundingBox bb : this.getBoxesInChunk(chunkPos))
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
                            BlockPos blockPos = new BlockPos(x, y, z);

                            BlockState stateSchematic = this.schematicWorld.getBlockState(blockPos);

                            if (stateSchematic.isAir())
                                continue;
                            
                            if (Configs.Generic.MATERIAL_LIST_AVOID_BEACONS.getBooleanValue() &&
                                DataManager.getBeaconManager().checkIfObstructs(blockPos, stateSchematic))
                                continue;

                            this.schematicWorldView.put(blockPos, stateSchematic);
                        }
                    }
                }

                List<Entity> entities = this.schematicWorld.getEntities(null, new AABB(startX, startY, startZ, endX + 1, endY + 1, endZ + 1));
                if (entities != null && !entities.isEmpty())
                {
                    for (Entity entity : entities)
                    {
                        // Mark entities processed, because they may reside in multiple chunks
                        if (entity.getUUID() != null && this.schematicEntities.containsKey(entity.getUUID()))
                            continue;
                        this.schematicEntities.put(entity.getUUID(), entity);
                    }
                }
            }
        }


        // Count blocks in the client world
        super.execute(profiler);

        // Refresh counts
        final long currentTime = System.currentTimeMillis();
        final long interval = Configs.Generic.MATERIAL_LIST_PLACEMENT_PERSISTENT_DELAY.getIntegerValue();
        if (currentTime - this.lastUpdateTime > interval)
        {
            this.countsTotal.clear();
            this.countsMissing.clear();
            this.countsMismatch.clear();
            this.itemTypesTotal.clear();
            this.itemTypesMissing.clear();
            this.itemTypesMismatch.clear();
    
            for (BlockPos pos : this.schematicWorldView.keySet())
            {
                BlockState stateSchematic = this.schematicWorldView.get(pos);
                BlockState stateClient = this.clientWorldView.get(pos);
    
                this.countsTotal.addTo(stateSchematic, 1);
   
                if (stateClient == null)
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
                if (schematicBlockEntity instanceof Container schematicInventory)
                {
                    schematicInventory.forEach(itemStack -> this.addItemStackToCount(itemStack, this.itemTypesTotal));
                    BlockEntity clientBlockEntity = this.clientWorld.getBlockEntity(pos);
                    if (!(clientBlockEntity instanceof Container))
                    {
                        schematicInventory.forEach(itemStack -> this.addItemStackToCount(itemStack, this.itemTypesMissing));
                    }
                    // clientWorld has empty Container, so it's not possible to compare
                }
            }

            for (Entity schematicEntity : this.schematicEntities.values())
            {
                this.countEntity(schematicEntity);
            }
    
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    MaterialListUtils.fromBlockStateCount(this.countsTotal, this.itemTypesTotal),
                    MaterialListUtils.fromBlockStateCount(this.countsMissing, this.itemTypesMissing),
                    MaterialListUtils.fromBlockStateCount(this.countsMismatch, this.itemTypesMismatch),
                    this.mc.player);
            this.materialList.setMaterialListEntries(list);

            this.lastUpdateTime = System.currentTimeMillis();
        }

        // Keep executing, so that block changes may reflect in the material list
        return false;
    }

    public boolean hasPendingChunks()
    {
        return this.pendingChunks.isEmpty() == false;
    }

    public void onChunkData(ChunkPos pos)
    {
        if (canExecute() == false) return;

        if (this.schematicChunkPos.contains(pos) == false)
            return;

        if (this.pendingChunks.contains(pos) == false)
            this.pendingChunks.add(pos);
    }

    public void onBlockUpdate(BlockPos pos, BlockState state)
    {
        if (canExecute() == false) return;

        if (state == null || state.isAir())
        {
            this.clientWorldView.remove(pos);
        }
        else
        {
            this.clientWorldView.put(pos, state);
        }
    }
}
