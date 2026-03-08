package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import fi.dy.masa.litematica.Litematica;

public class ChunkSchematic extends LevelChunk
{
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final List<Entity> entityList = new ArrayList<>();
    private final long timeCreated;
    private final int bottomY;
    private final int topY;
    private int entityCount;
    private boolean isEmpty = true;

    public ChunkSchematic(Level worldIn, ChunkPos pos)
    {
        super(worldIn, pos);

        this.timeCreated = worldIn.getGameTime();
        this.bottomY = worldIn.getMinY();
        this.topY = worldIn.getMaxY();
        this.entityCount = 0;
    }

    @Override
    public @Nonnull BlockState getBlockState(BlockPos pos)
    {
        int x = pos.getX() & 0xF;
        int y = pos.getY();
        int z = pos.getZ() & 0xF;
        int cy = this.getSectionIndex(y);
        y &= 0xF;

        LevelChunkSection[] sections = this.getSections();

        if (cy >= 0 && cy < sections.length)
        {
            LevelChunkSection chunkSection = sections[cy];

            if (!chunkSection.hasOnlyAir())
            {
                return chunkSection.getBlockState(x, y, z);
            }
         }

         return AIR;
    }

    @Override
    public BlockState setBlockState(@Nonnull BlockPos pos, @Nonnull BlockState state, int isMoving)
    {
        BlockState stateOld = this.getBlockState(pos);
        int y = pos.getY();

        if (stateOld == state || y >= this.topY || y < this.bottomY)
        {
            return null;
        }
        else
        {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;
            int cy = this.getSectionIndex(y);

            Block blockNew = state.getBlock();
            Block blockOld = stateOld.getBlock();
            LevelChunkSection section = this.getSections()[cy];

            if (section.hasOnlyAir() && state.isAir())
            {
                return null;
            }

            y &= 0xF;

            if (state.isAir() == false)
            {
                this.isEmpty = false;
            }

            section.setBlockState(x, y, z, state);

            if (blockOld != blockNew)
            {
                this.getLevel().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (state.hasBlockEntity() && blockNew instanceof EntityBlock)
                {
                    BlockEntity te = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                    if (te == null)
                    {
                        te = ((EntityBlock) blockNew).newBlockEntity(pos, state);

                        if (te != null)
                        {
                            this.getLevel().getChunkAt(pos).setBlockEntity(te);
                        }
                    }
                }

                this.isUnsaved();

                return stateOld;
            }
        }
    }

    public AABB getBoundingBox()
    {
        final ChunkPos pos = this.getPos();
        AABB bb = new AABB(pos.getMinBlockX(), this.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX(), this.getMaxY(), pos.getMaxBlockZ());
        Litematica.debugLog("ChunkSchematic#getBoundingBox(): --> {}", bb.toString());
        return bb;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addEntity(@Nonnull Entity entity)
    {
        this.entityList.forEach(
                (ent ->
                {
                    if (ent.getUUID() == entity.getUUID() || ent.getId() == entity.getId())
                    {
                        return;
                    }
                })
        );

        this.entityList.add(entity);
        ++this.entityCount;
    }

    // TODO --> MOVE TO EntityLookup
    public List<Entity> getEntityList()
    {
        return this.entityList;
    }

    public int getEntityCount()
    {
        return this.entityCount;
    }

    public int getTileEntityCount()
    {
        return this.blockEntities.size();
    }

    protected void clearEntities()
    {
        this.entityList.clear();
        this.entityCount = 0;
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    @Override
    public boolean isEmpty()
    {
        return this.isEmpty;
    }
}
