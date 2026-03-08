package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * AO Legacy AO Processor from Masa (2018)
 * -- Ported/merged to be compatible with the "AO Lightmap" system for 1.21.11+
 */
public class AOProcessorLegacy extends AOProcessor
{
	public final float[] shapeCache = new float[AOOrientation.values().length];     // field_58158

	@Override
	public void apply(BlockAndTintGetter world, BlockState state, BlockPos pos, Direction face, boolean hasShade)
	{
		AONeighborInfo neighborInfo = AONeighborInfo.getNeighbourInfo(face);
		AOTranslations vertexTranslations = AOTranslations.getVertexTranslations(face);
		int i, j, k, l, i1, i3, j1, k1, l1;
		i = j = k = l = i1 = i3 = j1 = k1 = l1 = ((15 << 20) | (15 << 4));
		float b1 = 1.0F;
		float b2 = 1.0F;
		float b3 = 1.0F;
		float b4 = 1.0F;

		if (this.hasNeighbors && neighborInfo.doNonCubicWeight)
		{
			float f13 = this.shapeCache[neighborInfo.vert0Weights[0].shape] * this.shapeCache[neighborInfo.vert0Weights[1].shape];
			float f14 = this.shapeCache[neighborInfo.vert0Weights[2].shape] * this.shapeCache[neighborInfo.vert0Weights[3].shape];
			float f15 = this.shapeCache[neighborInfo.vert0Weights[4].shape] * this.shapeCache[neighborInfo.vert0Weights[5].shape];
			float f16 = this.shapeCache[neighborInfo.vert0Weights[6].shape] * this.shapeCache[neighborInfo.vert0Weights[7].shape];
			float f17 = this.shapeCache[neighborInfo.vert1Weights[0].shape] * this.shapeCache[neighborInfo.vert1Weights[1].shape];
			float f18 = this.shapeCache[neighborInfo.vert1Weights[2].shape] * this.shapeCache[neighborInfo.vert1Weights[3].shape];
			float f19 = this.shapeCache[neighborInfo.vert1Weights[4].shape] * this.shapeCache[neighborInfo.vert1Weights[5].shape];
			float f20 = this.shapeCache[neighborInfo.vert1Weights[6].shape] * this.shapeCache[neighborInfo.vert1Weights[7].shape];
			float f21 = this.shapeCache[neighborInfo.vert2Weights[0].shape] * this.shapeCache[neighborInfo.vert2Weights[1].shape];
			float f22 = this.shapeCache[neighborInfo.vert2Weights[2].shape] * this.shapeCache[neighborInfo.vert2Weights[3].shape];
			float f23 = this.shapeCache[neighborInfo.vert2Weights[4].shape] * this.shapeCache[neighborInfo.vert2Weights[5].shape];
			float f24 = this.shapeCache[neighborInfo.vert2Weights[6].shape] * this.shapeCache[neighborInfo.vert2Weights[7].shape];
			float f25 = this.shapeCache[neighborInfo.vert3Weights[0].shape] * this.shapeCache[neighborInfo.vert3Weights[1].shape];
			float f26 = this.shapeCache[neighborInfo.vert3Weights[2].shape] * this.shapeCache[neighborInfo.vert3Weights[3].shape];
			float f27 = this.shapeCache[neighborInfo.vert3Weights[4].shape] * this.shapeCache[neighborInfo.vert3Weights[5].shape];
			float f28 = this.shapeCache[neighborInfo.vert3Weights[6].shape] * this.shapeCache[neighborInfo.vert3Weights[7].shape];
			this.fs[vertexTranslations.vert0] = b1 * f13 + b2 * f14 + b3 * f15 + b4 * f16;
			this.fs[vertexTranslations.vert1] = b1 * f17 + b2 * f18 + b3 * f19 + b4 * f20;
			this.fs[vertexTranslations.vert2] = b1 * f21 + b2 * f22 + b3 * f23 + b4 * f24;
			this.fs[vertexTranslations.vert3] = b1 * f25 + b2 * f26 + b3 * f27 + b4 * f28;
			int i2 = this.getAoBrightness(l, i, j1, i3);
			int j2 = this.getAoBrightness(k, i, i1, i3);
			int k2 = this.getAoBrightness(k, j, k1, i3);
			int l2 = this.getAoBrightness(l, j, l1, i3);
			this.is[vertexTranslations.vert0] = this.getVertexBrightness(i2, j2, k2, l2, f13, f14, f15, f16);
			this.is[vertexTranslations.vert1] = this.getVertexBrightness(i2, j2, k2, l2, f17, f18, f19, f20);
			this.is[vertexTranslations.vert2] = this.getVertexBrightness(i2, j2, k2, l2, f21, f22, f23, f24);
			this.is[vertexTranslations.vert3] = this.getVertexBrightness(i2, j2, k2, l2, f25, f26, f27, f28);
		}
		else
		{
			this.is[vertexTranslations.vert0] = this.getAoBrightness(l, i, j1, i3);
			this.is[vertexTranslations.vert1] = this.getAoBrightness(k, i, i1, i3);
			this.is[vertexTranslations.vert2] = this.getAoBrightness(k, j, k1, i3);
			this.is[vertexTranslations.vert3] = this.getAoBrightness(l, j, l1, i3);
			this.fs[vertexTranslations.vert0] = b1;
			this.fs[vertexTranslations.vert1] = b2;
			this.fs[vertexTranslations.vert2] = b3;
			this.fs[vertexTranslations.vert3] = b4;
		}

		float b = world.getShade(face, hasShade);

		for (int index = 0; index < this.fs.length; ++index)
		{
			this.fs[index] *= b;
		}
	}

	/**
	 * Get ambient occlusion brightness
	 */
	private int getAoBrightness(int br1, int br2, int br3, int br4)
	{
		if (br1 == 0)
		{
			br1 = br4;
		}

		if (br2 == 0)
		{
			br2 = br4;
		}

		if (br3 == 0)
		{
			br3 = br4;
		}

		return br1 + br2 + br3 + br4 >> 2 & 16711935;
	}

	private int getVertexBrightness(int p_178203_1_, int p_178203_2_, int p_178203_3_, int p_178203_4_, float p_178203_5_, float p_178203_6_, float p_178203_7_, float p_178203_8_)
	{
		int i = (int) ((float) (p_178203_1_ >> 16 & 255) * p_178203_5_ + (float) (p_178203_2_ >> 16 & 255) * p_178203_6_ + (float) (p_178203_3_ >> 16 & 255) * p_178203_7_ + (float) (p_178203_4_ >> 16 & 255) * p_178203_8_) & 255;
		int j = (int) ((float) (p_178203_1_ & 255) * p_178203_5_ + (float) (p_178203_2_ & 255) * p_178203_6_ + (float) (p_178203_3_ & 255) * p_178203_7_ + (float) (p_178203_4_ & 255) * p_178203_8_) & 255;
		return i << 16 | j;
	}
}
