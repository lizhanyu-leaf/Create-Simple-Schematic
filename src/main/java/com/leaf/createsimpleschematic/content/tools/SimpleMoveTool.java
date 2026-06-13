package com.leaf.createsimpleschematic.content.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.tools.MoveTool;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.leaf.createsimpleschematic.content.SimpleSchematicHandler;

public class SimpleMoveTool extends MoveTool {
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
