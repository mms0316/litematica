package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.OverlayRenderer;
import org.apache.commons.lang3.ArrayUtils;


public class InventoryUtils
{
    private static final Map<Integer, Long> PICK_BLOCKABLE_SLOTS = new HashMap<>();

    public static void setPickBlockableSlots(String configStr)
    {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");
        Pattern patternRange = Pattern.compile("^(?<start>[0-9])-(?<end>[0-9])$");

        for (String str : parts)
        {
            try
            {
                Matcher matcher = patternRange.matcher(str);

                if (matcher.matches())
                {
                    int slotStart = Integer.parseInt(matcher.group("start")) - 1;
                    int slotEnd = Integer.parseInt(matcher.group("end")) - 1;

                    if (slotStart <= slotEnd &&
                        PlayerInventory.isValidHotbarIndex(slotStart) &&
                        PlayerInventory.isValidHotbarIndex(slotEnd))
                    {
                        for (int slotNum = slotStart; slotNum <= slotEnd; ++slotNum)
                        {
                            PICK_BLOCKABLE_SLOTS.put(slotNum, 0L);
                        }
                    }
                }
                else
                {
                    int slotNum = Integer.parseInt(str) - 1;

                    if (PlayerInventory.isValidHotbarIndex(slotNum))
                    {
                        PICK_BLOCKABLE_SLOTS.put(slotNum, 0L);
                    }
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    public static boolean setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc)
    {
        boolean changed = false;
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            refreshSlotTimeout(sourceSlot);

            if (sourceSlot != inventory.selectedSlot)
            {
                inventory.selectedSlot = sourceSlot;
                changed = true;
            }
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return changed;
            }

            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || PlayerInventory.isValidHotbarIndex(sourceSlot) == false)
            {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1)
            {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1)
            {
                refreshSlotTimeout(hotbarSlot);

                inventory.selectedSlot = hotbarSlot;

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.main.set(hotbarSlot, stack.copy());
                    changed = true;
                }
                else
                {
                    changed = fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc) || changed;
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }

        return changed;
    }

    public static void refreshSlotTimeout(int slot)
    {
        if (PICK_BLOCKABLE_SLOTS.containsKey(slot))
        {
            final long now = System.nanoTime();
            final long nextTimeout = now + (20 + Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue()) * 1_000_000L;
            PICK_BLOCKABLE_SLOTS.put(slot, nextTimeout);
        }
    }

