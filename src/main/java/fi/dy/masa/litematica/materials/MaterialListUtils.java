package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3i;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;

//Custom Additions (easier to resolve future merge conflicts)
import java.util.Map;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class MaterialListUtils
{
    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic)
    {
        return createMaterialListFor(schematic, schematic.getAreas().keySet());
    }

    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();

        //Custom Additions (easier to resolve future merge conflicts)
        Object2IntOpenHashMap<ItemType> countsItemsTotal = new Object2IntOpenHashMap<>();

        for (String regionName : subRegions)
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

            if (container != null)
            {
                Vec3i size = container.getSize();
                final int sizeX = size.getX();
                final int sizeY = size.getY();
                final int sizeZ = size.getZ();

                for (int y = 0; y < sizeY; ++y)
                {
                    for (int z = 0; z < sizeZ; ++z)
                    {
                        for (int x = 0; x < sizeX; ++x)
                        {
                            BlockState state = container.get(x, y, z);
                            countsTotal.addTo(state, 1);
                        }
                    }
                }
            }

            //Custom Additions (easier to resolve future merge conflicts)
            List<LitematicaSchematic.EntityInfo> entityList = schematic.getEntityListForRegion(regionName);
            if (entityList != null)
            {
                for (LitematicaSchematic.EntityInfo entityInfo : entityList)
                {
                    Optional<Identifier> entityIdOpt = Optional.ofNullable(entityInfo.nbt.get("id"))
                        .flatMap(nbt -> nbt.asString())
                        .map(id -> Identifier.of(id));

                    if (entityIdOpt.isEmpty()) {
                        continue;
                    }

                    if (entityIdOpt.map(id -> Registries.ENTITY_TYPE.get(id)).isEmpty()) {
                        continue;
                    }

                    entityIdOpt.map(id -> Registries.ITEM.get(id)).ifPresent(item -> {
                        countsItemsTotal.addTo(new ItemType(new ItemStack(item), false, false), 1);

                        // Item Frame
                        entityInfo.nbt.getCompound("Item")
                            .flatMap(nbtItem -> fromNbtCompound(nbtItem))
                            .ifPresent(pair -> countsItemsTotal.addTo(pair.getLeft(), pair.getRight()));

                        // Boats and Minecarts
                        entityInfo.nbt.getList("Items").ifPresent(nbtItems -> {
                            for (var nbtItemElem : nbtItems) {
                                nbtItemElem.asCompound()
                                    .flatMap(nbtItem -> fromNbtCompound(nbtItem))
                                    .ifPresent(pair -> countsItemsTotal.addTo(pair.getLeft(), pair.getRight()));
                            }
                        });

                        // Armor Stand
                        entityInfo.nbt.getCompound("equipment").ifPresent(equipmentNbt -> {
                            equipmentNbt.forEach((key, nbtItem) -> {
                                if (nbtItem instanceof NbtCompound nbtCompound) {
                                    fromNbtCompound(nbtCompound).ifPresent(pair -> countsItemsTotal.addTo(pair.getLeft(), pair.getRight()));
                                }
                            });
                        });
                    });
                }
            }

            Map<BlockPos, NbtCompound> blockEntities = schematic.getBlockEntityMapForRegion(regionName);
            if (blockEntities != null) {
                for (NbtCompound blockEntityNbt : blockEntities.values()) {
                    // Chests
                    blockEntityNbt.getList("Items").ifPresent(nbtItems -> {
                        for (var nbtItemElem : nbtItems) {
                            nbtItemElem.asCompound()
                                .flatMap(nbtItem -> fromNbtCompound(nbtItem))
                                .ifPresent(pair -> countsItemsTotal.addTo(pair.getLeft(), pair.getRight()));
                        }
                    });

                    // Decorated Pots
                    blockEntityNbt.getCompound("item")
                        .flatMap(nbtItem -> fromNbtCompound(nbtItem))
                        .ifPresent(pair -> countsItemsTotal.addTo(pair.getLeft(), pair.getRight()));
                }
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        Object2IntOpenHashMap<ItemType> itemTypesTotal = fromBlockStateCount(countsTotal, countsItemsTotal);
        Object2IntOpenHashMap<ItemType> itemTypesMissing = itemTypesTotal.clone();

        return getMaterialList(itemTypesTotal, itemTypesMissing, null, mc.player);
    }

    public static Optional<net.minecraft.util.Pair<ItemType, Integer>> fromNbtCompound(NbtCompound nbt) {
        return nbt.getString("id")
            .map(idString -> Identifier.of(idString))
            .map(id -> Registries.ITEM.get(id))
            .map(item -> {
                int count = nbt.getInt("count").orElse(1);
                return new net.minecraft.util.Pair<ItemType, Integer>(new ItemType(new ItemStack(item), false, false), count);
            });
    }

    //Custom Additions (easier to resolve future merge conflicts)
    //<BlockState> changed to <ItemType>
    public static List<MaterialListEntry> getMaterialList(
            Object2IntOpenHashMap<ItemType> itemTypesTotal,
            Object2IntOpenHashMap<ItemType> itemTypesMissing,
            Object2IntOpenHashMap<ItemType> itemTypesMismatch,
            PlayerEntity player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (itemTypesTotal != null && !itemTypesTotal.isEmpty())
        {
            if (player != null)
            {
                Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing == null ? 0 : itemTypesMissing.getInt(type),
                                                   itemTypesMismatch == null ? 0 : itemTypesMismatch.getInt(type),
                                                   playerInvItems.getInt(type)));
                }
            }
            else
            {
                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing == null ? 0 : itemTypesMissing.getInt(type),
                                                   itemTypesMismatch == null ? 0 : itemTypesMismatch.getInt(type),
                                                   0));
                }
            }
        }

        return list;
    }

    //Custom Additions (easier to resolve future merge conflicts)
    public static void convertStatesToStacks(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            Object2IntOpenHashMap<ItemType> itemTypesOut)
    {
        //Custom Additions (easier to resolve future merge conflicts)
        final MaterialCache cache = MaterialCache.getInstance();

        for (BlockState state : blockStatesIn.keySet())
        {
            int count = blockStatesIn.getInt(state);
            BlockState stateToConvert = isWaterloggedBlock(state) ? getBaseBlockState(state) : state;

            // Add water bucket for waterlogged blocks
            if (isWaterloggedBlock(state))
            {
                itemTypesOut.addTo(new ItemType(new ItemStack(net.minecraft.item.Items.WATER_BUCKET), false, false), count);
            }

            // Convert block to items
            if (cache.requiresMultipleItems(stateToConvert))
            {
                for (ItemStack stack : cache.getItems(stateToConvert))
                {
                    if (!stack.isEmpty())
                    {
                        itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                    }
                }
            }
            else
            {
                ItemStack stack = cache.getRequiredBuildItemForState(stateToConvert);
                if (!stack.isEmpty())
                {
                    itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                }
            }
        }
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list, PlayerEntity player)
    {
        if (player == null) return;
        Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

        for (MaterialListEntry entry : list)
        {
            ItemType type = new ItemType(entry.getStack(), true, false);
            int countAvailable = playerInvItems.getInt(type);
            entry.setCountAvailable(countAvailable);
        }
    }

    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(Inventory inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.size();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getStack(slot);

            if (stack.isEmpty() == false)
            {
                Item item = stack.getItem();

                if (item instanceof BlockItem &&
                    ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock &&
                    InventoryUtils.shulkerBoxHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> boxCounts = getStoredItemCounts(stack);

                    for (ItemType boxType : boxCounts.keySet())
                    {
                        map.addTo(boxType, boxCounts.getInt(boxType));
                    }

                    boxCounts.clear();
                }
                else if (item instanceof BundleItem && InventoryUtils.bundleHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> bundleCounts = getBundleItemCounts(stack);

                    for (ItemType bundleType : bundleCounts.keySet())
                    {
                        map.addTo(bundleType, bundleCounts.getInt(bundleType));
                    }

                    bundleCounts.clear();
                }
                else
                {
                    map.addTo(new ItemType(stack, true, false), stack.getCount());
                }
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getStoredItemCounts(ItemStack stackShulkerBox)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        DefaultedList<ItemStack> items = InventoryUtils.getStoredItems(stackShulkerBox);

        for (ItemStack boxStack : items)
        {
            if (boxStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (boxStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(boxStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(boxStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(boxStack, false, false), boxStack.getCount());
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getBundleItemCounts(ItemStack stackBundle)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        DefaultedList<ItemStack> items = InventoryUtils.getBundleItems(stackBundle);

        for (ItemStack bundleStack : items)
        {
            if (bundleStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (bundleStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(bundleStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(bundleStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(bundleStack, false, false), bundleStack.getCount());
            }
        }

        return map;
    }

    private static boolean isWaterloggedBlock(BlockState state)
    {
        return state.contains(net.minecraft.state.property.Properties.WATERLOGGED) &&
               state.get(net.minecraft.state.property.Properties.WATERLOGGED);
    }

    private static BlockState getBaseBlockState(BlockState state)
    {
        if (state.contains(net.minecraft.state.property.Properties.WATERLOGGED))
        {
            return state.with(net.minecraft.state.property.Properties.WATERLOGGED, false);
        }
        return state;
    }

    //Custom Additions (easier to resolve future merge conflicts)
    public static Object2IntOpenHashMap<ItemType> fromBlockStateCount(Object2IntOpenHashMap<BlockState> blockStateCounts, Object2IntOpenHashMap<ItemType> additionalCounts)
    {
        Object2IntOpenHashMap<ItemType> combinedCounts = additionalCounts == null ? new Object2IntOpenHashMap<>() : additionalCounts;

        convertStatesToStacks(blockStateCounts, combinedCounts);

        return combinedCounts;
    }

    public static Object2IntOpenHashMap<ItemType> fromBlockStateCount(Object2IntOpenHashMap<BlockState> blockStateCounts)
    {
        return fromBlockStateCount(blockStateCounts, null);
    }

}
