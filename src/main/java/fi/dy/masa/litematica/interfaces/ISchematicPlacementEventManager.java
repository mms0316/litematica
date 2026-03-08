package fi.dy.masa.litematica.interfaces;

import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementEventFlag;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

public interface ISchematicPlacementEventManager
{
    void registerSchematicPlacementEventListener(@Nonnull ISchematicPlacementEventListener listener, @Nonnull List<SchematicPlacementEventFlag> flags);

    void invokePrePlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePostPlacementChange(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokePlacementModified(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement);

    void invokeSetSubRegionEnabled(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, boolean toggle);

    void invokeSetSubRegionOrigin(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, BlockPos pos);

    void invokeSetSubRegionMirror(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, Mirror mirror);

    void invokeSetSubRegionRotation(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion, Rotation rot);

    void invokeSubRegionModified(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SchematicPlacement placement, @Nonnull String subRegionName);

    void invokeResetSubRegion(@Nonnull ISchematicPlacementEventListener listener, @Nonnull SubRegionPlacement subRegion);
}
