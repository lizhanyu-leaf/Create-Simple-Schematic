package com.leaf.createsimpleschematic.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.leaf.createsimpleschematic.CreateSimpleSchematic;
import com.leaf.createsimpleschematic.mixin.create.BeltFunnelBlockAccessor;
import com.leaf.createsimpleschematic.mixin.create.BlockHelperAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StructureHelper {

    // ========== 旋转匹配 ==========

    public static boolean matchRotatedSize(Vec3i size, Vec3i size2, Rotation rotation) {
        return switch (rotation) {
            case NONE, CLOCKWISE_180 -> size.equals(size2);
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> size.getY() == size2.getY()
                    && size.getX() == size2.getZ() && size.getZ() == size2.getX();
        };
    }

    public static final Set<Property<?>> ignoredProperties = Set.of(
            BlockStateProperties.FACING, BlockStateProperties.AXIS,
            BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HORIZONTAL_AXIS,
            BlockStateProperties.UP, BlockStateProperties.DOWN, BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST
    );

    public static boolean matchPropertiesIgnoreRotation(BlockState state1, BlockState state2) {
        try {
            for (Property<?> property : state1.getProperties()) {
                if (ignoredProperties.contains(property)) continue;
                if (state1.getValue(property) != state2.getValue(property)) return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static boolean isIgnoredBlockEntity(BlockEntity entity) {
        return entity instanceof FluidTankBlockEntity;
    }

    public static BlockPos transform(BlockPos anchor, Vec3i length, Vec3i pos, Rotation rotation) {
        return switch (rotation) {
            case NONE -> anchor.offset(pos);
            case CLOCKWISE_90 -> anchor.offset(length.getX() - pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> anchor.offset(length.getX() - pos.getX(), pos.getY(), length.getZ() - pos.getZ());
            case COUNTERCLOCKWISE_90 -> anchor.offset(pos.getZ(), pos.getY(), length.getZ() - pos.getX());
        };
    }

    public static BlockPos backToZero(Vec3i length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> BlockPos.ZERO;
            case CLOCKWISE_90 -> new BlockPos(length.getX(), 0, 0);
            case CLOCKWISE_180 -> new BlockPos(length.getX(), 0, length.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(0, 0, length.getZ());
        };
    }

    // ========== 销毁 ==========

    public static boolean shouldDestroyLater(Block block) {
        return AllBlocks.WATER_WHEEL_STRUCTURAL.is(block);
    }

    static final int FLAGS = Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE;

    public static void destroyStructure(Level level, Iterable<BlockPos> bounds) {
        List<BlockPos> destroyLater = new ArrayList<>();
        for (BlockPos pos : bounds) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.AIR)) continue;

            if (shouldDestroyLater(state.getBlock())) {
                destroyLater.add(new BlockPos(pos));
                continue;
            }
            clearBlock(level, pos);
        }
        for (BlockPos pos : destroyLater) {
            clearBlock(level, pos);
        }
    }

    public static void clearBlock(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Clearable clearable) {
            clearable.clearContent();
        }
        level.removeBlockEntity(pos);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
    }

    // ========== 漏斗修复 ==========

    public static void updateFunnelShape(Level level, BlockPos pos) {
        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof FunnelBlock funnelBlock) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            if (!facing.getAxis().isHorizontal()) return;

            BlockState equivalent = funnelBlock.getEquivalentBeltFunnel(level, pos, state);
            if (!BeltFunnelBlock.isOnValidBelt(equivalent, level, pos)) return;

            equivalent = ProperWaterloggedBlock.withWater(level, equivalent, pos)
                    .setValue(BeltFunnelBlock.SHAPE, BeltFunnelBlock.getShapeForPosition(
                            level, pos, facing, state.getValue(FunnelBlock.EXTRACTING)));
            level.setBlock(pos, equivalent, 3);

        } else if (state.getBlock() instanceof BeltFunnelBlock beltFunnelBlock) {
            if (BeltFunnelBlock.isOnValidBelt(state, level, pos)) return;

            BlockState equivalent = ((BeltFunnelBlockAccessor) beltFunnelBlock).getParent().getDefaultState();
            equivalent = ProperWaterloggedBlock.withWater(level, equivalent, pos);
            if (state.getOptionalValue(AbstractFunnelBlock.POWERED).orElse(false)) {
                equivalent = equivalent.setValue(AbstractFunnelBlock.POWERED, true);
            }
            if (state.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.PUSHING) {
                equivalent = equivalent.setValue(FunnelBlock.EXTRACTING, true);
            }
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            equivalent = equivalent.setValue(FunnelBlock.FACING, facing);

            level.setBlock(pos, equivalent, 3);
        }
    }

    // ========== 放置方块 ==========

    public static void placeSchematicBlockUnlimited(
            Level world, BlockState state, BlockPos target, ItemStack stack, @Nullable CompoundTag data
    ) {
        BlockEntity existingBlockEntity = world.getBlockEntity(target);

        if (state.getBlock() instanceof BaseRailBlock) {
            BlockHelperAccessor.invokePlaceRailWithoutUpdate(world, state, target);
        } else if (AllBlocks.BELT.has(state)) {
            world.setBlock(target, state, 2);
        } else {
            world.setBlock(target, state, 18);
        }

        if (data != null) {
            if (existingBlockEntity instanceof IMergeableBE mergeable) {
                BlockEntity loaded = BlockEntity.loadStatic(target, state, data);
                if (loaded != null) {
                    if (existingBlockEntity.getType().equals(loaded.getType())) {
                        mergeable.accept(loaded);
                        return;
                    }
                }
            }
            BlockEntity blockEntity = world.getBlockEntity(target);
            if (blockEntity != null) {
                data.putInt("x", target.getX());
                data.putInt("y", target.getY());
                data.putInt("z", target.getZ());
                if (blockEntity instanceof KineticBlockEntity kbe)
                    kbe.warnOfMovement();
                if (blockEntity instanceof IMultiBlockEntityContainer imbe)
                    if (!imbe.isController())
                        data.put("Controller", NbtUtils.writeBlockPos(imbe.getController()));
                blockEntity.load(data);
            }
        }

        try {
            state.getBlock().setPlacedBy(world, target, state, null, stack);
        } catch (Exception ignored) {}
    }

    // ========== 传送带旋转 ==========

    public static void simpleBeltRotate(BlockState state, BlockEntity entity, CompoundTag data, Rotation rotation) {
        if (!(AllBlocks.BELT.has(state)) || !(entity instanceof BeltBlockEntity) || !data.contains("ScrollValue"))
            return;

        boolean reverse = false;
        switch (state.getValue(BeltBlock.SLOPE)) {
            case HORIZONTAL:
                if (rotation == Rotation.CLOCKWISE_180 || rotation == Rotation.COUNTERCLOCKWISE_90)
                    reverse = true;
                break;
            case UPWARD, DOWNWARD:
                if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.CLOCKWISE_180)
                    reverse = true;
                break;
        }
        if (reverse) {
            data.putInt("ScrollValue", -data.getInt("ScrollValue"));
        }
    }
}
