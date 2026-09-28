package com.leaf.createsimpleschematic.foundation;

import com.leaf.createsimpleschematic.AllItems;
import com.leaf.createsimpleschematic.content.deploy.SimpleSchematicHandler;
import com.leaf.createsimpleschematic.content.pack.SimplePackerRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = com.leaf.createsimpleschematic.CreateSimpleSchematic.MOD_ID, value = Dist.CLIENT)
public class ClientInit {

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(ClientResourceReloadListener.RESOURCE_RELOAD_LISTENER);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        com.leaf.createsimpleschematic.CreateSimpleSchematic.MOD_ID, "simple_schematic"),
                SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(SimpleCustomRenderer.create(AllItems.SIMPLE_PACKER.get(), new SimplePackerRenderer()),
                AllItems.SIMPLE_PACKER.get());
    }
}
