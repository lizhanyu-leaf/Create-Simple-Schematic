package com.leaf.createsimpleschematic;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = CreateAndesiteAbound.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class AllConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        // 构造配置
        Pair<Common, ForgeConfigSpec> pair = BUILDER.configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    // common 配置定义
    public static class Common {
        public final ForgeConfigSpec.BooleanValue ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Simple Schematic").push("simple_schematic");
            ENABLE_SIMPLE_SCHEMATIC_FLIP_TOOL = builder
                    .comment("Enable the Simple Schematic’s Flip Tool")
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