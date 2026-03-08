package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector3fc;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.position.PositionUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.render.IMixinBlockRenderManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.ao.*;

public class BlockModelRendererSchematic
{
	public static final ThreadLocal<AOBrightness> BRIGHTNESS_CACHE = ThreadLocal.withInitial(AOBrightness::new);
    private final SingleThreadedRandomSource random;
    private final BlockColors colorMap;
    private LiquidBlockRenderer liquidRenderer;
    private ModelManager bakedManager;

    public BlockModelRendererSchematic(BlockColors blockColorsIn, BlockRenderDispatcher manager)
    {
		this.random = new SingleThreadedRandomSource(42L);
        this.colorMap = blockColorsIn;
        this.reload(manager);
    }

	public static void enableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().enable();
		}
	}

	public static void disableCache()
	{
		if (Configs.Visuals.RENDER_AO_MODERN_ENABLE.getBooleanValue())
		{
			BRIGHTNESS_CACHE.get().disable();
		}
	}

	public void reload(BlockRenderDispatcher manager)
	{
		this.liquidRenderer = ((IMixinBlockRenderManager) manager).litematica_getFluidRenderer();
	}

    public void setBakedManager(ModelManager manager)
    {
        this.bakedManager = manager;
    }

	public SingleThreadedRandomSource getRandom()
	{
		return this.random;
	}

	public void setSeed(long seed)
	{
		this.random.setSeed(seed);
	}

    public boolean renderModel(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts,
                               BlockState stateIn, BlockPos posIn,
                               PoseStack matrices, VertexConsumer vertexConsumer,
                               boolean cull, int overlay)
    {
		if (!modelParts.isEmpty())
		{
			boolean ao = Minecraft.useAmbientOcclusion() &&
					stateIn.getLightEmission() == 0 && modelParts.getFirst().useAmbientOcclusion();

			Vec3 offset = stateIn.getOffset(posIn);
			matrices.translate((float) offset.x, (float) offset.y, (float) offset.z);

			try
			{
				if (ao)
				{
//                System.out.printf("renderModelSmooth(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.renderModelSmooth(worldIn, modelParts, stateIn, posIn, matrices, vertexConsumer, overlay, cull);
				}
				else
				{
//                System.out.printf("renderModelFlat(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.renderModelFlat(worldIn, modelParts, stateIn, posIn, matrices, vertexConsumer, overlay, cull);
				}
			}
			catch (Throwable throwable)
			{
				//Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
				CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
				CrashReportCategory.populateBlockDetails(crashreportcategory, worldIn, posIn, stateIn);
				crashreportcategory.setDetail("Using AO", ao);
				throw new ReportedException(crashreport);
			}
		}

	    return false;
    }

    public boolean renderModelFlat(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts,
                                   BlockState stateIn, BlockPos posIn,
                                   PoseStack matrices, VertexConsumer vertexConsumer,
                                   int overlay, boolean cull)
    {
		AOLightmap lightmap = new AOLightmap();
	    BlockPos.MutableBlockPos mutablePos = posIn.mutable();
	    boolean renderedSomething = false;
	    int i = 0;
	    int j = 0;

        for (BlockModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
				int index = 1 << side.ordinal();
				boolean bl = (i & index) == 1;
	            boolean bl2 = (j & index) == 1;

				if (!bl || bl2)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						BlockPos pos = lightmap.pos.setWithOffset(posIn, side);
						mutablePos.setWithOffset(posIn, side);

						if (!bl)
						{
							bl2 = shouldRenderModelSide(worldIn, stateIn, posIn, side, cull, mutablePos);
							i |= index;

							if (bl2)
							{
								j |= index;
							}
						}

						if (bl2)
						{
//							int light = WorldRenderer.getLightmapCoordinates(worldIn, stateIn, posIn.offset(side));
//							int light = WorldRenderer.getLightmapCoordinates(worldIn, mutablePos);
							int light = lightmap.brightnessCache.getInt(stateIn, worldIn, pos);

							this.renderQuadsFlat(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, lightmap, overlay, light, false);
							renderedSomething = true;
						}
					}
				}
            }

