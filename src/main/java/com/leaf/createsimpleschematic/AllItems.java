package com.leaf.createsimpleschematic;

import com.leaf.createsimpleschematic.content.pack.SimplePackerItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.leaf.createsimpleschematic.content.deploy.SimpleSchematicItem;
import net.minecraft.world.item.Item;

import static com.leaf.createsimpleschematic.CreateSimpleSchematic.REGISTRATE;


public final class AllItems {

    public static final ItemEntry<SimpleSchematicItem> SIMPLE_SCHEMATIC =
            REGISTRATE.item("simple_schematic", SimpleSchematicItem::new)
                    .properties(p -> p.stacksTo(16))
                    .register();

    public static final ItemEntry<SimplePackerItem> SIMPLE_PACKER =
            REGISTRATE.item("simple_packer", SimplePackerItem::new)
                    .properties(p -> p.stacksTo(1))
                    .model(AssetLookup.itemModelWithPartials())
                    .register();

    public static void register() {

    }
}
