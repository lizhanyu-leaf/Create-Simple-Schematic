package com.leaf.createsimpleschematic;

import com.leaf.createsimpleschematic.content.deploy.SimpleSchematicPlacePayload;
import com.leaf.createsimpleschematic.content.pack.SimplePackerPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CreateSimpleSchematic.MOD_ID)
public final class AllPackets {

    public static final int NETWORK_VERSION = 3;

    private AllPackets() {
    }

    @SubscribeEvent
    static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(String.valueOf(NETWORK_VERSION));

        // Client to Server
        registrar.playToServer(
                SimpleSchematicPlacePayload.TYPE,
                SimpleSchematicPlacePayload.STREAM_CODEC,
                SimpleSchematicPlacePayload::handle);
        registrar.playToServer(
                SimplePackerPayload.TYPE,
                SimplePackerPayload.STREAM_CODEC,
                SimplePackerPayload::handle);
    }
}
