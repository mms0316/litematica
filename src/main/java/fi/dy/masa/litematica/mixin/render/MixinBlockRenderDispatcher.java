package fi.dy.masa.litematica.mixin.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher
{
	@Inject(method = "onResourceManagerReload", at = @At("TAIL"))
	private void litematica_onBlockRenderManagerReload(ResourceManager manager, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().onBlockModelRendererReload((BlockRenderDispatcher) (Object) this);
	}
}
