//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.materials;

import static fi.dy.masa.malilib.util.JsonUtils.blockPosToJson;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.joml.Matrix4f;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

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
                    Litematica.LOGGER.warn("Failed to load material id at line {}", i);
                    continue;
                }
                var id = entry.get("id").getAsString();

                if (!JsonUtils.hasArray(entry, "pos")) {
                    Litematica.LOGGER.warn("Failed to load material positions at line {}", i);
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
                        Litematica.LOGGER.warn("Failed to load position at line {}", i);
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
        if (!Configs.InfoOverlays.MATERIAL_LIST_CONTAINER_OVERLAY_ENABLED.getBooleanValue()) return;

        var missingMaterials = materialList.getMaterialsMissingOnly(false);
        if (missingMaterials == null || missingMaterials.isEmpty()) return;

        final var color = Configs.Colors.MATERIAL_LIST_FETCH_CONTAINER_COLOR.getColor();

        for (var entry : missingMaterials) {
            var countMissing = entry.getCountMissing() - entry.getCountAvailable();
            if (countMissing <= 0) continue;

            var stackMissing = entry.getStack();
            var idMissing = stackMissing.getItem().toString();

            var positions = containerList.get(idMissing);
            if (positions == null) continue;

            for (var pos : positions) {
                fi.dy.masa.malilib.render.RenderUtils.renderBlockOutline(pos, 0.01f, 64f, color, true);
            }
        }
    }

    public void registerContainer(Minecraft mc) {
        if (mc.player == null || pos == null) return;
       
        var screenHandler = mc.player.containerMenu;
        if (!(screenHandler instanceof ChestMenu ||
                screenHandler instanceof ShulkerBoxMenu)) return;

        var screenSlots = screenHandler.slots;
        int screenMaxSlot = getScreenMaxSlot(screenHandler);

        // Clear matching positions first
        for (var blockPosList : containerList.values())
            blockPosList.remove(pos);

        // Add what's in the container
        int count = 0;
        for (var idx = 0; idx <= screenMaxSlot; idx++) {
            var containerSlot = screenSlots.get(idx);
            var containerStack = containerSlot.getItem();
            if (containerStack.isEmpty()) continue;
            var id = containerStack.getItem().toString();

            var blockPosList = this.containerList.computeIfAbsent(id, k -> new HashSet<>());
            blockPosList.add(pos);
            count++;
        }

        InfoUtils.printActionbarMessage("Registered " + count + " blocks from " + pos.toShortString());
    }

    public void unregisterContainer(Minecraft mc) {
        if (mc.player == null) return;

        boolean unregistered = false;
        BlockPos blockPos = null;
        var screenHandler = mc.player.containerMenu;

        if (screenHandler instanceof ChestMenu ||
                screenHandler instanceof ShulkerBoxMenu)
        {
            blockPos = pos;
        }
        else if (mc.level != null && GameWrap.getHitResult() instanceof BlockHitResult blockHitResult)
        {
            blockPos = blockHitResult.getBlockPos();
            var blockEntity = mc.level.getBlockEntity(blockPos);

            if (!(blockEntity instanceof RandomizableContainerBlockEntity)) {
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

    public void unregisterContainerAll(Minecraft mc) {
        if (containerList.isEmpty())
            InfoUtils.printActionbarMessage("Nothing to be unregistered");
        else {
            containerList.clear();
            InfoUtils.printActionbarMessage("Unregistered all containers");
        }
    }

    private int getScreenMaxSlot(AbstractContainerMenu screenHandler) {
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

    public void fetchMaterials(Minecraft mc) {
        if (mc.player == null) return;
        var screenHandler = mc.player.containerMenu;
        if (!(screenHandler instanceof ChestMenu ||
                screenHandler instanceof ShulkerBoxMenu)) return;

        var materialList = DataManager.getMaterialList();
        if (materialList == null) return;

        var missingMaterials = materialList.getMaterialsMissingOnly(true);
        if (missingMaterials == null || missingMaterials.isEmpty()) return;

        var screenSlots = screenHandler.slots;
        final int screenMaxSlot = getScreenMaxSlot(screenHandler);

        final int multiplier = materialList.getMultiplier();

        for (var entry : missingMaterials) {
            var stackMissing = entry.getStack();
            var countMissing = (entry.getCountMissing() * multiplier) - entry.getCountAvailable();
            if (countMissing <= 0) continue;

            for (var idx = screenMaxSlot; idx >= 0; idx--) {
                var containerSlot = screenSlots.get(idx);
                var repeatIteration = false;

                var containerStack = containerSlot.getItem();
                if (!InventoryUtils.areStacksEqualIgnoreNbt(containerStack, stackMissing)) continue;

                Litematica.debugLog("Fetching " + countMissing + " from " + containerStack.getHoverName());

                //https://wiki.vg/Protocol#Click_Container

                var containerCountOld = containerStack.getCount();
                if (containerCountOld <= countMissing) {
                    //Fetch entire stack
                    GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.QUICK_MOVE);
                    if (containerStack.getCount() != 0) {
                        //Couldn't get entire stack - this material is finished
                        break;
                    }
                } else {
                    if (containerCountOld / 2 <= countMissing) {
                        //Move half to cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 1, ClickType.PICKUP);
                        //Fetch the other half
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.QUICK_MOVE);
                        var remainingContainerStack = containerStack.getCount();
                        //Restore cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);

                        if (remainingContainerStack != 0) {
                            //Couldn't get entirely the other half - this material is finished
                            break;
                        } else {
                            repeatIteration = true;
                        }
                    } else {
                        //Find available stack to drop one
                        int targetSlot = findInventorySlotToFill(screenHandler, stackMissing);
                        if (targetSlot < 0)
                            break; //No space in inventory

                        //Move stack to cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);
                        //Drop one by one
                        for (int i = 0; i < countMissing; i++) {
                            var cursorBefore = screenHandler.getCarried().getCount();
                            GameWrap.clickSlot(screenHandler.containerId, targetSlot, 1, ClickType.PICKUP);
                            var cursorAfter = screenHandler.getCarried().getCount();
                            if (cursorBefore == cursorAfter) {
                                //Nothing moved - target slot has become full
                                repeatIteration = true; //Try another destination slot
                                break;
                            }
                        }
                        //Restore cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);
                    }
                }

                var containerCountNew = containerSlot.getItem().getCount();
                countMissing -= (containerCountOld - containerCountNew);
                if (countMissing <= 0)
                    break;

                if (repeatIteration)
                    idx++;
            }
        }

        materialList.getHudRenderer().clearUpdateTime();
    }

    public void fetchMaterialsKeepStacks(Minecraft mc) {
        if (mc.player == null) return;
        var screenHandler = mc.player.containerMenu;
        if (!(screenHandler instanceof ChestMenu ||
                screenHandler instanceof ShulkerBoxMenu)) return;

        var materialList = DataManager.getMaterialList();
        if (materialList == null) return;

        var missingMaterials = materialList.getMaterialsMissingOnly(true);
        if (missingMaterials == null || missingMaterials.isEmpty()) return;

        var screenSlots = screenHandler.slots;
        final int screenMaxSlot = getScreenMaxSlot(screenHandler);

        final int multiplier = materialList.getMultiplier();

        for (var entry : missingMaterials) {
            var stackMissing = entry.getStack();
            var countMissing = (entry.getCountMissing() * multiplier) - entry.getCountAvailable();
            if (countMissing <= 0) continue;

            int spaceForStackInInventory = 0;
            for (int invIdx = screenMaxSlot + 1; invIdx < screenSlots.size(); invIdx++) {
                var slot = screenHandler.slots.get(invIdx);
                var slotStack = slot.getItem();
                if (slotStack.isEmpty()) continue;

                if (!InventoryUtils.areStacksEqualIgnoreNbt(slotStack, stackMissing)) continue;

                spaceForStackInInventory += slotStack.getMaxStackSize() - slotStack.getCount();
            }
            if (spaceForStackInInventory <= 0) continue;

            for (var idx = screenMaxSlot; idx >= 0; idx--) {
                var containerSlot = screenSlots.get(idx);
                var repeatIteration = false;

                var containerStack = containerSlot.getItem();
                if (!InventoryUtils.areStacksEqualIgnoreNbt(containerStack, stackMissing)) continue;

                Litematica.debugLog("Fetching " + countMissing + " from " + containerStack.getHoverName());

                //https://minecraft.wiki/w/Java_Edition_protocol#Click_Container
                //https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Inventory#Windows

                // Possible action 1: Fetching entire stack without overshooting
                var containerCountOld = containerStack.getCount();
                if (containerCountOld <= countMissing && containerCountOld <= spaceForStackInInventory) {
                    //Fetch entire stack
                    GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.QUICK_MOVE);

                    spaceForStackInInventory -= containerCountOld;
                } else {
                    // Possible action 2: Fetching half of the stack without overshooting
                    if (containerCountOld / 2 <= countMissing && containerCountOld / 2 <= spaceForStackInInventory) {
                        //Move half to cursor (rounded up)
                        GameWrap.clickSlot(screenHandler.containerId, idx, 1, ClickType.PICKUP);
                        //Fetch the other half (rounded down)
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.QUICK_MOVE);
                        //Restore cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);

                        spaceForStackInInventory -= containerCountOld / 2;
                        repeatIteration = true;
                    } else {
                        //Find available stack to drop one
                        int targetSlot = findNonEmptyInventorySlotToFill(screenHandler, stackMissing);
                        if (targetSlot < 0)
                            break; //No space in inventory

                        //Move stack to cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);
                        //Drop one by one
                        for (int i = 0; i < countMissing; i++) {
                            var cursorBefore = screenHandler.getCarried().getCount();
                            GameWrap.clickSlot(screenHandler.containerId, targetSlot, 1, ClickType.PICKUP);
                            var cursorAfter = screenHandler.getCarried().getCount();
                            if (cursorBefore == cursorAfter) {
                                //Nothing moved - target slot has become full
                                repeatIteration = true; //Try another destination slot
                                break;
                            }

                            spaceForStackInInventory--;
                        }
                        //Restore cursor
                        GameWrap.clickSlot(screenHandler.containerId, idx, 0, ClickType.PICKUP);
                    }
                }

                if (spaceForStackInInventory <= 0)
                    break;

                var containerCountNew = containerSlot.getItem().getCount();
                countMissing -= (containerCountOld - containerCountNew);
                if (countMissing <= 0)
                    break;

                if (repeatIteration)
                    idx++;
            }
        }

        materialList.getHudRenderer().clearUpdateTime();
    }

    /**
     * @param screenHandler Chest / Double Chest / Shulker Box screen handler
     * @param match         ItemStack that will be searched for in screen handler
     * @return Slot index pointing to the slot with the fewest count of the ItemStack, or an empty slot, of the
     * player's inventory or hotbar.
     */
    private int findInventorySlotToFill(AbstractContainerMenu screenHandler, ItemStack match) {
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
            var slotStack = slot.getItem();

            if (slotStack.isEmpty()) {
                if (emptySlot == -1) {
                    emptySlot = destSlot;
                }
            } else {
                var slotStackCount = slotStack.getCount();
                if (slotStackCount == slotStack.getMaxStackSize()) {
                    //Not a partial slot
                    continue;
                }

                if (InventoryUtils.areStacksEqualIgnoreNbt(match, slotStack)) {
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

    /**
     * @param screenHandler Chest / Double Chest / Shulker Box screen handler
     * @param match         ItemStack that will be searched for in screen handler
     * @return Slot index pointing to the slot with the fewest count of the ItemStack of the
     * player's inventory or hotbar.
     */
    private int findNonEmptyInventorySlotToFill(AbstractContainerMenu screenHandler, ItemStack match) {
        //https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Inventory#Windows
        int minDestSlot = 27;
        int maxDestSlot = 62;
        if (screenHandler.slots.size() == 90) {
            minDestSlot += 27;
            maxDestSlot += 27;
        }

        int partialSlot = -1;
        int partialSlotCount = -1;
        // Reversed because shift+click also does this
        for (int destSlot = maxDestSlot; destSlot >= minDestSlot; destSlot--) {
            var slot = screenHandler.slots.get(destSlot);
            var slotStack = slot.getItem();
            if (slotStack.isEmpty()) continue;

            var slotStackCount = slotStack.getCount();
            if (slotStackCount == slotStack.getMaxStackSize()) {
                //Not a partial slot
                continue;
            }

            if (InventoryUtils.areStacksEqualIgnoreNbt(match, slotStack)) {
                if (partialSlot == -1 || slotStackCount < partialSlotCount) {
                    partialSlot = destSlot;
                    partialSlotCount = slotStack.getCount();

                    if (partialSlotCount == 1) {
                        break; //cant get lower than this
                    }
                }
            }
        }

        return partialSlot;
    }

    public void setContainerPos(BlockPos pos) {
        this.pos = pos;
    }
}
