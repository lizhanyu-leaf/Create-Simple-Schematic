package com.leaf.createsimpleschematic.mixin.schematic;

import com.simibubi.create.content.schematics.client.ToolSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ToolSelectionScreen.class, remap = false)
public interface ToolSelectionScreenAccessor {
    @Accessor("initialized")
    boolean hasInitialized();

    @Accessor("yOffset")
    float getYOffset();
}
