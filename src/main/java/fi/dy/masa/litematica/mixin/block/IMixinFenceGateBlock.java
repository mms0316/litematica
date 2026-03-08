package fi.dy.masa.litematica.mixin.block;

import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FenceGateBlock.class)
public interface IMixinFenceGateBlock
{
    @Invoker("isWall")
    boolean litematica_invokeIsWall(BlockState state);
}
