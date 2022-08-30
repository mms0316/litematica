package fi.dy.masa.litematica.mixin;

import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(value = SignEditScreen.class, priority = 999)
public abstract class MixinGuiEditSign
{
    @Shadow @Final private SignBlockEntity sign;

    @Shadow @Final private String[] text;

    @Inject(method = "init", at = @At("RETURN"))
    private void insertSignText(CallbackInfo ci)
    {
        if (Configs.Generic.SIGN_TEXT_PASTE.getBooleanValue())
        {
            WorldUtils.insertSignTextFromSchematic(this.sign, this.text);
        }
    }
}
