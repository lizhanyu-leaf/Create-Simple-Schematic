package com.leaf.createsimpleschematic.content.deploy;

import com.leaf.createsimpleschematic.content.StructureHelper;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

import static com.leaf.createsimpleschematic.CreateSimpleSchematic.MOD_ID;

public record SimpleSchematicPlacePayload(ItemStack stack, BlockPos anchor, Rotation rotation, Mirror mirror)
        implements CustomPacketPayload {

    public static final Type<SimpleSchematicPlacePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "place_schematic"));

    private static final StreamCodec<ByteBuf, Rotation> ROTATION_CODEC =
            ByteBufCodecs.<Rotation>idMapper(i -> Rotation.values()[i], Rotation::ordinal);
    private static final StreamCodec<ByteBuf, Mirror> MIRROR_CODEC =
            ByteBufCodecs.<Mirror>idMapper(i -> Mirror.values()[i], Mirror::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, SimpleSchematicPlacePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, SimpleSchematicPlacePayload::stack,
                    BlockPos.STREAM_CODEC, SimpleSchematicPlacePayload::anchor,
                    ROTATION_CODEC, SimpleSchematicPlacePayload::rotation,
                    MIRROR_CODEC, SimpleSchematicPlacePayload::mirror,
                    SimpleSchematicPlacePayload::new);

    public static SimpleSchematicPlacePayload of(ItemStack stack, BlockPos anchor, StructurePlaceSettings settings) {
        return new SimpleSchematicPlacePayload(stack, anchor, settings.getRotation(), settings.getMirror());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty() || !ItemStack.isSameItemSameComponents(heldItem, stack))
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

                    CompoundTag data = BlockHelper.prepareBlockEntityData(world, state, blockEntity);

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
    }
}
