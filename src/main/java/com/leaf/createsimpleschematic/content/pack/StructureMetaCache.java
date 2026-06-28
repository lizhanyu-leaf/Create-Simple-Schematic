package com.leaf.createsimpleschematic.content.pack;

import com.leaf.createsimpleschematic.CreateSimpleSchematic;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class StructureMetaCache {
    private static final ExecutorService IO_EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("AndesiteAbound-Schematics-Scan");
        thread.setDaemon(true);
        return thread;
    });

    private static final ConcurrentMap<Path, StructureMeta> metaCache = new ConcurrentHashMap<>();
    private static final AtomicLong timestamp = new AtomicLong();
    private static final int SCAN_MIN_INTERVAL = 20;

    public enum MatchResult {
        SUCCESS, SIZE_ERROR, BLOCK_ERROR
    }

    public static void matchAnyStructure(
            Level world, BlockPos anchor, Vec3i size,
            @NotNull BiConsumer<Path, SchematicLevel> success, Consumer<MatchResult> fail
    ) {
        if (timestamp.getAndAdd(SCAN_MIN_INTERVAL) > world.getGameTime()) {
            matchInner(world, anchor, size, success, fail);
            return;
        }
        timestamp.set(world.getGameTime());

        HolderLookup<Block> lookup = world.holderLookup(Registries.BLOCK);
        CompletableFuture
                .runAsync(() -> updateAllCache(lookup), IO_EXEC)
                .thenAccept($ -> {
                    if (world.getServer() == null)
                        return;
                    world.getServer().execute(() -> {
                        metaCache.values().forEach(v -> v.calcFeature(world));
                        metaCache.values().removeIf(v -> v.feature == null);
                        matchInner(world, anchor, size, success, fail);
                    });
                });
    }

    private static void matchInner(
            Level world, BlockPos anchor, Vec3i size,
            @NotNull BiConsumer<Path, SchematicLevel> success, Consumer<MatchResult> fail
    ) {
        Vec3i length = size.offset(-1, -1, -1);
        Long2ObjectMap<BlockInWorld> blockCache = new Long2ObjectOpenHashMap<>();
        boolean sizeMatched = false;
        for (var cacheEntry : metaCache.entrySet()) {
            for (Rotation rotation: Rotation.values()) {
                if (!matchRotatedSize(size, cacheEntry.getValue().template.getSize(), rotation))
                    continue;
                sizeMatched = true;
                if (!matchRotatedFeature(world, anchor, length, rotation, cacheEntry.getValue().feature, blockCache))
                    continue;

                SchematicLevel blockReader = new SchematicLevel(world);
                StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
                cacheEntry.getValue().template.placeInWorld(blockReader, backToZero(length, rotation), BlockPos.ZERO,
                        settings, blockReader.getRandom(), Block.UPDATE_CLIENTS);
                boolean match = true;
                for (var blockEntry: blockReader.getBlockMap().entrySet()) {
                    BlockPos targetPos = anchor.offset(blockEntry.getKey());
                    BlockInWorld inWorld = blockCache.computeIfAbsent(targetPos.asLong(),
                            k -> new BlockInWorld(world, targetPos, false));
                    BlockState state = inWorld.getState();
                    BlockEntity entity = inWorld.getEntity();
                    // noinspection ConstantValue
                    if (state == null || !state.is(blockEntry.getValue().getBlock()) ||
                            (!isIgnoredBlockEntity(entity) && !matchPropertiesIgnoreRotation(blockEntry.getValue(), state))
                    ) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    Path file = cacheEntry.getKey().subpath(1, cacheEntry.getKey().getNameCount());
                    success.accept(file, blockReader);
                    return;
                }
            }
        }
        if (fail != null) {
            fail.accept(sizeMatched ? MatchResult.BLOCK_ERROR : MatchResult.SIZE_ERROR);
        }
    }

    private static boolean matchRotatedSize(Vec3i size, Vec3i size2, Rotation rotation) {
        return switch (rotation) {
            case NONE, CLOCKWISE_180 -> size.equals(size2);
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> size.getY() == size2.getY()
                    && size.getX() == size2.getZ() && size.getZ() == size2.getX();
        };
    }

    private static boolean matchRotatedFeature(
            Level world, BlockPos anchor, Vec3i length, Rotation rotation,
            List<Pair<BlockPos, Block>> feature, Long2ObjectMap<BlockInWorld> blockCache
    ) {
        for (var pair: feature) {
            BlockPos targetPos = transform(anchor, length, pair.getFirst(), rotation);
            BlockState state = blockCache.computeIfAbsent(targetPos.asLong(),
                    k -> new BlockInWorld(world, targetPos, false)).getState();
            // noinspection ConstantValue
            if (state == null || !state.is(pair.getSecond()))
                return false;
        }
        return true;
    }

    private static boolean isIgnoredBlockEntity(BlockEntity entity) {
        return entity instanceof FluidTankBlockEntity;
    }

    private static final Set<Property<?>> ignoredProperties = Set.of(
            BlockStateProperties.FACING, BlockStateProperties.AXIS,
            BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HORIZONTAL_AXIS,
            BlockStateProperties.UP, BlockStateProperties.DOWN, BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST
    );

    private static boolean matchPropertiesIgnoreRotation(BlockState state1, BlockState state2) {
        try {
            for (Property<?> property: state1.getProperties()) {
                if (ignoredProperties.contains(property))
                    continue;
                if (state1.getValue(property) != state2.getValue(property))
                    return false;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private static BlockPos transform(BlockPos anchor, Vec3i length, Vec3i pos, Rotation rotation) {
        return switch (rotation) {
            case NONE -> anchor.offset(pos);
            case CLOCKWISE_90 -> anchor.offset(length.getX() - pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> anchor.offset(length.getX() - pos.getX(), pos.getY(), length.getZ() - pos.getZ());
            case COUNTERCLOCKWISE_90 -> anchor.offset(pos.getZ(), pos.getY(), length.getZ() - pos.getX());
        };
    }

    private static BlockPos backToZero(Vec3i length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> BlockPos.ZERO;
            case CLOCKWISE_90 -> new BlockPos(length.getX(), 0, 0);
            case CLOCKWISE_180 -> new BlockPos(length.getX(), 0, length.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(0, 0, length.getZ());
        };
    }

    private static void updateAllCache(HolderLookup<Block> lookup) {
        Path root = Path.of("schematics");
        Path skip = root.resolve("uploaded");
        Set<Path> visited = new HashSet<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>(){
                @ParametersAreNonnullByDefault
                @Override
                public @NotNull FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(skip))
                        return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @ParametersAreNonnullByDefault
                @Override
                public @NotNull FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".nbt"))
                        if (updateCache(lookup, file, attrs))
                            visited.add(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}
        metaCache.keySet().retainAll(visited);
    }

    private static boolean updateCache(HolderLookup<Block> lookup, Path file, BasicFileAttributes attrs) {
        FileTime time = attrs.lastModifiedTime();
        StructureMeta meta = metaCache.get(file);
        if (meta != null && meta.timestamp.equals(time))
            return true;
        StructureTemplate template = new StructureTemplate();
        if (!loadTemplate(lookup, file, template))
            return false;
        meta = StructureMeta.of(template, time);
        if (meta == null)
            return false;
        metaCache.put(file, meta);
        return true;
    }

    private static boolean loadTemplate(HolderLookup<Block> lookup, Path file, StructureTemplate template) {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(file, StandardOpenOption.READ)))))
        {
            CompoundTag nbt = NbtIo.read(stream, new NbtAccounter(0x20000000L));
            template.load(lookup, nbt);
            return true;
        } catch (IOException e) {
            CreateSimpleSchematic.LOGGER.warn("Failed to read schematic", e);
            return false;
        }
    }

    static class StructureMeta {
        FileTime timestamp;
        StructureTemplate template;
        List<Pair<BlockPos, Block>> feature;

        static StructureMeta of(StructureTemplate template, FileTime timestamp) {
            Vec3i size = template.getSize();
            if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
                return null;

            StructureMeta meta = new StructureMeta();
            meta.timestamp = timestamp;
            meta.template = template;

            return meta;
        }

        void calcFeature(Level world) {
            if (feature != null)
                return;
            SchematicLevel blockReader = new SchematicLevel(world);
            StructurePlaceSettings settings = new StructurePlaceSettings();
            template.placeInWorld(
                    blockReader, BlockPos.ZERO, BlockPos.ZERO, settings, blockReader.getRandom(), Block.UPDATE_CLIENTS);
            if (blockReader.getBlockMap().isEmpty() && blockReader.getEntityList().stream().findAny().isEmpty())
                return;

            Map<Block, ArrayList<BlockPos>> invertedMap = new HashMap<>();
            for (var entry: blockReader.getBlockMap().entrySet()) {
                invertedMap.computeIfAbsent(entry.getValue().getBlock(), k -> new ArrayList<>())
                        .add(entry.getKey());
            }

            feature = blockReader.getBlockMap().entrySet().stream()
                    .sorted(Comparator.comparingInt((e -> {
                        int depth = calcDepth(e.getKey(), template.getSize().offset(-1, -1, -1));
                        int amount = invertedMap.get(e.getValue().getBlock()).size();
                        return depth * invertedMap.size() + amount;
                    })))
                    .limit(8)
                    .map(e -> Pair.of(e.getKey(), e.getValue().getBlock()))
                    .toList();
        }

        static int calcDepth(Vec3i pos, Vec3i length) {
            Vec3i diff = length.subtract(pos);
            return Math.min(pos.getX(), diff.getX())
                    + Math.min(pos.getY(), diff.getY())
                    + Math.min(pos.getZ(), diff.getZ());
        }
    }
}