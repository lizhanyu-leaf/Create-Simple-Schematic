package com.leaf.createsimpleschematic.mixin.schematic;

import com.simibubi.create.content.schematics.SchematicWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SchematicWorld.class, remap = false)
public interface SchematicWorldAccessor {
    @Accessor("bounds")
    void setBounds(BoundingBox bounds);
}
