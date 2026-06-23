package com.leaf.createsimpleschematic.content.deploy;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.SchematicProcessor;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.foundation.utility.BBHelper;
import com.leaf.createsimpleschematic.CreateSimpleSchematic;
import com.leaf.createsimpleschematic.mixin.schematic.SchematicPrinterAccessor;
import com.leaf.createsimpleschematic.mixin.schematic.SchematicWorldAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class SimpleSchematicPrinter extends SchematicPrinter {
    public void loadSimpleSchematic(
            ItemStack blueprint, BlockPos anchor, Rotation rotation, Mirror mirror,
            Level world, boolean processNBT
    ) {
        // noinspection DataFlowIssue
        if (!blueprint.hasTag() || !blueprint.getTag().contains("File"))
            return;

        StructureTemplate activeTemplate = SimpleSchematicItem.loadSchematic(
                world.holderLookup(Registries.BLOCK), blueprint);
        SchematicWorld blockReader = new SchematicWorld(anchor, world);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setRotation(rotation);
        settings.setMirror(mirror);
        if (processNBT)
            settings.addProcessor(SchematicProcessor.INSTANCE);

        SchematicPrinterAccessor accessor = (SchematicPrinterAccessor) this;

        accessor.setSchematicAnchor(anchor);
        accessor.setBlockReader(blockReader);

        try {
            activeTemplate.placeInWorld(
                    blockReader, anchor, anchor, settings, blockReader.getRandom(), Block.UPDATE_CLIENTS);
        } catch (Exception e) {
            CreateSimpleSchematic.LOGGER.error("Failed to load Schematic for Printing", e);
            accessor.setSchematicLoaded(true);
            accessor.setIsErrored(true);
            return;
        }

        BlockPos extraBounds = StructureTemplate.calculateRelativePosition(
                settings, new BlockPos(activeTemplate.getSize()).offset(-1, -1, -1));
        ((SchematicWorldAccessor) blockReader).setBounds(BBHelper.encapsulate(blockReader.getBounds(), extraBounds));

        StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Direction.Axis.Y,
                settings.getRotation(), settings.getMirror());
        for (BlockEntity be : blockReader.getBlockEntities())
            transform.apply(be);

        accessor.setPrintingEntityIndex(-1);
        accessor.setPrintStage(PrintStage.BLOCKS);
        accessor.getDeferredBlocks().clear();
        BoundingBox bounds = blockReader.getBounds();
        accessor.setCurrentPos(new BlockPos(bounds.minX() - 1, bounds.minY(), bounds.minZ()));
        accessor.setSchematicLoaded(true);
    }
}
