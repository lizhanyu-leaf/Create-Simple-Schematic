package com.leaf.createsimpleschematic.content;

import com.leaf.createsimpleschematic.CreateSimpleSchematic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

public class SimpleSchematicItem extends Item {

    public SimpleSchematicItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        String name = super.getName(stack).getString();
        String key = getTranslateKey(stack);
        if (key == null) {
            return Component.literal(name)
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        } else {
            return Component.literal(name)
                    .withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("item.createsimpleschematic.simple_schematic.dash")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable(key)
                            .withStyle(ChatFormatting.GOLD));
        }
    }

    public static String getTranslateKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("File")) {
                String fileName = tag.getString("File").strip();
                if (fileName.contains(".nbt")) {
                    String cleanName = fileName.replaceAll("§[0-9a-fk-or]", "");
                    return cleanName.endsWith(".nbt") ? cleanName.substring(0, cleanName.length() - 4) : cleanName;
                }
            }
        }

        return null;
    }

    public static StructureTemplate loadSchematic(HolderGetter<Block> lookup, ItemStack blueprint) {
        StructureTemplate t = new StructureTemplate();
        CompoundTag tag = blueprint.getTag();
        if (tag == null)
            return t;
        String schematic = tag.getString("File");

        if (!schematic.endsWith(".nbt"))
            return t;

        Path dir = Paths.get("schematics").toAbsolutePath();
        Path file = Paths.get(schematic);

        Path path = dir.resolve(file).normalize();
        if (!path.startsWith(dir))
            return t;

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
            CompoundTag nbt = NbtIo.read(stream, new NbtAccounter(0x20000000L));
            t.load(lookup, nbt);
        } catch (IOException e) {
            CreateSimpleSchematic.LOGGER.warn("Failed to read schematic", e);
        }

        return t;
    }
}
