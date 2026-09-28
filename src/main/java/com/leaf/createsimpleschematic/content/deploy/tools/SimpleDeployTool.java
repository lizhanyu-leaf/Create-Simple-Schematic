package com.leaf.createsimpleschematic.content.deploy.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.tools.DeployTool;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import com.leaf.createsimpleschematic.content.deploy.SimpleSchematicHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class SimpleDeployTool extends DeployTool {
    @Override
    public void init() {
        super.init();
        schematicHandler = SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER;
    }

    @Override
    public void renderOnSchematic(PoseStack ms, SuperRenderTypeBuffer buffer) {
        ISimpleSchematicTool.renderOnSchematic(ms, buffer, schematicHandler, renderSelectedFace, selectedFace);
    }

    @Override
    public boolean handleRightClick() {
        if (selectedPos == null)
            return super.handleRightClick();
        Vec3 center = schematicHandler.getBounds().getCenter();
        BlockPos target = selectedPos.offset(-((int) center.x), 0, -((int) center.z));

        schematicHandler.getTransformation().startAt(target);
        schematicHandler.deploy();

        return true;
    }
}
