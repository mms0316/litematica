package fi.dy.masa.litematica.util;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * This code is meant to make it's "Best Effort" to get a valid
 * Entity Render State for Player Models; but also to not crash if it fails.
 */
public interface IEntityRendererInvoker
{
	<E extends Entity> EntityRenderer<? super E, ?> litematica_getEntityRendererNullSafe(E entity);
	<E extends Entity> EntityRenderState litematica_getRenderStateNullSafe(E entity, float tickProgress);
}
