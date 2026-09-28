package com.leaf.createsimpleschematic;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateSimpleSchematic.MOD_ID)
public class AllConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;

    static {
        // 构造配置
        COMMON = new Common(BUILDER);
        COMMON_SPEC = BUILDER.build();
    }

    // common 配置定义
    public static class Common {
        public final ModConfigSpec.BooleanValue ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Simple Schematic").push("simple_schematic");
            ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL = builder
                    .comment("Enable the Simple Schematic's Flip Tool")
                    .define("enable_simple_schematic_flip_tool", false);
            builder.pop();
        }
    }

    // 缓存配置值
    public static boolean enable_simple_schematic_flip_tool;

    // 重载配置时，更新缓存
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != COMMON_SPEC) return;

        enable_simple_schematic_flip_tool = COMMON.ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL.get();
    }
}