//            random.setSeed(seedIn);
//            modelIn.getQuads(stateIn, null, random)
            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                this.renderQuadsFlat(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, lightmap, overlay, -1, true);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }

	public boolean renderModelSmooth(BlockAndTintGetter worldIn, List<BlockModelPart> modelParts, BlockState stateIn, BlockPos posIn,
	                                 PoseStack matrices, VertexConsumer vertexConsumer,
	                                 int overlay, boolean cull)
	{
		AOProcessor ao = AOProcessor.get();
		BlockPos.MutableBlockPos mutablePos = posIn.mutable();
		boolean renderedSomething = false;
		int i = 0;
		int j = 0;

		for (BlockModelPart part : modelParts)
		{
			for (Direction side : PositionUtils.ALL_DIRECTIONS)
			{
				int index = 1 << side.ordinal();
				boolean bl = (i & index) == 1;
				boolean bl2 = (j & index) == 1;

				if (!bl || bl2)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						mutablePos.setWithOffset(posIn, side);

						if (!bl)
						{
							bl2 = shouldRenderModelSide(worldIn, stateIn, posIn, side, cull, mutablePos);
							i |= index;

							if (bl2)
							{
								j |= index;
							}
						}

						if (bl2)
						{
							this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, ao, overlay);
							renderedSomething = true;
						}
					}
				}
			}

