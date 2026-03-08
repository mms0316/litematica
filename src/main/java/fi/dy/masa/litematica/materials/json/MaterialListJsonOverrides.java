package fi.dy.masa.litematica.materials.json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import fi.dy.masa.malilib.data.CachedTagUtils;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import fi.dy.masa.litematica.data.CachedTagManager;

public class MaterialListJsonOverrides
{
    public static final MaterialListJsonOverrides INSTANCE = new MaterialListJsonOverrides();
    private final Set<ResultOverride> overrides = new HashSet<>();
    private final Set<ResultOverride> packingOverrides = new HashSet<>();

    protected MaterialListJsonOverrides()
    {
        this.initOverrides();
        this.initPackingOverrides();
    }

    private Holder<Item> add(Item item)
    {
        return BuiltInRegistries.ITEM.wrapAsHolder(item);
    }

    private void initOverrides()
    {
        // Copper Block
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER),         this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER),       this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER),        this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER),   this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER), this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER),  this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        // Copper Grate
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_GRATE),         this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_GRATE),       this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_GRATE),        this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_GRATE),   this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_GRATE), this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_GRATE),  this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        // Cut Copper
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER),         this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER),       this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER),        this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER),   this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER), this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER),  this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        // Chiseled Copper
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CHISELED_COPPER),         this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CHISELED_COPPER),       this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CHISELED_COPPER),        this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CHISELED_COPPER),   this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CHISELED_COPPER), this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CHISELED_COPPER),  this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        // Copper Bulb
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_BULB),         this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_BULB),       this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_BULB),        this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_BULB),   this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_BULB), this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_BULB),  this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        // Copper Slab
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER_SLAB),         this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER_SLAB),       this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER_SLAB),        this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER_SLAB),   this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER_SLAB), this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB),  this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        // Copper Stairs
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER_STAIRS),         this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER_STAIRS),       this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER_STAIRS),        this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS),   this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS), this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS),  this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        // Copper Door
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_DOOR),         this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_DOOR),       this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_DOOR),        this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_DOOR),   this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_DOOR), this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_DOOR),  this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        // Copper Trap Door
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_TRAPDOOR),         this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_TRAPDOOR),       this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_TRAPDOOR),        this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_TRAPDOOR),   this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_TRAPDOOR), this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR),  this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        // Stripped Woods
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_ACACIA_LOG),   this.add(Items.ACACIA_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_BAMBOO_BLOCK), this.add(Items.BAMBOO), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_BIRCH_LOG),    this.add(Items.BIRCH_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_CHERRY_LOG),   this.add(Items.CHERRY_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_CRIMSON_STEM), this.add(Items.CRIMSON_STEM), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_DARK_OAK_LOG), this.add(Items.DARK_OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_JUNGLE_LOG),   this.add(Items.JUNGLE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_MANGROVE_LOG), this.add(Items.MANGROVE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_OAK_LOG),      this.add(Items.OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_PALE_OAK_LOG), this.add(Items.PALE_OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_SPRUCE_LOG),   this.add(Items.SPRUCE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_WARPED_STEM),  this.add(Items.WARPED_STEM), Fraction.ONE));
        // Concrete
        this.overrides.add(new ResultOverride(this.add(Items.BLACK_CONCRETE),       this.add(Items.BLACK_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.BLUE_CONCRETE),        this.add(Items.BLUE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.BROWN_CONCRETE),       this.add(Items.BROWN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.CYAN_CONCRETE),        this.add(Items.CYAN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.GRAY_CONCRETE),        this.add(Items.GRAY_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.GREEN_CONCRETE),       this.add(Items.GREEN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIGHT_BLUE_CONCRETE),  this.add(Items.LIGHT_BLUE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIGHT_GRAY_CONCRETE),  this.add(Items.LIGHT_GRAY_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIME_CONCRETE),        this.add(Items.LIME_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.MAGENTA_CONCRETE),     this.add(Items.MAGENTA_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.ORANGE_CONCRETE),      this.add(Items.ORANGE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.PINK_CONCRETE),        this.add(Items.PINK_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.PURPLE_CONCRETE),      this.add(Items.PURPLE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.RED_CONCRETE),         this.add(Items.RED_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.YELLOW_CONCRETE),      this.add(Items.YELLOW_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WHITE_CONCRETE),       this.add(Items.WHITE_CONCRETE_POWDER), Fraction.ONE));
        // Anvils
        this.overrides.add(new ResultOverride(this.add(Items.CHIPPED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.DAMAGED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
    }

    private void initPackingOverrides()
    {
        Fraction by9 = Fraction.getFraction(1, 9);
        // x4 = x1
        this.packingOverrides.add(new ResultOverride(this.add(Items.CLAY_BALL), this.add(Items.CLAY), Fraction.ONE_QUARTER));
        this.packingOverrides.add(new ResultOverride(this.add(Items.HONEY_BOTTLE), this.add(Items.HONEY_BLOCK), Fraction.ONE_QUARTER));

        // x9 = x1
        this.packingOverrides.add(new ResultOverride(this.add(Items.BONE_MEAL), this.add(Items.BONE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.COAL), this.add(Items.COAL_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.COPPER_INGOT), this.add(Items.COPPER_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.DIAMOND), this.add(Items.DIAMOND_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.EMERALD), this.add(Items.EMERALD_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.GOLD_INGOT), this.add(Items.GOLD_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.GOLD_NUGGET), this.add(Items.GOLD_INGOT), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.IRON_INGOT), this.add(Items.IRON_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.IRON_NUGGET), this.add(Items.IRON_INGOT), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.LAPIS_LAZULI), this.add(Items.LAPIS_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.MELON_SLICE), this.add(Items.MELON), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.NETHERITE_INGOT), this.add(Items.NETHERITE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.REDSTONE), this.add(Items.REDSTONE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.RESIN_BRICK), this.add(Items.RESIN_BRICKS), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.RESIN_CLUMP), this.add(Items.RESIN_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.SLIME_BALL), this.add(Items.SLIME_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.WHEAT), this.add(Items.HAY_BLOCK), by9));
    }

    protected Pair<Holder<Item>, Integer> matchOverride(Holder<Item> result, Integer total)
    {
        for (ResultOverride map : this.overrides)
        {
            if (map.match(result))
            {
                return Pair.of(map.result(), map.mulInt(total));
            }
        }

        return Pair.of(result, total);
    }

    protected Triple<Holder<Item>, Float, Integer> matchPackingOverride(Holder<Item> result, Integer total)
    {
        for (ResultOverride map : this.packingOverrides)
        {
            if (map.match(result))
            {
                return Triple.of(map.result(), map.mulFloat(total), map.divisor());
            }
        }

        return Triple.of(result, total.floatValue(), 1);
    }

    // Overrides for re-dying recipe's
    protected Holder<Item> overridePrimaryMaterial(Holder<Item> firstItem)
    {
        if (firstItem.is(ItemTags.WOOL))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_WOOL);
        }
        else if (firstItem.is(ItemTags.WOOL_CARPETS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_CARPET);
        }
        else if (firstItem.is(ItemTags.BEDS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_BED);
        }
        else if (firstItem.is(ItemTags.CANDLES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.CANDLE);
        }
        else if (firstItem.is(ItemTags.SHULKER_BOXES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.SHULKER_BOX);
        }
        else if (firstItem.is(ItemTags.BANNERS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_BANNER);
        }
        else if (firstItem.is(ItemTags.TERRACOTTA))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.TERRACOTTA);
        }
        else if (firstItem.is(ItemTags.BUNDLES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.BUNDLE);
        }
        else if (firstItem.is(ItemTags.HARNESSES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_HARNESS);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.GLASS);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.GLASS_PANE);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_POWDER_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_CONCRETE_POWDER);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_CONCRETE);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLAZED_TERRACOTTA_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WHITE_GLAZED_TERRACOTTA);
        }

        return this.matchOverride(firstItem, 1).getLeft();
    }

    // Overrides for particular cases, such as redying of beds instead of choosing the Wool recipe.
    protected boolean overrideShouldSkipRecipe(Holder<Item> input, List<Ingredient> ingredients)
    {
        for (Ingredient ing : ingredients)
        {
            if (input.is(ItemTags.BEDS))
            {
                if (ing.test(Items.WHITE_BED.getDefaultInstance()) ||
                    ing.test(Items.BLACK_BED.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.WOOL))
            {
                if (ing.test(Items.WHITE_WOOL.getDefaultInstance()) ||
                    ing.test(Items.BLACK_WOOL.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.WOOL_CARPETS))
            {
                if (ing.test(Items.WHITE_CARPET.getDefaultInstance()) ||
                    ing.test(Items.BLACK_CARPET.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.CANDLES))
            {
                if (ing.test(Items.WHITE_CANDLE.getDefaultInstance()) ||
                    ing.test(Items.BLACK_CANDLE.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.SHULKER_BOXES))
            {
                if (ing.test(Items.WHITE_SHULKER_BOX.getDefaultInstance()) ||
                    ing.test(Items.BLACK_SHULKER_BOX.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.BANNERS))
            {
                if (ing.test(Items.WHITE_BANNER.getDefaultInstance()) ||
                    ing.test(Items.BLACK_BANNER.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.TERRACOTTA))
            {
                if (ing.test(Items.WHITE_TERRACOTTA.getDefaultInstance()) ||
                    ing.test(Items.BLACK_TERRACOTTA.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_STAINED_GLASS.getDefaultInstance()) ||
                    ing.test(Items.BLACK_STAINED_GLASS.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_STAINED_GLASS_PANE.getDefaultInstance()) ||
                    ing.test(Items.BLACK_STAINED_GLASS_PANE.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_CONCRETE.getDefaultInstance()) ||
                    ing.test(Items.BLACK_CONCRETE.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_POWDER_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_CONCRETE_POWDER.getDefaultInstance()) ||
                    ing.test(Items.BLACK_CONCRETE_POWDER.getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLAZED_TERRACOTTA_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_GLAZED_TERRACOTTA.getDefaultInstance()) ||
                    ing.test(Items.BLACK_GLAZED_TERRACOTTA.getDefaultInstance()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean shouldKeepItemOrBlock(Holder<Item> input)
    {
//        if (CachedTagManager.matchItemTag(CachedTagManager.PACKED_BLOCK_ITEMS_KEY, input))
//        {
//            return true;
//        }
//        else
        // Only count Ingots / Nuggets as base material
        return CachedTagUtils.matchItemTag(CachedTagManager.UNPACKED_BLOCK_ITEMS_KEY, input);
    }

    public record ResultOverride(Holder<Item> input, Holder<Item> result, Fraction multiplier)
    {
        public boolean match(Holder<Item> otherItem) { return this.input().is(otherItem.unwrapKey().orElseThrow()); }

        public Integer mulInt(Integer totalIn) { return totalIn * this.multiplier().intValue(); }

        public Float mulFloat(Integer totalIn) { return totalIn * this.multiplier().floatValue(); }

        public Integer divisor() { return this.multiplier().getDenominator(); }
    }
}
