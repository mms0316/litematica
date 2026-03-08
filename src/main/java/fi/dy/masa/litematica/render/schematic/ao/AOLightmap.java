package fi.dy.masa.litematica.render.schematic.ao;

import fi.dy.masa.litematica.render.schematic.BlockModelRendererSchematic;
import net.minecraft.core.BlockPos;

public class AOLightmap
{
	public final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	public boolean hasOffset;       // has Offset (field_58160)
	public boolean hasNeighbors;    // has NeighborData (field_58161)
	public final float[] fs = new float[4];
	public final int[] is = new int[4];
	public int lastTintIndex = -1;
	public int colorOfLastTintIndex;
	public final AOBrightness brightnessCache = BlockModelRendererSchematic.BRIGHTNESS_CACHE.get();
}
