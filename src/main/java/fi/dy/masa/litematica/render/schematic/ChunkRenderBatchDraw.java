package fi.dy.masa.litematica.render.schematic;

import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.util.profiling.ProfilerFiller;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;

public record ChunkRenderBatchDraw(
		GpuTextureView atlasTexture,
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawData,
        boolean renderCollidingBlocks,
		boolean renderTranslucent,
        int maxIndicesRequired,
		GpuBufferSlice[] dynamicTransforms,
		GpuBuffer chunkFixUBO)
{
    public void draw(ChunkSectionLayerGroup group, GpuSampler sampler, ProfilerFiller profiler)
    {
        RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpuBuffer = this.maxIndicesRequired() == 0 ? null : shapeIndexBuffer.getBuffer(this.maxIndicesRequired());
        VertexFormat.IndexType indexType = this.maxIndicesRequired() == 0 ? null : shapeIndexBuffer.type();
        ChunkSectionLayer[] layers = group.layers();
        Minecraft mc = Minecraft.getInstance();
        RenderTarget fb = group.outputTarget();

        profiler.push("draw_group");

		try (RenderPass pass = RenderSystem.getDevice()
		                                   .createCommandEncoder()
		                                   .createRenderPass(
				                                   () -> "litematica:schematic_chunk/" + group.label(),
				                                   fb.getColorTextureView(),
				                                   OptionalInt.empty(),
				                                   fb.getDepthTextureView(),
				                                   OptionalDouble.empty()
		                                   ))
		{
			RenderSystem.bindDefaultUniforms(pass);

//			if (renderTranslucent() && this.dynamicTransform() != null)
//			{
//				pass.setUniform("DynamicTransforms", this.dynamicTransform());
//			}

			pass.setUniform("ChunkFix", this.chunkFixUBO);
			pass.bindTexture("Sampler2",
			                 mc.gameRenderer.lightTexture().getTextureView(),
			                 RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
			);

			for (ChunkSectionLayer layer : layers)
			{
				List<RenderPass.Draw<GpuBufferSlice[]>> list = this.drawData().get(layer);

				profiler.popPush("draw_group_" + layer.label());
				if (!list.isEmpty())
				{
					if (layer == ChunkSectionLayer.TRANSLUCENT)
					{
						list = list.reversed();
					}

					if (this.renderTranslucent())
					{
						pass.setPipeline(this.renderCollidingBlocks()
						                 ? ChunkRenderLayers.PIPELINE_MAP.get(ChunkSectionLayer.TRANSLUCENT).getRight()
						                 : ChunkRenderLayers.PIPELINE_MAP.get(ChunkSectionLayer.TRANSLUCENT).getLeft()
						);
					}
					else
					{
						pass.setPipeline(this.renderCollidingBlocks()
						                 ? ChunkRenderLayers.PIPELINE_MAP.get(layer).getRight()
						                 : ChunkRenderLayers.PIPELINE_MAP.get(layer).getLeft()
						);
					}

					pass.bindTexture("Sampler0", this.atlasTexture(), sampler);
//					pass.drawMultipleIndexed(list, gpuBuffer, indexType, List.of("ChunkSection"), this.chunkSections());
					pass.drawMultipleIndexed(list, gpuBuffer, indexType, List.of("DynamicTransforms"), this.dynamicTransforms());
				}
			}
		}
		catch (Exception ignored) { }

        profiler.pop();
    }
}
