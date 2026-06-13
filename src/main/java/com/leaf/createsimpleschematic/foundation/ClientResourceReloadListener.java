package com.leaf.createsimpleschematic.foundation;

import com.leaf.createsimpleschematic.content.SimpleSchematicHandler;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jetbrains.annotations.NotNull;

public class ClientResourceReloadListener implements ResourceManagerReloadListener {

    public static final ClientResourceReloadListener RESOURCE_RELOAD_LISTENER = new ClientResourceReloadListener();

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        SimpleSchematicHandler.SIMPLE_SCHEMATIC_HANDLER.updateRenderers();
    }
}
