package fi.dy.masa.litematica.schematic.conversion;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.nbt.NbtBlockUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.Litematica;

public class SchematicDowngradeConverter
{
//    private static final AnsiLogger LOGGER = new AnsiLogger(SchematicDowngradeConverter.class, true, true);

    public static CompoundTag downgradeEntity_to_1_20_4(CompoundTag oldEntity, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag newEntity = new CompoundTag();

        if (!oldEntity.contains("id"))
        {
            return oldEntity;
        }
        for (String key : oldEntity.keySet())
        {
            switch (key)
            {
                case "x" -> newEntity.putInt("x", oldEntity.getIntOr("x", 0));
                case "y" -> newEntity.putInt("y", oldEntity.getIntOr("y", 0));
                case "z" -> newEntity.putInt("z", oldEntity.getIntOr("z", 0));
                case "id" -> newEntity.putString("id", oldEntity.getStringOr("id", ""));
                case "attributes" -> newEntity.put("Attributes", processAttributes(oldEntity.get(key), minecraftDataVersion, registryManager));
                case "flower_pos" -> newEntity.put("FlowerPos", processFlowerPos(oldEntity, key, minecraftDataVersion, registryManager));
                case "hive_pos" -> newEntity.put("HivePos", processFlowerPos(oldEntity, key, minecraftDataVersion, registryManager));
                case "ArmorItems" -> newEntity.put("ArmorItems", processEntityItems(oldEntity.getListOrEmpty(key), minecraftDataVersion, registryManager, 4));
                case "HandItems" -> newEntity.put("HandItems", processEntityItems(oldEntity.getListOrEmpty(key), minecraftDataVersion, registryManager, 2));
                case "Item" -> newEntity.put("Item", processEntityItem(oldEntity.get(key), minecraftDataVersion, registryManager));
                case "Inventory" -> newEntity.put("Inventory", processEntityItems(oldEntity.getListOrEmpty(key), minecraftDataVersion, registryManager, 1));
                // 1.21.5+ tags
                case "equipment" -> newEntity.merge(processEntityEquipment(oldEntity.get(key), minecraftDataVersion, registryManager));
                case "drop_chances" -> newEntity.merge(processEntityDropChances(oldEntity.get(key)));
                case "fall_distance" -> newEntity.putFloat("FallDistance", oldEntity.getFloatOr(key, 0f));
                // NbtUtils.readBlockPosFromArrayTag() // get(key, BlockPos.CODEC).orElse(null)
                case "anchor_pos" -> processBlockPosTag(NbtUtils.readBlockPosFromIntArray(oldEntity, key), "A", newEntity);
                case "block_pos" -> processBlockPosTag(NbtUtils.readBlockPosFromIntArray(oldEntity, key), "Tile", newEntity);
                case "bound_pos" -> processBlockPosTag(NbtUtils.readBlockPosFromIntArray(oldEntity, key), "Bound", newEntity);
                case "home_pos" -> processBlockPosTag(NbtUtils.readBlockPosFromIntArray(oldEntity, key), "HomePos", newEntity);
                case "sleeping_pos" -> processBlockPosTag(NbtUtils.readBlockPosFromIntArray(oldEntity, key), "Sleeping", newEntity);
                case "has_egg" -> newEntity.putBoolean("HasEgg", oldEntity.getBooleanOr(key, false));
                case "life_ticks" -> newEntity.putInt("LifeTicks", oldEntity.getIntOr(key, 0));
                case "size" -> newEntity.putInt("Size", oldEntity.getIntOr(key, 0));
                default -> newEntity.put(key, oldEntity.get(key));
            }
        }

        return newEntity;
    }

    private static void processBlockPosTag(@Nullable BlockPos oldPos, String prefix, CompoundTag newTags)
    {
        if (oldPos != null)
        {
            newTags.putInt(prefix+"X", oldPos.getX());
            newTags.putInt(prefix+"Y", oldPos.getY());
            newTags.putInt(prefix+"Z", oldPos.getZ());
        }
    }

    private static CompoundTag processEntityDropChances(Tag nbtElement)
    {
        CompoundTag oldTags = nbtElement.asCompound().orElse(new CompoundTag());
        CompoundTag newTags = new CompoundTag();
        ListTag handDrops = new ListTag();
        ListTag armorDrops = new ListTag();

        for (int i = 0; i < 2; i++)
        {
            handDrops.add(FloatTag.valueOf(DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE));
        }

        for (int i = 0; i < 4; i++)
        {
            armorDrops.add(FloatTag.valueOf(DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE));
        }

        for (String key : oldTags.keySet())
        {
            switch (key)
            {
                case "mainhand" -> handDrops.set(0, oldTags.get(key));
                case "offhand" -> handDrops.set(1, oldTags.get(key));
                case "feet" -> armorDrops.set(0, oldTags.get(key));
                case "legs" -> armorDrops.set(1, oldTags.get(key));
                case "chest" -> armorDrops.set(2, oldTags.get(key));
                case "head" -> armorDrops.set(3, oldTags.get(key));
                // Not used
                //case "body" -> newTags.put("body_armor_drop_chance", oldTags.get(key));
                //case "saddle" -> newTags.put("SaddleItem", oldTags.get(key));
                default -> {}
            }
        }

        newTags.put("HandDropChances", handDrops);
        newTags.put("ArmorDropChances", armorDrops);

        return newTags;
    }

