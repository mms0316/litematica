package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class AddonUtils {
    private static final List<String[]> SUBSTITUTIONS = new ArrayList<>();

    public static ActionResult checkEasyPlaceFluidBucket(MinecraftClient mc) {
        //Re-run traces to stop wasting liquid on liquid, and ignoring easyPlaceFirst config, as interactItem works differently

        final double traceMaxRange = mc.interactionManager.getReachDistance();
        final World world = SchematicWorldHandler.getSchematicWorld();

        //Raytrace first non-liquid block
        var hitResult = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return ActionResult.FAIL;
        var blockHitResult = (BlockHitResult)hitResult;
        final var blockPosLast = blockHitResult.getBlockPos();
        //Keep block before first non-liquid block
        final var blockPosBeforeLast = blockPosLast.offset(blockHitResult.getSide());

        //Raytrace first block including liquid
        hitResult = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, true, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return ActionResult.FAIL;
        final var blockPosFirst = ((BlockHitResult)hitResult).getBlockPos();

        //Fail if there are liquids in-between
        //If there are liquids in-between, it'd waste liquid or create obsidian
        if (blockPosFirst.toCenterPos().squaredDistanceTo(blockPosBeforeLast.toCenterPos()) > 1.0 + Math.ulp(1.0))
            return ActionResult.FAIL;

        final var blockStateSchematic = world.getBlockState(blockPosBeforeLast);
        final var blockSchematic = blockStateSchematic.getBlock();
        final var blockStateVanilla = mc.world.getBlockState(blockPosBeforeLast);
        final var blockVanilla = blockStateVanilla.getBlock();

        //Fail if target is not to be a liquid source
        if (!(blockSchematic instanceof FluidBlock))
            return ActionResult.FAIL;

        if (!blockStateVanilla.isAir())
        {
            //Fail if target is not of the desired fluid
            // (this comparison works because all Blocks are pointers to a single instance)
            if (blockSchematic != blockVanilla)
                return ActionResult.FAIL;

            //Fail if world already has block as a liquid source
            if (blockStateVanilla.get(FluidBlock.LEVEL) == 0)
                return ActionResult.FAIL;
        }

        return ActionResult.SUCCESS;
    }


    //Adapted from malilib liteloader_1.12.2 branch, and changed code to use a single packet
    /**
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether or not to allow taking items from other hotbar slots
     */
    public static boolean preRestockHand(PlayerEntity player, Hand hand, int threshold, boolean allowHotbar)
    {
        boolean changed = false;
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

                if (ItemStack.canCombine(stackHand, stackSlot))
                {
                    if (hand == Hand.OFF_HAND)
                    {
                        // If all the items from the found slot can fit into the current
                        // stack in hand, then left click, otherwise right click to split the stack
                        int button = stackSlot.getCount() + count <= max ? 0 : 1;

                        mc.interactionManager.clickSlot(container.syncId, slotNum, button, SlotActionType.PICKUP, player);
                        mc.interactionManager.clickSlot(container.syncId, currentSlot, 0, SlotActionType.PICKUP, player);
                    }
                    else
                    {
                        //Do shift-click
                        mc.interactionManager.clickSlot(container.syncId, slotNum, 0, SlotActionType.QUICK_MOVE, player);
                    }
                    changed = true;

                    break;
                }
            }
        }

        return changed;
    }

    public static void setSubstitutions(List<String> substitutionList)
    {
        SUBSTITUTIONS.clear();

        for (String substitutionItem : substitutionList)
        {
            //Each substitution is separated by semicolons
            String[] substitutions = substitutionItem.split(";");

            SUBSTITUTIONS.add(substitutions);
        }
    }

    public static HashSet<String> getSubstitutions(String id)
    {
        HashSet<String> substitutionList = new HashSet<>();

        for (String[] substitutions : SUBSTITUTIONS)
        {
            if (ArrayUtils.contains(substitutions, id))
            {
                Collections.addAll(substitutionList, substitutions);
                //does not break here, because there may be multiple entries
            }
        }

        substitutionList.remove(id); //remove self

        return substitutionList;
    }

    public static boolean maySubstitute(Identifier schematicId, Identifier clientId)
    {
        if (schematicId.equals(clientId))
        {
            return true;
        }

        for (String[] substitutions : SUBSTITUTIONS)
        {
            if (ArrayUtils.contains(substitutions, schematicId.toString()) &&
                    ArrayUtils.contains(substitutions, clientId.toString()))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean hasEqualProperties(BlockState blockState1, BlockState blockState2)
    {
        final var properties1 = blockState1.getEntries();
        final var properties2 = blockState2.getEntries();

        if (properties1 == properties2) return true;
        if (properties1.size() != properties2.size()) return false;

        for (var entry : properties1.entrySet()) {
            var val1 = entry.getValue();
            var val2 = properties2.get(entry.getKey());

            if (val1 != val2) return false;
        }

        return true;
    }

    public static OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient)
    {
        boolean ignoreClientWorldFluids = Configs.Visuals.IGNORE_EXISTING_FLUIDS.getBooleanValue();
        if (stateSchematic == stateClient)
        {
            return OverlayType.NONE;
        }
        else
        {
            boolean clientHasAir = stateClient.isAir();
            boolean schematicHasAir = stateSchematic.isAir();

            if (schematicHasAir)
            {
                return (clientHasAir || (ignoreClientWorldFluids && stateClient.isLiquid())) ? OverlayType.NONE : OverlayType.EXTRA;
            }
            else
            {
                if (clientHasAir || (ignoreClientWorldFluids && stateClient.isLiquid()))
                {
                    return OverlayType.MISSING;
                }

                final Block schematicBlock = stateSchematic.getBlock();
                final Block clientBlock = stateClient.getBlock();
                final Identifier schematicBlockName = Registries.BLOCK.getId(schematicBlock);
                final Identifier clientBlockName = Registries.BLOCK.getId(clientBlock);

                if (!maySubstitute(schematicBlockName, clientBlockName))
                {
                    return OverlayType.WRONG_BLOCK;
                }

                if (!hasEqualProperties(stateSchematic, stateClient))
                {
                    return OverlayType.WRONG_STATE;
                }

                return OverlayType.NONE;
            }
        }
    }

    /**
     * @param screenHandler Chest / Double Chest / Shulker Box screen handler
     * @param match ItemStack that will be searched for in screen handler
     * @return Slot index pointing to the slot with the fewest count of the ItemStack, or an empty slot, of the
     * player's inventory or hotbar.
     */
    public static int findInventorySlotToFill(ScreenHandler screenHandler, ItemStack match) {
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
}
