package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import com.mojang.datafixers.DataFixer;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.enums.Attachment;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.MessageOutputType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.SubChunkPos;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class WorldUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static long easyPlaceNextSwap = 0;
    private static final HashMap<Block, Boolean> HAS_USE_ACTION_CACHE = new HashMap<>();
    private static boolean isHandlingEasyPlace;
    private static boolean isFirstClickEasyPlace;
    private static boolean isFirstClickPlacementRestriction;
    private static boolean easyPlaceShowFailMessage;
    private static final Property<?>[] CHECKED_PROPERTIES = new Property<?>[] {
            Properties.NOTE,
            Properties.DELAY
    };

    public static boolean shouldPreventBlockUpdates(World world)
    {
        return ((IWorldUpdateSuppressor) world).litematica_getShouldPreventBlockUpdates();
    }

    public static void setShouldPreventBlockUpdates(World world, boolean preventUpdates)
    {
        ((IWorldUpdateSuppressor) world).litematica_setShouldPreventBlockUpdates(preventUpdates);
    }

    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(File inputDir, String inputFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(new File(inputDir, inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        loadChunksSchematicWorld(world, BlockPos.ORIGIN, schematic.getSize());
        StructurePlacementData placementSettings = new StructurePlacementData();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, (new BlockPos(schematic.getSize())).add(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, info, "?", feedback);

        if (litematicaSchematic != null && ignoreEntities == false)
        {
            litematicaSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        return litematicaSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(File structureDir, String structureFileName,
            File outputDir, String outputFileName, boolean override)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertSpongeSchematicToLitematicaSchematic(File dir, String fileName)
    {
        try
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, fileName, FileType.SPONGE_SCHEMATIC);

            if (schematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the Sponge schematic from '" + fileName + '"');
            }

            return schematic;
        }
        catch (Exception e)
        {
            String msg = "Exception while trying to load the Sponge schematic: " + e.getMessage();
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
            Litematica.logger.error(msg);
        }

        return null;
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(File structureDir, String structureFileName)
    {
        try
        {
            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(structureDir, structureFileName, FileType.VANILLA_STRUCTURE);

            if (litematicaSchematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the vanilla structure template from '" + structureFileName + '"');
            }

            return litematicaSchematic;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Exception while trying to load the vanilla structure: " + e.getMessage());
            Litematica.logger.error("Exception while trying to load the vanilla structure: " + e.getMessage());
        }

        return null;
    }

    public static boolean convertLitematicaSchematicToSchematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        //SchematicaSchematic schematic = convertLitematicaSchematicToSchematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        //return schematic != null && schematic.writeToFile(outputDir, outputFileName, override, feedback);
        // TODO 1.13
        return false;
    }

    public static boolean convertLitematicaSchematicToVanillaStructure(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        StructureTemplate template = convertLitematicaSchematicToVanillaStructure(inputDir, inputFileName, ignoreEntities, feedback);
        return writeVanillaStructureToFile(template, outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static StructureTemplate convertLitematicaSchematicToVanillaStructure(File inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ORIGIN);
        litematicaSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        StructureTemplate template = new StructureTemplate();
        template.saveFromWorld(world, BlockPos.ORIGIN, size, ignoreEntities == false, Blocks.STRUCTURE_VOID);

        return template;
    }

    private static boolean writeVanillaStructureToFile(StructureTemplate template, File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;
        String extension = ".nbt";

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        File file = new File(dir, fileName);
        FileOutputStream os = null;

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && file.exists())
            {
                feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exists", file.getAbsolutePath()));
                return false;
            }

            NbtCompound tag = template.writeNbt(new NbtCompound());
            os = new FileOutputStream(file);
            NbtIo.writeCompressed(tag, os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exception", file.getAbsolutePath()));
        }

        return false;
    }

    private static StructureTemplate readTemplateFromStream(InputStream stream, DataFixer fixer) throws IOException
    {
        NbtCompound nbt = NbtIo.readCompressed(stream);
        StructureTemplate template = new StructureTemplate();
        //template.read(fixer.process(FixTypes.STRUCTURE, nbt));
        template.readNbt(nbt);

        return template;
    }

    public static boolean isClientChunkLoaded(ClientWorld world, int chunkX, int chunkZ)
    {
        return ((ClientChunkManager) world.getChunkManager()).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    public static void loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, MinecraftClient mc)
    {
        BlockState state = Blocks.AIR.getDefaultState();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 6);

        if (wrapper != null)
        {
            BlockHitResult trace = wrapper.getBlockHitResult();

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA_BLOCK)
                {
                    state = mc.world.getBlockState(pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param closest
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, MinecraftClient mc)
    {
        BlockPos pos;

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, mc.player, 6);
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldBlockBeforeVanilla(mc.world, mc.player, 6, true);
        }

        if (pos != null)
        {
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);

            InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);

            return true;
        }

        return false;
    }

    public static void easyPlaceOnUseTick(MinecraftClient mc)
    {
        if (mc.player != null && isHandlingEasyPlace == false &&
            shouldDoEasyPlaceActions() &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
        {
            isHandlingEasyPlace = true;
            WorldUtils.doEasyPlaceAction(mc);
            isHandlingEasyPlace = false;
        }
    }

    private static ActionResult doEasyPlaceAction(MinecraftClient mc)
    {
        RayTraceWrapper traceWrapper;
        double traceMaxRange = mc.interactionManager.getReachDistance();

        final boolean ignoreEnderChest = Configs.Generic.EASY_PLACE_IGNORE_ENDER_CHEST.getBooleanValue();
        final boolean ignoreShulkerBox = Configs.Generic.EASY_PLACE_IGNORE_SHULKER_BOX.getBooleanValue();
        if (ignoreEnderChest || ignoreShulkerBox)
        {
            // Allow opening ender chest / shulker box
            if (mc.player.isSneaking() == false && mc.crosshairTarget instanceof BlockHitResult blockHitResult)
            {
                Block blockClient = mc.world.getBlockState(blockHitResult.getBlockPos()).getBlock();
                if ((ignoreEnderChest && (blockClient instanceof EnderChestBlock)) ||
                        (ignoreShulkerBox && (blockClient instanceof ShulkerBoxBlock)))
                {
                    return ActionResult.PASS;
                }
            }
        }


        if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue())
        {
            // Temporary hack, using this same config here
            boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
            traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, traceMaxRange, true, targetFluids, false);
        }
        else
        {
            traceWrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.world, mc.player, traceMaxRange);
        }

        if (traceWrapper == null)
        {
            return ActionResult.PASS;
        }

        if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, traceMaxRange);
            BlockPos pos = trace.getBlockPos();
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // Already placed to that position, possible server sync delay
            if (easyPlaceIsPositionCached(pos))
            {
                return ActionResult.FAIL;
            }

            if (stack.isEmpty() == false)
            {
                boolean mayPlace = false;
                
                if (ignoreEnderChest || ignoreShulkerBox)
                {
                    final Block blockInHand = Block.getBlockFromItem(mc.player.getStackInHand(Hand.MAIN_HAND).getItem());
                    if ((ignoreEnderChest && (blockInHand instanceof EnderChestBlock)) ||
                            (ignoreShulkerBox && (blockInHand instanceof ShulkerBoxBlock)))
                    {
                        mayPlace = true;
                    }
                }

                BlockState stateClient = mc.world.getBlockState(pos);

                if (stateSchematic == stateClient)
                {
                    return mayPlace ? ActionResult.PASS : ActionResult.FAIL;
                }

                final boolean hasUseAction = hasUseAction(stateSchematic.getBlock());

                // Abort if there is already a block in the target position
                ActionResult actionResult = easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, traceVanilla, stack, hasUseAction);
                if (actionResult == ActionResult.FAIL)
                {
                    return mayPlace ? ActionResult.PASS : ActionResult.FAIL;
                }
                else if (actionResult == ActionResult.SUCCESS)
                {
                    cacheEasyPlacePosition(pos, hasUseAction);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, trace);
                    easyPlaceShowFailMessage = false;
                    return ActionResult.FAIL;
                }

                final var changed = InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
                final var swapInterval = Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue();
                if (changed && swapInterval > 0 && EntityUtils.isCreativeMode(mc.player) == false)
                {
                    final var now = System.nanoTime();
                    final var beforeSwap = easyPlaceNextSwap;
                    easyPlaceNextSwap = now + swapInterval * 1_000_000L;
                    if (now < beforeSwap)
                    {
                        //An inventory packet was already sent, so it's better not to emit too many packets
                        //This could cause wrong blocks
                        return ActionResult.FAIL;
                    }
                }

                Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return mayPlace ? ActionResult.PASS : ActionResult.FAIL;
                }

                // Check if it's the last remaining item
                if (Configs.Generic.EASY_PLACE_LEAVE_ONE.getBooleanValue() && EntityUtils.isCreativeMode(mc.player) == false)
                {
                    final var handStack = mc.player.getStackInHand(hand);
                    if (handStack.getMaxCount() > 1 && handStack.getCount() == 1)
                    {
                        InfoUtils.printActionbarMessage(GuiBase.TXT_RED + "Last item of " + GuiBase.TXT_RST + handStack.getName().getString());
                        return ActionResult.FAIL;
                    }
                }

                Vec3d hitPos = trace.getPos();
                Direction sideOrig = trace.getSide();

                if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                {
                    Litematica.logger.info("sideTrace: " + sideOrig + " hitPosTrace: " + hitPos);
                }

                // If there is a block in the world right behind the targeted schematic block, then use
                // that block as the click position
                HitResult.Type type = traceVanilla.getType();
                if (type == HitResult.Type.BLOCK || type == HitResult.Type.MISS)
                {
                    BlockHitResult hitResult = (BlockHitResult) traceVanilla;
                    BlockPos posVanilla = hitResult.getBlockPos();
                    Direction sideVanilla = hitResult.getSide();
                    BlockState stateVanilla = mc.world.getBlockState(posVanilla);
                    Vec3d hit = traceVanilla.getPos();
                    ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, hand, hitResult));

                    if (stateVanilla.canReplace(ctx) == false)
                    {
                        posVanilla = posVanilla.offset(sideVanilla);

                        if (pos.equals(posVanilla))
                        {
                            hitPos = hit;
                            sideOrig = sideVanilla;
                        }
                    }
                }

                if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                {
                    Litematica.logger.info("sideIn: " + sideOrig + " hitPosIn: " + hitPos);
                }

                EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();

                BlockPos posOut = pos;
                Direction sideOut = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                if (protocol == EasyPlaceProtocol.V3)
                {
                    hitPos = applyPlacementProtocolV3(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.V2)
                {
                    // Carpet Accurate Block Placement protocol support, plus slab support
                    hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // Slab support only
                    hitPos = applyBlockSlabProtocol(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.RESTRICTED)
                {
                    //Use vanilla / Paper restrictions
                    hitPos = applyBlockSlabProtocol(pos, stateSchematic, hitPos);
                    if (stateSchematic.contains(Properties.SLAB_TYPE) == false)
                    {
                        var changedHitResult = applyRestrictedProtocol(pos, stateSchematic, sideOut, hitPos, mc, hand);
                        if (changedHitResult == null)
                        {
                            if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                            {
                                Litematica.logger.info("Can't orientate");
                            }
                            return ActionResult.FAIL;
                        }

                        posOut = changedHitResult.getLeft();
                        sideOut = changedHitResult.getMiddle();
                        hitPos = changedHitResult.getRight();
                    }
                }

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos, hasUseAction);

                BlockHitResult hitResult = new BlockHitResult(hitPos, sideOut, posOut, false);

                if (Configs.Generic.DEBUG_LOGGING.getBooleanValue())
                {
                    Litematica.logger.info("sideOut: " + sideOut + " hitPosOut: " + hitPos + " posOut: " + posOut);
                }
                mc.interactionManager.interactBlock(mc.player, hand, hitResult);

                if (stateSchematic.getBlock() instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
                {
                    stateClient = mc.world.getBlockState(pos);

                    if (stateClient.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
                    {
                        sideOut = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        hitResult = new BlockHitResult(hitPos, sideOut, pos, false);
                        mc.interactionManager.interactBlock(mc.player, hand, hitResult);
                    }
                }
            }

            return ActionResult.SUCCESS;
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            if (ignoreEnderChest || ignoreShulkerBox)
            {
                final Block blockInHand = Block.getBlockFromItem(mc.player.getStackInHand(Hand.MAIN_HAND).getItem());
                if ((ignoreEnderChest && (blockInHand instanceof EnderChestBlock)) ||
                        (ignoreShulkerBox && (blockInHand instanceof ShulkerBoxBlock)))
                {
                    return ActionResult.PASS;
                }
            }
            return placementRestrictionInEffect(mc) ? ActionResult.FAIL : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    private static ActionResult easyPlaceBlockChecksCancel(BlockState stateSchematic, BlockState stateClient,
            PlayerEntity player, HitResult trace, ItemStack stack, boolean hasUseAction)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                return blockSchematic == blockClient ? ActionResult.PASS : ActionResult.FAIL;
            }
        }
        else if (player.isSneaking() == false)
        {
            boolean checkedProperty = false;

            for (Property<?> property : CHECKED_PROPERTIES)
            {
                if (stateSchematic.contains(property) && stateClient.contains(property))
                {
                    if (stateSchematic.get(property) == stateClient.get(property))
                        return ActionResult.FAIL;
                    checkedProperty = true;
                }
            }

            if (checkedProperty)
                return ActionResult.SUCCESS;
        }

        HitResult.Type type = trace.getType();
        if (type != HitResult.Type.BLOCK && type != HitResult.Type.MISS)
        {
            return ActionResult.PASS;
        }

        BlockHitResult hitResult = (BlockHitResult) trace;
        ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(player, Hand.MAIN_HAND, hitResult));

        if (stateClient.canReplace(ctx) == false)
        {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    /**
     * Apply the Carpet-Extra mod accurate block placement protocol support
     */
    public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
        final int propertyIncrement = 16;
        boolean hasData = false;
        int protocolValue = 0;

        if (facing != null)
        {
            protocolValue = facing.getId();
            hasData = true; // without this down rotation would not be detected >_>
        }
        else if (state.contains(Properties.AXIS))
        {
            Direction.Axis axis = state.get(Properties.AXIS);
            protocolValue = axis.ordinal();
            hasData = true; // without this id 0 would not be detected >_>
        }

        if (block instanceof RepeaterBlock)
        {
            protocolValue += state.get(RepeaterBlock.DELAY) * propertyIncrement;
        }
        else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.contains(Properties.BLOCK_HALF) && state.get(Properties.BLOCK_HALF) == BlockHalf.TOP)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.contains(Properties.SLAB_TYPE) && state.get(Properties.SLAB_TYPE) == SlabType.TOP)
        {
            protocolValue += propertyIncrement;
        }

        y = applySlabOrStairHitVecY(y, pos, state);

        if (protocolValue != 0 || hasData)
        {
            x += (protocolValue * 2) + 2;
        }

        return new Vec3d(x, y, z);
    }

    private static double applySlabOrStairHitVecY(double origY, BlockPos pos, BlockState state)
    {
        double y = origY;

        if (state.contains(Properties.SLAB_TYPE))
        {
            y = pos.getY();

            if (state.get(Properties.SLAB_TYPE) == SlabType.TOP)
            {
                y += 0.99;
            }
        }
        else if (state.contains(Properties.BLOCK_HALF))
        {
            y = pos.getY();

            if (state.get(Properties.BLOCK_HALF) == BlockHalf.TOP)
            {
                y += 0.99;
            }
        }

        return y;
    }

    private static Vec3d applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        double newY = applySlabOrStairHitVecY(hitVecIn.y, pos, state);
        return newY != hitVecIn.y ? new Vec3d(hitVecIn.x, newY, hitVecIn.z) : hitVecIn;
    }

    private static boolean isMatchingStateRestrictedProtocol (BlockState state1, BlockState state2)
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
                Properties.FACING, //pistons
                Properties.BLOCK_HALF, //stairs, trapdoors
                Properties.HOPPER_FACING,
                Properties.DOOR_HINGE,
                Properties.HORIZONTAL_FACING, //small dripleaf
                Properties.AXIS, //logs
                Properties.SLAB_TYPE,
                Properties.VERTICAL_DIRECTION,
                Properties.ROTATION, //banners
                Properties.HANGING, //lanterns
                Properties.WALL_MOUNT_LOCATION, //lever
                Properties.ATTACHMENT, //bell (double-check for single-wall / double-wall)
                //Properties.HORIZONTAL_AXIS, //Nether portals, though they aren't directly placeable
                //Properties.ORIENTATION, //jigsaw blocks
        };

        for (var property : orientationProperties)
        {
            boolean hasProperty1 = state1.contains(property);
            boolean hasProperty2 = state2.contains(property);

            if (hasProperty1 != hasProperty2)
                return false;
            if (!hasProperty1)
                continue;

            if (state1.get(property) != state2.get(property))
                return false;
        }

        //Other properties are considered as matching
        return true;
    }

    private static boolean isMatchingStateRestrictedProtocol(BlockPos pos, BlockState stateSchematic, Direction direction, Vec3d hitVecIn, MinecraftClient mc, Hand hand)
    {
        final var updatedHitResult = new BlockHitResult(hitVecIn, direction, pos, false);
        final var ctx = new ItemPlacementContext(mc.player, hand, mc.player.getStackInHand(hand), updatedHitResult);
        final var attemptState = stateSchematic.getBlock().getPlacementState(ctx);
        return isMatchingStateRestrictedProtocol(attemptState, stateSchematic);
    }

    private static Triple<BlockPos, Direction, Vec3d> applyRestrictedProtocol(BlockPos pos, BlockState stateSchematic, Direction sideIn, Vec3d hitVecIn, MinecraftClient mc, Hand hand)
    {
        var block = stateSchematic.getBlock();
        if (block instanceof TorchBlock) //Torch, Soul Torch, Redstone Torch
        {
            boolean isOnWall = block instanceof WallTorchBlock || block instanceof WallRedstoneTorchBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof AbstractBannerBlock)
        {
            boolean isOnWall = block instanceof WallBannerBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof AbstractSignBlock)
        {
            boolean isOnWall = block instanceof WallSignBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }
        else if (block instanceof AbstractSkullBlock) //Wither Skull, Player Skull
        {
            boolean isOnWall = block instanceof WallSkullBlock;
            return getWallPlaceableOrientation(pos, stateSchematic, hitVecIn, mc, hand, isOnWall);
        }

        return Direction.stream()
                .filter(direction -> isMatchingStateRestrictedProtocol(pos, stateSchematic, direction, hitVecIn, mc, hand))
                .findAny()
                .map(direction -> Triple.of(pos, direction, hitVecIn))
                .orElse(null);
    }

    private static Triple<BlockPos, Direction, Vec3d> getWallPlaceableOrientation(BlockPos pos, BlockState stateSchematic, Vec3d hitVecOut, MinecraftClient mc, Hand hand, boolean isOnWall) {
        Direction sideOut;
        BlockPos posOrig = pos;

        if (isOnWall)
        {
            if (!stateSchematic.contains(Properties.HORIZONTAL_FACING))
            {
                //Shouldn't happen, fail instead of crashing just in case
                return null;
            }

            sideOut = stateSchematic.get(Properties.HORIZONTAL_FACING);
            pos = pos.offset(sideOut.getOpposite());
        }
        else
        {
            sideOut = Direction.UP;
            pos = pos.down();
        }
        BlockState stateFacing = mc.world.getBlockState(pos);

        if (stateFacing == null || stateFacing.isAir())
            return null;

        //Check for blocks that have rotation property (Banners, Signs, Skulls)
        if (stateSchematic.contains(Properties.ROTATION))
        {
            if (!isMatchingStateRestrictedProtocol(posOrig, stateSchematic, sideOut, hitVecOut, mc, hand))
                return null;
        }

        return Triple.of(pos, sideOut, hitVecOut);
    }

    private static Pair<Direction, Vec3d> getAxisOrientation(BlockPos pos, BlockState stateSchematic, Direction sideIn, Vec3d hitVecIn) {
        var property = stateSchematic.get(Properties.AXIS);
        if (property == Direction.Axis.Y)
        {
            if (sideIn == Direction.DOWN || sideIn == Direction.UP)
            {
                //Side already correct
                return Pair.of(sideIn, hitVecIn);
            }

            //Wrong side - check which direction to use
            return Pair.of(hitVecIn.y - pos.getY() < 0.5 ? Direction.DOWN : Direction.UP, hitVecIn);
        }
        else if (property == Direction.Axis.X)
        {
            if (sideIn == Direction.WEST || sideIn == Direction.EAST)
            {
                //Side already correct
                return Pair.of(sideIn, hitVecIn);
            }

            //Wrong side - check which direction to use
            return Pair.of(hitVecIn.x - pos.getX() < 0.5 ? Direction.WEST : Direction.EAST, hitVecIn);
        }
        else if (property == Direction.Axis.Z)
        {
            if (sideIn == Direction.NORTH || sideIn == Direction.SOUTH)
            {
                //Side already correct
                return Pair.of(sideIn, hitVecIn);
            }

            //Wrong side - check which direction to use
            return Pair.of(hitVecIn.z - pos.getZ() < 0.5 ? Direction.NORTH : Direction.SOUTH, hitVecIn);
        }
        return null;
    }
    private static Pair<Direction, Vec3d> getSlabOrientation(BlockState stateSchematic, Direction sideIn, Vec3d hitVecIn) {
        var property = stateSchematic.get(Properties.SLAB_TYPE);

        if (property == SlabType.TOP)
        {
            return Pair.of(Direction.DOWN, new Vec3d(hitVecIn.x, hitVecIn.y + 0.9, hitVecIn.z));
        }
        else if (property == SlabType.BOTTOM)
        {
            return Pair.of(Direction.UP, hitVecIn);
        }
        else
        {
            return Pair.of(sideIn, hitVecIn);
        }
    }

    public static <T extends Comparable<T>> Vec3d applyPlacementProtocolV3(BlockPos pos, BlockState state, Vec3d hitVecIn)
    {
        Collection<Property<?>> props = state.getBlock().getStateManager().getProperties();

        if (props.isEmpty())
        {
            return hitVecIn;
        }

        double relX = hitVecIn.x - pos.getX();
        int protocolValue = 0;
        int shiftAmount = 1;
        int propCount = 0;

        @Nullable DirectionProperty property = fi.dy.masa.malilib.util.BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null && property != Properties.VERTICAL_DIRECTION)
        {
            Direction direction = state.get(property);
            protocolValue |= direction.getId() << shiftAmount;
            shiftAmount += 3;
            ++propCount;
        }

        List<Property<?>> propList = new ArrayList<>(props);
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                if ((p instanceof DirectionProperty) == false &&
                    PlacementHandler.WHITELISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.floorLog2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int valueIndex = list.indexOf(state.get(prop));

                    if (valueIndex != -1)
                    {
                        //System.out.printf("requesting: %s = %s, index: %d\n", prop.getName(), state.get(prop), valueIndex);
                        protocolValue |= (valueIndex << shiftAmount);
                        shiftAmount += requiredBits;
                        ++propCount;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Exception trying to request placement protocol value", e);
        }

        if (propCount > 0)
        {
            double x = pos.getX() + relX + 2 + protocolValue;
            //System.out.printf("request prot value 0x%08X\n", protocolValue + 2);
            return new Vec3d(x, hitVecIn.y, hitVecIn.z);
        }

        return hitVecIn;
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof SlabBlock)
        {
            if (stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE &&
                blockClient instanceof SlabBlock &&
                stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                if (stateClient.get(SlabBlock.TYPE) == SlabType.TOP)
                {
                    return Direction.DOWN;
                }
                else
                {
                    return Direction.UP;
                }
            }
            // Single slab
            else
            {
                return Direction.NORTH;
            }
        }
        else if (stateSchematic.contains(Properties.BLOCK_HALF))
        {
            side = stateSchematic.get(Properties.BLOCK_HALF) == BlockHalf.TOP ? Direction.DOWN : Direction.UP;
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return
     */
    public static boolean handlePlacementRestriction(MinecraftClient mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

            if (type == MessageOutputType.MESSAGE)
            {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, 1000, "litematica.message.placement_restriction_fail");
            }
            else if (type == MessageOutputType.ACTIONBAR)
            {
                InfoUtils.printActionbarMessage("litematica.message.placement_restriction_fail");
            }
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect(MinecraftClient mc)
    {
        HitResult trace = mc.crosshairTarget;

        ItemStack stack = mc.player.getMainHandStack();

        if (stack.isEmpty())
        {
            stack = mc.player.getOffHandStack();
        }

        if (stack.isEmpty())
        {
            return false;
        }

        if (trace != null && trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            BlockPos pos = blockHitResult.getBlockPos();
            ItemPlacementContext ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, blockHitResult));

            // Get the possibly offset position, if the targeted block is not replaceable
            pos = ctx.getBlockPos();

            BlockState stateClient = mc.world.getBlockState(pos);

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();
            boolean schematicHasAir = worldSchematic.isAir(pos);

            // The targeted position is outside the current render range
            if (schematicHasAir == false && range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            // There should not be anything in the targeted position,
            // and the position is within or close to a schematic sub-region
            if (schematicHasAir && isPositionWithinRangeOfSchematicRegions(pos, 2))
            {
                return true;
            }

            blockHitResult = new BlockHitResult(blockHitResult.getPos(), blockHitResult.getSide(), pos, false);
            ctx = new ItemPlacementContext(new ItemUsageContext(mc.player, Hand.MAIN_HAND, (BlockHitResult) trace));

            // Placement position is already occupied
            if (stateClient.canReplace(ctx) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack) == null)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int minCX = (pos.getX() - range) >> 4;
        final int minCY = (pos.getY() - range) >> 4;
        final int minCZ = (pos.getZ() - range) >> 4;
        final int maxCX = (pos.getX() + range) >> 4;
        final int maxCY = (pos.getY() + range) >> 4;
        final int maxCZ = (pos.getZ() + range) >> 4;
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int cy = minCY; cy <= maxCY; ++cy)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                for (int cx = minCX; cx <= maxCX; ++cx)
                {
                    List<IntBoundingBox> boxes = manager.getTouchedBoxesInSubChunk(new SubChunkPos(cx, cy, cz));

                    for (int i = 0; i < boxes.size(); ++i)
                    {
                        IntBoundingBox box = boxes.get(i);

                        if (x >= box.minX - range && x <= box.maxX + range &&
                            y >= box.minY - range && y <= box.maxY + range &&
                            z >= box.minZ - range && z <= box.maxZ + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, Direction.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > chunk.getHighestNonEmptySectionYOffset() + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
    }

    public static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos, boolean hasUseAction)
    {
        long timeout;

        if (hasUseAction)
        {
            timeout = Configs.Generic.EASY_PLACE_USE_INTERVAL.getIntegerValue() * 1_000_000L;
        }
        else
        {
            timeout = 2_000_000_000L;
        }

        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), timeout));
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }

    public static boolean isHandlingEasyPlace()
    {
        return isHandlingEasyPlace;
    }

    public static void setHandlingEasyPlace(boolean handling)
    {
        isHandlingEasyPlace = handling;
    }

    public static void setIsFirstClickEasyPlace()
    {
        if (shouldDoEasyPlaceActions())
        {
            isFirstClickEasyPlace = true;
        }

        if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
        {
            isFirstClickPlacementRestriction = true;
        }
    }

    public static boolean shouldDoEasyPlaceActions()
    {
        return Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && DataManager.getToolMode() != ToolMode.REBUILD &&
                Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld();
    }

    public static boolean handleEasyPlaceWithMessage(MinecraftClient mc)
    {
        if (isHandlingEasyPlace)
        {
            return false;
        }

        isHandlingEasyPlace = true;
        easyPlaceShowFailMessage = true;
        ActionResult result = doEasyPlaceAction(mc);
        isHandlingEasyPlace = false;

        // Only print the warning message once per right click
        if (isFirstClickEasyPlace && result == ActionResult.FAIL && easyPlaceShowFailMessage)
        {
            MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

            if (type == MessageOutputType.MESSAGE)
            {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, 1000, "litematica.message.easy_place_fail");
            }
            else if (type == MessageOutputType.ACTIONBAR)
            {
                InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
            }
        }

        isFirstClickEasyPlace = false;

        return result != ActionResult.PASS;
    }

    public static void onRightClickTail(MinecraftClient mc)
    {
        // If the click wasn't handled yet, handle it now.
        // This is only called when right clicking on air with an empty hand,
        // as in that case neither the processRightClickBlock nor the processRightClick method get called.
        if (isFirstClickEasyPlace)
        {
            handleEasyPlaceWithMessage(mc);
        }
    }

    private static boolean hasUseAction(Block block)
    {
        Boolean val = HAS_USE_ACTION_CACHE.get(block);

        if (val == null)
        {
            try
            {
                String name = "method_9534"; //onUse
                Method method = block.getClass().getMethod(name, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class);
                Method baseMethod = Block.class.getMethod(name, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class);
                val = method.equals(baseMethod) == false;
            }
            catch (Exception e)
            {
                Litematica.logger.warn("WorldUtils: Failed to reflect method Block::onUse", e);
                val = false;
            }

            HAS_USE_ACTION_CACHE.put(block, val);
        }

        return val;
    }

    public static void insertSignTextFromSchematic(SignBlockEntity sbeClient, String[] text)
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic != null)
        {
            BlockEntity be = worldSchematic.getBlockEntity(sbeClient.getPos());

            if (be instanceof SignBlockEntity sbeSchematic)
            {
                for (int i = 0; i < text.length; ++i)
                {
                    text[i] = sbeSchematic.getTextOnRow(i, true).getString();
                }
            }
        }
    }
}
