package com.leaf.createsimpleschematic.content.deploy.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.tools.PlaceTool;
import com.leaf.createsimpleschematic.content.deploy.SimpleSchematicHandler;
import net.createmod.catnip.render.SuperRenderTypeBuffer;

public class SimplePlaceTool extends PlaceTool {
    @Override
    public void init() {
        super.init();
        schematicHandler = SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER;
    }

    @Override
    public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer) {
        ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
    }
}
