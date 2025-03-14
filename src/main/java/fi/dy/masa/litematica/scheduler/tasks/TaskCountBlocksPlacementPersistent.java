package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.profiler.Profiler;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;

public class TaskCountBlocksPlacementPersistent extends TaskCountBlocksPlacement
{
    private final Map<BlockPos, BlockState> schematicWorldView = new HashMap<>();
    private final Map<BlockPos, BlockState> clientWorldView = new HashMap<>();
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
    public boolean execute(Profiler profiler)
    {
        // Process the schematic world
        // This can't be done once in the constructor because the schematic world may not be loaded yet
        // (e.g. when the placement is far from the player when running the material list)
        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();

        var chunkManager = this.schematicWorld.getChunkManager();
        var schematicPlacementManager = DataManager.getSchematicPlacementManager();

        for (int chunkIndex = 0; chunkIndex < this.pendingChunks.size(); ++chunkIndex)
        {
            ChunkPos chunkPos = this.pendingChunks.get(chunkIndex);

            if (!chunkManager.isChunkLoaded(chunkPos.x, chunkPos.z))
            {
                schematicPlacementManager.markChunkForRebuild(chunkPos);
                continue;
            }

            for (IntBoundingBox bb : this.getBoxesInChunk(chunkPos))
            {
                final int startX = axis == Direction.Axis.X ? Math.max(bb.minX, range.getLayerMin()) : bb.minX;
                final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY, range.getLayerMin()) : bb.minY;
                final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ, range.getLayerMin()) : bb.minZ;
                final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX, range.getLayerMax()) : bb.maxX;
                final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY, range.getLayerMax()) : bb.maxY;
                final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ, range.getLayerMax()) : bb.maxZ;

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
            }
    
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    this.countsTotal, this.countsMissing, this.countsMismatch, this.mc.player);
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
