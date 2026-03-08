package fi.dy.masa.litematica.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Post Re-Write code
 */
@Mixin(value = MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager
{
    //Custom Additions (easier to resolve future merge conflicts)
    //Mixin 1.21.8: public ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult)
    //1.21.11: public InteractionResult useItemOn(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult)

    @Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "HEAD"))
    private void litematica_onInteractBlock(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir)
    {
        var pos = blockHitResult.getBlockPos();
        BlockEntity blockEntity = localPlayer.level().getBlockEntity(pos);
        if (blockEntity instanceof RandomizableContainerBlockEntity)
        {
            DataManager.getContainerManager().setContainerPos(pos);
        }
    }
}
