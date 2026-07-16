package com.mamiyaotaru.voxelmap.gui.settings;

import com.mamiyaotaru.voxelmap.Radar;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconElement;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public final class EntityTypeDialog extends AbstractWidget implements ContainerEventHandler {
    private final GuiMinimapOptions screen;
    private final int dialogX;
    private final int dialogY;
    private final int dialogWidth;
    private final int dialogHeight;
    private final EditBox filter;
    private final EntityList list;
    private final Button done;
    private final List<? extends GuiEventListener> children;
    private GuiEventListener focused;
    private boolean dragging;

    public EntityTypeDialog(GuiMinimapOptions screen, Runnable closeAction) {
        super(0, 0, screen.getWidth(), screen.getHeight(), Component.translatable("options.voxelmap.entityDialog.title"));
        this.screen = screen;
        this.dialogWidth = Math.min(500, Math.max(180, screen.getWidth() - 24));
        this.dialogHeight = Math.min(360, Math.max(150, screen.getHeight() - 24));
        this.dialogX = (screen.getWidth() - dialogWidth) / 2;
        this.dialogY = (screen.getHeight() - dialogHeight) / 2;

        int innerX = dialogX + 12;
        int innerWidth = dialogWidth - 24;
        this.list = new EntityList(innerX, dialogY + 56, innerWidth, dialogHeight - 90);
        this.filter = new EditBox(screen.getFont(), innerX, dialogY + 30, innerWidth, 20, Component.translatable("options.voxelmap.entityDialog.search"));
        this.filter.setHint(Component.translatable("options.voxelmap.entityDialog.search"));
        this.filter.setMaxLength(128);
        this.filter.setResponder(list::filter);
        this.done = Button.builder(Component.translatable("gui.done"), ignored -> closeAction.run())
                .bounds(dialogX + dialogWidth / 2 - 75, dialogY + dialogHeight - 26, 150, 20).build();
        this.children = List.of(filter, list, done);
        setFocused(filter);
        filter.setFocused(true);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, screen.getWidth(), screen.getHeight(), 0x99000000);
        graphics.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF181818);
        graphics.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + 24, 0xFF303030);
        graphics.centeredText(screen.getFont(), getMessage().copy().withStyle(ChatFormatting.BOLD), screen.getWidth() / 2, dialogY + 8, 0xFFFFFFFF);

        filter.extractRenderState(graphics, mouseX, mouseY, delta);
        list.extractRenderState(graphics, mouseX, mouseY, delta);
        done.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        if (this.focused != null)
            this.focused.setFocused(false);
        this.focused = focused;
        if (focused != null)
            focused.setFocused(true);
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return ContainerEventHandler.super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return ContainerEventHandler.super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return ContainerEventHandler.super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> consumer) {
        consumer.accept(filter);
        consumer.accept(list);
        consumer.accept(done);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private final class EntityList extends AbstractSelectionList<EntityEntry> {
        private final List<EntityEntry> allEntries = new ArrayList<>();

        private EntityList(int x, int y, int width, int height) {
            super(VoxelConstants.getMinecraft(), width, height, y, 28);
            centerListVertically = false;
            updateSizeAndPosition(width, height, x, y);
            loadEntries();
            filter("");
        }

        private void loadEntries() {
            RadarSettingsManager radar = VoxelConstants.getVoxelMapInstance().getRadarOptions();
            if (VoxelConstants.getMinecraft().level == null)
                return;

            BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(registryEntry -> {
                EntityType<?> type = registryEntry.getValue();
                VoxelMapMobCategory category = VoxelMapMobCategory.forEntityType(type);
                boolean living = type.create(VoxelConstants.getMinecraft().level, EntitySpawnReason.LOAD) instanceof LivingEntity;
                if (EntityTypeSelection.includes(category, living, radar.showHostiles, radar.showNeutrals)) {
                    allEntries.add(new EntityEntry(type, registryEntry.getKey().identifier(), category));
                }
            });
            allEntries.sort(Comparator.comparing(EntityEntry::category).thenComparing(entry -> entry.name.getString(), String.CASE_INSENSITIVE_ORDER));
        }

        private void filter(String query) {
            clearEntries();
            setScrollAmount(0.0);
            for (EntityEntry entry : allEntries) {
                if (EntityTypeSelection.matches(entry.name.getString(), entry.id.toString(), query))
                    addEntry(entry);
            }
        }

        @Override
        public int getRowWidth() {
            return Math.max(120, getWidth() - 10);
        }

        @Override
        protected int scrollBarX() {
            return getX() + getWidth() - 6;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
            if (getItemCount() == 0) {
                graphics.centeredText(screen.getFont(), Component.translatable("options.voxelmap.entityDialog.empty"),
                        getX() + getWidth() / 2, getY() + 12, 0xFFA0A0A0);
            }
        }
    }

    private final class EntityEntry extends AbstractSelectionList.Entry<EntityEntry> {
        private final EntityType<?> type;
        private final Identifier id;
        private final VoxelMapMobCategory category;
        private final Component name;
        private final Button toggle;
        private final GuiIconElement icon;
        private Sprite sprite;
        private boolean requested;

        private EntityEntry(EntityType<?> type, Identifier id, VoxelMapMobCategory category) {
            this.type = type;
            this.id = id;
            this.category = category;
            this.name = type.getDescription();
            this.toggle = Button.builder(Component.empty(), ignored -> toggleVisibility()).bounds(0, 0, 92, 20).build();
            this.icon = new GuiIconElement(0, 0, 20, 20, false, ignored -> {
            });
        }

        private VoxelMapMobCategory category() {
            return category;
        }

        private void toggleVisibility() {
            RadarSettingsManager radar = VoxelConstants.getVoxelMapInstance().getRadarOptions();
            EntityTypeSelection.toggleHidden(radar.hiddenMobs, id);
            radar.markChanged();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            RadarSettingsManager radar = VoxelConstants.getVoxelMapInstance().getRadarOptions();
            boolean shown = !radar.hiddenMobs.contains(id);
            toggle.setRectangle(92, 20, getX() + getWidth() - 100, getY() + 4);
            toggle.setMessage(Component.translatable(shown ? "options.voxelmap.mob.shown" : "options.voxelmap.mob.hidden"));
            toggle.setTooltip(Tooltip.create(Component.translatable(shown ? "options.minimap.mobs.disableTooltip" : "options.minimap.mobs.enableTooltip")));

            if (!requested) {
                requested = true;
                Radar fullRadar = VoxelConstants.getVoxelMapInstance().getFullRadar();
                EntityMapImageManager manager = fullRadar == null ? null : fullRadar.getEntityMapImageManager();
                if (manager != null)
                    sprite = manager.requestImageForMobType(type, true);
            }
            if (sprite != null) {
                icon.setPosition(getX() + 8, getY() + 4);
                icon.setIconForRender(RenderPipelines.GUI_TEXTURED, sprite, Math.min(20, sprite.getIconWidth() / 3), Math.min(20, sprite.getIconHeight() / 3), 0xFFFFFFFF);
                icon.extractRenderState(graphics, mouseX, mouseY, delta);
            }

            Component categoryLabel = Component.translatable(category == VoxelMapMobCategory.HOSTILE
                    ? "options.voxelmap.entityCategory.hostile"
                    : "options.voxelmap.entityCategory.nonHostile");
            int categoryColor = category == VoxelMapMobCategory.HOSTILE ? 0xFFFF8080 : 0xFF80FF80;
            int categoryWidth = screen.getFont().width(categoryLabel);
            int categoryX = toggle.getX() - categoryWidth - 8;
            int nameWidth = Math.max(20, categoryX - getX() - 42);
            graphics.text(screen.getFont(), screen.getFont().plainSubstrByWidth(name.getString(), nameWidth), getX() + 34, getY() + 10, 0xFFFFFFFF);
            graphics.text(screen.getFont(), categoryLabel, categoryX, getY() + 10, categoryColor);
            toggle.extractRenderState(graphics, mouseX, mouseY, delta);
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> consumer) {
            consumer.accept(toggle);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return toggle.mouseClicked(event, doubleClick);
        }
    }
}
