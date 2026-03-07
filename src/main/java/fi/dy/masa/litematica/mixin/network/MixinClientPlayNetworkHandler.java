package fi.dy.masa.litematica.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.*;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.util.AddonUtils;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacementPersistent;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;


@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler
{
    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void litematica_onUpdateChunk(ChunkDataS2CPacket packet, CallbackInfo ci)
    {
        int chunkX = packet.getChunkX();
        int chunkZ = packet.getChunkZ();
        //Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onUpdateChunk({}, {})", chunkX, chunkZ);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(chunkX, chunkZ);

            //Custom Additions (easier to resolve future merge conflicts)
            if (Configs.Generic.SCHEMATIC_VERIFIER_CHECK_CHUNK_RELOAD.getBooleanValue())
            {
                SchematicVerifier.markVerifierChunkChanges(chunkX, chunkZ);
            }
        }

        DataManager.getSchematicPlacementManager().onClientChunkLoad(chunkX, chunkZ);

        //Custom Additions (easier to resolve future merge conflicts)
        TaskScheduler.getInstanceClient().getAllTasks().stream()
        .filter(task -> task instanceof TaskCountBlocksPlacementPersistent)
        .forEach(task -> ((TaskCountBlocksPlacementPersistent)task).onChunkData(new ChunkPos(chunkX, chunkZ)));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void litematica_onChunkUnload(UnloadChunkS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            //Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onChunkUnload({}, {})", packet.pos().x, packet.pos().z);
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.pos().x, packet.pos().z);
        }
    }

    @Inject(method = "onGameMessage", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"))
    private void litematica_onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }

    /**
     * They keep moving where the effective onCustomPayload handling is... keeping them both
     */
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void litematica_onCustomPayload(CustomPayload payload, CallbackInfo ci)
    {
        if (payload.getId().id().equals(DataManager.CARPET_HELLO))
        {
            Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onCustomPayload(): received carpet hello packet");
            DataManager.setIsCarpetServer(true);
        }
    }

    @Inject(method = "onNbtQueryResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/DataQueryHandler;handleQueryResponse(ILnet/minecraft/nbt/NbtCompound;)Z"))
    private void litematica_onQueryResponse(NbtQueryResponseS2CPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            EntitiesDataStorage.getInstance().handleVanillaQueryNbt(packet.getTransactionId(), packet.getNbt());
        }
    }

    @Inject(method = "onCommandTree", at = @At("RETURN"))
    private void minihud_onCommandTree(CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            // when the player becomes OP, the server sends the command tree to the client
            EntitiesDataStorage.getInstance().resetOpCheck();
        }
    }

    //Custom Additions (easier to resolve future merge conflicts)
    //Mixin at
    /*
    public void onInventory(InventoryS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client);
      PlayerEntity playerEntity = this.client.player;
      if (packet.syncId() == 0) {
         playerEntity.playerScreenHandler.updateSlotStacks(packet.revision(), packet.contents(), packet.cursorStack());
      } else if (packet.syncId() == playerEntity.currentScreenHandler.syncId) {
         playerEntity.currentScreenHandler.updateSlotStacks(packet.revision(), packet.contents(), packet.cursorStack());
      }
    }
     */
    @Inject(method = "onInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/PlayerScreenHandler;updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V"), cancellable = true)
    private void litematica_onPlayerInventoryUpdate(InventoryS2CPacket packet, CallbackInfo ci)
    {
        if (AddonUtils.isInventoryUpdateSkipped()) {
            ci.cancel();
        }
    }
}
