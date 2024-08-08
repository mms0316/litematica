package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class AddonUtils {
    public static ActionResult checkEasyPlaceFluidBucket(MinecraftClient mc) {
        //Re-run traces to stop wasting liquid on liquid, and ignoring easyPlaceFirst config, as interactItem works differently

        final double traceMaxRange = mc.interactionManager.getReachDistance();
        final World world = SchematicWorldHandler.getSchematicWorld();

        //Raytrace first non-liquid block
        var hitResult = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return ActionResult.FAIL;
        var blockHitResult = (BlockHitResult)hitResult;
        final var blockPosLast = blockHitResult.getBlockPos();
        //Keep block before first non-liquid block
        final var blockPosBeforeLast = blockPosLast.offset(blockHitResult.getSide());

        //Raytrace first block including liquid
        hitResult = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, true, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return ActionResult.FAIL;
        final var blockPosFirst = ((BlockHitResult)hitResult).getBlockPos();

        //Fail if there are liquids in-between
        //If there are liquids in-between, it'd waste liquid or create obsidian
        if (blockPosFirst.toCenterPos().squaredDistanceTo(blockPosBeforeLast.toCenterPos()) > 1.0 + Math.ulp(1.0))
            return ActionResult.FAIL;

        final var blockStateSchematic = world.getBlockState(blockPosBeforeLast);
        final var blockSchematic = blockStateSchematic.getBlock();
        final var blockStateVanilla = mc.world.getBlockState(blockPosBeforeLast);
        final var blockVanilla = blockStateVanilla.getBlock();

        //Fail if target is not to be a liquid source
        if (!(blockSchematic instanceof FluidBlock))
            return ActionResult.FAIL;

        if (!blockStateVanilla.isAir())
        {
            //Fail if target is not of the desired fluid
            // (this comparison works because all Blocks are pointers to a single instance)
            if (blockSchematic != blockVanilla)
                return ActionResult.FAIL;

            //Fail if world already has block as a liquid source
            if (blockStateVanilla.get(FluidBlock.LEVEL) == 0)
                return ActionResult.FAIL;
        }

        return ActionResult.SUCCESS;
    }
}
