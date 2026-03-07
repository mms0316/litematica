package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.util.EquipmentUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.world.WorldSchematic;

//Custom Additions (easier to resolve future merge conflicts)
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class InventoryUtils
{
    //Custom Additions (easier to resolve future merge conflicts)
    //Before:  List<Integer> 
    private static final Map<Integer, Long> PICK_BLOCKABLE_SLOTS = new HashMap<>();
    private static Pair<BlockPos, InventoryOverlay.Context> lastBlockEntityContext = null;

    public static void setPickBlockableSlots(String configStr)
    {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");

        //Custom Additions (easier to resolve future merge conflicts)
        Pattern patternRange = Pattern.compile("^(?<start>[0-9])-(?<end>[0-9])$");

        for (String str : parts)
        {
            try
            {
                //Custom Additions (easier to resolve future merge conflicts)
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

    //Custom Additions (easier to resolve future merge conflicts)
    //Changed to return boolean
    public static boolean setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        if (mc.player == null) return false;
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        return setPickedItemToHand(slotNum, stack, mc);
    }

    //Custom Additions (easier to resolve future merge conflicts)
    //Changed to return boolean
    public static boolean setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc)
    {
        if (mc.player == null) return false;
        boolean changed = false;
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            //Custom Additions (easier to resolve future merge conflicts)
            refreshSlotTimeout(sourceSlot);

            if (sourceSlot != inventory.getSelectedSlot())
            {
                inventory.setSelectedSlot(sourceSlot);
                changed = true;
            }
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                //Custom Additions (easier to resolve future merge conflicts)
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
                //Custom Additions (easier to resolve future merge conflicts)
                refreshSlotTimeout(hotbarSlot);

                inventory.setSelectedSlot(hotbarSlot);

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.getMainStacks().set(hotbarSlot, stack.copy());

                    //Custom Additions (easier to resolve future merge conflicts)
                    changed = true;
                }
                else
                {
                    //Custom Additions (easier to resolve future merge conflicts)
                    changed = fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc) || changed;
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }

        //Custom Additions (easier to resolve future merge conflicts)
        return changed;
    }

	/**
	 * Simulates the 'setPickedItemToHand' logic but does not actually swap the item.
	 * @param stack (Reference Stack)
	 * @param mc ()
	 * @return (Slot Number, or -1)
	 */
	public static int getPickedItemHandSlotNoSwap(ItemStack stack, MinecraftClient mc)
	{
		if (mc.player == null) return -1;
		int slotNum = mc.player.getInventory().getSlotWithStack(stack);
		return getPickedItemHandSlotNoSwap(slotNum, stack, mc);
	}

	/**
	 * Simulates the 'setPickedItemToHand' logic but does not actually swap the item.
	 * @param sourceSlot ()
	 * @param stack (Reference Stack)
	 * @param mc ()
	 * @return (Slot Number, or -1)
	 */
	public static int getPickedItemHandSlotNoSwap(int sourceSlot, ItemStack stack, MinecraftClient mc)
	{
		if (mc.player == null) return -1;
		PlayerEntity player = mc.player;
		PlayerInventory inventory = player.getInventory();

		if (PlayerInventory.isValidHotbarIndex(sourceSlot))
		{
			inventory.setSelectedSlot(sourceSlot);
		}
		else
		{
			if (PICK_BLOCKABLE_SLOTS.size() == 0)
			{
				InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
				return -1;
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
				int resultSlot = -1;
				inventory.setSelectedSlot(hotbarSlot);

				if (EntityUtils.isCreativeMode(player))
				{
					resultSlot = hotbarSlot;
				}
				else
				{
					resultSlot = getMainHandSlotForItem(stack.copy(), mc);
				}

				// Can still be -1
				if (resultSlot != -1)
				{
					WorldUtils.setEasyPlaceLastPickBlockTime();
				}

				return resultSlot;
			}
			else
			{
				InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
			}
		}

		return -1;
	}

	/**
	 * Simulates the MaLiLib -> swapItemToMainHand() without actually swapping the item.
	 * @param stackReference ()
	 * @param mc ()
	 * @return (The Slot ID or -1)
	 */
	private static int getMainHandSlotForItem(ItemStack stackReference, MinecraftClient mc)
	{
		PlayerEntity player = mc.player;
		if (mc.player == null) return -1;
		boolean isCreative = player.isInCreativeMode();

		if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(stackReference, player.getMainHandStack()))
		{
			return -1;
		}

		if (isCreative)
		{
			player.getInventory().setSelectedSlot(player.getInventory().getSwappableHotbarSlot());
			return 36 + player.getInventory().getSelectedSlot();
		}
		else
		{
			return fi.dy.masa.malilib.util.InventoryUtils.findSlotWithItem(player.playerScreenHandler, stackReference, true);
		}
	}

    //Custom Additions (easier to resolve future merge conflicts)
    //Changed to return PickBlockResult
    public static PickBlockResult schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               World schematicWorld, MinecraftClient mc)
    {
        //Custom Additions (easier to resolve future merge conflicts)
        //Removed null checks

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
                    //te.setStackNbt(stack, schematicWorld.getRegistryManager());
                    fi.dy.masa.malilib.util.game.BlockUtils.setStackNbt(stack, te, schematicWorld.getRegistryManager());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.getSelectedSlot());

                //Custom Additions (easier to resolve future merge conflicts)
                return new PickBlockResult(inv.getSelectedSlot(), false, true);
            }
            else
            {
                int slot = inv.getSlotWithStack(stack);

                //Custom Additions (easier to resolve future merge conflicts)
                var pickBlockResult = pickBlockSurvival(slot, stack, inv, mc);

                // Pick block did not happen - try substitutions
                if (pickBlockResult.changed == false && pickBlockResult.slot == -1)
                {
                    HashSet<String> substitutions = AddonUtils.getSubstitutions(Registries.ITEM.getId(stack.getItem()).toString());

                    for (int i = 0; i < inv.getMainStacks().size(); ++i)
                    {
                        ItemStack iter = inv.getMainStacks().get(i);
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
                            slot = findBestPlayerSlotWithBoxWithItem(mc.player.playerScreenHandler, substStack);
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
                    AddonUtils.addRanOutItem(stack);
                }
                else if (pickBlockResult.pickedShulker)
                {
                    InfoUtils.printActionbarMessage(GuiBase.TXT_YELLOW + "Refill " + GuiBase.TXT_RST + stack.getName().getString());
                    AddonUtils.addRefillItem(stack);
                }

                return pickBlockResult;
            }
        }

        return new PickBlockResult(-1, false, false);
    }

    public record PickBlockResult(int slot, boolean pickedShulker, boolean changed) { }

    //Custom Additions (easier to resolve future merge conflicts)
    private static PickBlockResult pickBlockSurvival(int slot, ItemStack stack, PlayerInventory inv, MinecraftClient mc)
    {
        boolean shouldPick = inv.getSelectedSlot() != slot;
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
            slot = findBestPlayerSlotWithBoxWithItem(mc.player.playerScreenHandler, stack);

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
        //Custom Additions (easier to resolve future merge conflicts)
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
                //(stack.getItem() instanceof MiningToolItem) == false);
                (EquipmentUtils.isRegularTool(stack)) == false);
    }

    private static int getPickBlockTargetSlot(PlayerEntity player)
    {
        if (PICK_BLOCKABLE_SLOTS.isEmpty() || player == null)
        {
            return -1;
        }

        //Custom Additions (easier to resolve future merge conflicts)
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
        //Custom Additions (easier to resolve future merge conflicts)
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
     * @return (The result InventoryOverlay.Context | NULL if not obtainable)
     */
    public static @Nullable InventoryOverlay.Context getTargetInventory(World world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        Block blockTmp = state.getBlock();
        NbtCompound nbt = new NbtCompound();
        BlockEntity be = null;

        if (blockTmp instanceof BlockEntityProvider)
        {
            if (world instanceof ServerWorld || world instanceof WorldSchematic)
            {
                be = world.getWorldChunk(pos).getBlockEntity(pos);

                if (be != null)
                {
                    nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                }
            }
            else
            {
                Pair<BlockEntity, NbtCompound> pair = EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

                if (pair != null)
                {
                    nbt = pair.getRight();
                    be = pair.getLeft();
                }
            }

//            Litematica.LOGGER.warn("getTarget():2: pos [{}], be [{}], nbt [{}]", pos.toShortString(), be != null, nbt != null);
            InventoryOverlay.Context ctx = getTargetInventoryFromBlock(world, pos, be, nbt);

            if (world instanceof WorldSchematic)
            {
                return ctx;
            }

            if (lastBlockEntityContext != null && !lastBlockEntityContext.getLeft().equals(pos))
            {
                lastBlockEntityContext = null;
            }

            if (ctx != null && ctx.inv() != null)
            {
                lastBlockEntityContext = Pair.of(pos, ctx);
                return ctx;
            }
            else if (lastBlockEntityContext != null && lastBlockEntityContext.getLeft().equals(pos))
            {
                return lastBlockEntityContext.getRight();
            }
        }

        return null;
    }

    private static @Nullable InventoryOverlay.Context getTargetInventoryFromBlock(World world, BlockPos pos, @Nullable BlockEntity be, NbtCompound nbt)
    {
        Inventory inv;

        if (be != null)
        {
            if (nbt.isEmpty())
            {
                nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
            }
            inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
        }
        else
        {
            if (nbt.isEmpty())
            {
                Pair<BlockEntity, NbtCompound> pair = EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

                if (pair != null)
                {
                    nbt = pair.getRight();
                }
            }

            inv = EntitiesDataStorage.getInstance().getBlockInventory(world, pos, false);
        }

        if (nbt != null && !nbt.isEmpty())
        {
            Inventory inv2 = fi.dy.masa.malilib.util.InventoryUtils.getNbtInventory(nbt, inv != null ? inv.size() : -1, world.getRegistryManager());

            if (inv == null)
            {
                inv = inv2;
            }
        }

//        Litematica.LOGGER.warn("getTarget(): [SchematicWorld? {}] pos [{}], inv [{}], be [{}], nbt [{}]", world instanceof WorldSchematic ? "YES" : "NO", pos.toShortString(), inv != null, be != null, nbt != null ? nbt.getString("id") : new NbtCompound());

        if (inv == null || nbt == null)
        {
            return null;
        }

        return new InventoryOverlay.Context(InventoryOverlay.getBestInventoryType(inv, nbt), inv, be != null ? be : world.getBlockEntity(pos), null, nbt, new Refresher());
    }

    // This really isn't used for this use case; but this is just here for Compat
    public static class Refresher implements InventoryOverlay.Refresher
    {

        @Override
        public InventoryOverlay.Context onContextRefresh(InventoryOverlay.Context data, World world)
        {
            // Refresh data
            if (data.be() != null)
            {
                getTargetInventory(world, data.be().getPos());
                data = getTargetInventoryFromBlock(data.be().getWorld(), data.be().getPos(), data.be(), data.nbt());
            }
            /*
            else if (data.entity() != null)
            {
                EntitiesDataStorage.getInstance().requestEntity(world, data.entity().getId());
                data = getTargetInventoryFromEntity(data.entity(), data.nbt());
            }
             */

            return data;
        }
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
            result.append(nbt.getString("id", "?"));
        }
        else
        {
            return null;
        }
        if (nbt.contains("components"))
        {
            NbtCompound components = nbt.getCompoundOrEmpty("components");
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
            int count = nbt.getInt("count", 1);

            if (count > 1)
            {
                result.append(" ");
                result.append(count);
            }
        }

        return result.toString();
    }

    /**
     * Post Re-Write Code
     * -
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether to allow taking items from other hotbar slots
     */
    @ApiStatus.Experimental
    public static void preRestockHand(PlayerEntity player,
                                      Hand hand,
                                      int threshold,
                                      boolean allowHotbar)
    {
        if (player == null) return;
        PlayerInventory container = player.getInventory();
        final ItemStack handStack = player.getStackInHand(hand);
        final int count = handStack.getCount();
        final int max = handStack.getMaxCount();

        if (handStack.isEmpty() == false &&
            getCursorStack().isEmpty() &&
            (count <= threshold && count < max))
        {
            int endSlot = allowHotbar ? 44 : 35;
            int currentMainHandSlot = getSelectedHotbarSlot() + 36;
            int currentSlot = hand == Hand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                MinecraftClient mc = MinecraftClient.getInstance();
                ScreenHandler handler = player.playerScreenHandler;

                Slot slot = handler.slots.get(slotNum);
                ItemStack stackSlot = container.getStack(slotNum);

                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreDurability(stackSlot, handStack))
                {
                    // If all the items from the found slot can fit into the current
                    // stack in hand, then left click, otherwise right click to split the stack
                    int button = stackSlot.getCount() + count <= max ? 0 : 1;

                    //clickSlot(container, slot, button, ClickType.PICKUP);
                    //clickSlot(container, currentSlot, 0, ClickType.PICKUP);

                    mc.interactionManager.clickSlot(handler.syncId, slot.id, button, SlotActionType.PICKUP, player);
                    mc.interactionManager.clickSlot(handler.syncId, currentSlot, 0, SlotActionType.PICKUP, player);

                    break;
                }
            }
        }
    }

    //Custom Additions (easier to resolve future merge conflicts)
    public static void refreshSlotTimeout(int slot)
    {
        if (PICK_BLOCKABLE_SLOTS.containsKey(slot))
        {
            final long now = System.nanoTime();
            final long nextTimeout = now + (20 + Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue()) * 1_000_000L;
            PICK_BLOCKABLE_SLOTS.put(slot, nextTimeout);
        }
    }
    private static int getListAmount(DefaultedList<ItemStack> items, ItemStack referenceItem)
    {
        int amount = 0;
        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem))
                {
                    amount += item.getCount();
                }
            }
        }

        return amount;
    }
    public static int findBestPlayerSlotWithBoxWithItem(ScreenHandler container, ItemStack stackReference)
    {
        if (!(container instanceof PlayerScreenHandler))
            return -1;

        // Start looking at hotbar (slots 36 ~ 44)
        for (int slotNum = 36; slotNum <= 44; ++slotNum)
        {
            Slot slot = container.slots.get(slotNum);

            if (doesShulkerBoxContainItem(slot.getStack(), stackReference))
            {
                return slot.id;
            }
        }

        // For slots 9 ~ 35, check which box has the least amount of items
        int bestSlot = -1;
        int bestCount = Integer.MAX_VALUE;

        for (int slotNum = 9; slotNum <= 35; ++slotNum)
        {
            Slot slot = container.slots.get(slotNum);

            if (slot.getStack().isEmpty())
                continue;

            int count = getListAmount(fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(slot.getStack()), stackReference);
            if (count == 0)
                continue;

            if (count < bestCount)
            {
                bestCount = count;
                bestSlot = slot.id;
            }
        }

        return bestSlot;
    }

    @ApiStatus.Experimental
    public static ItemStack getCursorStack()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        PlayerInventory inv = player.getInventory();
        return inv != null ? inv.getSelectedStack() : ItemStack.EMPTY;
    }

    @ApiStatus.Experimental
    public static int getSelectedHotbarSlot()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return 0;
        }
        PlayerInventory inv = player.getInventory();
        return inv != null ? inv.getSelectedSlot() : 0;
    }
}
