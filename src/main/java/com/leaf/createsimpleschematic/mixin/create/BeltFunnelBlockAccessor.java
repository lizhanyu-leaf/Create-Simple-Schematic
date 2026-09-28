package com.leaf.createsimpleschematic.mixin.create;

import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeltFunnelBlock.class)
public interface BeltFunnelBlockAccessor {
    @Accessor("parent")
    BlockEntry<? extends FunnelBlock> getParent();
}