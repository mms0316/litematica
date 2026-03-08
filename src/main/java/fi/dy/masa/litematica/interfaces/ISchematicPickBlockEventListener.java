package fi.dy.masa.litematica.interfaces;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventResult;

public interface ISchematicPickBlockEventListener
{
	Supplier<String> getName();

	void onSchematicPickBlockCancelled(Supplier<String> cancelledBy);

	SchematicPickBlockEventResult onSchematicPickBlockStart(boolean closest);

	SchematicPickBlockEventResult onSchematicPickBlockPreGather(Level schematicWorld, BlockPos pos, BlockState expectedState);

	SchematicPickBlockEventResult onSchematicPickBlockPrePick(Level schematicWorld, BlockPos pos, BlockState expectedState, ItemStack stack);

	void onSchematicPickBlockSuccess();
}
