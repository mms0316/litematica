package fi.dy.masa.litematica.mixin.network;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Post Re-Write code
 */
@Mixin(value = ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager
{
    /*
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            // Prevent recursion, since the Easy Place mode can call this code again
            if (EasyPlaceUtils.isHandling() == false)
            {
                if (EasyPlaceUtils.shouldDoEasyPlaceActions())
                {
                    if (EasyPlaceUtils.handleEasyPlaceWithMessage())
                    {
                        cir.setReturnValue(ActionResult.FAIL);
                    }
                }
                else
                {
                    if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
                    {
                        if (EasyPlaceUtils.handlePlacementRestriction())
                        {
                            cir.setReturnValue(ActionResult.FAIL);
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "interactBlockInternal",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;",
            shift = At.Shift.BEFORE), cancellable = true)
    private void onInteractBlockInternal(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            // Prevent recursion, since the Easy Place mode can call this code again
            if (EasyPlaceUtils.isHandling() == false)
            {
                if (EasyPlaceUtils.shouldDoEasyPlaceActions() &&
                    EasyPlaceUtils.handleEasyPlaceWithMessage())
                {
                    cir.setReturnValue(ActionResult.FAIL);
                }
            }
        }
    }
     */

    //Custom Additions (easier to resolve future merge conflicts)
    //Mixin @ public ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult)

    @Inject(method = "interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "HEAD"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir)
    {
        var pos = hitResult.getBlockPos();
        BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
        if (blockEntity instanceof LootableContainerBlockEntity)
        {
            DataManager.getContainerManager().setContainerPos(pos);
        }
    }
}
