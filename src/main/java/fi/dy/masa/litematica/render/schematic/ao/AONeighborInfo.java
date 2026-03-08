package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.Direction;
import net.minecraft.util.Util;

public enum AONeighborInfo
{
	DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true,
	     new AOOrientation[]{AOOrientation.FLIP_WEST, AOOrientation.SOUTH, AOOrientation.FLIP_WEST, AOOrientation.FLIP_SOUTH, AOOrientation.WEST, AOOrientation.FLIP_SOUTH, AOOrientation.WEST, AOOrientation.SOUTH},
	     new AOOrientation[]{AOOrientation.FLIP_WEST, AOOrientation.NORTH, AOOrientation.FLIP_WEST, AOOrientation.FLIP_NORTH, AOOrientation.WEST, AOOrientation.FLIP_NORTH, AOOrientation.WEST, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.FLIP_EAST, AOOrientation.NORTH, AOOrientation.FLIP_EAST, AOOrientation.FLIP_NORTH, AOOrientation.EAST, AOOrientation.FLIP_NORTH, AOOrientation.EAST, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.FLIP_EAST, AOOrientation.SOUTH, AOOrientation.FLIP_EAST, AOOrientation.FLIP_SOUTH, AOOrientation.EAST, AOOrientation.FLIP_SOUTH, AOOrientation.EAST, AOOrientation.SOUTH}),
	UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true,
	   new AOOrientation[]{AOOrientation.EAST, AOOrientation.SOUTH, AOOrientation.EAST, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_EAST, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_EAST, AOOrientation.SOUTH},
	   new AOOrientation[]{AOOrientation.EAST, AOOrientation.NORTH, AOOrientation.EAST, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_EAST, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_EAST, AOOrientation.NORTH},
	   new AOOrientation[]{AOOrientation.WEST, AOOrientation.NORTH, AOOrientation.WEST, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_WEST, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_WEST, AOOrientation.NORTH},
	   new AOOrientation[]{AOOrientation.WEST, AOOrientation.SOUTH, AOOrientation.WEST, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_WEST, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_WEST, AOOrientation.SOUTH}),
	NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true,
	      new AOOrientation[]{AOOrientation.UP, AOOrientation.FLIP_WEST, AOOrientation.UP, AOOrientation.WEST, AOOrientation.FLIP_UP, AOOrientation.WEST, AOOrientation.FLIP_UP, AOOrientation.FLIP_WEST},
	      new AOOrientation[]{AOOrientation.UP, AOOrientation.FLIP_EAST, AOOrientation.UP, AOOrientation.EAST, AOOrientation.FLIP_UP, AOOrientation.EAST, AOOrientation.FLIP_UP, AOOrientation.FLIP_EAST},
	      new AOOrientation[]{AOOrientation.DOWN, AOOrientation.FLIP_EAST, AOOrientation.DOWN, AOOrientation.EAST, AOOrientation.FLIP_DOWN, AOOrientation.EAST, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_EAST},
	      new AOOrientation[]{AOOrientation.DOWN, AOOrientation.FLIP_WEST, AOOrientation.DOWN, AOOrientation.WEST, AOOrientation.FLIP_DOWN, AOOrientation.WEST, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_WEST}),
	SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true,
	      new AOOrientation[]{AOOrientation.UP, AOOrientation.FLIP_WEST, AOOrientation.FLIP_UP, AOOrientation.FLIP_WEST, AOOrientation.FLIP_UP, AOOrientation.WEST, AOOrientation.UP, AOOrientation.WEST},
	      new AOOrientation[]{AOOrientation.DOWN, AOOrientation.FLIP_WEST, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_WEST, AOOrientation.FLIP_DOWN, AOOrientation.WEST, AOOrientation.DOWN, AOOrientation.WEST},
	      new AOOrientation[]{AOOrientation.DOWN, AOOrientation.FLIP_EAST, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_EAST, AOOrientation.FLIP_DOWN, AOOrientation.EAST, AOOrientation.DOWN, AOOrientation.EAST},
	      new AOOrientation[]{AOOrientation.UP, AOOrientation.FLIP_EAST, AOOrientation.FLIP_UP, AOOrientation.FLIP_EAST, AOOrientation.FLIP_UP, AOOrientation.EAST, AOOrientation.UP, AOOrientation.EAST}),
	WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOOrientation[]{AOOrientation.UP, AOOrientation.SOUTH, AOOrientation.UP, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_UP, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_UP, AOOrientation.SOUTH},
	     new AOOrientation[]{AOOrientation.UP, AOOrientation.NORTH, AOOrientation.UP, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_UP, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_UP, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.DOWN, AOOrientation.NORTH, AOOrientation.DOWN, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_NORTH, AOOrientation.FLIP_DOWN, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.DOWN, AOOrientation.SOUTH, AOOrientation.DOWN, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_SOUTH, AOOrientation.FLIP_DOWN, AOOrientation.SOUTH}),
	EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOOrientation[]{AOOrientation.FLIP_DOWN, AOOrientation.SOUTH, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_SOUTH, AOOrientation.DOWN, AOOrientation.FLIP_SOUTH, AOOrientation.DOWN, AOOrientation.SOUTH},
	     new AOOrientation[]{AOOrientation.FLIP_DOWN, AOOrientation.NORTH, AOOrientation.FLIP_DOWN, AOOrientation.FLIP_NORTH, AOOrientation.DOWN, AOOrientation.FLIP_NORTH, AOOrientation.DOWN, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.FLIP_UP, AOOrientation.NORTH, AOOrientation.FLIP_UP, AOOrientation.FLIP_NORTH, AOOrientation.UP, AOOrientation.FLIP_NORTH, AOOrientation.UP, AOOrientation.NORTH},
	     new AOOrientation[]{AOOrientation.FLIP_UP, AOOrientation.SOUTH, AOOrientation.FLIP_UP, AOOrientation.FLIP_SOUTH, AOOrientation.UP, AOOrientation.FLIP_SOUTH, AOOrientation.UP, AOOrientation.SOUTH});

	final Direction[] corners;
//	final float shadeWeight;
	final boolean doNonCubicWeight;
	final AOOrientation[] vert0Weights;
	final AOOrientation[] vert1Weights;
	final AOOrientation[] vert2Weights;
	final AOOrientation[] vert3Weights;
	private static final AONeighborInfo[] VALUES = Util.make(new AONeighborInfo[6], (values) ->
	{
		values[Direction.DOWN.get3DDataValue()] = DOWN;
		values[Direction.UP.get3DDataValue()] = UP;
		values[Direction.NORTH.get3DDataValue()] = NORTH;
		values[Direction.SOUTH.get3DDataValue()] = SOUTH;
		values[Direction.WEST.get3DDataValue()] = WEST;
		values[Direction.EAST.get3DDataValue()] = EAST;
	});

	AONeighborInfo(final Direction[] corners, final float f, final boolean nonCubicWeight, final AOOrientation[] neighbor1, final AOOrientation[] neighbor2, final AOOrientation[] neighbor3, final AOOrientation[] neighbor4)
	{
		this.corners = corners;
		this.doNonCubicWeight = nonCubicWeight;
		this.vert0Weights = neighbor1;
		this.vert1Weights = neighbor2;
		this.vert2Weights = neighbor3;
		this.vert3Weights = neighbor4;
	}

	public static AONeighborInfo getNeighbourInfo(Direction direction)
	{
		return VALUES[direction.get3DDataValue()];
	}
}
