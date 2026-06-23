package com.leaf.createsimpleschematic.content.pack;

import com.leaf.createsimpleschematic.AllItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;

public class SimplePackerPacket extends SimplePacketBase {

    public BlockPos anchor;
    public BlockPos size;

    public SimplePackerPacket(BlockPos anchor, BlockPos size) {
        this.anchor = anchor;
        this.size = size;
    }

    public SimplePackerPacket(FriendlyByteBuf buffer) {
        anchor = buffer.readBlockPos();
        size = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(anchor);
        buffer.writeBlockPos(size);
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null)
                return;
            Level world = player.level();
            StructureMetaCache.matchAnyStructure(world, anchor, size, (path, blockReader) -> {
                List<BlockPos> destroyLater = new ArrayList<>();
                for (var entry: blockReader.getBlockMap().entrySet()) {
                    BlockPos targetPos = anchor.offset(entry.getKey());
                    Block block = entry.getValue().getBlock();
                    if (AllBlocks.WATER_WHEEL_STRUCTURAL.is(block)
                    ) {
                        destroyLater.add(targetPos);
                        continue;
                    }
                    world.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 50);
                }
                for (BlockPos targetPos: destroyLater) {
                    world.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 50);
                }
                blockReader.getEntityList().forEach(entity -> {
                    AABB bounds = entity.getBoundingBox().move(anchor);
                    world.getEntitiesOfClass(entity.getClass(), bounds)
                            .stream().findAny().ifPresent(Entity::discard);
                });

                ItemStack schematic = AllItems.SIMPLE_SCHEMATIC.asStack();
                CompoundTag tag = new CompoundTag();
                tag.putString("File", path.toString().replace("\\", "/"));
                schematic.setTag(tag);
                player.getInventory().placeItemBackInInventory(schematic);
            }, result -> {
                String key = switch (result) {
                    case SUCCESS -> "";
                    case SIZE_ERROR -> "css.packer.error.size";
                    case BLOCK_ERROR -> "css.packer.error.block";
                };
                if (key.isEmpty())
                    return;
                player.displayClientMessage(Component.translatable(key)
                        .withStyle(ChatFormatting.RED), true);
            });
        });
        return true;
    }
}