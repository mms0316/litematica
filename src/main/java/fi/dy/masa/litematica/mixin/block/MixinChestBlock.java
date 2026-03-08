package fi.dy.masa.litematica.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.BlockUtils;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

@Mixin(ChestBlock.class)
public class MixinChestBlock
{
    @Inject(method = "mirror", at = @At("HEAD"), cancellable = true)
    private void litematica_fixChestMirror(BlockState state, Mirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        ChestType type = state.getValue(ChestBlock.TYPE);

        if (Configs.Generic.FIX_CHEST_MIRROR.getBooleanValue() && type != ChestType.SINGLE)
        {
            state = BlockUtils.fixMirrorDoubleChest(state, mirror, type);
            cir.setReturnValue(state);
        }
    }
}