    public static PickBlockResult schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               World schematicWorld, MinecraftClient mc)
    {
        if (stack.isEmpty() == false)
        {
            PlayerInventory inv = mc.player.getInventory();
            stack = stack.copy();

            if (EntityUtils.isCreativeMode(mc.player))
            {
                BlockEntity te = schematicWorld.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos))
                {
                    te.setStackNbt(stack, schematicWorld.getRegistryManager());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);

                return new PickBlockResult(inv.selectedSlot, false, true);
            }
            else
            {
                int slot = inv.getSlotWithStack(stack);

                var pickBlockResult = pickBlockSurvival(slot, stack, inv, mc);

                // Pick block did not happen - try substitutions
                if (pickBlockResult.changed == false && pickBlockResult.slot == -1)
                {
                    HashSet<String> substitutions = AddonUtils.getSubstitutions(Registries.ITEM.getId(stack.getItem()).toString());

                    for (int i = 0; i < inv.main.size(); ++i)
                    {
                        ItemStack iter = inv.main.get(i);
                        if (iter.isEmpty()) continue;

                        if (!substitutions.contains(Registries.ITEM.getId(iter.getItem()).toString())) continue;

                        pickBlockResult = pickBlockSurvival(i, iter, inv, mc);
                        if (pickBlockResult.slot != -1) break;
                    }

                    // Try Shulker boxes
                    if (pickBlockResult.changed == false && pickBlockResult.slot == -1 &&
                            Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
                    {
                        for (String s : substitutions)
                        {
                            ItemStack substStack = new ItemStack(Registries.ITEM.get(Identifier.of(s)));
                            slot = findSlotWithBoxWithItem(mc.player.playerScreenHandler, substStack, false);
                            if (slot != -1)
                            {
                                ItemStack boxStack = mc.player.playerScreenHandler.slots.get(slot).getStack();
                                setPickedItemToHand(boxStack, mc);

                                pickBlockResult = new PickBlockResult(slot, true, true);
                                break;
                            }
                        }
                    }
                }

                if (pickBlockResult.changed == false && pickBlockResult.pickedShulker == false)
                {
                    final var changed = AddonUtils.preRestockHand(mc.player, Hand.MAIN_HAND, 6, true);
                    pickBlockResult = new PickBlockResult(pickBlockResult.slot, false, changed);
                }

                if (pickBlockResult.slot == -1)
                {
                    InfoUtils.printActionbarMessage(GuiBase.TXT_RED + "Ran out of " + GuiBase.TXT_RST + stack.getName().getString());
                }
                else if (pickBlockResult.pickedShulker)
                {
                    InfoUtils.printActionbarMessage(GuiBase.TXT_YELLOW + "Refill " + GuiBase.TXT_RST + stack.getName().getString());
                }

                return pickBlockResult;
            }
        }

        return new PickBlockResult(-1, false, false);
    }

    public record PickBlockResult(int slot, boolean pickedShulker, boolean changed) { }

    private static PickBlockResult pickBlockSurvival(int slot, ItemStack stack, PlayerInventory inv, MinecraftClient mc)
    {
        boolean shouldPick = inv.selectedSlot != slot;
        boolean pickedShulker = false;
        boolean changed = false;

        if (slot != -1)
        {
            if (shouldPick)
            {
                setPickedItemToHand(stack, mc);
                changed = true;
            }
        }
        else if (Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
        {
            slot = findSlotWithBoxWithItem(mc.player.playerScreenHandler, stack, false);

            if (slot != -1)
            {
                ItemStack boxStack = mc.player.playerScreenHandler.slots.get(slot).getStack();
                setPickedItemToHand(boxStack, mc);
                pickedShulker = true;
                changed = true;
            }
        }

        return new PickBlockResult(slot, pickedShulker, changed);
    }


    private static boolean canPickToSlot(PlayerInventory inventory, int slotNum)
    {
        if (!PICK_BLOCKABLE_SLOTS.containsKey(slotNum))
        {
            return false;
        }

        ItemStack stack = inventory.getStack(slotNum);

        if (stack.isEmpty())
        {
            return true;
        }

        return (Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() == false ||
                stack.isDamageable() == false) &&
               (Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() == false ||
                (stack.getItem() instanceof ToolItem) == false);
    }

    private static int getPickBlockTargetSlot(PlayerEntity player)
    {
        int slotNum = -1;
        long now = System.nanoTime();

        // Find slot with lowest expired timeout
        long lowestTimeout = Long.MAX_VALUE;
        for (var entry : PICK_BLOCKABLE_SLOTS.entrySet())
        {
            long thisTimeout = entry.getValue();
            if (thisTimeout <= now && thisTimeout < lowestTimeout)
            {
                var possibleSlot = entry.getKey();
                if (canPickToSlot(player.getInventory(), possibleSlot))
                {
                    lowestTimeout = thisTimeout;
                    slotNum = possibleSlot;
                }
            }
        }

        if (slotNum < 0)
            return -1;

        return slotNum;
    }

    private static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory)
    {
        for (int slotNum : PICK_BLOCKABLE_SLOTS.keySet())
        {
            if (PlayerInventory.isValidHotbarIndex(slotNum))
            {
                ItemStack stack = inventory.getStack(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    public static boolean doesShulkerBoxContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    public static boolean doesBundleContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getBundleItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    private static boolean doesListContainItem(DefaultedList<ItemStack> items, ItemStack referenceItem)
    {
        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static int findSlotWithBoxWithItem(ScreenHandler container, ItemStack stackReference, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof PlayerScreenHandler;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((isPlayerInv == false || fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.id, false)) &&
                doesShulkerBoxContainItem(slot.getStack(), stackReference))
            {
                return slot.id;
            }
        }

        return -1;
    }
    /**
     * Get a valid Inventory Object by any means necessary.
     *
     * @param world (Input ClientWorld)
     * @param pos (Pos of the Tile Entity)
     * @return (The result Inventory | NULL if not obtainable)
     */
    @Nullable
    public static Inventory getInventory(World world, BlockPos pos)
    {
        Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);

        if ((inv == null || inv.isEmpty()) && DataManager.getInstance().hasIntegratedServer() == false)
        {
            OverlayRenderer.getInstance().requestBlockEntityAt(world, pos);
        }

        return inv;
    }

    /**
     * Converts an NbtCompound representation of an ItemStack into a '/give' compatible string.
     * This is the format used by the ItemStringReader(), including Data Components.
     *
     * @param nbt (Nbt Input, must be valid ItemStack.encode() format)
     * @return (The String Result | NULL if the NBT is invalid)
     */
    @Nullable
    public static String convertItemNbtToString(NbtCompound nbt)
    {
        StringBuilder result = new StringBuilder();

        if (nbt.isEmpty())
        {
            return null;
        }

        if (nbt.contains("id"))
        {
            result.append(nbt.getString("id"));
        }
        else
        {
            return null;
        }
        if (nbt.contains("components"))
        {
            NbtCompound components = nbt.getCompound("components");
            int count = 0;

            result.append("[");

            for (String key : components.getKeys())
            {
                if (count > 0)
                {
                    result.append(", ");
                }

                result.append(key);
                result.append("=");
                result.append(components.get(key));
                count++;
            }

            result.append("]");
        }
        if (nbt.contains("count"))
        {
            int count = nbt.getInt("count");

            if (count > 1)
            {
                result.append(" ");
                result.append(count);
            }
        }

        return result.toString();
    }

}
