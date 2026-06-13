package com.leaf.createsimpleschematic;

import com.tterrag.registrate.util.entry.ItemEntry;
import com.leaf.createsimpleschematic.content.SimpleSchematicItem;

import static com.leaf.createsimpleschematic.CreateAndesiteAbound.REGISTRATE;


public class AllItems {

    public static final ItemEntry<SimpleSchematicItem> SIMPLE_SCHEMATIC =
            REGISTRATE.item("simple_schematic", SimpleSchematicItem::new)
                    .properties(p -> p.stacksTo(16))
                    .register();

    public static void register() {}
}
