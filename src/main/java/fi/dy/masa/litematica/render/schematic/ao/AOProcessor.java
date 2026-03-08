package fi.dy.masa.litematica.render.schematic.ao;

import fi.dy.masa.litematica.config.Configs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AOProcessor extends AOLightmap
{
	public static final Direction[] DIRECTIONS = Direction.values();

    public static AOProcessor get()
    {
        if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
        {
            return new AOProcessorModern();
        }
        else
        {
            return new AOProcessorLegacy();
        }
    }

    public void apply(BlockAndTintGetter world, BlockState state, BlockPos pos, Direction face, boolean hasShade)
    {
    }
}
