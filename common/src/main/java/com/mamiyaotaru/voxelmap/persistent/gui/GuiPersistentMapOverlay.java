package com.mamiyaotaru.voxelmap.persistent.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.persistent.PersistentMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GuiPersistentMapOverlay extends AbstractContainerWidget {
    private static final Identifier CAVE_ICON = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/worldmap/cave_button.png");
    private final ArrayList<AbstractWidget> widgets = new ArrayList<>();
    private final PersistentMap map;
    private OverlayElement currentOverlay;

    public GuiPersistentMapOverlay(PersistentMap map, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings(0));
        this.map = map;
        widgets.add(new OverlayButton(CAVE_ICON, x + getWidth() - 30, y + getHeight() - 30, 20, 20, (button) -> createPopup(button, 0)));
    }

    private void createPopup(Button button, int type) {
        if (currentOverlay != null) {
            widgets.remove(currentOverlay);
            currentOverlay = null;
        } else {
            int baseX = button.getX();
            int baseY = button.getY();
            switch (type) {
                case 0 -> currentOverlay = new GuiOverlayCaveMode(map, baseX - 130, baseY - 30, 120, 60);
            }
            widgets.add(currentOverlay);
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        for (AbstractWidget widget : widgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, delta);
        }
        isHovered = currentOverlay != null && currentOverlay.isHovered();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgets;
    }

    @Override
    protected int contentHeight() {
        return 0;
    }

    @Override
    protected double scrollRate() {
        return 0;
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    static class OverlayButton extends Button.Plain {
        private final Identifier icon;

        public OverlayButton(Identifier icon, int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractContents(graphics, mouseX, mouseY, delta);
            int width = getWidth() - 4;
            int height = getHeight() - 4;
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, getX() + 2, getY() + 2, 0.0F, 0.0F, width, height, width, height, 0xFFFFFFFF);
        }
    }

    public static class OverlayElement extends AbstractContainerWidget {
        private final ArrayList<AbstractWidget> widgets = new ArrayList<>();
        protected final PersistentMap map;

        public OverlayElement(PersistentMap map, int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), AbstractScrollArea.defaultSettings(0));
            this.map = map;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.fill(RenderPipelines.GUI, getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xBF000000);
            for (AbstractWidget widget : widgets) {
                widget.extractRenderState(graphics, mouseX, mouseY, delta);
            }
            isHovered = isMouseOver(mouseX, mouseY);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        public void addWidget(AbstractWidget widget) {
            widgets.add(widget);
        }

        public void removeWidget(AbstractWidget widget) {
            widgets.remove(widget);
        }

        @Override
        protected int contentHeight() {
            return 0;
        }

        @Override
        protected double scrollRate() {
            return 0;
        }

        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

}