    private static CompoundTag processEntityEquipment(Tag equipmentEntries, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag oldTags = equipmentEntries.asCompound().orElse(new CompoundTag());
        CompoundTag newTags = new CompoundTag();
        ListTag newHandItems = new ListTag();
        ListTag newArmorItems = new ListTag();

        for (int i = 0; i < 2; i++)
        {
            newHandItems.add(new CompoundTag());
        }

        for (int i = 0; i < 4; i++)
        {
            newArmorItems.add(new CompoundTag());
        }

        for (String key : oldTags.keySet())
        {
            switch (key)
            {
                case "mainhand" -> newHandItems.set(0, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "offhand" -> newHandItems.set(1, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "feet" -> newArmorItems.set(0, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "legs" -> newArmorItems.set(1, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "chest" -> newArmorItems.set(2, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "head" -> newArmorItems.set(3, processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                case "body" ->
                {
                    // Why is this duplicated in 1.20.4?  the world may never know...
                    Tag ele = processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager);
                    newArmorItems.set(2, ele);
                    newTags.put("ArmorItem", ele);
                }
                case "saddle" -> newTags.put("SaddleItem", processEntityItem(oldTags.get(key), minecraftDataVersion, registryManager));
                default -> {}
            }
        }

        newTags.put("HandItems", newHandItems);
        newTags.put("ArmorItems", newArmorItems);

        return newTags;
    }

    private static Tag processEntityItem(Tag itemEntry, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag oldItem = itemEntry.asCompound().orElse(new CompoundTag());;
        CompoundTag newItem = new CompoundTag();

        if (!oldItem.contains("id"))
        {
            return itemEntry;
        }
        String idName = oldItem.getStringOr("id", "");
        newItem.putString("id", idName);
        if (oldItem.contains("count"))
        {
            newItem.putByte("Count", (byte) oldItem.getIntOr("count", 1));
        }
        if (oldItem.contains("components"))
        {
            newItem.put("tag", processComponentsTag(oldItem.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
        }
        else
        {
            if (needsDamageTag(idName))
            {
                CompoundTag newTag = new CompoundTag();
                newTag.putInt("Damage", 0);
                newItem.put("tag", newTag);
            }
        }

        return newItem;
    }

    private static ListTag processEntityItems(ListTag oldItems, int minecraftDataVersion, RegistryAccess registryManager, int expectedSize)
    {
        ListTag newItems = new ListTag();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundTag itemEntry = oldItems.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            if (itemEntry.contains("id"))
            {
                String idName = itemEntry.getStringOr("id", "");
                newEntry.putString("id", idName);

                if (itemEntry.contains("count"))
                {
                    newEntry.putByte("Count", (byte) itemEntry.getIntOr("count", 1));
                }
                else
                {
                    newEntry.putByte("Count", (byte) 1);
                }
                if (itemEntry.contains("components"))
                {
                    newEntry.put("tag", processComponentsTag(itemEntry.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
                }
                else
                {
                    if (needsDamageTag(idName))
                    {
                        CompoundTag newTag = new CompoundTag();
                        newTag.putInt("Damage", 0);
                        newEntry.put("tag", newTag);
                    }
                }
            }

            newItems.add(newEntry);
        }

        if (newItems.size() < expectedSize)
        {
            int addTotal = expectedSize - newItems.size();

            for (int i = 0; i < addTotal; i++)
            {
                newItems.add(i, new CompoundTag());
            }
        }

        return newItems;
    }

    private static Tag processAttributes(Tag attrib, int minecraftDataVersion, RegistryAccess registryManager)
    {
        ListTag oldAttr = attrib.asList().orElse(new ListTag());

        if (oldAttr.isEmpty())
        {
            CompoundTag oldTag = attrib.asCompound().orElse(new CompoundTag());

            if (oldTag.isEmpty()) return attrib;

            for (String key : oldTag.keySet())
            {
                if (key.equals("modifiers"))
                {
                    return processAttributeModifiers(oldTag.getListOrEmpty(key), minecraftDataVersion, registryManager);
                }
            }
        }

        return processAttributeBase(oldAttr, minecraftDataVersion, registryManager);
    }

    private static Tag processAttributeBase(ListTag oldAttr, int minecraftDataVersion, RegistryAccess registryManager)
    {
        ListTag newAttr = new ListTag();

        for (int i = 0; i < oldAttr.size(); i++)
        {
            CompoundTag attrEntry = oldAttr.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            if (attrEntry.contains("type"))
            {
                newEntry.putString("Name", attributeRename(attrEntry.getStringOr("type", "")));
                newEntry.putDouble("Base", attrEntry.getDoubleOr("amount", 0D));
            }
            else
            {
                newEntry.putString("Name", attributeRename(attrEntry.getStringOr("id", "")));
                newEntry.putDouble("Base", attrEntry.getDoubleOr("base", 0D));
            }

            ListTag listEntry = attrEntry.getListOrEmpty("modifiers");
            ListTag newMods = processAttributeModifiers(listEntry, minecraftDataVersion, registryManager);

            if (!newMods.isEmpty())
            {
                newEntry.put("Modifiers", newMods);
            }
            newAttr.add(newEntry);
        }

        return newAttr;
    }

    private static ListTag processAttributeModifiers(ListTag modifiers, int minecraftDataVersion, RegistryAccess registryManager)
    {
        ListTag newMods = new ListTag();

        if (modifiers.isEmpty()) return modifiers;

        for (int y = 0; y < modifiers.size(); y++)
        {
            CompoundTag modEntry = modifiers.getCompoundOrEmpty(y);
            CompoundTag newMod = new CompoundTag();

            if (modEntry.contains("type"))
            {
                newMod.putString("Name", attributeRename(modEntry.getStringOr("type", "")));
                newMod.putDouble("Base", modEntry.getDoubleOr("amount", 0D));
            }
            else
            {
                newMod.putDouble("Amount", modEntry.getDoubleOr("amount", 0D));
                newMod.putString("Name", modifierIdToName(modEntry.getStringOr("id", "")));
            }

            newMod.putInt("Operation", modifierOperationToInt(modEntry.getStringOr("operation", "")));
            //newMod.putUuid("UUID", modEntry.contains("UUID") ? modEntry.getUuid("UUID") : UUID.randomUUID());
            newMod.store("UUID", UUIDUtil.AUTHLIB_CODEC, modEntry.read("UUID", UUIDUtil.AUTHLIB_CODEC, registryManager.createSerializationContext(NbtOps.INSTANCE)).orElse(UUID.randomUUID()));
            newMods.add(newMod);
        }

        return newMods;
    }

    private static String attributeRename(String idIn)
    {
        switch (idIn)
        {
            case "minecraft:armor" ->
            {
                return "minecraft:generic.armor";
            }
            case "minecraft:armor_toughness" ->
            {
                return "minecraft:generic.armor_toughness";
            }
            case "minecraft:attack_damage" ->
            {
                return "minecraft:generic.attack_damage";
            }
            case "minecraft:attack_knockback" ->
            {
                return "minecraft:generic.attack_knockback";
            }
            case "minecraft:attack_speed" ->
            {
                return "minecraft:generic.attack_speed";
            }
            case "minecraft:flying_speed" ->
            {
                return "minecraft:generic.flying_speed";
            }
            case "minecraft:follow_range" ->
            {
                return "minecraft:generic.follow_range";
            }
            case "minecraft:jump_strength" ->
            {
                return "minecraft:horse.jump_strength";
                // return "minecraft:generic.jump_strength"; --> (1.20.6 / 1.21 only)
            }
            case "minecraft:knockback_resistance" ->
            {
                return "minecraft:generic.knockback_resistance";
            }
            case "minecraft:luck" ->
            {
                return "minecraft:generic.luck";
            }
            case "minecraft:max_absorption" ->
            {
                return "minecraft:generic.max_absorption";
            }
            case "minecraft:max_health" ->
            {
                return "minecraft:generic.max_health";
            }
            case "minecraft:movement_speed" ->
            {
                return "minecraft:generic.movement_speed";
            }
            case "minecraft:spawn_reinforcements" ->
            {
                return "minecraft:zombie.spawn_reinforcements";
            }

            // tempt_range --> No match
            // These don't exist in 1.20.4 (1.20.6 / 1.21 only)
            case "minecraft:block_break_speed" ->
            {
                return "minecraft:player.block_break_speed";
            }
            case "minecraft:block_interaction_range" ->
            {
                return "minecraft:player.block_interaction_range";
            }
            case "minecraft:burning_time" ->
            {
                return "minecraft:generic.burning_time";
            }
            case "minecraft:explosion_knockback_resistance" ->
            {
                return "minecraft:generic.explosion_knockback_resistance";
            }
            case "minecraft:entity_interaction_range" ->
            {
                return "minecraft:player.entity_interaction_range";
            }
            case "minecraft:fall_damage_multiplier" ->
            {
                return "minecraft:generic.fall_damage_multiplier";
            }
            case "minecraft:gravity" ->
            {
                return "minecraft:generic.gravity";
            }
            case "minecraft:mining_efficiency" ->
            {
                return "minecraft:player.mining_efficiency";
            }
            case "minecraft:movement_efficiency" ->
            {
                return "minecraft:generic.movement_efficiency";
            }
            case "minecraft:oxygen_bonus" ->
            {
                return "minecraft:generic.oxygen_bonus";
            }
            case "minecraft:safe_fall_distance" ->
            {
                return "minecraft:generic.safe_fall_distance";
            }
            case "minecraft:scale" ->
            {
                return "minecraft:generic.scale";
            }
            case "minecraft:sneaking_speed" ->
            {
                return "minecraft:player.sneaking_speed";
            }
            case "minecraft:step_height" ->
            {
                return "minecraft:generic.step_height";
            }
            case "minecraft:submerged_mining_speed" ->
            {
                return "minecraft:player.submerged_mining_speed";
            }
            case "minecraft:sweeping_damage_ratio" ->
            {
                return "minecraft:player.sweeping_damage_ratio";
            }
            case "minecraft:water_movement_efficiency" ->
            {
                return "minecraft:generic.water_movement_efficiency";
            }
        }

        return idIn;
    }

    private static String modifierIdToName(String idIn)
    {
        if (idIn.equals("minecraft:random_spawn_bonus"))
        {
            return "Random spawn bonus";
        }

        return "";
    }

    private static int modifierOperationToInt(String op)
    {
        switch (op)
        {
            case "add_value" ->
            {
                return 0;
            }
            case "add_multiplied_base" ->
            {
                return 1;
            }
            case "add_multiplied_total" ->
            {
                return 2;
            }
        }

        return 0;
    }

    public static CompoundTag downgradeBlockEntity_to_1_20_4(CompoundTag oldTE, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag newTE = new CompoundTag();

        if (!oldTE.contains("id"))
        {
            oldTE.merge(SchematicConversionMaps.checkForIdTag(oldTE));
        }
        for (String key : oldTE.keySet())
        {
            switch (key)
            {
                case "x" -> newTE.putInt("x", oldTE.getIntOr("x", 0));
                case "y" -> newTE.putInt("y", oldTE.getIntOr("y", 0));
                case "z" -> newTE.putInt("z", oldTE.getIntOr("z", 0));
                case "id" -> newTE.putString("id", oldTE.getStringOr("id", ""));
                case "Items" -> newTE.put("Items", processItemsTag(oldTE.getListOrEmpty("Items"), minecraftDataVersion, registryManager));
                case "patterns" -> newTE.put("Patterns", processBannerPatterns(oldTE.get(key)));
                case "profile" -> newTE.put("SkullOwner", processSkullProfile(oldTE.get(key), newTE, minecraftDataVersion, registryManager));
                case "flower_pos" -> newTE.put("FlowerPos", processFlowerPos(oldTE, key, minecraftDataVersion, registryManager));
                case "bees" -> newTE.put("Bees", processBeesTag(oldTE.get(key), minecraftDataVersion, registryManager));
                case "item" -> newTE.put("item", processDecoratedPot(oldTE.get(key), minecraftDataVersion, registryManager));
                case "last_interacted_slot" -> newTE.put("last_interacted_slot", oldTE.get(key));
                case "ticks_since_song_started" ->
                {
                    newTE.putLong("RecordStartTick", 0L);
                    newTE.putLong("TickCount", oldTE.getLongOr(key, 0L));
                    newTE.putByte("IsPlaying", (byte) 0);
                }
                case "RecordItem" -> newTE.put("RecordItem", processRecordItem(oldTE.get(key), minecraftDataVersion, registryManager));
                case "Book" -> newTE.put("Book", processBookTag(oldTE.get(key), minecraftDataVersion, registryManager));
                // 1.21.5+
                //case "RecipesUsed" -> newTE.put("RecipesUsed", processRecipesUsedTag(oldTE));
                case "CustomName" -> newTE.putString("CustomName", processCustomNameTag(oldTE, key, registryManager));
                case "custom_name" -> newTE.putString("CustomName", processCustomNameTag(oldTE, key, registryManager));
                default -> newTE.put(key, oldTE.get(key));
            }
        }

        return newTE;
    }

    // 1.21.5+ Only ?  Might not even be needed
    private static CompoundTag processRecipesUsedTag(Tag nbtIn)
    {
        CompoundTag oldNbt = nbtIn.asCompound().orElse(new CompoundTag());
        CompoundTag newNbt = new CompoundTag();
        Codec<Map<ResourceKey<Recipe<?>>, Integer>> CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();

        // todo -- make sure this even needed
        recipesUsed.putAll(oldNbt.read("RecipesUsed", CODEC).orElse(Map.of()));
        recipesUsed.forEach((id, count) ->
        {
            newNbt.putInt(id.identifier().toString(), count);
        });

        return newNbt;
    }

    private static ListTag processItemsTag(ListTag oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        ListTag newItems = new ListTag();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundTag itemEntry = oldItems.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            if (!itemEntry.contains("id"))
            {
                continue;
            }
            String idName = itemEntry.getStringOr("id", "");

            newEntry.putString("id", idName);
            if (itemEntry.contains("count"))
            {
                newEntry.putByte("Count", (byte) itemEntry.getIntOr("count", 1));
            }
            if (itemEntry.contains("Slot"))
            {
                newEntry.putByte("Slot", itemEntry.getByteOr("Slot", (byte) 1));
            }
            if (itemEntry.contains("components"))
            {
                newEntry.put("tag", processComponentsTag(itemEntry.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
            }
            else
            {
                if (needsDamageTag(idName))
                {
                    CompoundTag newTag = new CompoundTag();
                    newTag.putInt("Damage", 0);
                    newEntry.put("tag", newTag);
                }
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static ListTag processItemsTag_Nested(ListTag oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        ListTag newItems = new ListTag();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundTag itemEntry = oldItems.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            int slotNum = itemEntry.getIntOr("slot", 0);
            CompoundTag itemSlot = itemEntry.getCompoundOrEmpty("item");

            if (!itemSlot.contains("id"))
            {
                continue;
            }
            String idName = itemSlot.getStringOr("id", "");

            newEntry.putString("id", idName);
            if (itemSlot.contains("count"))
            {
                newEntry.putByte("Count", (byte) itemSlot.getIntOr("count", 1));
            }
            newEntry.putByte("Slot", (byte) slotNum);

            if (itemSlot.contains("components"))
            {
                newEntry.put("tag", processComponentsTag(itemSlot.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
            }
            else
            {
                if (needsDamageTag(idName))
                {
                    CompoundTag newTag = new CompoundTag();
                    newTag.putInt("Damage", 0);
                    newEntry.put("tag", newTag);
                }
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static CompoundTag processDecoratedPot_Nested(ListTag oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag itemEntry = oldItems.getCompoundOrEmpty(0);
        CompoundTag newEntry = new CompoundTag();

        int slotNum = itemEntry.getIntOr("slot", 1);
        CompoundTag itemSlot = itemEntry.getCompoundOrEmpty("item");

        if (!itemSlot.contains("id"))
        {
            return itemEntry;
        }
        String idName = itemSlot.getStringOr("id", "");
        newEntry.putString("id", idName);
        newEntry.putByte("Count", (byte) (itemSlot.contains("count") ? itemSlot.getInt("count") : 1));

        if (itemSlot.contains("components"))
        {
            newEntry.put("tag", processComponentsTag(itemSlot.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
        }
        else
        {
            if (needsDamageTag(idName))
            {
                CompoundTag newTag = new CompoundTag();
                newTag.putInt("Damage", 0);
                newEntry.put("tag", newTag);
            }
        }

        return newEntry;
    }

    private static boolean needsDamageTag(String id)
    {
        ItemStack stack = InventoryUtils.getItemStackFromString(id);

        return stack != null && !stack.isEmpty() && stack.isDamageableItem();
    }

    private static CompoundTag processComponentsTag(CompoundTag nbt, String itemId, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag outNbt = new CompoundTag();
        CompoundTag beNbt = new CompoundTag();
        CompoundTag dispNbt = new CompoundTag();
        boolean needsDamage = needsDamageTag(itemId);

        for (String key : nbt.keySet())
        {
            switch (key)
            {
                case "minecraft:attribute_modifiers" -> outNbt.put("AttributeModifiers", processAttributes(nbt.get(key), minecraftDataVersion, registryManager));
                case "minecraft:banner_patterns" ->
                {
                    beNbt.put("Patterns", processBannerPatterns(nbt.get(key)));
                    beNbt.putString("id", "minecraft:banner");
                }
                case "minecraft:bees" ->
                {
                    beNbt.put("Bees", processBeesTag(nbt.get(key), minecraftDataVersion, registryManager));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:block_state" -> outNbt.put("BlockStateTag", processBlockState(nbt.get(key)));
                case "minecraft:block_entity_data" -> processBlockEntityData(nbt.get(key), beNbt, minecraftDataVersion, registryManager);       // TODO --> check that this works or not
                case "minecraft:bucket_entity_data" -> processBucketEntityData(nbt.get(key), beNbt, minecraftDataVersion, registryManager);
                case "minecraft:bundle_contents" -> outNbt.put("Items", processItemsTag(nbt.getListOrEmpty(key), minecraftDataVersion, registryManager));
                case "minecraft:can_break" -> outNbt.put("CanDestroy", nbt.get(key));
                case "minecraft:can_place_on" -> outNbt.put("CanPlaceOn", nbt.get(key));
                case "minecraft:container" ->
                {
                    if (itemId.contains("decorated_pot"))
                    {
                        beNbt.put("item", processDecoratedPot_Nested(nbt.getListOrEmpty(key), minecraftDataVersion, registryManager));
                    }
                    else
                    {
                        beNbt.put("Items", processItemsTag_Nested(nbt.getListOrEmpty(key), minecraftDataVersion, registryManager));
                    }
                    if (itemId.contains("shulker"))
                    {
                        beNbt.putString("id", "minecraft:shulker_box");
                    }
                    else
                    {
                        beNbt.putString("id", itemId);
                    }
                }
                case "minecraft:charged_projectiles" ->
                {
                    outNbt.put("ChargedProjectiles", processChargedProjectile(nbt.get(key), minecraftDataVersion, registryManager));
                    outNbt.putBoolean("Charged", true);
                }
                case "minecraft:container_loot" ->
                {
                    beNbt.put("LootTable", processLootTable(nbt.get(key)));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:custom_data" -> processCustomData(nbt.get(key), outNbt);
                case "minecraft:custom_model_data" -> outNbt.putInt("CustomModelData", nbt.getIntOr(key, 0));
                case "minecraft:custom_name" -> dispNbt.putString("Name", processCustomNameTag(nbt, key, registryManager));
                case "minecraft:damage" -> outNbt.putInt("Damage", nbt.getIntOr(key, 0));
                case "minecraft:debug_stick_state" -> outNbt.put("DebugProperty", nbt.get(key));
                case "minecraft:dyed_color" -> dispNbt.putInt("color", processDyedColor(nbt.get(key)));
                case "minecraft:enchantments" -> outNbt.put("Enchantments", processEnchantments(nbt.get(key), true, true));
                case "minecraft:entity_data" -> outNbt.put("EntityTag", downgradeEntity_to_1_20_4((CompoundTag) nbt.get(key), minecraftDataVersion, registryManager));
                case "minecraft:stored_enchantments" -> outNbt.put("StoredEnchantments", processEnchantments(nbt.get(key), true, true));
                case "minecraft:fireworks" -> outNbt.put("Fireworks", processFireworks(nbt.get(key)));
                case "minecraft:firework_explosion" -> outNbt.put("Explosion", processFireworkExplosion(nbt.get(key)));
                // "minecraft:hide_additional_tooltip" --> ignore
                case "minecraft:instrument" -> outNbt.put("instrument", processInstrument(nbt.get(key)));
                case "minecraft:item_name" -> dispNbt.putString("Name", processItemName(nbt.get(key), registryManager));
                case "minecraft:lock" ->
                {
                    beNbt.put("Lock", nbt.get(key));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:lodestone_tracker" -> processLodestoneTracker(nbt.get(key), outNbt);
                case "minecraft:lore" -> dispNbt.put("Lore", nbt.get(key));
                case "minecraft:map_id" -> outNbt.put("map", processMapId(nbt.get(key)));
                case "minecraft:map_color" -> dispNbt.put("MapColor", nbt.get(key));
                case "minecraft:map_decorations" -> outNbt.put("Decorations", processMapDecorations(nbt.get(key)));
                case "minecraft:note_block_sound" -> beNbt.put("note_block_sound", nbt.get(key));
                case "minecraft:pot_decorations" ->
                {
                    beNbt.put("sherds", processSherds(nbt.get(key)));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:potion_contents" -> processPotions(nbt.get(key), outNbt);
                case "minecraft:profile" -> outNbt.put("SkullOwner", processSkullProfile(nbt.get(key), dispNbt, minecraftDataVersion, registryManager));
                case "minecraft:repair_cost" -> outNbt.putInt("RepairCost", nbt.getIntOr(key, 0));
                case "minecraft:recipes" -> outNbt.put("Recipes", processRecipes(nbt.get(key)));
                case "minecraft:suspicious_stew_effects" -> outNbt.put("effects", processSuspiciousStewEffects(nbt.get(key)));
                case "minecraft:trim" -> outNbt.put("Trim", processTrim(nbt.get(key)));
                case "minecraft:writable_book_content" ->
                {
                    CompoundTag bookNbt = nbt.getCompoundOrEmpty(key);
                    bookNbt = processWritableBookContent(bookNbt, minecraftDataVersion, registryManager);
                    for (String bookKey : bookNbt.keySet())
                    {
                        outNbt.put(bookKey, bookNbt.get(bookKey));
                    }
                }
                case "minecraft:written_book_content" ->
                {
                    CompoundTag bookNbt = nbt.getCompoundOrEmpty(key);
                    bookNbt = processWrittenBookContent(bookNbt, minecraftDataVersion, registryManager);
                    for (String bookKey : bookNbt.keySet())
                    {
                        outNbt.put(bookKey, bookNbt.get(bookKey));
                    }
                }
                case "minecraft:unbreakable" -> outNbt.putBoolean("Unbreakable", processUnbreakable(nbt.get(key)));
            }
        }
        if (!beNbt.isEmpty())
        {
            outNbt.put("BlockEntityTag", beNbt);
        }
        if (!dispNbt.isEmpty())
        {
            outNbt.put("display", dispNbt);
        }
        if (!outNbt.contains("RepairCost") && (itemId.equals("minecraft:dragon_head") || needsDamage))
        {
            outNbt.putInt("RepairCost", 0);
        }
        if (!outNbt.contains("Damage") && needsDamage)
        {
            outNbt.putInt("Damage", 0);
        }

        return outNbt;
    }

    private static void processCustomData(Tag oldNbt, CompoundTag outNbt)
    {
        CompoundTag origData = oldNbt.asCompound().orElse(new CompoundTag());;

        for (String keyData : origData.keySet())
        {
            outNbt.put(keyData, origData.get(keyData));
        }
    }

    private static void processLodestoneTracker(Tag oldEle, CompoundTag outNbt)
    {
        CompoundTag oldNbt = oldEle.asCompound().orElse(new CompoundTag());;

        if (oldNbt.contains("tracked"))
        {
            outNbt.putBoolean("LodestoneTracked", oldNbt.getBooleanOr("tracked", false));
        }
        if (oldNbt.contains("target"))
        {
            CompoundTag target = oldNbt.getCompoundOrEmpty("target");

            outNbt.put("LodestoneDimension", target.get("dimension"));
            outNbt.put("LodestonePos", target.get("pos"));
        }
    }

    private static void processBucketEntityData(Tag oldTags, CompoundTag beNbt, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag oldNbt = oldTags.asCompound().orElse(new CompoundTag());;

//        NbtCompound newNbt = downgradeEntity_to_1_20_4(oldNbt, minecraftDataVersion, registryManager);
//        beNbt.copyFrom(newNbt);

        for (String key : oldNbt.keySet())
        {
            beNbt.put(key, oldNbt.get(key));
        }
    }

    private static void processPotions(Tag oldPots, CompoundTag outNbt)
    {
        CompoundTag oldNbt = oldPots.asCompound().orElse(new CompoundTag());;

        if (oldNbt.contains("potion"))
        {
            outNbt.putString("Potion", oldNbt.getStringOr("potion", ""));
        }
        if (oldNbt.contains("custom_color"))
        {
            outNbt.put("CustomPotionColor", Objects.requireNonNull(oldNbt.get("custom_color")));
        }
        if (oldNbt.contains("custom_effects"))
        {
            outNbt.put("custom_potion_effects", Objects.requireNonNull(oldNbt.get("custom_effects")));
        }
    }

    private static Tag processMapDecorations(Tag oldDeco)
    {
        CompoundTag oldTag = oldDeco.asCompound().orElse(new CompoundTag());;
        ListTag newTags = new ListTag();

        for (String key : oldTag.keySet())
        {
            CompoundTag entryOld = oldTag.getCompoundOrEmpty(key);
            CompoundTag entryNew = new CompoundTag();

            entryNew.putString("id", key);
            entryNew.putDouble("x", entryOld.contains("x") ? entryOld.getDoubleOr("x", 0d) : 0.0);
            entryNew.putDouble("z", entryOld.contains("z") ? entryOld.getDoubleOr("z", 0d) : 0.0);
            entryNew.putDouble("rot", entryOld.contains("rotation") ? (double) entryOld.getFloatOr("rotation", 0f) : 0.0);
            entryNew.putByte("type", (byte) (entryOld.contains("type") ? convertMapDecoration(entryOld.getStringOr("type", "")) : 0));

            newTags.add(entryNew);
        }

        return newTags;
    }

    private static int convertMapDecoration(String type)
    {
        return switch (type)
        {
            case "minecraft:player" -> 0;
            case "minecraft:frame" -> 1;
            case "minecraft:red_marker" -> 2;
            case "minecraft:blue_marker" -> 3;
            case "minecraft:target_x" -> 4;
            case "minecraft:target_point" -> 5;
            case "minecraft:player_off_map" -> 6;
            case "minecraft:player_off_limits" -> 7;
            case "minecraft:mansion" -> 8;
            case "minecraft:monument" -> 9;
            case "minecraft:banner_white" -> 10;
            case "minecraft:banner_orange" -> 11;
            case "minecraft:banner_magenta" -> 12;
            case "minecraft:banner_light_blue" -> 13;
            case "minecraft:banner_yellow" -> 14;
            case "minecraft:banner_lime" -> 15;
            case "minecraft:banner_pink" -> 16;
            case "minecraft:banner_gray" -> 17;
            case "minecraft:banner_light_gray" -> 18;
            case "minecraft:banner_cyan" -> 19;
            case "minecraft:banner_purple" -> 20;
            case "minecraft:banner_blue" -> 21;
            case "minecraft:banner_brown" -> 22;
            case "minecraft:banner_green" -> 23;
            case "minecraft:banner_red" -> 24;
            case "minecraft:banner_black" -> 25;
            case "minecraft:red_x" -> 26;
            case "minecraft:village_desert" -> 27;
            case "minecraft:village_plains" -> 28;
            case "minecraft:village_savanna" -> 29;
            case "minecraft:village_snowy" -> 30;
            case "minecraft:village_taiga" -> 31;
            case "minecraft:jungle_temple" -> 32;
            case "minecraft:swamp_hut" -> 33;
            default -> 0;
        };
    }

    private static Tag processSherds(Tag oldSherds)
    {
        return oldSherds;
    }

    private static Tag processLootTable(Tag oldLoot)
    {
        CompoundTag oldTable = oldLoot.asCompound().orElse(new CompoundTag());;
        CompoundTag newTable = new CompoundTag();

        if (oldTable.contains("loot_table"))
        {
            CompoundTag loot = oldTable.getCompoundOrEmpty("loot_table");
            newTable.merge(loot);
        }
        if (oldTable.contains("seed"))
        {
            newTable.putLong("LootTableSeed", oldTable.getLongOr("seed", 0L));
        }

        return newTable;
    }

    private static String processItemName(Tag oldName, RegistryAccess registryManager)
    {
        if (oldName != null)
        {
            return oldName.toString();
        }

        return "minecraft:air";
    }

    private static int processDyedColor(Tag oldDye)
    {
        CompoundTag oldColor = oldDye.asCompound().orElse(new CompoundTag());;

        if (oldColor.contains("rgb"))
        {
            return oldColor.getIntOr("rgb", 10511680);
        }

        // Default
        return 10511680;
    }

    private static Tag processRecipes(Tag oldRecipes)
    {
        return oldRecipes;
    }

    private static Tag processInstrument(Tag oldGoat)
    {
        return oldGoat;
    }

    private static Tag processSuspiciousStewEffects(Tag oldEffects)
    {
        return oldEffects;
    }

    private static Tag processMapId(Tag oldMapId)
    {
        return oldMapId;
    }

    private static Tag processTrim(Tag oldTrim)
    {
        return oldTrim;
    }

    private static ListTag processChargedProjectile(Tag oldProjectiles, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        ListTag oldNbt = oldProjectiles.asList().orElse(new ListTag());
        ListTag newNbt = new ListTag();

        for (int i = 0; i < oldNbt.size(); i++)
        {
            CompoundTag itemEntry = oldNbt.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            if (!itemEntry.contains("id"))
            {
                continue;
            }
            String idName = itemEntry.getStringOr("id", "");
            newEntry.putString("id", idName);
            newEntry.putByte("Count", (byte) (itemEntry.contains("count") ? itemEntry.getInt("count") : 1));
            if (itemEntry.contains("components"))
            {
                newEntry.put("tag", processComponentsTag(itemEntry.getCompoundOrEmpty("components"), idName, minecraftDataVersion, registryManager));
            }

            newNbt.add(newEntry);
        }

        return newNbt;
    }

    private static boolean processUnbreakable(Tag oldNbt)
    {
        CompoundTag oldUnbr = oldNbt.asCompound().orElse(new CompoundTag());;

        if (oldUnbr.contains("show_in_tooltip"))
        {
            return oldUnbr.getBooleanOr("show_in_tooltip", false);
        }

        return false;
    }

    private static void processBlockEntityData(Tag oldBeData, CompoundTag beNbt, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag newData = downgradeBlockEntity_to_1_20_4((CompoundTag) oldBeData, minecraftDataVersion, registryManager);

        for (String key : newData.keySet())
        {
            beNbt.put(key, newData.get(key));
        }
    }

    private static Tag processDecoratedPot(Tag oldPot, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag oldNbt = oldPot.asCompound().orElse(new CompoundTag());;
        CompoundTag newNbt = new CompoundTag();

        for (String key : oldNbt.keySet())
        {
            switch (key)
            {
                case "id" -> newNbt.putString("id", oldNbt.getStringOr("id", ""));
                case "count" -> newNbt.putByte("Count", (byte) oldNbt.getIntOr("count", 1));
                case "components" -> newNbt.put("tag", processComponentsTag(oldNbt.getCompoundOrEmpty("components"), oldNbt.getStringOr("id", ""), minecraftDataVersion, registryManager));
            }
        }

        if (!newNbt.contains("tag") && oldNbt.contains("id") && needsDamageTag(oldNbt.getStringOr("id", "")))
        {
            CompoundTag newTag = new CompoundTag();
            newTag.putInt("Damage", 0);
            newNbt.put("tag", newTag);
        }

        return newNbt;
    }

    private static Tag processEnchantments(Tag oldNbt, boolean fullId, boolean shortInt)
    {
        CompoundTag oldEnchants = oldNbt.asCompound().orElse(new CompoundTag());;
        CompoundTag oldLevels = oldEnchants.getCompoundOrEmpty("levels");
        ListTag newEnchants = new ListTag();
        boolean showTooltip = false;

        if (oldEnchants.contains("show_in_tooltip"))
        {
            showTooltip = oldEnchants.getBooleanOr("show_in_tooltip", false);
            // todo - Has no function under 1.20.4
        }

        for (String key : oldLevels.keySet())
        {
            CompoundTag newEntry = new CompoundTag();
            Identifier id = Identifier.parse(key);
            if (shortInt)
            {
                newEntry.putShort("lvl", (short) oldLevels.getIntOr(key, 1));
            }
            else
            {
                newEntry.putInt("lvl", oldLevels.getIntOr(key, 1));
            }
            newEntry.putString("id", fullId ? id.toString() : id.getPath());
            newEnchants.add(newEntry);
        }

        return newEnchants;
    }

    private static String processCustomNameTag(CompoundTag nameTag, String key, @Nonnull RegistryAccess registry)
    {
        // Sometimes this is missing the 'text' designation ?

        /*
        String oldNameString = nameTag.getString(key);
        MutableText oldCustomName = Text.Serialization.fromJson(oldNameString, registryManager);

        //System.out.printf("processCustomNameTag(): oldName [%s], text: [%s], newString [%s]\n", oldNameString, oldCustomName.getString(), newCustomName);

         */

        MutableComponent oldName = (MutableComponent) NbtBlockUtils.getCustomNameFromNbt(nameTag, registry, key);
        return legacyTextDeserializer(oldName, registry);
    }

    private static String legacyTextDeserializer(MutableComponent oldText, @Nonnull RegistryAccess registry)
    {
        try
        {
            JsonElement element = ComponentSerialization.CODEC.encodeStart(registry.createSerializationContext(JsonOps.INSTANCE), oldText).getOrThrow();
            return new GsonBuilder().disableHtmlEscaping().create().toJson(element);
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("legacyTextDeserializer: Failed to convert MutableText to JSON; (falling back to just 'getString'); {}", err.getLocalizedMessage());
            return oldText.getString();
        }
    }

    private static @Nullable MutableComponent legacyTextSerializer(String json, @Nonnull RegistryAccess registry)
    {
        try
        {
            return (MutableComponent) ComponentSerialization.CODEC.parse(registry.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).getOrThrow();
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("legacyTextSerializer: Failed to convert JSON to MutableText; {}", err.getLocalizedMessage());
            return null;
        }
    }

    private static Tag processBlockState(Tag bsTag)
    {
        CompoundTag oldBS = bsTag.asCompound().orElse(new CompoundTag());;
        CompoundTag newBS = new CompoundTag();

        for (String key : oldBS.keySet())
        {
            newBS.put(key, oldBS.get(key));
        }

        return bsTag;
    }

    private static Tag processFireworks(Tag rocket)
    {
        CompoundTag oldRocket = rocket.asCompound().orElse(new CompoundTag());;
        CompoundTag newRocket = new CompoundTag();

        if (oldRocket.contains("flight_duration"))
        {
            newRocket.putByte("Flight", oldRocket.getByteOr("flight_duration", (byte) 1));
        }
        if (oldRocket.contains("explosions"))
        {
            ListTag oldExplosions = oldRocket.getListOrEmpty("explosions");
            ListTag newExplosions = new ListTag();

            for (int i = 0; i < oldExplosions.size(); i++)
            {
                newExplosions.add(processFireworkExplosion(oldExplosions.getCompoundOrEmpty(i)));
            }

            newRocket.put("Explosions", newExplosions);
        }

        return newRocket;
    }

    private static Tag processFireworkExplosion(Tag explosion)
    {
        CompoundTag oldExplosion = explosion.asCompound().orElse(new CompoundTag());;
        CompoundTag newExplosion = new CompoundTag();

        if (oldExplosion.contains("shape"))
        {
            newExplosion.putByte("Type", (byte) convertFireworkShape(oldExplosion.getStringOr("shape", "")));
        }
        if (oldExplosion.contains("colors"))
        {
            newExplosion.putIntArray("Colors", oldExplosion.getIntArray("colors").orElse(new int[0]));
        }
        if (oldExplosion.contains("fade_colors"))
        {
            newExplosion.putIntArray("FadeColors", oldExplosion.getIntArray("fade_colors").orElse(new int[0]));
        }
        if (oldExplosion.contains("has_trail"))
        {
            newExplosion.putBoolean("Trail", oldExplosion.getBooleanOr("has_trail", false));
        }
        if (oldExplosion.contains("has_twinkle"))
        {
            newExplosion.putBoolean("Flicker", oldExplosion.getBooleanOr("has_twinkle", false));
        }

        return newExplosion;
    }

    private static int convertFireworkShape(String shape)
    {
        return switch (shape)
        {
            case "small_ball" -> 0;
            case "large_ball" -> 1;
            case "star" -> 2;
            case "creeper" -> 3;
            case "burst" -> 4;
            default -> 0;
        };
    }

    private static Tag processRecordItem(Tag itemIn, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag oldRecord = itemIn.asCompound().orElse(new CompoundTag());;
        CompoundTag recordOut = new CompoundTag();

        recordOut.putString("id", oldRecord.getStringOr("id", ""));
        recordOut.putByte("Count", (byte) oldRecord.getIntOr("count", 1));

        if (oldRecord.contains("components"))
        {
            recordOut.put("tag", processComponentsTag(oldRecord.getCompoundOrEmpty("components"), oldRecord.getStringOr("id", ""), minecraftDataVersion, registryManager));
        }

        return recordOut;
    }

    private static Tag processBookTag(Tag bookNbt, int minecraftDataVersion, RegistryAccess registryManager)
    {
        CompoundTag oldBook = bookNbt.asCompound().orElse(new CompoundTag());;
        CompoundTag newBook = new CompoundTag();

        newBook.putString("id", oldBook.getStringOr("id", ""));
        newBook.putByte("Count", (byte) oldBook.getIntOr("count", 1));

        if (oldBook.contains("Page"))
        {
            newBook.putInt("Page", oldBook.getIntOr("Page", 1));
        }
        if (oldBook.contains("components"))
        {
            newBook.put("tag", processComponentsTag(oldBook.getCompoundOrEmpty("components"), oldBook.getStringOr("id", ""), minecraftDataVersion, registryManager));
        }

        return newBook;
    }

    private static CompoundTag processWritableBookContent(CompoundTag bookNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundTag newBook = new CompoundTag();
        ListTag newPages = new ListTag();

        if (bookNbt.contains("pages"))
        {
            ListTag pages = bookNbt.getListOrEmpty("pages");

            for (int i = 0; i < pages.size(); i++)
            {
                CompoundTag page = pages.getCompoundOrEmpty(i);
                String oldPage = page.getStringOr("raw", "");

                try
                {
                    MutableComponent oldText = legacyTextSerializer(oldPage, registry);
//                    MutableText oldText = Text.Serialization.fromJson(oldPage, registry);
//                    String newPage = Text.Serialization.toJsonString(oldText, registry);
                    String newPage = legacyTextDeserializer(oldText, registry);
                    newPages.add(i, StringTag.valueOf(newPage));
                }
                catch (Exception e)
                {
                    newPages.add(i, StringTag.valueOf(oldPage));
                }
            }
        }
        if (!newPages.isEmpty())
        {
            newBook.put("pages", newPages);
        }

        return newBook;
    }

    private static CompoundTag processWrittenBookContent(CompoundTag bookNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundTag newBook = new CompoundTag();
        CompoundTag filtered = new CompoundTag();
        ListTag newPages = new ListTag();

        if (bookNbt.contains("author"))
        {
            newBook.putString("author", bookNbt.getStringOr("author", "?"));
        }
        if (bookNbt.contains("title"))
        {
            CompoundTag title = bookNbt.getCompoundOrEmpty("title");
            newBook.putString("title", title.getStringOr("raw", ""));
        }
        if (bookNbt.contains("resolved"))
        {
            newBook.putBoolean("resolved", bookNbt.getBooleanOr("resolved", false));
        }
        if (bookNbt.contains("generation"))
        {
            newBook.putInt("generation", bookNbt.getIntOr("generation", 1));
        }

        if (bookNbt.contains("pages"))
        {
            ListTag pages = bookNbt.getListOrEmpty("pages");

            for (int i = 0; i < pages.size(); i++)
            {
                CompoundTag page = pages.getCompoundOrEmpty(i);
                String oldPage = page.getStringOr("raw", "");

                if (page.contains("filtered"))
                {
                    String filterPage = page.getStringOr("filtered", "");
                    try
                    {
                        MutableComponent filteredText = legacyTextSerializer(filterPage, registry);
//                        MutableText filteredText = Text.Serialization.fromJson(filterPage, registry);
//                        String newFilterPage = Text.Serialization.toJsonString(filteredText, registry);
                        String newFilterPage = legacyTextDeserializer(filteredText, registry);
                        filtered.putString(filterPage, newFilterPage);
                        // This seems like A terrible idea
                    }
                    catch (Exception e)
                    {
                        filtered.putString(filterPage, filterPage);
                    }
                }
                try
                {
                    MutableComponent oldText = legacyTextSerializer(oldPage, registry);
//                    MutableText oldText = Text.Serialization.fromJson(oldPage, registry);
//                    String newPage = Text.Serialization.toJsonString(oldText, registry);
                    String newPage = legacyTextDeserializer(oldText, registry);
                    newPages.add(i, StringTag.valueOf(newPage));
                }
                catch (Exception e)
                {
                    newPages.add(i, StringTag.valueOf(oldPage));
                }
            }
        }
        if (!newPages.isEmpty())
        {
            newBook.put("pages", newPages);
        }
        if (!filtered.isEmpty())
        {
            newBook.put("filtered_pages", filtered);
        }

        return newBook;
    }

    private static Tag processBannerPatterns(Tag oldPatterns)
    {
        ListTag oldList = oldPatterns.asList().orElse(new ListTag());
        ListTag newList = new ListTag();

        for (int i = 0; i < oldList.size(); i++)
        {
            CompoundTag oldEntry = oldList.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();
            String color = oldEntry.getStringOr("color", "");
            String pattern = oldEntry.getStringOr("pattern", "");
            DyeColor dye = DyeColor.byName(color, DyeColor.WHITE);

            newEntry.putString("Pattern", convertBannerPattern(pattern));
            newEntry.putInt("Color", dye.getId());

            newList.add(newEntry);
        }

        return newList;
    }

    private static String convertBannerPattern(String patternId)
    {
        return switch (patternId)
        {
            case "minecraft:base" -> "b";
            case "minecraft:square_bottom_left" -> "bl";
            case "minecraft:square_bottom_right" -> "br";
            case "minecraft:square_top_left" -> "tl";
            case "minecraft:square_top_right" -> "tr";
            case "minecraft:stripe_bottom" -> "bs";
            case "minecraft:stripe_top" -> "ts";
            case "minecraft:stripe_left" -> "ls";
            case "minecraft:stripe_right" -> "rs";
            case "minecraft:stripe_center" -> "cs";
            case "minecraft:stripe_middle" -> "ms";
            case "minecraft:stripe_downright" -> "drs";
            case "minecraft:stripe_downleft" -> "dls";
            case "minecraft:small_stripes" -> "ss";
            case "minecraft:cross" -> "cr";
            case "minecraft:straight_cross" -> "sc";
            case "minecraft:triangle_bottom" -> "bt";
            case "minecraft:triangle_top" -> "tt";
            case "minecraft:triangles_bottom" -> "bts";
            case "minecraft:triangles_top" -> "tts";
            case "minecraft:diagonal_left" -> "ld";
            case "minecraft:diagonal_up_right" -> "rd";
            case "minecraft:diagonal_up_left" -> "lud";
            case "minecraft:diagonal_right" -> "rud";
            case "minecraft:circle" -> "mc";
            case "minecraft:rhombus" -> "mr";
            case "minecraft:half_vertical" -> "vh";
            case "minecraft:half_horizontal" -> "hh";
            case "minecraft:half_vertical_right" -> "vhr";
            case "minecraft:half_horizontal_bottom" -> "hhb";
            case "minecraft:border" -> "bo";
            case "minecraft:curly_border" -> "cbo";
            case "minecraft:gradient" -> "gra";
            case "minecraft:gradient_up" -> "gru";
            case "minecraft:bricks" -> "bri";
            case "minecraft:globe" -> "glb";
            case "minecraft:creeper" -> "cre";
            case "minecraft:skull" -> "sku";
            case "minecraft:flower" -> "flo";
            case "minecraft:mojang" -> "moj";
            case "minecraft:piglin" -> "pig";
            // Doesn't exist in 1.20.4
            //case "minecraft:flow" -> "flo";
            //case "minecraft:guster" -> "gus";
            default -> "b";
        };
    }

    private static Tag processSkullProfile(Tag oldProfile, CompoundTag dispNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundTag profile = oldProfile.asCompound().orElse(new CompoundTag());
        CompoundTag newProfile = new CompoundTag();
        String customName1 = dispNbt.getStringOr("Name", "");         // Can be either an Item Name or Custom Name Data Component
        String customName2 = dispNbt.getStringOr("CustomName", "");   // Only if invoked without it being stored in a Chest
        String name = profile.getStringOr("name", "");                // The regular Skull Owner Name
        //UUID uuid = profile.getUuid("id");
        UUID uuid = profile.read("id", UUIDUtil.AUTHLIB_CODEC, registry.createSerializationContext(NbtOps.INSTANCE)).orElse(Util.NIL_UUID);

//        LOGGER.debug("processSkullProfile(): oldNBT [{}]", profile.toString());
        if (name.isEmpty() && !customName1.isEmpty())
        {
            try
            {
//                Text disp = Text.Serialization.fromJson(customName1, registry);
                MutableComponent disp = legacyTextSerializer(customName1, registry);

                if (disp != null)
                {
                    name = disp.tryCollapseToString();
                }

                if (name == null)
                {
                    name = customName1;
                }

//                LOGGER.debug("processSkullProfile(): customName1 [{}], disp [{}] // name [{}]", customName1, disp != null ? disp.getString() : "<null>", name);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("processSkullProfile(): Exception deserializing CustomName1 for Head Name.");
                name = customName1;
            }
        }
        else if (name.isEmpty() && !customName2.isEmpty())
        {
            try
            {
//                Text disp = Text.Serialization.fromJson(customName2, registry);
                MutableComponent disp = legacyTextSerializer(customName2, registry);

                if (disp != null)
                {
                    name = disp.tryCollapseToString();
                }

                if (name == null)
                {
                    name = customName2;
                }

//                LOGGER.debug("processSkullProfile(): customName2 [{}], disp[{}] // name [{}]", customName2, disp != null ? disp.getString() : "<null>", name);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("processSkullProfile(): Exception deserializing CustomName2 for Head Name.");
                name = customName2;
            }
        }

        newProfile.putString("Name", name);
        //newProfile.putUuid("Id", uuid);
        newProfile.store("Id", UUIDUtil.CODEC, uuid);

//        LOGGER.debug("processSkullProfile(): name [{}], uuid [{}]", name, uuid.toString());

        ListTag properties = profile.getListOrEmpty("properties");
        CompoundTag newProperties = new CompoundTag();

        for (int i = 0; i < properties.size(); i++)
        {
            CompoundTag property = properties.getCompoundOrEmpty(i);
            String propName = property.getStringOr("name", "");
            String propValue = property.getStringOr("value", "");

//            LOGGER.debug("processSkullProfile(): entry[{}], name [{}]", i, propName);

            if (propName.equals("textures"))
            {
                ListTag textures = new ListTag();
                CompoundTag value = new CompoundTag();
                value.putString("Value", propValue);
                textures.add(value);
                newProperties.put("textures", textures);
            }
        }

        newProfile.put("Properties", newProperties);
//        LOGGER.debug("processSkullProfile(): newNBT [{}]", newProfile.toString());

        return newProfile;
    }

    private static Tag processFlowerPos(CompoundTag oldNbt, String key, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        CompoundTag flowerOut = new CompoundTag();
        //BlockPos flowerPos = NbtHelper.toBlockPos(oldNbt, key).orElse(null);
        BlockPos flowerPos = oldNbt.read(key, BlockPos.CODEC, registryManager.createSerializationContext(NbtOps.INSTANCE)).orElse(null);

        if (flowerPos != null)
        {
            flowerOut.putInt("X", flowerPos.getX());
            flowerOut.putInt("Y", flowerPos.getY());
            flowerOut.putInt("Z", flowerPos.getZ());
        }

        return flowerOut;
    }

    private static Tag processBeesTag(Tag beesTag, int minecraftDataVersion, @Nonnull RegistryAccess registryManager)
    {
        ListTag oldBees = beesTag.asList().orElse(new ListTag());
        ListTag newBees = new ListTag();

        for (int i = 0; i < oldBees.size(); i++)
        {
            CompoundTag oldEntry = oldBees.getCompoundOrEmpty(i);
            CompoundTag newEntry = new CompoundTag();

            newEntry.putInt("TicksInHive", oldEntry.getIntOr("ticks_in_hive", 0));
            newEntry.putInt("MinOccupationTicks", oldEntry.getIntOr("min_ticks_in_hive", 0));
            newEntry.put("EntityData", downgradeEntity_to_1_20_4(oldEntry.getCompoundOrEmpty("entity_data"), minecraftDataVersion, registryManager));

            newBees.add(newEntry);
        }

        return newBees;
    }
}
