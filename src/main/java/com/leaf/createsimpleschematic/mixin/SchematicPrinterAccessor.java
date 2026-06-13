package com.leaf.createsimpleschematic.mixin;

import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.SchematicWorld;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = SchematicPrinter.class, remap = false)
public interface SchematicPrinterAccessor {
    @Accessor("schematicAnchor")
    void setSchematicAnchor(BlockPos anchor);

    @Accessor("blockReader")
    void setBlockReader(SchematicWorld world);

    @Accessor("schematicLoaded")
    void setSchematicLoaded(boolean loaded);

    @Accessor("isErrored")
    void setIsErrored(boolean errored);

    @Accessor("printingEntityIndex")
    void setPrintingEntityIndex(int index);

    @Accessor("printStage")
    void setPrintStage(SchematicPrinter.PrintStage stage);

    @Accessor("deferredBlocks")
    List<BlockPos> getDeferredBlocks();

    @Accessor("currentPos")
    void setCurrentPos(BlockPos pos);
}
