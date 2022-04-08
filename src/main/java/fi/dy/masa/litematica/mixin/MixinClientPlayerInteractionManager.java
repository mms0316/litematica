package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.WorldUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.litematica.config.Configs;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager
{
    @Shadow @Final private net.minecraft.client.MinecraftClient client;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir)
    {
        if (WorldUtils.allowNestedInteractBlock)
        {
            // Recursion - mc.interactionManager.interactBlock was called from easyPlace
            // Let vanilla code run
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("interactBlock recursion allowed");
            WorldUtils.allowNestedInteractBlock = false;
            return;
        }

        if (WorldUtils.shouldDoEasyPlaceActions())
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("interactBlock 1st");
            WorldUtils.easyPlaceAllowedInTick = false;

            if (WorldUtils.handleEasyPlaceWithMessage(this.client))
            {
                cir.setReturnValue(ActionResult.FAIL);
                if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                    Litematica.logger.info("interactBlock aborted");
            }
        }
        else if (WorldUtils.easyPlaceAllowedInTick == false)
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("interactBlock 2nd");
            // Second or more call in a same render() / tick() call. Must cancel.
            cir.setReturnValue(ActionResult.FAIL);
        }
        else
        {
            if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
            {
                if (WorldUtils.handlePlacementRestriction(this.client))
                {
                    if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                        Litematica.logger.info("interactBlock restricted");
                    cir.setReturnValue(ActionResult.FAIL);
                }
            }
        }
    }

    // Handle right clicks on air, which needs to happen for Easy Place mode
    @Inject(method = "interactItem", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;syncSelectedSlot()V"))
    private void onInteractItem(PlayerEntity player, World world, Hand hand, CallbackInfoReturnable<ActionResult> cir)
    {
        if (WorldUtils.shouldDoEasyPlaceActions())
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("interactItem 1st");
            WorldUtils.easyPlaceAllowedInTick = false;

            if (WorldUtils.handleEasyPlaceWithMessage(this.client))
            {
                cir.setReturnValue(ActionResult.FAIL);

                if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                    Litematica.logger.info("interactBlock aborted");
            }
        }
        else if (WorldUtils.easyPlaceAllowedInTick == false)
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("interactItem 2nd");
            // Second or more call in a same render() / tick() call. Must cancel.
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
