package com.leaf.createsimpleschematic.content.deploy;

import com.leaf.createsimpleschematic.content.StructureHelper;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.ArrayList;
import java.util.List;

public class SimpleSchematicPlacePacket extends SimplePacketBase {

    ItemStack stack;
    public BlockPos anchor;
    public Rotation rotation;
    public Mirror mirror;

    public SimpleSchematicPlacePacket(ItemStack stack, BlockPos anchor, StructurePlaceSettings settings) {
        this.stack = stack;
        this.anchor = anchor;
        this.rotation = settings.getRotation();
        this.mirror = settings.getMirror();
    }

    public SimpleSchematicPlacePacket(FriendlyByteBuf buffer) {
        stack = buffer.readItem();
        anchor = buffer.readBlockPos();
        rotation = buffer.readEnum(Rotation.class);
        mirror = buffer.readEnum(Mirror.class);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeItem(stack);
        buffer.writeBlockPos(anchor);
        buffer.writeEnum(rotation);
        buffer.writeEnum(mirror);
    }

    @Override
    public boolean handle(Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty() || !ItemStack.isSameItemSameTags(heldItem, stack))
                return;

            Level world = player.level();
            SimpleSchematicPrinter printer = new SimpleSchematicPrinter();
            printer.loadSimpleSchematic(stack, anchor, rotation, mirror, world,
                    !player.canUseGameMasterBlocks());

            // 1. 失败处理
            if (!printer.isLoaded() || printer.isErrored()) {
                AllSoundEvents.DENY.playFrom(player);
                player.displayClientMessage(
                        Component.translatable("css.schematic.error.invalid")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }

            boolean includeAir = AllConfigs.server().schematics.creativePrintIncludesAir.get();
            List<BlockPos> funnels = new ArrayList<>();

            while (printer.advanceCurrentPos()) {
                if (!printer.shouldPlaceCurrent(world)) continue;

                printer.handleCurrentTarget((pos, state, blockEntity) -> {
                    boolean placingAir = state.isAir();
                    if (placingAir && !includeAir) return;

                    // 2. 收集漏斗
                    if (state.getBlock() instanceof AbstractFunnelBlock) {
                        funnels.add(pos);
                    }

                    CompoundTag data = BlockHelper.prepareBlockEntityData(state, blockEntity);

                    // 3. 传送带旋转
                    StructureHelper.simpleBeltRotate(state, blockEntity, data, rotation);

                    // 4. 无限制放置
                    StructureHelper.placeSchematicBlockUnlimited(world, state, pos, null, data);
                }, (pos, entity) -> world.addFreshEntity(entity));
            }

            // 5. 统一修复漏斗
            for (BlockPos pos : funnels) {
                StructureHelper.updateFunnelShape(world, pos);
            }

            AllSoundEvents.SCHEMATICANNON_FINISH.playFrom(player);

            if (!player.isCreative()) {
                heldItem.shrink(1);
            }
        });
        return true;
    }
}
