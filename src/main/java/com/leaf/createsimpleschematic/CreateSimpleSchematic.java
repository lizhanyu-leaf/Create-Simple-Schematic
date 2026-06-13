package com.leaf.createsimpleschematic;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateSimpleSchematic.MOD_ID)
public class CreateSimpleSchematic {
    public static final String MOD_ID = "createsimpleschematic";

    public static final Logger LOGGER = LogUtils.getLogger();

    static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    @SuppressWarnings("removal")
    public CreateSimpleSchematic() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        REGISTRATE.registerEventListeners(modEventBus);

        AllItems.register();
        AllPackets.registerPackets();
    }
}