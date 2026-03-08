package fi.dy.masa.litematica.interfaces;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;

public interface ISchematicPickBlockSlotHandler
{
	Supplier<String> getName();

	SchematicPickBlockEventResult executePickBlock(Level schematicWorld, BlockPos pos, ItemStack stack);
}
