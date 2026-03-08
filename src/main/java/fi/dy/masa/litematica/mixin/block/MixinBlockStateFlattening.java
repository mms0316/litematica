package fi.dy.masa.litematica.mixin.block;

import java.util.Arrays;
import java.util.List;
import net.minecraft.util.datafix.fixes.BlockStateData;
import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;

@Mixin(BlockStateData.class)
public abstract class MixinBlockStateFlattening
{
    @Inject(method = "register", at = @At("HEAD"))
    private static void litematica_onAddEntry(int oldIdAndMeta, Dynamic<?> newStateDynamic, Dynamic<?>[] oldStateDynamics, CallbackInfo ci)
    {
        List<Dynamic<?>> oldDynamics = Arrays.stream(oldStateDynamics).toList();
		SchematicConversionMaps.addDynamicEntry(oldIdAndMeta, newStateDynamic, oldDynamics);
    }
}
