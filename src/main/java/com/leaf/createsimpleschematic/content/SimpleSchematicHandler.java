package com.leaf.createsimpleschematic.content;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicProcessor;
import com.simibubi.create.content.schematics.SchematicWorld;
import com.simibubi.create.content.schematics.client.*;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.outliner.AABBOutline;
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import com.leaf.createsimpleschematic.AllItems;
import com.leaf.createsimpleschematic.AllPackets;
import com.leaf.createsimpleschematic.CreateAndesiteAbound;
import com.leaf.createsimpleschematic.content.tools.SimpleToolType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.List;
import java.util.Vector;

public class SimpleSchematicHandler extends SchematicHandler {

    public static final SimpleSchematicHandler SIMPLE_SCHEMATIC_HANDLER = new SimpleSchematicHandler();

    private SchematicTransformation transformation;
    private AABB bounds;
    private boolean deployed;
    private boolean active;
    private SimpleToolType currentTool;

    private ItemStack activeSchematicItem;
    private AABBOutline outline;

    private final Vector<SchematicRenderer> renderers;
    private final SchematicHotbarSlotOverlay overlay;
    private ToolSelectionScreen selectionScreen;

    public SimpleSchematicHandler() {
        renderers = new Vector<>(3);
        for (int i = 0; i < renderers.capacity(); i++)
            renderers.add(new SchematicRenderer());

        overlay = new SchematicHotbarSlotOverlay();
        currentTool = SimpleToolType.DEPLOY;
        selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
        transformation = new SchematicTransformation();
    }

    @Override
    public void tick() {
        // 仅接受非观察模式玩家
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gameMode == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            if (active) {
                activeSchematicItem = null;
                setInactive();
            }
            return;
        }

        // 动画与渲染器 tick
        if (activeSchematicItem != null && transformation != null)
            transformation.tick();

        renderers.forEach(SchematicRenderer::tick);

        // 检查玩家手持物，设置渲染状态
        ItemStack stackBefore = activeSchematicItem;
        ItemStack stack = findBlueprintInHand(player);
        // 未手持蓝图时，关闭渲染并返回
        if (stack == null) {
            if (activeSchematicItem != null && itemLost(player)) {
                activeSchematicItem = null;
            }
            setInactive();
            return;
        }

        // 玩家手持全新的蓝图物品时，重置部署位置等所有状态
        if (stackBefore == null || !ItemStack.isSameItemSameTags(stack, stackBefore)) {
            setInactive();
            active = true;
            deployed = false;

            // noinspection DataFlowIssue
            StructureTemplate template = SimpleSchematicItem.loadSchematic(
                    Minecraft.getInstance().level.holderLookup(Registries.BLOCK), stack);
            Vec3i size = template.getSize();
            bounds = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());

            outline = new AABBOutline(bounds);
            outline.getParams().colored(0x32CD32).lineWidth(1 / 16f);

            StructurePlaceSettings settings = new StructurePlaceSettings();
            settings.addProcessor(SchematicProcessor.INSTANCE);
            transformation = new SchematicTransformation();
            transformation.init(BlockPos.ZERO, settings, bounds);

            selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
        }
        // 玩家主手切换回上次的蓝图物品时
        else if (!active) {
            setInactive();
            active = true;

            setupRenderer();
            if (deployed) {
                ToolType toolBefore = currentTool.getToolType();
                selectionScreen = new SimpleToolSelectionScreen(SimpleToolType.getTools(), this::equip);
                if (toolBefore != null) {
                    selectionScreen.setSelectedElement(toolBefore);
                    equip(toolBefore);
                }
            } else {
                selectionScreen = new SimpleToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
            }
        }

        if (!active)
            return;

        // 工具菜单动画 tick
        selectionScreen.update();
        currentTool.getTool().updateSelection();
    }

    private void setupRenderer() {
        Level clientWorld = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (clientWorld == null || player == null)
            return;

        // 加载蓝图 nbt
        StructureTemplate schematic =
                SimpleSchematicItem.loadSchematic(clientWorld.holderLookup(Registries.BLOCK), activeSchematicItem);
        Vec3i size = schematic.getSize();
        if (size.equals(Vec3i.ZERO))
            return;

        // 创建蓝图世界
        SchematicWorld w = new SchematicWorld(clientWorld);
        SchematicWorld wMirroredFB = new SchematicWorld(clientWorld);
        SchematicWorld wMirroredLR = new SchematicWorld(clientWorld);
        StructurePlaceSettings placementSettings = new StructurePlaceSettings();

        // 放置到蓝图世界
        try {
            schematic.placeInWorld(w, BlockPos.ZERO, BlockPos.ZERO, placementSettings, w.getRandom(), Block.UPDATE_CLIENTS);
            for (BlockEntity blockEntity : w.getBlockEntities())
                blockEntity.setLevel(w);
            w.fixControllerBlockEntities();
        } catch (Exception e) {
            player.displayClientMessage(Lang.translate("schematic.error").component(), false);
            CreateAndesiteAbound.LOGGER.error("Failed to load Schematic for Previewing", e);
            return;
        }

        // 放置到镜像蓝图世界
        Couple.create(wMirroredFB, wMirroredLR).forEachWithContext((world, first) -> {
            StructureTransform transform;
            BlockPos pos;
            if (first) {
                placementSettings.setMirror(Mirror.FRONT_BACK);
                pos = BlockPos.ZERO.east(size.getX() - 1);
            } else {
                placementSettings.setMirror(Mirror.LEFT_RIGHT);
                pos = BlockPos.ZERO.south(size.getZ() - 1);
            }
            schematic.placeInWorld(world, pos, pos, placementSettings, world.getRandom(), Block.UPDATE_CLIENTS);
            transform = new StructureTransform(placementSettings.getRotationPivot(), Direction.Axis.Y, Rotation.NONE,
                    placementSettings.getMirror());
            for (BlockEntity be : world.getRenderedBlockEntities())
                transform.apply(be);
            world.fixControllerBlockEntities();
        });

        // 绑定到渲染器
        renderers.get(0).display(w);
        renderers.get(1).display(wMirroredFB);
        renderers.get(2).display(wMirroredLR);
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
        boolean present = activeSchematicItem != null;
        if (!active && !present)
            return;

        if (active) {
            ms.pushPose();
            currentTool.getTool().renderTool(ms, buffer, camera);
            ms.popPose();
        }

        ms.pushPose();
        transformation.applyTransformations(ms, camera);

        if (!renderers.isEmpty()) {
            float pt = AnimationTickHolder.getPartialTicks();
            boolean lr = transformation.getScaleLR().getValue(pt) < 0;
            boolean fb = transformation.getScaleFB().getValue(pt) < 0;
            if (lr && !fb)
                renderers.get(2).render(ms, buffer);
            else if (fb && !lr)
                renderers.get(1).render(ms, buffer);
            else
                renderers.get(0).render(ms, buffer);
        }

        if (active)
            currentTool.getTool().renderOnSchematic(ms, buffer);

        ms.popPose();
    }

    @Override
    public void updateRenderers() {
        for (SchematicRenderer renderer : renderers) {
            renderer.update();
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (mc.options.hideGui || !active || player == null)
            return;
        // 由于不再记录蓝图物品所在槽位，因此需要额外判断来防止切换物品后的短暂错误渲染
        if (activeSchematicItem != null && ItemStack.isSameItemSameTags(player.getMainHandItem(), activeSchematicItem)) {
            this.overlay.renderOn(graphics, player.getInventory().selected);
        }

        currentTool.getTool()
                .renderOverlay(gui, graphics, partialTicks, width, height);
        selectionScreen.renderPassive(graphics, partialTicks);
    }

    @Override
    public boolean onMouseInput(int button, boolean pressed) {
        if (!active || !pressed || button != 1)
            return false;

        return currentTool.getTool().handleRightClick();
    }

    @Override
    public void onKeyInput(int key, boolean pressed) {
        if (!active)
            return;
        if (key != AllKeys.TOOL_MENU.getBoundCode())
            return;

        if (pressed && !selectionScreen.focused)
            selectionScreen.focused = true;
        if (!pressed && selectionScreen.focused) {
            selectionScreen.focused = false;
            selectionScreen.onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double delta) {
        if (!active)
            return false;

        if (selectionScreen.focused) {
            selectionScreen.cycle((int) delta);
            return true;
        }
        if (AllKeys.ctrlDown())
            return currentTool.getTool().handleMouseWheel(delta);
        return false;
    }

    private ItemStack findBlueprintInHand(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty())
            return null;
        if (!AllItems.SIMPLE_SCHEMATIC.isIn(stack))
            return null;
        if (!stack.hasTag())
            return null;

        activeSchematicItem = stack;
        return stack;
    }

    private boolean itemLost(Player player) {
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.matches(stack, activeSchematicItem))
                continue;
            return false;
        }
        return true;
    }

    @Override
    public void equip(ToolType tool) {
        this.currentTool = SimpleToolType.of(tool);
        currentTool.getTool().init();
    }

    @Override
    public void deploy() {
        if (!deployed) {
            List<ToolType> tools = SimpleToolType.getTools();
            selectionScreen = new SimpleToolSelectionScreen(tools, this::equip);
        }
        deployed = true;
        setupRenderer();
    }

    @Override
    public void printInstantly() {
        // 发送打印操作
        AllPackets.getChannel().sendToServer(new SimpleSchematicPlacePacket(
                activeSchematicItem, transformation.getAnchor(), transformation.toSettings()));
        // 重置部署位置状态
        activeSchematicItem = null;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public AABB getBounds() {
        return bounds;
    }

    @Override
    public SchematicTransformation getTransformation() {
        return transformation;
    }

    @Override
    public boolean isDeployed() {
        return deployed;
    }

    @Override
    public AABBOutline getOutline() {
        return outline;
    }

    public void setInactive() {
        active = false;
        for (var it: renderers)
            it.setActive(false);
    }
}
