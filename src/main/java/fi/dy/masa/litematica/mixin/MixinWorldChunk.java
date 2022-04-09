package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk
{
    @Redirect(method = "setBlockState",
                slice = @Slice(from = @At(value = "INVOKE",
                                target = "Lnet/minecraft/world/chunk/ChunkSection;getBlockState(III)" +
                                          "Lnet/minecraft/block/BlockState;")),
                at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isClient:Z", ordinal = 0))
    private boolean litematica_redirectIsRemote(World world)
    {
        return WorldUtils.shouldPreventBlockUpdates(world) || world.isClient;
    }

    @Inject(method = "setBlockState", at = @At(value = "TAIL"))
    private void onSetBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir)
    {
        WorldUtils.easyPlaceRemovePosition(pos);
    }
}
