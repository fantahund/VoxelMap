package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiIconButton;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiListMinimap;
import com.mamiyaotaru.voxelmap.options.containers.RadarOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Map;

public class GuiMobs extends GuiScreenMinimap {
    private final RadarOptions options;
    private MobList mobsList;
    private Button buttonEnable;
    private Button buttonDisable;
    private EditBox filter;
    private Identifier selectedMobId;

    public GuiMobs(Screen parentGui) {
        super(parentGui, Component.translatable("options.minimap.mobs.title"));
        options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        mobsList = new MobList(0, 40, getWidth(), getHeight() - 110);
        addRenderableWidget(mobsList);

        filter = new EditBox(getFont(), getWidth() / 2 - 150, getHeight() - 54, 300, 20, Component.empty());
        filter.setHint(Component.translatable("gui.selectWorld.search").withStyle(ChatFormatting.GRAY));
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);
        setFocused(filter);
        addRenderableWidget(filter);

        addRenderableWidget(buttonEnable = new Button.Builder(Component.translatable("options.minimap.mobs.enable"), button -> setMobEnabled(selectedMobId, true)).bounds(getWidth() / 2 - 154, getHeight() - 26, 100, 20).build());
        addRenderableWidget(buttonDisable = new Button.Builder(Component.translatable("options.minimap.mobs.disable"), button -> setMobEnabled(selectedMobId, false)).bounds(getWidth() / 2 - 50, getHeight() - 26, 100, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 + 4 + 50, getHeight() - 26, 100, 20).build());
    }

    private void filterUpdated(String string) {
        mobsList.updateFilter(string.toLowerCase());
    }

    private void setSelectedMob(Identifier id) {
        selectedMobId = id;
    }

    private boolean isMobEnabled(Identifier mobId) {
        return !options.hiddenMobs.contains(mobId);
    }

    private void setMobEnabled(Identifier mobId, boolean enabled) {
        if (enabled) {
            options.hiddenMobs.remove(mobId);
        } else {
            options.hiddenMobs.add(mobId);
        }
    }

    private void toggleMobVisibility() {
        setMobEnabled(selectedMobId, !isMobEnabled(selectedMobId));
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        boolean isSomethingSelected = selectedMobId != null;
        buttonEnable.active = isSomethingSelected && !isMobEnabled(selectedMobId);
        buttonDisable.active = isSomethingSelected && isMobEnabled(selectedMobId);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    class MobList extends GuiListMinimap<MobList.Entry> {
        private static final Tooltip TOOLTIP_ENABLED = Tooltip.create(Component.translatable("options.minimap.mobs.enabled"));
        private static final Tooltip TOOLTIP_DISABLED = Tooltip.create(Component.translatable("options.minimap.mobs.disabled"));
        private static final Tooltip TOOLTIP_CLICK_TO_ENABLE = Tooltip.create(Component.translatable("options.minimap.mobs.enableTooltip"));
        private static final Tooltip TOOLTIP_CLICK_TO_DISABLE = Tooltip.create(Component.translatable("options.minimap.mobs.disableTooltip"));

        private final ArrayList<Entry> mobs;

        public MobList(int x, int y, int width, int height) {
            super(x, y, width, height, 18);

            OptionEnumRadar.ShowMobs mobsFlag = options.showMobs.get();
            mobs = new ArrayList<>();
            for (Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>> mobEntry : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
                Identifier id = mobEntry.getKey().identifier();
                EntityType<?> type = mobEntry.getValue();

                if (type.create(VoxelConstants.getMinecraft().level, EntitySpawnReason.LOAD) instanceof LivingEntity) {
                    VoxelMapMobCategory category = VoxelMapMobCategory.forEntityType(type);

                    if (mobsFlag == OptionEnumRadar.ShowMobs.OFF) continue;
                    if ((mobsFlag == OptionEnumRadar.ShowMobs.HOSTILES) && category != VoxelMapMobCategory.HOSTILE) continue;
                    if ((mobsFlag == OptionEnumRadar.ShowMobs.NEUTRALS) && category != VoxelMapMobCategory.NEUTRAL) continue;

                    mobs.add(new Entry(type, id));
                }
            }

            mobs.sort((mob1, mob2) -> {
                int dcat = mob1.category.compareTo(mob2.category);
                if (dcat != 0) {
                    return dcat;
                }
                return String.CASE_INSENSITIVE_ORDER.compare(mob1.name, mob2.name);
            });

            mobs.forEach(this::addEntry);
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            if (getSelected() != null) {
                GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
                narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().name));
            }

            setSelectedMob(entry.id);
        }

        private void updateFilter(String filterString) {
            setScrollAmount(0.0);
            clearEntries();

            for (Entry entry : mobs) {
                if (entry.name.toLowerCase().contains(filterString)) {
                    if (entry.id.equals(selectedMobId)) {
                        setSelectedMob(null);
                    }

                    addEntry(entry);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        public class Entry extends GuiListMinimap.Entry<Entry> {
            private final EntityType<?> type;
            private final Identifier id;
            private final String name;
            private final VoxelMapMobCategory category;
            private final GuiIconButton mobIcon;
            private final GuiIconButton mobToggle;

            private Sprite mobIconSprite;

            public Entry(EntityType<?> type, Identifier id) {
                super(MobList.this);

                this.type = type;
                this.id = id;
                name = type.getDescription().getString();
                category = VoxelMapMobCategory.forEntityType(type);

                addWidget(mobIcon = new GuiIconButton(0, 0, 18, 18, element -> {}));
                addWidget(mobToggle = new GuiIconButton(0, 0, 18, 18, element -> toggleMobVisibility()));
            }

            @Override
            public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                super.renderContent(drawContext, mouseX, mouseY, hovered, tickDelta);

                boolean isHostile = category == VoxelMapMobCategory.HOSTILE;
                boolean isNeutral = !isHostile;
                boolean isEnabled = options.isMobEnabled(type);

                int red = isHostile ? 255 : 0;
                int green = isNeutral ? 255 : 0;
                int color = 0xFF000000 + (red << 16) + (green << 8);
                drawContext.drawCenteredString(getFont(), name, GuiMobs.this.getWidth() / 2, getY() + 5, color);

                if (mobIconSprite == null) {
                    mobIconSprite = VoxelConstants.getVoxelMapInstance().getEntityMapImageManager().requestImageForMobType(type, true);
                } else {
                    int iconWidth = Math.min(18, mobIconSprite.getIconWidth() / 3);
                    int iconHeight = Math.min(18, mobIconSprite.getIconHeight() / 3);
                    mobIcon.setIcon(mobIconSprite, iconWidth, iconHeight, 0xFFFFFFFF);
                }
                mobIcon.setPosition(getX() + 2, getY());
                mobIcon.setTooltip(isEnabled ? TOOLTIP_ENABLED : TOOLTIP_DISABLED);

                mobToggle.setPosition(getX() + getWidth() - 20, getY());
                mobToggle.setIcon(isEnabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
                mobToggle.setTooltip( isEnabled ? TOOLTIP_CLICK_TO_DISABLE : TOOLTIP_CLICK_TO_ENABLE);
            }
        }
    }
}
