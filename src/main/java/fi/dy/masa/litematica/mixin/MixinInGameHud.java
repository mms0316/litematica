//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.util.AddonUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(Gui.class)
public class MixinInGameHud {

    //Mixin before 1.21.8
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
    //Mixin after 1.21.11
    /*
    private void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed) {
      if (!itemStack.isEmpty()) {
         float f = (float)itemStack.getPopTime() - deltaTracker.getGameTimeDeltaPartialTick(false);
         if (f > 0.0F) {
            float g = 1.0F + f / 5.0F;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(x + 8), (float)(y + 12));
            guiGraphics.pose().scale(1.0F / g, (g + 1.0F) / 2.0F);
            guiGraphics.pose().translate((float)(-(x + 8)), (float)(-(y + 12)));
         }

         guiGraphics.renderItem(player, itemStack, x, y, seed);
         if (f > 0.0F) {
            guiGraphics.pose().popMatrix();
         }

         guiGraphics.renderItemDecorations(this.minecraft.font, itemStack, x, y);
      }
    }
     */

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V", at = @At(value = "RETURN"))
    private void litematica_onRenderHotbarItem(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int seed, CallbackInfo ci)
    {
        AddonUtils.renderHotbarItem(guiGraphics, x, y, itemStack);
    }
}
