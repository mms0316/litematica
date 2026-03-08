package fi.dy.masa.litematica.render;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.util.ItemUtils;

public class BlockInfo
{
    private final String title;
    private final BlockState state;
    private final ItemStack stack;
    private final String blockRegistryName;
    private final String stackName;
    private final List<String> props;
    private final int totalWidth;
    private final int totalHeight;
    private boolean useBackgroundMask = false;

    public BlockInfo(BlockState state, String titleKey)
    {
        String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
        this.title = pre + StringUtils.translate(titleKey) + GuiBase.TXT_RST;
        this.state = state;
        this.stack = ItemUtils.getItemForState(this.state);

        Identifier rl = BuiltInRegistries.BLOCK.getKey(this.state.getBlock());
        this.blockRegistryName = rl.toString();

        this.stackName = this.stack.getHoverName().getString();

        int w = StringUtils.getStringWidth(this.stackName) + 20;
        w = Math.max(w, StringUtils.getStringWidth(this.blockRegistryName));
        w = Math.max(w, StringUtils.getStringWidth(this.title));
        this.props = BlockUtils.getFormattedBlockStateProperties(this.state, " = ");
        this.totalWidth = w + 40;
        this.totalHeight = this.props.size() * (StringUtils.getFontHeight() + 2) + 60;
    }

    public int getTotalWidth()
    {
        return totalWidth;
    }

    public int getTotalHeight()
    {
        return totalHeight;
    }

    public void toggleUseBackgroundMask(boolean toggle)
    {
        this.useBackgroundMask = toggle;
    }

    public void render(GuiContext ctx, int x, int y)
    {
        if (this.state != null)
        {
            if (this.useBackgroundMask)
            {
                fi.dy.masa.litematica.render.RenderUtils.renderBackgroundMask(ctx, x + 1, y + 1, this.totalWidth - 1, this.totalHeight - 1);
            }

            RenderUtils.drawOutlinedBox(ctx, x, y, this.totalWidth, this.totalHeight, 0xFF000000, GuiBase.COLOR_HORIZONTAL_BAR);

            int x1 = x + 10;
            y += 4;

	        ctx.drawString(ctx.fontRenderer(), this.title, x1, y, 0xFFFFFFFF, false);
            y += 12;

            //mc.getRenderItem().zLevel += 100;
            RenderUtils.drawRect(ctx, x1, y, 16, 16, 0x20FFFFFF); // light background for the item
	        ctx.renderItem(this.stack, x1, y);
	        ctx.renderItemDecorations(ctx.fontRenderer(), this.stack, x1, y);
            //mc.getRenderItem().zLevel -= 100;

	        ctx.drawString(ctx.fontRenderer(), this.stackName, x1 + 20, y + 4, 0xFFFFFFFF, false);

            y += 20;
	        ctx.drawString(ctx.fontRenderer(), this.blockRegistryName, x1, y, 0xFF4060FF, false);
            y += ctx.fontRenderer().lineHeight + 4;

            RenderUtils.renderText(ctx, x1, y, 0xFFB0B0B0, this.props);
        }
    }
}
