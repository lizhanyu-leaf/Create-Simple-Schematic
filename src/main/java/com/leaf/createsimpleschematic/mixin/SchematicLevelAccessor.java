package com.leaf.createsimpleschematic.mixin;

import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicLevel.class, remap = false)
public interface SchematicLevelAccessor {
    @Accessor("bounds")
    void setBounds(BoundingBox bounds);
}
