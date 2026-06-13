package com.leaf.createsimpleschematic.content.tools;

import com.simibubi.create.content.schematics.client.tools.*;
import com.leaf.createsimpleschematic.AllConfig;

import java.util.List;

public enum SimpleToolType {

    DEPLOY(new SimpleDeployTool()),
    MOVE(new SimpleMoveTool()),
    MOVE_Y(new SimpleMoveVerticalTool()),
    ROTATE(new SimpleRotateTool()),
    FLIP(new SimpleFlipTool()),
    PRINT(new SimplePlaceTool());

    private final ISchematicTool tool;

    SimpleToolType(ISchematicTool tool) {
        this.tool = tool;
    }

    public ISchematicTool getTool() {
        return tool;
    }

    public static List<ToolType> getTools() {
        // 检查是否启用翻转工具
        if (AllConfig.enable_simple_schematic_flip_tool) {
            return List.of(
                    ToolType.MOVE, ToolType.MOVE_Y, ToolType.DEPLOY, ToolType.ROTATE, ToolType.PRINT, ToolType.FLIP);
        }
        return List.of(ToolType.MOVE, ToolType.MOVE_Y, ToolType.DEPLOY, ToolType.ROTATE, ToolType.PRINT);
    }

    public static SimpleToolType of(ToolType toolType) {
        return SimpleToolType.values()[toolType.ordinal()];
    }

    public ToolType getToolType() {
        return ToolType.values()[this.ordinal()];
    }
}
