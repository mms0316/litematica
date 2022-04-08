package fi.dy.masa.litematica.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.dy.masa.litematica.Litematica;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.gui.GuiBase;

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
                    int slotStart = Integer.parseInt(matcher.group("start"));
                    int slotEnd = Integer.parseInt(matcher.group("end"));

                    if (slotStart <= slotEnd &&
                        PlayerInventory.isValidHotbarIndex(slotStart - 1) &&
                        PlayerInventory.isValidHotbarIndex(slotEnd - 1))
                    {
                        for (int slotNum = slotStart; slotNum <= slotEnd; ++slotNum)
                        {
                            PICK_BLOCKABLE_SLOTS.put(slotNum, 0L);
                        }
                    }
                }
                else
                {
                    int slotNum = Integer.parseInt(str);

                    if (PlayerInventory.isValidHotbarIndex(slotNum - 1))
                    {
                        PICK_BLOCKABLE_SLOTS.put(slotNum, 0L);
                    }
                }
            }
            catch (NumberFormatException e)
            {
            }
        }
    }

    public static void setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();
        final long now = System.nanoTime();
        final long nextTimeout = now + (20 + Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue()) * 1_000_000L;

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            if (PICK_BLOCKABLE_SLOTS.containsKey(sourceSlot + 1))
            {
                PICK_BLOCKABLE_SLOTS.put(sourceSlot + 1, nextTimeout);
            }
            inventory.selectedSlot = sourceSlot;
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                return;
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
                PICK_BLOCKABLE_SLOTS.put(hotbarSlot + 1, nextTimeout);
                inventory.selectedSlot = hotbarSlot;

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.main.set(hotbarSlot, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }
            }
        }
    }

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
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
                    ItemUtils.storeTEInStack(stack, te);
                }

                setPickedItemToHand(stack, mc);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);

                //return true;
            }
            else
            {
                int slot = inv.getSlotWithStack(stack);
                boolean shouldPick = inv.selectedSlot != slot;

                if (slot != -1)
                {
                    if (shouldPick)
                    {
                        setPickedItemToHand(stack, mc);
                    }
                    else
                    {
                        preRestockHand(mc.player, Hand.MAIN_HAND, 6, true);
                    }
                }
                else if (Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
                {
                    slot = findSlotWithBoxWithItem(mc.player.playerScreenHandler, stack, false);

                    if (slot != -1)
                    {
                        ItemStack boxStack = mc.player.playerScreenHandler.slots.get(slot).getStack();
                        setPickedItemToHand(boxStack, mc);
                    }
                }

                //return shouldPick == false || canPick;
            }
        }
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
                lowestTimeout = thisTimeout;
                slotNum = entry.getKey() - 1;
            }
        }

        if (slotNum < 0)
            return -1;

        return slotNum;
    }

    private static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory)
    {
        for (int slot : PICK_BLOCKABLE_SLOTS.keySet())
        {
            int slotNum = slot - 1;

            if (slotNum >= 0 && slotNum < inventory.main.size())
            {
                ItemStack stack = inventory.main.get(slotNum);

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

        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(item, referenceItem))
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

    //Adapted from malilib liteloader_1.12.2 branch
    /**
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether or not to allow taking items from other hotbar slots
     */
    public static void preRestockHand(PlayerEntity player, Hand hand, int threshold, boolean allowHotbar)
    {
        final ItemStack stackHand = player.getEquippedStack(hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        final int count = stackHand.getCount();
        final int max = stackHand.getMaxCount();

        if (stackHand.isEmpty() == false &&
                (count <= threshold && count < max))
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            ScreenHandler container = player.playerScreenHandler;
            //mc.interactionManager.clickSlot() considers these slot numbers: https://wiki.vg/Inventory#Player_Inventory
            //36 - 44: hotbar
            //9 - 35: main inventory
            //45: offhand
            //Meanwhile, player.getInventory() considers these slot numbers:
            //0 - 8: hotbar
            //9 - 35: main inventory
            //40: offhand
            int endSlot = allowHotbar ? 44 : 35;
            PlayerInventory inventory = player.getInventory();
            int currentMainHandSlot = inventory.selectedSlot + 36;
            int currentSlot = hand == Hand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                ItemStack stackSlot = inventory.getStack(slotNum >= 36 ? slotNum - 36 : slotNum);

                if (stackHand.isItemEqualIgnoreDamage(stackSlot))
                {
                    // If all the items from the found slot can fit into the current
                    // stack in hand, then left click, otherwise right click to split the stack
                    int button = stackSlot.getCount() + count <= max ? 0 : 1;

                    mc.interactionManager.clickSlot(container.syncId, slotNum, button, SlotActionType.PICKUP, player);
                    mc.interactionManager.clickSlot(container.syncId, currentSlot, 0, SlotActionType.PICKUP, player);

                    break;
                }
            }
        }

    }
}
