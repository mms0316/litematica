package fi.dy.masa.litematica.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntityDataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;

//Custom Additions (easier to resolve future merge conflicts)
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacementPersistent;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.util.AddonUtils;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.level.ChunkPos;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener
{
    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void litematica_onUpdateChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci)
    {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
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

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void litematica_onChunkUnload(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            //Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onChunkUnload({}, {})", packet.pos().x, packet.pos().z);
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.pos().x, packet.pos().z);
        }
    }

    @Inject(method = "handleSystemChat", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void litematica_onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }

    /**
     * They keep moving where the effective onCustomPayload handling is... keeping them both
     */
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void litematica_onCustomPayload(CustomPacketPayload payload, CallbackInfo ci)
    {
        if (payload.type().id().equals(DataManager.CARPET_HELLO))
        {
            Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onCustomPayload(): received carpet hello packet");
            DataManager.setIsCarpetServer(true);
        }
        else if (payload.type().id().getNamespace().equals("servux"))
        {
            DataManager.setHasServuxServer(true);
        }
    }

    @Inject(method = "handleTagQueryPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DebugQueryHandler;handleResponse(ILnet/minecraft/nbt/CompoundTag;)Z"))
    private void litematica_onQueryResponse(ClientboundTagQueryPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            EntityDataManager.getInstance().handleVanillaQueryNbt(packet.getTransactionId(), packet.getTag());
        }
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void minihud_onCommandTree(CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            // when the player becomes OP, the server sends the command tree to the client
            EntityDataManager.getInstance().resetOpCheck();
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

    // Mixin 1.21.11:
    /*
    public void handleContainerContent(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket) {
      PacketUtils.ensureRunningOnSameThread(clientboundContainerSetContentPacket, this, this.minecraft.packetProcessor());
      Player player = this.minecraft.player;
      if (clientboundContainerSetContentPacket.containerId() == 0) {
         player.inventoryMenu.initializeContents(clientboundContainerSetContentPacket.stateId(), clientboundContainerSetContentPacket.items(), clientboundContainerSetContentPacket.carriedItem());
      } else if (clientboundContainerSetContentPacket.containerId() == player.containerMenu.containerId) {
         player.containerMenu.initializeContents(clientboundContainerSetContentPacket.stateId(), clientboundContainerSetContentPacket.items(), clientboundContainerSetContentPacket.carriedItem());
      }

    }
     */
    @Inject(method = "handleContainerContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/InventoryMenu;initializeContents(ILjava/util/List;Lnet/minecraft/world/item/ItemStack;)V"), cancellable = true)
    private void litematica_onPlayerInventoryUpdate(ClientboundContainerSetContentPacket clientboundContainerSetContentPacket, CallbackInfo ci)
    {
        if (AddonUtils.isInventoryUpdateSkipped()) {
            ci.cancel();
        }
    }
}
