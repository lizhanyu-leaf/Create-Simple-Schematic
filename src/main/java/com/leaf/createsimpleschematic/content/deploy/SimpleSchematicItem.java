package com.leaf.createsimpleschematic.content.deploy;

import com.leaf.createsimpleschematic.CreateSimpleSchematic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /** 读取物品上的蓝图文件名（去掉 .nbt 后缀与格式化代码），无蓝图时返回 null。 */
    @Nullable
    public static String getTranslateKey(ItemStack stack) {
        String fileName = getSchematicFile(stack);
        if (fileName == null) return null;

        fileName = fileName.strip();
        if (fileName.contains(".nbt")) {
            String cleanName = fileName.replaceAll("§[0-9a-fk-or]", "");
            return cleanName.endsWith(".nbt") ? cleanName.substring(0, cleanName.length() - 4) : cleanName;
        }
        return null;
    }

    /** 蓝图文件名存储在 CUSTOM_DATA 组件的 "File" 键下。 */
    @Nullable
    public static String getSchematicFile(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        if (tag.contains("File")) {
            String schematic = tag.getString("File");
            return schematic.isEmpty() ? null : schematic;
        }
        return null;
    }

    public static StructureTemplate loadSchematic(HolderGetter<Block> lookup, ItemStack blueprint) {
        StructureTemplate t = new StructureTemplate();
        String schematic = getSchematicFile(blueprint);
        if (schematic == null)
            return t;

        if (!schematic.endsWith(".nbt"))
            return t;

        Path dir = Paths.get("schematics").toAbsolutePath();
        Path file = Paths.get(schematic);

        Path path = dir.resolve(file).normalize();
        if (!path.startsWith(dir))
            return t;

        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
            CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(0x20000000L));
            t.load(lookup, nbt);
        } catch (IOException e) {
            CreateSimpleSchematic.LOGGER.warn("Failed to read schematic", e);
        }

        return t;
    }
}
