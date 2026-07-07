package fi.dy.masa.litematica.mixin.screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.AddonUtils;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen
{
    private MixinAbstractContainerScreen(Component title)
    {
        super(title);
    }

    @Inject(method = "renderContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void litematica_renderSlotHighlightsPre(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory(GuiContext.fromGuiGraphics(drawContext), (AbstractContainerScreen<?>) (Object) this, this.minecraft);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void litematica_renderSlotHighlightsPost(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        final var context = GuiContext.fromGuiGraphics(drawContext);

        MaterialListHudRenderer.renderLookedAtBlockInInventory(context, (AbstractContainerScreen<?>) (Object) this, this.minecraft);

        //Custom Additions (easier to resolve future merge conflicts)
        if (Configs.Generic.HIGHLIGHT_REFILL_IN_INV.getBooleanValue())
        {
            final var color = Configs.Colors.HIGHLIGHT_REFILL_IN_INV_COLOR.getColor();
            final var guiScreen = (AbstractContainerScreen<?>) (Object) this;

            final var refillItems = AddonUtils.getRefillItems();
            refillItems.forEach(itemStack -> MaterialListHudRenderer.highlightSlotsWithItem(context, itemStack, guiScreen, color, this.minecraft));

            final var ranOutItem = AddonUtils.getRanOutItems();
            ranOutItem.forEach(itemStack -> MaterialListHudRenderer.highlightSlotsWithItem(context, itemStack, guiScreen, color, this.minecraft));
        }
    }
}
