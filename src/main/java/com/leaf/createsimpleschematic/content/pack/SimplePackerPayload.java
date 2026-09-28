package com.leaf.createsimpleschematic.content.pack;

import com.leaf.createsimpleschematic.AllItems;
import com.leaf.createsimpleschematic.content.StructureHelper;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

import static com.leaf.createsimpleschematic.CreateSimpleSchematic.MOD_ID;

public record SimplePackerPayload(BlockPos anchor, BlockPos size) implements CustomPacketPayload {

    public static final Type<SimplePackerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "recycle_schematic"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, SimplePackerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SimplePackerPayload::anchor,
                    BlockPos.STREAM_CODEC, SimplePackerPayload::size,
                    SimplePackerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!AllItems.SIMPLE_PACKER.isIn(stack)) return;

            Level world = player.level();

            StructureMetaCache.matchAnyStructure(world, anchor, size, (path, blockReader) -> {
                // 1. 收集所有非空气方块
                List<BlockPos> blockPosList = blockReader.getBlockMap().entrySet().stream()
                        .filter(e -> !e.getValue().is(Blocks.AIR))
                        .map(e -> anchor.offset(e.getKey()))
                        .toList();

                // 2. 统一销毁
                StructureHelper.destroyStructure(world, blockPosList);

                // 3. 删除实体
                blockReader.getEntityList().forEach(entity -> {
                    AABB bounds = entity.getBoundingBox().move(anchor);
                    world.getEntitiesOfClass(entity.getClass(), bounds)
                            .stream().findAny().ifPresent(Entity::discard);
                });

                // 4. 清理强力胶
                AABB glueBounds = new AABB(Vec3.atLowerCornerOf(anchor), Vec3.atLowerCornerOf(anchor.offset(size)));
                for (SuperGlueEntity glue : world.getEntitiesOfClass(SuperGlueEntity.class, glueBounds)) {
                    glue.discard();
                }

                // 5. 生成蓝图物品
                ItemStack schematic = AllItems.SIMPLE_SCHEMATIC.asStack();
                CompoundTag tag = new CompoundTag();
                tag.putString("File", path.toString().replace("\\", "/"));
                schematic.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
                player.getInventory().placeItemBackInInventory(schematic);

                // 6. 成功音效
                AllSoundEvents.CONFIRM.playFrom(player);

            }, result -> {
                String key = switch (result) {
                    case SUCCESS -> "";
                    case SIZE_ERROR -> "css.packer.error.size";
                    case BLOCK_ERROR -> "css.packer.error.block";
                };
                if (key.isEmpty()) return;

                AllSoundEvents.DENY.playFrom(player);
                player.displayClientMessage(Component.translatable(key)
                        .withStyle(ChatFormatting.RED), true);
            });
        });
    }
}