//            random.setSeed(seedIn);
//            modelIn.getQuads(stateIn, null, random)
			List<BakedQuad> quads = part.getQuads(null);

			if (!quads.isEmpty())
			{
				this.renderQuadsSmooth(worldIn, stateIn, posIn, matrices, vertexConsumer, quads, ao, overlay);
				renderedSomething = true;
			}
		}

		return renderedSomething;
	}

	public static boolean shouldRenderModelSide(BlockAndTintGetter worldIn, BlockState stateIn, BlockPos posIn,
	                                            Direction side, boolean cull, BlockPos mutable)
    {
//		if (!cull) return true;
        return (DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
		        Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() &&
		        (Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue())) ||
		        Block.shouldRenderFace(stateIn, worldIn.getBlockState(mutable), side);
	}

    private void renderQuadsFlat(BlockAndTintGetter world, BlockState state, BlockPos pos,
                                 PoseStack matrices, VertexConsumer vertexConsumer,
                                 List<BakedQuad> quads, AOLightmap lightmap,
                                 int overlay, int light, boolean useWorldLight)
    {
        //final int size = list.size();

        for (BakedQuad bakedQuad : quads)
        {
            if (useWorldLight)
            {
                this.getQuadDimensions(world, state, pos, bakedQuad, lightmap);
                BlockPos blockPos = lightmap.hasOffset ? lightmap.pos.setWithOffset(pos, bakedQuad.direction()) : pos;
                light = lightmap.brightnessCache.isEnabled()
                        ? lightmap.brightnessCache.getInt(state, world, pos)
                        : LevelRenderer.getLightColor(world, blockPos);
            }

            float b = world.getShade(bakedQuad.direction(), bakedQuad.shade());
//            float[] bo = new float[]{b, b, b, b};
//	          int[] lo = new int[]{light, light, light, light};

	        lightmap.fs[0] = b;
	        lightmap.fs[1] = b;
	        lightmap.fs[2] = b;
	        lightmap.fs[3] = b;
			lightmap.is[0] = light;
	        lightmap.is[1] = light;
	        lightmap.is[2] = light;
	        lightmap.is[3] = light;

            this.renderQuad(world, state, pos, vertexConsumer, matrices, bakedQuad, lightmap, overlay);
        }
    }

	private void renderQuadsSmooth(BlockAndTintGetter world, BlockState state, BlockPos pos,
	                               PoseStack matrices, VertexConsumer vertexConsumer,
	                               List<BakedQuad> quads, AOProcessor ao,
	                               int overlay)
	{
		//System.out.printf("renderQuad(): pos [%s] / state [%s] / quad size [%d]\n", pos.toShortString(), state, size);
		for (BakedQuad bakedQuad : quads)
		{
			this.getQuadDimensions(world, state, pos, bakedQuad, ao);
			ao.apply(world, state, pos, bakedQuad.direction(), bakedQuad.shade());
			//System.out.printf("renderQuad(): pos [%s] / state [%s] / quad face [%s]\n", pos.toShortString(), state, bakedQuad.getFace().getName());
			this.renderQuad(world, state, pos, vertexConsumer, matrices, bakedQuad, ao, overlay);
		}
	}

	private void renderQuad(BlockAndTintGetter world, BlockState state, BlockPos pos,
                            VertexConsumer vertexConsumer, PoseStack matrices,
                            BakedQuad quad,
                            AOLightmap lightmap,
                            int overlay)
    {
		int tint = quad.tintIndex();
        float r;
        float g;
        float b;
	    float a;

        if (quad.isTinted())
        {
            int color;

			if (lightmap.lastTintIndex == tint)
			{
				color = lightmap.colorOfLastTintIndex;
			}
			else
			{
				color = this.colorMap.getColor(state, world, pos, tint);
				lightmap.lastTintIndex = tint;
				lightmap.colorOfLastTintIndex = color;
			}

            r = (float) (color >> 16 & 0xFF) / 255.0F;
            g = (float) (color >> 8 & 0xFF) / 255.0F;
            b = (float) (color & 0xFF) / 255.0F;
        }
        else
        {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

	    a = 1.0f;

        //System.out.printf("quad(): pos [%s] / state [%s] --> SPRITE [%s]\n", pos.toShortString(), state, quad.getSprite().toString());
        vertexConsumer.putBulkData(matrices.last(), quad, lightmap.fs, r, g, b, a, lightmap.is, overlay);
    }

    private void getQuadDimensions(BlockAndTintGetter world, BlockState state, BlockPos pos,
                                   BakedQuad quad,
                                   AOLightmap lightmap)
    {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;
        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;

        for (int index = 0; index < 4; index++)
        {
	        Vector3fc v3fc = quad.position(index);
            float x = v3fc.x();
            float y = v3fc.y();
            float z = v3fc.z();

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (lightmap instanceof AOProcessorModern aoModern)
        {
            aoModern.shapeCache[Direction.WEST.get3DDataValue()] = minX;
	        aoModern.shapeCache[Direction.EAST.get3DDataValue()] = maxX;
	        aoModern.shapeCache[Direction.DOWN.get3DDataValue()] = minY;
	        aoModern.shapeCache[Direction.UP.get3DDataValue()] = maxY;
	        aoModern.shapeCache[Direction.NORTH.get3DDataValue()] = minZ;
	        aoModern.shapeCache[Direction.SOUTH.get3DDataValue()] = maxZ;

	        aoModern.shapeCache[Direction.WEST.get3DDataValue() + 6] = 1.0F - minX;
	        aoModern.shapeCache[Direction.EAST.get3DDataValue() + 6] = 1.0F - maxX;
	        aoModern.shapeCache[Direction.DOWN.get3DDataValue() + 6] = 1.0F - minY;
	        aoModern.shapeCache[Direction.UP.get3DDataValue() + 6] = 1.0F - maxY;
	        aoModern.shapeCache[Direction.NORTH.get3DDataValue() + 6] = 1.0F - minZ;
	        aoModern.shapeCache[Direction.SOUTH.get3DDataValue() + 6] = 1.0F - maxZ;
        }
		else if (lightmap instanceof AOProcessorLegacy aoLegacy)
        {
	        aoLegacy.shapeCache[Direction.WEST.get3DDataValue()] = minX;
	        aoLegacy.shapeCache[Direction.EAST.get3DDataValue()] = maxX;
	        aoLegacy.shapeCache[Direction.DOWN.get3DDataValue()] = minY;
	        aoLegacy.shapeCache[Direction.UP.get3DDataValue()] = maxY;
	        aoLegacy.shapeCache[Direction.NORTH.get3DDataValue()] = minZ;
	        aoLegacy.shapeCache[Direction.SOUTH.get3DDataValue()] = maxZ;

	        aoLegacy.shapeCache[Direction.WEST.get3DDataValue() + 6] = 1.0F - minX;
	        aoLegacy.shapeCache[Direction.EAST.get3DDataValue() + 6] = 1.0F - maxX;
	        aoLegacy.shapeCache[Direction.DOWN.get3DDataValue() + 6] = 1.0F - minY;
	        aoLegacy.shapeCache[Direction.UP.get3DDataValue() + 6] = 1.0F - maxY;
	        aoLegacy.shapeCache[Direction.NORTH.get3DDataValue() + 6] = 1.0F - minZ;
	        aoLegacy.shapeCache[Direction.SOUTH.get3DDataValue() + 6] = 1.0F - maxZ;
        }

        float min = 1.0E-4F;
        float max = 0.9999F;

	    lightmap.hasNeighbors = switch (quad.direction())
	    {
		    case DOWN, UP -> minX >= min || minZ >= min || maxX <= max || maxZ <= max;
		    case NORTH, SOUTH -> minX >= min || minY >= min || maxX <= max || maxY <= max;
		    case WEST, EAST -> minY >= min || minZ >= min || maxY <= max || maxZ <= max;
	    };

	    lightmap.hasOffset = switch (quad.direction())
	    {
		    case DOWN -> minY == maxY && (minY < min || state.isCollisionShapeFullBlock(world, pos));
		    case UP -> minY == maxY && (maxY > max || state.isCollisionShapeFullBlock(world, pos));
		    case NORTH -> minZ == maxZ && (minZ < min || state.isCollisionShapeFullBlock(world, pos));
		    case SOUTH -> minZ == maxZ && (maxZ > max || state.isCollisionShapeFullBlock(world, pos));
		    case WEST -> minX == maxX && (minX < min || state.isCollisionShapeFullBlock(world, pos));
		    case EAST -> minX == maxX && (maxX > max || state.isCollisionShapeFullBlock(world, pos));
	    };
    }

	public void renderQuadsState(PoseStack.Pose entry, VertexConsumer vertexConsumer,
	                             BlockState state,
	                             float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		this.renderQuadsModel(entry, vertexConsumer,
		                      this.bakedManager.getBlockModelShaper().getBlockModel(state),
		                      rgb, light, overlay, state);
	}

	public void renderQuadsModel(PoseStack.Pose entry, VertexConsumer vertexConsumer,
	                             BlockStateModel model,
	                             float[] rgb, int light, int overlay,
	                             @Nullable BlockState fallbackState)
	{
		List<BlockModelPart> parts = model.collectParts(this.random);
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		if (parts.isEmpty() && fallbackState != null)
		{
			BlockState state = LitematicaRenderer.getInstance().getWorldRenderer().getFallbackState(fallbackState);
			model = this.bakedManager.getBlockModelShaper().getBlockModel(state);
			parts = model.collectParts(this.random);
		}

		// Because of other mods.
		if (parts.isEmpty())
		{
			return;
		}

		for (BlockModelPart part : parts)
		{
			this.renderQuadsPart(entry, vertexConsumer, part, rgb, light, overlay);
		}
	}

	public void renderQuadsPart(PoseStack.Pose entry, VertexConsumer vertexConsumer,
	                            BlockModelPart part,
	                            float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		for (Direction side : PositionUtils.ALL_DIRECTIONS)
		{
			this.renderQuads(entry, vertexConsumer, part.getQuads(side), rgb, light, overlay);
		}

		this.renderQuads(entry, vertexConsumer, part.getQuads(null), rgb, light, overlay);
	}

	public void renderQuads(PoseStack.Pose entry, VertexConsumer vertexConsumer,
	                        List<BakedQuad> quads,
	                        float[] rgb, int light, int overlay)
	{
		if (rgb.length < 3)
		{
			rgb = new float[]{1.0f, 1.0f, 1.0f};
		}

		for (BakedQuad quad : quads)
		{
			float red = 1.0f;
			float green = 1.0f;
			float blue = 1.0f;
			float alpha = 1.0f;

			if (quad.isTinted())
			{
				red = MathUtils.clamp(rgb[0], 0.0f, 1.0f);
				green = MathUtils.clamp(rgb[1], 0.0f, 1.0f);
				blue = MathUtils.clamp(rgb[2], 0.0f, 1.0f);
			}

			vertexConsumer.putBulkData(entry, quad, red, green, blue, alpha, light, overlay);
		}
	}

    @ApiStatus.Experimental
    public void renderLiquid(VertexConsumer consumer, BlockAndTintGetter world, BlockPos pos, BlockState stateIn, FluidState fluid)
    {
        try
        {
            this.liquidRenderer.tesselate(world, pos, consumer, stateIn, fluid);
        }
        catch (Throwable var9)
        {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Tesselating liquid in world");
            CrashReportCategory crashReportSection = crashReport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportSection, world, pos, stateIn);
            throw new ReportedException(crashReport);
        }
    }

    public boolean renderBlockState(PoseStack matrices, MultiBufferSource consumer, BlockState stateIn, int light, int overlay)
    {
        RenderShape blockRenderType = stateIn.getRenderShape();

        if (blockRenderType == RenderShape.INVISIBLE)
        {
            return false;
        }

        BlockStateModel model = this.bakedManager.getBlockModelShaper().getBlockModel(stateIn);
        int i = this.colorMap.getColor(stateIn, null, null, 0);
        float red = (float) (i >> 16 & 0xFF) / 255.0f;
        float green = (float) (i >> 8 & 0xFF) / 255.0f;
        float blue = (float) (i & 0xFF) / 255.0f;

        this.renderQuadsModel(matrices.last(),
                              consumer.getBuffer(ItemBlockRenderTypes.getRenderType(stateIn)),
                              model,
                              new float[]{red, green, blue},
                              light, overlay,
                              stateIn);
        return true;
    }
}
