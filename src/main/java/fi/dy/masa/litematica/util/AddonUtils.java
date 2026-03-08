//Custom Additions (easier to resolve future merge conflicts)
package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.InventoryUtils;

import net.minecraft.world.level.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.Level;


import net.minecraft.world.level.block.state.BlockState;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class AddonUtils {
    private static final List<String[]> SUBSTITUTIONS = new ArrayList<>();

    private static final List<ItemStack> ranOutItems = new ArrayList<>();
    private static final List<ItemStack> refillItems = new ArrayList<>();
    private static long lastRefillTimeCheck;

    private static long inventoryUpdateTime = 0;

    public static boolean isMatchingStateRestrictedProtocol (BlockState state1, BlockState state2)
    {
        if (state1 == null || state2 == null)
        {
            return false;
        }

        if (state1 == state2)
        {
            return true;
        }

        var orientationProperties = new Property<?>[] {
                BlockStateProperties.FACING, //pistons
                BlockStateProperties.HALF, //stairs, trapdoors
                BlockStateProperties.FACING_HOPPER,
                BlockStateProperties.DOOR_HINGE,
                BlockStateProperties.HORIZONTAL_FACING, //small dripleaf
                BlockStateProperties.AXIS, //logs
                BlockStateProperties.SLAB_TYPE,
                BlockStateProperties.VERTICAL_DIRECTION,
                BlockStateProperties.ROTATION_16, //banners
                BlockStateProperties.HANGING, //lanterns
                BlockStateProperties.ATTACH_FACE, //lever
                BlockStateProperties.BELL_ATTACHMENT, //bell (double-check for single-wall / double-wall)
                //BlockStateProperties.HORIZONTAL_AXIS, //Nether portals, though they aren't directly placeable
                //BlockStateProperties.ORIENTATION, //jigsaw blocks
        };

        for (var property : orientationProperties)
        {
            boolean hasProperty1 = state1.hasProperty(property);
            boolean hasProperty2 = state2.hasProperty(property);

            if (hasProperty1 != hasProperty2)
                return false;
            if (!hasProperty1)
                continue;

            if (state1.getValue(property) != state2.getValue(property))
                return false;
        }

        //Other properties are considered as matching
        return true;
    }

    public static boolean isMatchingStateRestrictedProtocol(BlockPos pos, BlockState stateSchematic, Direction direction, Vec3 hitVecIn, Minecraft mc, InteractionHand hand)
    {
        final var updatedHitResult = new BlockHitResult(hitVecIn, direction, pos, false);
        final var ctx = new BlockPlaceContext(mc.player, hand, mc.player.getItemInHand(hand), updatedHitResult);
        final var attemptState = stateSchematic.getBlock().getStateForPlacement(ctx);
        return isMatchingStateRestrictedProtocol(attemptState, stateSchematic);
    }

    public static Triple<BlockPos, Direction, Vec3> applyRestrictedProtocol(BlockPos pos, BlockState stateSchematic, Direction sideIn, Vec3 hitVecIn, Minecraft mc, InteractionHand hand)
    {
        var block = stateSchematic.getBlock();

        if (block instanceof BaseTorchBlock) //Torch, Soul Torch, Redstone Torch
        {
            boolean isOnWall = block instanceof WallTorchBlock || block instanceof RedstoneWallTorchBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof AbstractBannerBlock)
        {
            boolean isOnWall = block instanceof WallBannerBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof SignBlock)
        {
            boolean isOnWall = block instanceof WallSignBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof AbstractSkullBlock) //Wither Skull, Player Skull
        {
            boolean isOnWall = block instanceof WallSkullBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof MultifaceSpreadeableBlock) //Sculk Vein, Glow Lichen
        {
            final var clientState = mc.level.getBlockState(pos);
            final boolean isSameClass = clientState.getBlock().getClass().equals(block.getClass());

            Direction direction = sideIn.getOpposite();
            if (isSameClass && MultifaceSpreadeableBlock.hasFace(clientState, direction))
                // This direction is already placed.
                return null;

            final var posSupport = pos.relative(direction);

            // Check if supporting block exists
            if (!MultifaceSpreadeableBlock.canAttachTo(mc.level, direction, pos, mc.level.getBlockState(posSupport)))
                return null;

            return Triple.of(posSupport, sideIn, hitVecIn);
        }

        return Direction.stream()
                .filter(direction -> isMatchingStateRestrictedProtocol(pos, stateSchematic, direction, hitVecIn, mc, hand))
                .findAny()
                .map(direction -> Triple.of(pos, direction, hitVecIn))
                .orElse(null);
    }

    private static Triple<BlockPos, Direction, Vec3> getWallPlaceableOrientation(BlockPos pos, BlockState stateSchematic, Vec3 hitVecOut, Minecraft mc, InteractionHand hand, boolean isOnWall) {
        Direction sideOut;
        BlockPos posOrig = pos;

        if (isOnWall)
        {
            if (!stateSchematic.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            {
                //Shouldn't happen, fail instead of crashing just in case
                return null;
            }

            sideOut = stateSchematic.getValue(BlockStateProperties.HORIZONTAL_FACING);
            pos = pos.relative(sideOut.getOpposite());
        }
        else
        {
            sideOut = Direction.UP;
            pos = pos.below();
        }
        BlockState stateFacing = mc.level.getBlockState(pos);

        if (stateFacing == null || stateFacing.isAir())
            return null;

        //Check for blocks that have rotation property (Banners, Signs, Skulls)
        if (stateSchematic.hasProperty(BlockStateProperties.ROTATION_16))
        {
            if (!isMatchingStateRestrictedProtocol(posOrig, stateSchematic, sideOut, hitVecOut, mc, hand))
                return null;
        }

        return Triple.of(pos, sideOut, hitVecOut);
    }

    public static InteractionResult checkEasyPlaceFluidBucket(Minecraft mc) {
        //Re-run traces to stop wasting liquid on liquid, and ignoring easyPlaceFirst config, as interactItem works differently

        final double traceMaxRange = mc.player.blockInteractionRange();
        final Level world = SchematicWorldHandler.getSchematicWorld();

        //Raytrace first non-liquid block
        var hitResult = RayTraceUtils.getRayTraceFromEntity(mc.level, mc.player, false, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return InteractionResult.FAIL;
        var blockHitResult = (BlockHitResult)hitResult;
        final var blockPosLast = blockHitResult.getBlockPos();
        //Keep block before first non-liquid block
        final var blockPosBeforeLast = blockPosLast.relative(blockHitResult.getDirection());

        //Raytrace first block including liquid
        hitResult = RayTraceUtils.getRayTraceFromEntity(mc.level, mc.player, true, traceMaxRange);
        if (hitResult.getType() != HitResult.Type.BLOCK)
            return InteractionResult.FAIL;
        final var blockPosFirst = ((BlockHitResult)hitResult).getBlockPos();

        //Fail if there are liquids in-between
        //If there are liquids in-between, it'd waste liquid or create obsidian
        if (blockPosFirst.getCenter().distanceTo(blockPosBeforeLast.getCenter()) > 1.0 + Math.ulp(1.0))
            return InteractionResult.FAIL;

        final var blockStateSchematic = world.getBlockState(blockPosBeforeLast);
        final var blockSchematic = blockStateSchematic.getBlock();
        final var blockStateVanilla = mc.level.getBlockState(blockPosBeforeLast);
        final var blockVanilla = blockStateVanilla.getBlock();

        //Fail if target is not to be a liquid source
        if (!(blockSchematic instanceof LiquidBlock))
            return InteractionResult.FAIL;

        if (!blockStateVanilla.isAir())
        {
            //Fail if target is not of the desired fluid
            // (this comparison works because all Blocks are pointers to a single instance)
            if (blockSchematic != blockVanilla)
                return InteractionResult.FAIL;

            //Fail if world already has block as a liquid source
            if (blockStateVanilla.getValue(LiquidBlock.LEVEL) == 0)
                return InteractionResult.FAIL;
        }

        return InteractionResult.SUCCESS;
    }


    //Adapted from malilib liteloader_1.12.2 branch, and changed code to use a single packet
    /**
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether or not to allow taking items from other hotbar slots
     */
    public static boolean preRestockHand(Player player, InteractionHand hand, int threshold, boolean allowHotbar)
    {
        boolean changed = false;
        final ItemStack stackHand = player.getItemInHand(hand);
        final int count = stackHand.getCount();
        final int max = stackHand.getMaxStackSize();

        if (stackHand.isEmpty() == false &&
                (count <= threshold && count < max))
        {
            Minecraft mc = Minecraft.getInstance();
            //mc.gameMode.handleInventoryMouseClick() considers these slot numbers: https://minecraft.wiki/w/Java_Edition_protocol/Inventory
            //36 - 44: hotbar
            //9 - 35: main inventory
            //45: offhand
            //Meanwhile, player.getInventory() considers these slot numbers:
            //0 - 8: hotbar
            //9 - 35: main inventory
            //40: offhand
            int endSlot = allowHotbar ? 44 : 35;
            Inventory inventory = player.getInventory();
            int currentMainHandSlot = inventory.getSelectedSlot() + 36;
            int currentSlot = hand == InteractionHand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                ItemStack stackSlot = inventory.getItem(slotNum >= 36 ? slotNum - 36 : slotNum);

                if (InventoryUtils.areStacksEqualIgnoreNbt(stackHand, stackSlot))
                {
                    if (hand == InteractionHand.OFF_HAND)
                    {
                        // If all the items from the found slot can fit into the current
                        // stack in hand, then left click, otherwise right click to split the stack
                        int button = stackSlot.getCount() + count <= max ? 0 : 1;

                        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slotNum, button, ClickType.PICKUP, player);
                        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, currentSlot, 0, ClickType.PICKUP, player);
                    }
                    else
                    {
                        //Do shift-click
                        mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slotNum, 0, ClickType.QUICK_MOVE, player);
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
        final var properties1 = blockState1.getProperties();
        final var properties2 = blockState2.getProperties();

        if (properties1.equals(properties2)) return true;
        if (properties1.size() != properties2.size()) return false;

        for (var property1 : properties1) {
            if (properties2.contains(property1) == false) return false;
            if (blockState1.getValue(property1) != blockState2.getValue(property1)) return false;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    public static OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient, IgnoreBlockRegistry ignoreBlockRegistry)
    {
        //Extracted + adapted from: getOverlayType@ChunkRendererSchematicVbo.java
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
                if (clientHasAir)
                {
                    return OverlayType.NONE;
                }
                else if (ignoreClientWorldFluids && stateClient.liquid())
                {
                    return OverlayType.NONE;
                }
                else if (ignoreBlockRegistry.hasBlock(stateClient.getBlock()))
                {
                    return OverlayType.NONE;
                }
                else
                {
                    return OverlayType.EXTRA;
                }
            }
            else
            {
                if (clientHasAir || (ignoreClientWorldFluids && stateClient.liquid()))
                {
                    return OverlayType.MISSING;
                }
                // Wrong block
                if (stateSchematic.getBlock() != stateClient.getBlock())
                {
                    if (Configs.Generic.ENABLE_DIFFERENT_BLOCKS.getBooleanValue() &&
                        BlockUtils.isInSameGroup(stateSchematic, stateClient))
                    {
                        if (BlockUtils.matchPropertiesOnly(stateSchematic, stateClient))
                        {
                            // Different block of a common BlockTags Group, and same state
                            return OverlayType.DIFF_BLOCK;
                        }
                        else
                        {
                            return OverlayType.WRONG_STATE;
                        }
                    }
                }
                // Wrong state
                //Custom Additions (easier to resolve future merge conflicts)
                final Block schematicBlock = stateSchematic.getBlock();
                final Block clientBlock = stateClient.getBlock();
                final Identifier schematicBlockName = BuiltInRegistries.BLOCK.getKey(schematicBlock);
                final Identifier clientBlockName = BuiltInRegistries.BLOCK.getKey(clientBlock);

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

    public static void addRanOutItem(ItemStack stack) {
        for (var item : ranOutItems) {
            if (InventoryUtils.areStacksEqualIgnoreNbt(item, stack)) {
                return;
            }
        }

        ranOutItems.add(stack.copy());

        lastRefillTimeCheck = 0;
    }

    public static List<ItemStack> getRanOutItems() {
        return ranOutItems;
    }

    public static void clearRanOutItems() {
        ranOutItems.clear();

        lastRefillTimeCheck = 0;
    }

    public static void addRefillItem(ItemStack stack) {
        for (var item : refillItems) {
            if (InventoryUtils.areStacksEqualIgnoreNbt(item, stack)) {
                return;
            }
        }

        refillItems.add(stack.copy());

        lastRefillTimeCheck = 0;
    }

    public static List<ItemStack> getRefillItems() {
        return refillItems;
    }

    public static void clearRefillItems() {
        refillItems.clear();

        lastRefillTimeCheck = 0;
    }

    public static void checkClearLastItems() {
        if (!Configs.Generic.HIGHLIGHT_REFILL_IN_INV.getBooleanValue()) return;
        if (ranOutItems.isEmpty() && refillItems.isEmpty()) return;

        final long now = System.currentTimeMillis();
        if (now - lastRefillTimeCheck <= 5_000L) return;

        final var player = Minecraft.getInstance().player;
        if (player == null) return;
        final var inv = player.getInventory();
        if (inv == null) return;

        ranOutItems.removeIf(inv::contains);
        refillItems.removeIf(inv::contains);

        lastRefillTimeCheck = now;
    }

    public static void renderHotbarItem(GuiGraphics context, int x, int y, ItemStack stack) {
        if (!Configs.Generic.HIGHLIGHT_REFILL_IN_INV.getBooleanValue()) return;

        if (stack.isEmpty()) return;

        final var refillItems = AddonUtils.getRefillItems();
        final var ranOutItems = AddonUtils.getRanOutItems();

        Stream<ItemStack> combinedStream = Stream.concat(refillItems.stream(), ranOutItems.stream());

        final var stackItem = stack.getItem();
        if (stackItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            if (combinedStream.noneMatch(itemStack -> fi.dy.masa.litematica.util.InventoryUtils.doesShulkerBoxContainItem(stack, itemStack)))
                return;
        }
        else if (stackItem instanceof BundleItem) {
            if (combinedStream.noneMatch(itemStack -> fi.dy.masa.litematica.util.InventoryUtils.doesBundleContainItem(stack, itemStack)))
                return;
        }
        else
            return;

        int borderColor = Configs.Colors.HIGHLIGHT_REFILL_IN_INV_COLOR.getColor().intValue;
        int borderThickness = 2;

        context.pose().pushMatrix();
        context.fill(x - borderThickness, y - borderThickness, x + 16 + borderThickness, y + 16 + borderThickness, borderColor);
        context.pose().popMatrix();
    }

    public static String getFormattedCountString(int count, int maxStackSize, boolean bigFormat) {
        if (count <= maxStackSize)
            return Integer.toString(count);

        if (Configs.Generic.MATERIAL_LIST_USE_BSI_FORMAT.getBooleanValue())
            return getFormattedCountStringBSI(count, maxStackSize);

        if (bigFormat) {
            return getFormattedCountStringBig(count, maxStackSize);
        } else {
            return getFormattedCountStringSmall(count, maxStackSize);
        }
    }

    public static String getFormattedCountStringBSI(int total, int maxStackSize) {
        int stacks = total / maxStackSize;
        int remainder = total % maxStackSize;
        int boxCount = stacks / 27;

        StringBuilder sb = new StringBuilder();

        if (boxCount != 0) {
            sb.append(boxCount);
            sb.append('B');
        }

        if (stacks % 27 != 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(stacks % 27);
            sb.append('S');
        }

        if (remainder != 0) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(remainder);
            sb.append('I');
        }

        return sb.toString();
    }

    public static String getFormattedCountStringBig(int total, int maxStackSize) {
        int stacks = total / maxStackSize;
        int remainder = total % maxStackSize;
        double boxCount = (double) total / (27D * maxStackSize);
        final String shulkerBoxAbbr = StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box");

        if (maxStackSize > 1) {
            if (stacks >= 27)
                return String.format("%d = %d %s + %d x %d + %d = %.2f %s", total, stacks / 27, shulkerBoxAbbr, stacks % 27, maxStackSize, remainder, boxCount, shulkerBoxAbbr);
            else if (remainder > 0)
                return String.format("%d = %d x %d + %d = %.2f %s", total, stacks, maxStackSize, remainder, boxCount, shulkerBoxAbbr);
            else
                return String.format("%d = %d x %d = %.2f %s", total, stacks, maxStackSize, boxCount, shulkerBoxAbbr);
        }
        else
            return String.format("%d = %.2f %s", total, boxCount, shulkerBoxAbbr);
    }

    public static String getFormattedCountStringSmall(int total, int maxStackSize) {
        int stacks = total / maxStackSize;
        int remainder = total % maxStackSize;
        double boxCount = (double) total / (27D * maxStackSize);
        final String shulkerBoxAbbr = StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box");

        if (boxCount >= 1.0)
            return String.format("%d (%.2f %s)", total, boxCount, shulkerBoxAbbr);
        else if (remainder > 0)
            return String.format("%d (%d x %d + %d)", total, stacks, maxStackSize, remainder);
        else
            return String.format("%d (%d x %d)", total, stacks, maxStackSize);
    }

    public static void skipInventoryUpdate() {
        inventoryUpdateTime = System.currentTimeMillis() + Configs.Generic.EASY_PLACE_SKIP_INVENTORY_UPDATE_DURATION.getIntegerValue();
    }
    public static boolean isInventoryUpdateSkipped() {
        return System.currentTimeMillis() < inventoryUpdateTime;
    }
}
