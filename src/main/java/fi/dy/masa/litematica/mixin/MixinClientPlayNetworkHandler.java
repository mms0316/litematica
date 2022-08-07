package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void litematica_onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci)
    {
        final int x = packet.getX();
        final int z = packet.getZ();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(x, z);

            if (Configs.Generic.SCHEMATIC_VERIFIER_CHECK_CHUNK_RELOAD.getBooleanValue())
            {
                SchematicVerifier.markVerifierChunkChanges(x, z);
            }
        }

        DataManager.getSchematicPlacementManager().onClientChunkLoad(x, z);
    }

    @Inject(method = "onExplosion", at = @At("RETURN"))
    private void onExplosion(ExplosionS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            for (BlockPos block : packet.getAffectedBlocks())
            {
                SchematicVerifier.markVerifierBlockChanges(block);
            }
        }
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci)
    {
        packet.visitUpdates((p, s) -> {
            SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(p);
            SchematicVerifier.markVerifierBlockChanges(p);
        });
    }

    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci)
    {
        final var p = packet.getPos();
        SchematicWorldRefresher.INSTANCE.markSchematicChunkForRenderUpdate(p);
        SchematicVerifier.markVerifierBlockChanges(p);
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void onChunkUnload(UnloadChunkS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.getX(), packet.getZ());
        }
    }

    @Inject(method = "onGameMessage", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }
}
