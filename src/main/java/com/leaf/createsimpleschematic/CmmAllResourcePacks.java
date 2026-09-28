package com.leaf.createsimpleschematic;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = CreateSimpleSchematic.MOD_ID)
public class CmmAllResourcePacks {

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;

        // 注册内嵌资源包（位于 mod jar 的 resourcepacks/ 目录下）
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(CreateSimpleSchematic.MOD_ID,
                        "resourcepacks/simple_packer_slimeli_texture"),
                PackType.CLIENT_RESOURCES,
                Component.translatable("css.packer.resourcepack.name"),
                PackSource.BUILT_IN,
                false,
                Pack.Position.TOP
        );
    }
}
