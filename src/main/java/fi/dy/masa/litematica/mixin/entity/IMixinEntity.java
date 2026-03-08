package fi.dy.masa.litematica.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IMixinEntity
{
    @Accessor("level")
    void litematica_setWorld(Level world);

    @Invoker("readAdditionalSaveData")
    void litematica_readCustomData(ValueInput view);

    @Accessor("wasTouchingWater")
    boolean litematica_isTouchingWater();
}
