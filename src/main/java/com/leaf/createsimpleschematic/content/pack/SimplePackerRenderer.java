package com.leaf.createsimpleschematic.content.pack;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static com.leaf.createsimpleschematic.CreateSimpleSchematic.MOD_ID;

public class SimplePackerRenderer extends CustomRenderedItemModelRenderer {
    @SuppressWarnings("removal")
    private static final PartialModel SLIMELI_ =
            new PartialModel(new ResourceLocation(MOD_ID, "item/simple_packer/slimeli_"));

    @Override
    protected void render(
            ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
            ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay
    ) {
        renderer.render(model.getOriginalModel(), light);
        float partial = AnimationTickHolder.getPartialTicks();
        float degree = SimplePackerHandler.SIMPLE_PACKER_HANDLER.getScroll(partial);
        float yOffset = SimplePackerHandler.SIMPLE_PACKER_HANDLER.height.getValue(partial);
        ms.mulPose(Axis.YP.rotationDegrees(degree));
        ms.translate(0, yOffset, 0);
        renderer.render(SLIMELI_.get(), light);
    }
}