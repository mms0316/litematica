package fi.dy.masa.litematica.materials;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static fi.dy.masa.malilib.util.JsonUtils.blockPosToJson;

public class ContainerManager {
    private final HashMap<String, Set<BlockPos>> containerList = new HashMap<>();
    private BlockPos pos;

    public ContainerManager() {
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        if (!this.containerList.isEmpty()) {
            JsonArray arr = new JsonArray();

            for (var entries : this.containerList.entrySet()) {
                var id = entries.getKey();
                var posList = entries.getValue();
                var posArray = new JsonArray();

                for (var pos : posList) {
                    var posJson = blockPosToJson(pos);
                    posArray.add(posJson);
                }

                JsonObject child = new JsonObject();

                child.addProperty("id", id);
                child.add("pos", posArray);

                arr.add(child);
            }

            obj.add("materials", arr);
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj) {
        containerList.clear();

        if (JsonUtils.hasArray(obj, "materials")) {
            JsonArray arr = obj.get("materials").getAsJsonArray();
            final int size = arr.size();

            for (int i = 0; i < size; ++i) {
                var entry = arr.get(i).getAsJsonObject();

                if (!JsonUtils.hasString(entry, "id")) {
                    Litematica.logger.warn("Failed to load material id at line {}", i);
                    continue;
                }
                var id = entry.get("id").getAsString();

                if (!JsonUtils.hasArray(entry, "pos")) {
                    Litematica.logger.warn("Failed to load material positions at line {}", i);
                    continue;
                }
                var posArray = entry.getAsJsonArray("pos");

                for (int j = 0; j < posArray.size(); j++) {
                    BlockPos blockPos = null;
                    try {
                        var pos = posArray.get(j).getAsJsonArray();
                        blockPos = new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());
                    } catch (Exception ignored) { }

                    if (blockPos == null) {
                        Litematica.logger.warn("Failed to load position at line {}", i);
                        continue;
                    }

                    var list = containerList.computeIfAbsent(id, k -> new HashSet<>());
                    list.add(blockPos);
                }
            }
        }
    }

    public void renderMatchingMaterials(MaterialListBase materialList, Matrix4f projMatrix) {
        if (materialList == null) return;
        if (containerList.isEmpty()) return;

        var missingMaterials = materialList.getMaterialsMissingOnly(false);
        if (missingMaterials == null || missingMaterials.isEmpty()) return;

        final var mc = MinecraftClient.getInstance();
        final var color = Configs.Colors.MATERIAL_LIST_FETCH_CONTAINER_COLOR.getColor();

        RenderSystem.disableDepthTest();

        for (var entry : missingMaterials) {
            var countMissing = entry.getCountMissing() - entry.getCountAvailable();
            if (countMissing <= 0) continue;

            var stackMissing = entry.getStack();
            var idMissing = stackMissing.getItem().toString();

            var positions = containerList.get(idMissing);
            if (positions == null) continue;

            for (var pos : positions) {
                fi.dy.masa.litematica.render.RenderUtils.renderBlockOutline(pos, 0.01f, 64f, color, mc);
            }
        }
    }

    public void registerContainer(MinecraftClient mc) {
        if (mc.player == null || pos == null) return;

        var screenHandler = mc.player.currentScreenHandler;
        if (!(screenHandler instanceof GenericContainerScreenHandler ||
                screenHandler instanceof ShulkerBoxScreenHandler)) return;

        var screenSlots = screenHandler.slots;
        int screenMaxSlot = getScreenMaxSlot(screenHandler);

        // Clear matching positions first
        for (var blockPosList : containerList.values())
            blockPosList.remove(pos);

        // Add what's in the container
        int count = 0;
        for (var idx = 0; idx <= screenMaxSlot; idx++) {
            var containerSlot = screenSlots.get(idx);
            var containerStack = containerSlot.getStack();
            if (containerStack.isEmpty()) continue;
            var id = containerStack.getItem().toString();

            var blockPosList = this.containerList.computeIfAbsent(id, k -> new HashSet<>());
            blockPosList.add(pos);
            count++;
        }

        InfoUtils.printActionbarMessage("Registered " + count + " blocks from " + pos.toShortString());
    }

    public void unregisterContainer(MinecraftClient mc) {
        if (mc.player == null) return;

        boolean unregistered = false;
        BlockPos blockPos = null;
        var screenHandler = mc.player.currentScreenHandler;
        if (screenHandler instanceof GenericContainerScreenHandler ||
                screenHandler instanceof ShulkerBoxScreenHandler)
        {
            blockPos = pos;
        }
        else if (mc.crosshairTarget instanceof BlockHitResult blockHitResult && mc.world != null)
        {
            blockPos = blockHitResult.getBlockPos();
            var blockEntity = mc.world.getBlockEntity(blockPos);
            if (!(blockEntity instanceof LootableContainerBlockEntity)) {
                blockPos = null;
            }
        }

        if (blockPos != null)
        {
            for (var blockPosList : containerList.values())
                if (blockPosList.remove(blockPos))
                    unregistered = true;

            if (unregistered)
                InfoUtils.printActionbarMessage("Unregistered from " + blockPos.toShortString());
            else
                InfoUtils.printActionbarMessage("Nothing to be unregistered");
        }
    }

    private int getScreenMaxSlot(ScreenHandler screenHandler) {
        if (screenHandler.slots.size() == 90) {
            //https://wiki.vg/Inventory#Large_chest
            //Skip if slot is already in player's inventory or hotbar
            return 53;
        } else {
            //https://wiki.vg/Inventory#Chest
            //https://wiki.vg/Inventory#Shulker_box
            return 26;
        }
    }

