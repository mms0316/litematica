package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A "Modern" AO Processor based upon 24w36a through 1.21.11+ that utilizes the "brightness cache"
 */
public class AOProcessorModern extends AOProcessor
{
	public final float[] shapeCache = new float[AOOrientation.values().length];     // field_58158

    @Override
    public void apply(BlockAndTintGetter world, BlockState state, BlockPos pos, Direction face, boolean hasShade)
    {
        // 1.21.11-pre1
        BlockPos blockPos = this.hasOffset ? pos.relative(face) : pos;
        AONeighborInfo nd = AONeighborInfo.getNeighbourInfo(face);
        BlockPos.MutableBlockPos mutable = this.pos;
        mutable.setWithOffset(blockPos, nd.corners[0]);

        BlockState bs1 = world.getBlockState(mutable);
        int i = this.brightnessCache.getInt(bs1, world, mutable);
        float f = this.brightnessCache.getFloat(bs1, world, mutable);
        mutable.setWithOffset(blockPos, nd.corners[1]);
        BlockState bs2 = world.getBlockState(mutable);
        int j = this.brightnessCache.getInt(bs2, world, mutable);
        float g = this.brightnessCache.getFloat(bs2, world, mutable);
        mutable.setWithOffset(blockPos, nd.corners[2]);
        BlockState bs3 = world.getBlockState(mutable);
        int k = this.brightnessCache.getInt(bs3, world, mutable);
        float h = this.brightnessCache.getFloat(bs3, world, mutable);
        mutable.setWithOffset(blockPos, nd.corners[3]);
        BlockState bs4 = world.getBlockState(mutable);
        int l = this.brightnessCache.getInt(bs4, world, mutable);
        float m = this.brightnessCache.getFloat(bs4, world, mutable);
        BlockState bs5 = world.getBlockState(mutable.setWithOffset(blockPos, nd.corners[0]).move(face));
        boolean bl2 = !bs5.isViewBlocking(world, mutable) || bs5.getLightBlock() == 0;
        BlockState bs6 = world.getBlockState(mutable.setWithOffset(blockPos, nd.corners[1]).move(face));
        boolean bl3 = !bs6.isViewBlocking(world, mutable) || bs6.getLightBlock() == 0;
        BlockState bs7 = world.getBlockState(mutable.setWithOffset(blockPos, nd.corners[2]).move(face));
        boolean bl4 = !bs7.isViewBlocking(world, mutable) || bs7.getLightBlock() == 0;
        BlockState bs8 = world.getBlockState(mutable.setWithOffset(blockPos, nd.corners[3]).move(face));
        boolean bl5 = !bs8.isViewBlocking(world, mutable) || bs8.getLightBlock() == 0;

        float n;
        int o;
        BlockState bs9;

        if (!bl4 && !bl2)
        {
            n = f;
            o = i;
        }
        else
        {
            mutable.setWithOffset(blockPos, nd.corners[0]).move(nd.corners[2]);
            bs9 = world.getBlockState(mutable);
            n = this.brightnessCache.getFloat(bs9, world, mutable);
            o = this.brightnessCache.getInt(bs9, world, mutable);
        }

        float p;
        int q;
        if (!bl5 && !bl2)
        {
            p = f;
            q = i;
        }
        else
        {
            mutable.setWithOffset(blockPos, nd.corners[0]).move(nd.corners[3]);
            bs9 = world.getBlockState(mutable);
            p = this.brightnessCache.getFloat(bs9, world, mutable);
            q = this.brightnessCache.getInt(bs9, world, mutable);
        }

        float r;
        int s;
        if (!bl4 && !bl3)
        {
            r = f;
            s = i;
        }
        else
        {
            mutable.setWithOffset(blockPos, nd.corners[1]).move(nd.corners[2]);
            bs9 = world.getBlockState(mutable);
            r = this.brightnessCache.getFloat(bs9, world, mutable);
            s = this.brightnessCache.getInt(bs9, world, mutable);
        }

        float t;
        int u;
        if (!bl5 && !bl3)
        {
            t = f;
            u = i;
        }
        else
        {
            mutable.setWithOffset(blockPos, nd.corners[1]).move(nd.corners[3]);
            bs9 = world.getBlockState(mutable);
            t = this.brightnessCache.getFloat(bs9, world, mutable);
            u = this.brightnessCache.getInt(bs9, world, mutable);
        }

        int v = this.brightnessCache.getInt(state, world, pos);
        mutable.setWithOffset(pos, face);
        BlockState bs10 = world.getBlockState(mutable);

        if (this.hasOffset || !bs10.isSolidRender())
        {
            v = this.brightnessCache.getInt(bs10, world, mutable);
        }

        float w = this.hasOffset
                  ? this.brightnessCache.getFloat(world.getBlockState(blockPos), world, blockPos)
                  : this.brightnessCache.getFloat(world.getBlockState(pos), world, pos);

        AOTranslations translation = AOTranslations.getVertexTranslations(face);

        if (this.hasNeighbors && nd.doNonCubicWeight)
        {
	        float x = (m + f + p + w) * 0.25F;
	        float y = (h + f + n + w) * 0.25F;
	        float z = (h + g + r + w) * 0.25F;
	        float aa = (m + g + t + w) * 0.25F;
            float ab = this.shapeCache[nd.vert0Weights[0].shape] * this.shapeCache[nd.vert0Weights[1].shape];
            float ac = this.shapeCache[nd.vert0Weights[2].shape] * this.shapeCache[nd.vert0Weights[3].shape];
            float ad = this.shapeCache[nd.vert0Weights[4].shape] * this.shapeCache[nd.vert0Weights[5].shape];
            float ae = this.shapeCache[nd.vert0Weights[6].shape] * this.shapeCache[nd.vert0Weights[7].shape];
            float af = this.shapeCache[nd.vert1Weights[0].shape] * this.shapeCache[nd.vert1Weights[1].shape];
            float ag = this.shapeCache[nd.vert1Weights[2].shape] * this.shapeCache[nd.vert1Weights[3].shape];
            float ah = this.shapeCache[nd.vert1Weights[4].shape] * this.shapeCache[nd.vert1Weights[5].shape];
            float ai = this.shapeCache[nd.vert1Weights[6].shape] * this.shapeCache[nd.vert1Weights[7].shape];
            float aj = this.shapeCache[nd.vert2Weights[0].shape] * this.shapeCache[nd.vert2Weights[1].shape];
            float ak = this.shapeCache[nd.vert2Weights[2].shape] * this.shapeCache[nd.vert2Weights[3].shape];
            float al = this.shapeCache[nd.vert2Weights[4].shape] * this.shapeCache[nd.vert2Weights[5].shape];
            float am = this.shapeCache[nd.vert2Weights[6].shape] * this.shapeCache[nd.vert2Weights[7].shape];
            float an = this.shapeCache[nd.vert3Weights[0].shape] * this.shapeCache[nd.vert3Weights[1].shape];
            float ao = this.shapeCache[nd.vert3Weights[2].shape] * this.shapeCache[nd.vert3Weights[3].shape];
            float ap = this.shapeCache[nd.vert3Weights[4].shape] * this.shapeCache[nd.vert3Weights[5].shape];
            float aq = this.shapeCache[nd.vert3Weights[6].shape] * this.shapeCache[nd.vert3Weights[7].shape];
            this.fs[translation.vert0] = Math.clamp(x * ab + y * ac + z * ad + aa * ae, 0.0F, 1.0F);
            this.fs[translation.vert1] = Math.clamp(x * af + y * ag + z * ah + aa * ai, 0.0F, 1.0F);
            this.fs[translation.vert2] = Math.clamp(x * aj + y * ak + z * al + aa * am, 0.0F, 1.0F);
            this.fs[translation.vert3] = Math.clamp(x * an + y * ao + z * ap + aa * aq, 0.0F, 1.0F);
            int ar = this.getAoBrightness(l, i, q, v);
            int as = this.getAoBrightness(k, i, o, v);
            int at = this.getAoBrightness(k, j, s, v);
            int au = this.getAoBrightness(l, j, u, v);
            this.is[translation.vert0] = this.getVertexBrightness(ar, as, at, au, ab, ac, ad, ae);
            this.is[translation.vert1] = this.getVertexBrightness(ar, as, at, au, af, ag, ah, ai);
            this.is[translation.vert2] = this.getVertexBrightness(ar, as, at, au, aj, ak, al, am);
            this.is[translation.vert3] = this.getVertexBrightness(ar, as, at, au, an, ao, ap, aq);
        }
        else
        {
	        float x = (m + f + p + w) * 0.25F;
	        float y = (h + f + n + w) * 0.25F;
	        float z = (h + g + r + w) * 0.25F;
	        float aa = (m + g + t + w) * 0.25F;
            this.is[translation.vert0] = this.getAoBrightness(l, i, q, v);
            this.is[translation.vert1] = this.getAoBrightness(k, i, o, v);
            this.is[translation.vert2] = this.getAoBrightness(k, j, s, v);
            this.is[translation.vert3] = this.getAoBrightness(l, j, u, v);
            this.fs[translation.vert0] = x;
            this.fs[translation.vert1] = y;
            this.fs[translation.vert2] = z;
            this.fs[translation.vert3] = aa;
        }

	    float x = world.getShade(face, hasShade);

        for (int av = 0; av < this.fs.length; av++)
        {
            this.fs[av] *= x;
        }
    }

    private int getAoBrightness(int i, int j, int k, int l)
    {
        if (i == 0)
        {
            i = l;
        }

        if (j == 0)
        {
            j = l;
        }

        if (k == 0)
        {
            k = l;
        }

        return i + j + k + l >> 2 & 16711935;
    }

    private int getVertexBrightness(int i, int j, int k, int l, float f, float g, float h, float m)
    {
        int n = (int) ((float) (i >> 16 & 255) * f + (float) (j >> 16 & 255) * g + (float) (k >> 16 & 255) * h + (float) (l >> 16 & 255) * m) & 255;
        int o = (int) ((float) (i & 255) * f + (float) (j & 255) * g + (float) (k & 255) * h + (float) (l & 255) * m) & 255;
        return n << 16 | o;
    }
}
