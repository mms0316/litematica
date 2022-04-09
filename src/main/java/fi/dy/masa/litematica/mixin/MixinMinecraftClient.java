package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient extends ReentrantThreadExecutor<Runnable>
{
    public MixinMinecraftClient(String string_1)
    {
        super(string_1);
    }

    @Inject(method = "handleInputEvents()V", at = @At("HEAD"))
    private void onHandleInputEventsHead(CallbackInfo ci)
    {
        //Reset easyPlace variables
        WorldUtils.easyPlaceAllowedInTick = true;
        WorldUtils.allowNestedInteractBlock = false;
        WorldUtils.easyPlaceInformFailure = false;
    }

    @Inject(method = "handleInputEvents()V", at = @At("RETURN"))
    private void onHandleInputEventsReturn(CallbackInfo ci)
    {
        if (WorldUtils.shouldDoEasyPlaceActions())
        {
            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                Litematica.logger.info("handleInputEvents last");
            WorldUtils.easyPlaceAllowedInTick = false;
            WorldUtils.easyPlaceInformFailure = false;
            WorldUtils.handleEasyPlaceWithMessage(MinecraftClient.getInstance());
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }
}
