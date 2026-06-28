package com.leaf.createsimpleschematic;

import com.simibubi.create.foundation.pack.ModFilePackResources;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateSimpleSchematic.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CmmAllResourcePacks {

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            var pack = Pack.readMetaAndCreate(
                    "createsimpleschematic:simple_packer_slimeli_texture",                          // 资源包 ID
                    Component.translatable("css.packer.resourcepack.name"), // 显示名称
                    false,                                       // 始终启用
                    p -> new ModFilePackResources(
                        "simple_packer_slimeli_texture",
                            ModList.get().getModFileById("createsimpleschematic").getFile(),
                            "resourcepacks/simple_packer_slimeli_texture"
                    ),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );

            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
