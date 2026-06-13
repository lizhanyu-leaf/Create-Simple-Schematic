package com.leaf.createsimpleschematic.foundation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.antlr.runtime.misc.IntArray;

import java.util.HashMap;
import java.util.Map;

public class VisitedItemStackTracker {
    Map<AbstractItemStack, SlotAmountRecord> itemStackMap = new HashMap<>();

    public SlotAmountRecord update(ItemStack itemStack, int slot) {
        SlotAmountRecord record = itemStackMap.computeIfAbsent(new AbstractItemStack(itemStack), k -> new SlotAmountRecord());
        record.add(slot, itemStack.getCount());
        return record;
    }

    public static class AbstractItemStack {
        private final Item item;
        private final CompoundTag nbt;

        private boolean initialized = false;
        private int hashCode;
        AbstractItemStack(ItemStack itemStack) {
            item = itemStack.getItem();
            if (itemStack.hasTag())
                nbt = itemStack.getTag();
            else
                nbt = null;
        }

        @Override
        public int hashCode() {
            if (!initialized) {
                initialized = true;
                hashCode = item.hashCode();
                if (nbt != null) {
                    hashCode = hashCode * 31 + nbt.hashCode();
                }
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof AbstractItemStack otherStack))
                return false;
            if (item != otherStack.item)
                return false;
            if (nbt == null ^ otherStack.nbt == null)
                return false;
            return nbt == null || nbt.equals(otherStack.nbt);
        }
    }

    public static class SlotAmountRecord {
        private final IntArray slots = new IntArray();
        private int totalAmount = 0;

        void add(int slot, int amount) {
            slots.add(slot);
            totalAmount += amount;
        }

        public int getTotalAmount() {
            return totalAmount;
        }

        public int[] getSlotsIndex() {
            return slots.data;
        }
    }
}
