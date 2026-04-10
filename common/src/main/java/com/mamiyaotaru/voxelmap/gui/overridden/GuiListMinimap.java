package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

import java.time.Duration;
import java.util.ArrayList;

public abstract class GuiListMinimap<E extends GuiListMinimap.Entry<E>> extends AbstractSelectionList<E> {
    private boolean doubleClicked;
    private long lastClicked;
    private Entry<E> lastSelected;

    public GuiListMinimap(int x, int y, int width, int height, int itemHeight) {
        super(Minecraft.getInstance(), width, height, 0, itemHeight);
        setPosition(x, y);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        Entry<E> selected = getEntryAtPosition(mouseButtonEvent.x(), mouseButtonEvent.y());
        doubleClicked = false;
        if (selected != null && selected == lastSelected) {
            if (System.currentTimeMillis() - lastClicked < 250L) {
                doubleClicked = true;
            }
        }
        lastClicked = System.currentTimeMillis();
        lastSelected = selected;

        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    protected boolean doubleClicked() {
        return doubleClicked;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public static abstract class Entry<E extends Entry<E>> extends AbstractSelectionList.Entry<E> {
        private final ArrayList<Renderable> renderables = new ArrayList<>();
        private final ArrayList<GuiEventListener> interactables = new ArrayList<>();
        private final WidgetTooltipHolder tooltipHolder = new WidgetTooltipHolder();
        private final GuiListMinimap<E> selectionList;

        public Entry(GuiListMinimap<E> selectionList) {
            this.selectionList = selectionList;
        }

        protected GuiListMinimap<E> getSelectionList() {
            return selectionList;
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovered, float delta) {
            for (Renderable renderable : renderables) {
                renderable.render(guiGraphics, mouseX, mouseY, delta);
            }

            tooltipHolder.refreshTooltipForNextRenderPass(guiGraphics, mouseX, mouseY, canDisplayTooltip(mouseX, mouseY), isFocused(), getRectangle());
        }

        protected <T extends Renderable & GuiEventListener> void addWidget(T widget) {
            renderables.add(widget);
            interactables.add(widget);
        }

        protected <T extends Renderable & GuiEventListener> void removeWidget(T widget) {
            renderables.remove(widget);
            interactables.remove(widget);
        }

        protected boolean canDisplayTooltip(int mouseX, int mouseY) {
            return getRectangle().containsPoint(mouseX, mouseY) && !anyHovered(mouseX, mouseY);
        }

        public void setTooltip(Tooltip tooltip) {
            tooltipHolder.set(tooltip);
        }

        public void setTooltipDelay(Duration duration) {
            tooltipHolder.setDelay(duration);
        }

        public boolean anyHovered(int mouseX, int mouseY) {
            for (GuiEventListener interactable : interactables) {
                if (interactable.isMouseOver(mouseX, mouseY)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (!getRectangle().containsPoint((int) mouseX, (int) mouseY)) {
                return false;
            }
            getSelectionList().setSelected((E) this);
            boolean anyClicked = false;
            for (GuiEventListener interactable : interactables) {
                anyClicked = anyClicked || interactable.mouseClicked(mouseButtonEvent, doubleClick);
            }
            if (!anyClicked) {
                onClick(mouseButtonEvent, doubleClick);
            }
            return true;
        }

        public void onClick(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        }
    }
}
