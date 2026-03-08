package fi.dy.masa.litematica.mixin.easyplace;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import net.minecraft.client.Minecraft;

/**
 * Post Re-Write code
 */
@Mixin(value = Minecraft.class)
public abstract class MixinMinecraftClient_easyPlace
{
    @Inject(method = "handleKeybinds",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;startUseItem()V"))
    private void onUseKeyPre(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            EasyPlaceUtils.setIsFirstClick();
        }
    }

    @Inject(method = "startUseItem", at = @At("TAIL"))
    private void onUseKeyPost(CallbackInfo ci)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
        {
            if (EasyPlaceUtils.shouldDoEasyPlaceActions())
            {
                EasyPlaceUtils.onRightClickTail();
            }
        }
    }
}
