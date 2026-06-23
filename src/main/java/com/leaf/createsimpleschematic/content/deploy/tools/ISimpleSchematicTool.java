package com.leaf.createsimpleschematic.content.deploy.tools;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class ISimpleSchematicTool {

    public static void renderOnSchematic(
            PoseStack ms, SuperRenderTypeBuffer buffer,
            SchematicHandler handler, Boolean renderSelected, Direction selected
    ) {
        if (!handler.isDeployed())
            return;

        ms.pushPose();
        AABBOutline outline = handler.getOutline();
        if (renderSelected) {
            outline.getParams()
                    .highlightFace(selected)
                    .withFaceTextures(AllSpecialTextures.CHECKERED,
                            AllKeys.ctrlDown() ? AllSpecialTextures.HIGHLIGHT_CHECKERED : AllSpecialTextures.CHECKERED);
        }
        outline.getParams()
                .colored(0x32CD32)
                .withFaceTexture(AllSpecialTextures.CHECKERED)
                .lineWidth(1 / 16f);
        outline.render(ms, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());
        outline.getParams()
                .clearTextures();
        ms.popPose();
    }
}
