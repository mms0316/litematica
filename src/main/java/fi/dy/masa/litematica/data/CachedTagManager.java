package fi.dy.masa.litematica.data;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.data.CachedItemTags;
import fi.dy.masa.malilib.data.CachedTagKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

/**
 * Caches Block/Item Tags as if they are real Vanilla Block/Item tags.
 */
public class CachedTagManager
{
	public static final CachedTagKey GLASS_ITEMS_KEY                = new CachedTagKey(Reference.MOD_ID, "glass_items");
	public static final CachedTagKey GLASS_PANE_ITEMS_KEY           = new CachedTagKey(Reference.MOD_ID, "glass_pane_items");
	public static final CachedTagKey CONCRETE_POWDER_ITEMS_KEY      = new CachedTagKey(Reference.MOD_ID, "concrete_powder_items");
	public static final CachedTagKey CONCRETE_ITEMS_KEY             = new CachedTagKey(Reference.MOD_ID, "concrete_items");
	public static final CachedTagKey GLAZED_TERRACOTTA_ITEMS_KEY    = new CachedTagKey(Reference.MOD_ID, "glazed_terracotta_items");
	public static final CachedTagKey PACKED_BLOCK_ITEMS_KEY         = new CachedTagKey(Reference.MOD_ID, "packed_block_items");
    public static final CachedTagKey UNPACKED_BLOCK_ITEMS_KEY       = new CachedTagKey(Reference.MOD_ID, "unpacked_block_items");

    public List<CachedTagKey> getKeys()
    {
        List<CachedTagKey> list = new ArrayList<>();

        list.add(GLASS_ITEMS_KEY);
        list.add(GLASS_PANE_ITEMS_KEY);
        list.add(CONCRETE_POWDER_ITEMS_KEY);
        list.add(CONCRETE_ITEMS_KEY);
        list.add(GLAZED_TERRACOTTA_ITEMS_KEY);
        list.add(PACKED_BLOCK_ITEMS_KEY);
        list.add(UNPACKED_BLOCK_ITEMS_KEY);

        return list;
    }

    public static void startCache()
    {
        clearCache();

		CachedItemTags.getInstance().build(GLASS_ITEMS_KEY, buildGlassItemCache());
		CachedItemTags.getInstance().build(GLASS_PANE_ITEMS_KEY, buildGlassPanesItemCache());
		CachedItemTags.getInstance().build(CONCRETE_POWDER_ITEMS_KEY, buildConcretePowderItemCache());
		CachedItemTags.getInstance().build(CONCRETE_ITEMS_KEY, buildConcreteItemCache());
		CachedItemTags.getInstance().build(GLAZED_TERRACOTTA_ITEMS_KEY, buildGlazedTerracottaItemCache());
        CachedItemTags.getInstance().build(PACKED_BLOCK_ITEMS_KEY, buildPackedBlockItemCache());
        CachedItemTags.getInstance().build(UNPACKED_BLOCK_ITEMS_KEY, buildUnpackedBlockItemCache());
    }

	private static List<String> buildGlassItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLACK_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLUE_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BROWN_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.CYAN_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GRAY_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GREEN_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_BLUE_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_GRAY_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIME_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.MAGENTA_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.ORANGE_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PINK_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PURPLE_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.RED_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.YELLOW_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.WHITE_STAINED_GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.TINTED_GLASS).toString());

		return list;
	}

	private static List<String> buildGlassPanesItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLACK_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLUE_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BROWN_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.CYAN_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GRAY_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GREEN_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_BLUE_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_GRAY_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIME_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.MAGENTA_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.ORANGE_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PINK_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PURPLE_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.RED_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.YELLOW_STAINED_GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.WHITE_STAINED_GLASS_PANE).toString());

		return list;
	}

	private static List<String> buildConcretePowderItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.BLACK_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLUE_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BROWN_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.CYAN_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GRAY_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GREEN_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_BLUE_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_GRAY_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIME_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.MAGENTA_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.ORANGE_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PINK_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PURPLE_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.RED_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.YELLOW_CONCRETE_POWDER).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.WHITE_CONCRETE_POWDER).toString());

		return list;
	}

	private static List<String> buildConcreteItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.BLACK_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLUE_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BROWN_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.CYAN_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GRAY_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GREEN_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_BLUE_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_GRAY_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIME_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.MAGENTA_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.ORANGE_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PINK_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PURPLE_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.RED_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.YELLOW_CONCRETE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.WHITE_CONCRETE).toString());

		return list;
	}

	private static List<String> buildGlazedTerracottaItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.BLACK_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BLUE_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.BROWN_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.CYAN_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GRAY_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.GREEN_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_BLUE_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIGHT_GRAY_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.LIME_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.MAGENTA_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.ORANGE_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PINK_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.PURPLE_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.RED_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.YELLOW_GLAZED_TERRACOTTA).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.WHITE_GLAZED_TERRACOTTA).toString());

		return list;
	}
    private static List<String> buildPackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(BuiltInRegistries.ITEM.getKey(Items.BONE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.CLAY).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COAL_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COPPER_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.EMERALD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HAY_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HONEY_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.LAPIS_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.MELON).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_COPPER_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_GOLD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.REDSTONE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BRICKS).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.SLIME_BLOCK).toString());

        return list;
    }

    private static List<String> buildUnpackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(BuiltInRegistries.ITEM.getKey(Items.BONE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.CLAY_BALL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COAL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COPPER_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.EMERALD).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GLOWSTONE_DUST).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_NUGGET).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HONEY_BOTTLE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.ICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.LAPIS_LAZULI).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.MELON_SLICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHER_WART).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.PACKED_ICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.REDSTONE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BRICK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_CLUMP).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.SLIME_BALL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.WHEAT).toString());

        return list;
    }

    private static void clearCache()
    {
		CachedItemTags.getInstance().clearEntry(GLASS_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(GLASS_PANE_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(CONCRETE_POWDER_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(CONCRETE_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(GLAZED_TERRACOTTA_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(PACKED_BLOCK_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(UNPACKED_BLOCK_ITEMS_KEY);
    }
}