    public void fetchMaterials(MinecraftClient mc) {
        if (mc.player == null) return;
        var screenHandler = mc.player.currentScreenHandler;
        if (!(screenHandler instanceof GenericContainerScreenHandler ||
                screenHandler instanceof ShulkerBoxScreenHandler)) return;

        var materialList = DataManager.getMaterialList();
        if (materialList == null) return;

        var missingMaterials = materialList.getMaterialsMissingOnly(true);
        if (missingMaterials == null || missingMaterials.isEmpty()) return;

        var screenSlots = screenHandler.slots;
        final int screenMaxSlot = getScreenMaxSlot(screenHandler);

        for (var entry : missingMaterials) {
            var stackMissing = entry.getStack();
            var countMissing = entry.getCountMissing() - entry.getCountAvailable();
            if (countMissing <= 0) continue;

            for (var idx = 0; idx <= screenMaxSlot; idx++) {
                var containerSlot = screenSlots.get(idx);
                var repeatIteration = false;

                var containerStack = containerSlot.getStack();
                if (!containerStack.isItemEqual(stackMissing)) continue;

                Litematica.debugLog("Fetching " + countMissing + " from " + containerStack.getName());

                //https://wiki.vg/Protocol#Click_Container

                var containerCountOld = containerStack.getCount();
                if (containerCountOld <= countMissing) {
                    //Fetch entire stack
                    mc.interactionManager.clickSlot(screenHandler.syncId, idx, 0, SlotActionType.QUICK_MOVE, mc.player);
                    if (containerStack.getCount() != 0) {
                        //Couldn't get entire stack - this material is finished
                        break;
                    }
                } else {
                    if (containerCountOld / 2 <= countMissing) {
                        //Move half to cursor
                        mc.interactionManager.clickSlot(screenHandler.syncId, idx, 1, SlotActionType.PICKUP, mc.player);
                        //Fetch the other half
                        mc.interactionManager.clickSlot(screenHandler.syncId, idx, 0, SlotActionType.QUICK_MOVE, mc.player);
                        var remainingContainerStack = containerStack.getCount();
                        //Restore cursor
                        mc.interactionManager.clickSlot(screenHandler.syncId, idx, 0, SlotActionType.PICKUP, mc.player);

                        if (remainingContainerStack != 0) {
                            //Couldn't get entirely the other half - this material is finished
                            break;
                        } else {
                            repeatIteration = true;
                        }
                    } else {
                        //Find available stack to drop one
                        var targetSlot = findInventorySlotToFill(screenHandler, stackMissing);
                        if (targetSlot < 0)
                            break; //No space in inventory

                        //Move stack to cursor
                        mc.interactionManager.clickSlot(screenHandler.syncId, idx, 0, SlotActionType.PICKUP, mc.player);
                        //Drop one by one
                        for (int i = 0; i < countMissing; i++) {
                            var cursorBefore = screenHandler.getCursorStack().getCount();
                            mc.interactionManager.clickSlot(screenHandler.syncId, targetSlot, 1, SlotActionType.PICKUP, mc.player);
                            var cursorAfter = screenHandler.getCursorStack().getCount();
                            if (cursorBefore == cursorAfter) {
                                //Nothing moved - target slot has become full
                                repeatIteration = true; //Try another destination slot
                                break;
                            }
                        }
                        //Restore cursor
                        mc.interactionManager.clickSlot(screenHandler.syncId, idx, 0, SlotActionType.PICKUP, mc.player);
                    }
                }

                var containerCountNew = containerSlot.getStack().getCount();
                countMissing -= (containerCountOld - containerCountNew);
                if (countMissing <= 0)
                    break;

                if (repeatIteration)
                    idx--;
            }
        }
    }


    /**
     * @param screenHandler Chest / Double Chest / Shulker Box screen handler
     * @param match         ItemStack that will be searched for in screen handler
     * @return Slot index pointing to the slot with the fewest count of the ItemStack, or an empty slot, of the
     * player's inventory or hotbar.
     */
    private int findInventorySlotToFill(ScreenHandler screenHandler, ItemStack match) {
        //https://wiki.vg/Inventory#Chest
        int minDestSlot = 27;
        int maxDestSlot = 62;
        if (screenHandler.slots.size() == 90) {
            //https://wiki.vg/Inventory#Large_chest
            minDestSlot += 27;
            maxDestSlot += 27;
        }

        int emptySlot = -1;
        int partialSlot = -1;
        int partialSlotCount = -1;
        // Reversed because shift+click also does this
        for (int destSlot = maxDestSlot; destSlot >= minDestSlot; destSlot--) {
            var slot = screenHandler.slots.get(destSlot);
            var slotStack = slot.getStack();

            if (slotStack.isEmpty()) {
                if (emptySlot == -1) {
                    emptySlot = destSlot;
                }
            } else {
                var slotStackCount = slotStack.getCount();
                if (slotStackCount == slotStack.getMaxCount()) {
                    //Not a partial slot
                    continue;
                }

                if (ItemStack.canCombine(match, slotStack)) {
                    if (partialSlot == -1 || slotStackCount < partialSlotCount) {
                        partialSlot = destSlot;
                        partialSlotCount = slotStack.getCount();

                        if (partialSlotCount == 1) {
                            break; //cant get lower than this
                        }
                    }
                }
            }
        }

        if (partialSlot == -1) {
            return emptySlot;
        } else {
            return partialSlot;
        }
    }

    public void setContainerPos(BlockPos pos) {
        this.pos = pos;
    }
}
