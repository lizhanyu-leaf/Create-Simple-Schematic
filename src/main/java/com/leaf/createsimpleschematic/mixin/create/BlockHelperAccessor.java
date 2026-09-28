package com.leaf.createsimpleschematic.mixin.create;

import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockHelper.class)
public interface BlockHelperAccessor {
    @Invoker("placeRailWithoutUpdate")
    static void invokePlaceRailWithoutUpdate(Level world, BlockState state, BlockPos target) {
        throw new AssertionError();
    }
}