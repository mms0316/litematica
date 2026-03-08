package fi.dy.masa.litematica.mixin.render;

import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderDispatcher.class)
public interface IMixinBlockRenderManager
{
	@Accessor("liquidBlockRenderer")
	LiquidBlockRenderer litematica_getFluidRenderer();
}
