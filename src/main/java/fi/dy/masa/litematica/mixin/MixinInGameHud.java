//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.util.AddonUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    //Mixin at
    /*
    private void renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed) {
      if (!stack.isEmpty()) {
         float f = (float)stack.getBobbingAnimationTime() - tickCounter.getTickProgress(false);
         if (f > 0.0F) {
            float g = 1.0F + f / 5.0F;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float)(x + 8), (float)(y + 12));
            context.getMatrices().scale(1.0F / g, (g + 1.0F) / 2.0F);
            context.getMatrices().translate((float)(-(x + 8)), (float)(-(y + 12)));
         }

         context.drawItem(player, stack, x, y, seed);
         if (f > 0.0F) {
            context.getMatrices().popMatrix();
         }

         context.drawStackOverlay(this.client.textRenderer, stack, x, y);
      }
    }
     */
    @Inject(method = "renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V", at = @At(value = "RETURN"))
    private void litematica_onRenderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci)
    {
        AddonUtils.renderHotbarItem(context, x, y, stack);
    }
}
