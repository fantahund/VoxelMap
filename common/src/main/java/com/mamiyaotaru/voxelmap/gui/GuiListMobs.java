package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.Radar;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconElement;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;

class GuiListMobs extends AbstractSelectionList<GuiListMobs.MobItem> {
    private final ArrayList<MobItem> mobs;
    private ArrayList<Entry<?>> mobsFiltered;
    private final GuiMobs parentGui;
    private final RadarSettingsManager options;

    static final Component ENABLED = Component.translatable("options.minimap.mobs.enabled");
    static final Component DISABLED = Component.translatable("options.minimap.mobs.disabled");
    static final Component TOOLTIP_ENABLE = Component.translatable("options.minimap.mobs.enableTooltip");
    static final Component TOOLTIP_DISABLE = Component.translatable("options.minimap.mobs.disableTooltip");

    GuiListMobs(GuiMobs parentGui) {
        super(VoxelConstants.getMinecraft(), parentGui.getWidth(), parentGui.getHeight() - 110, 40, 18);

        this.parentGui = parentGui;
        options = parentGui.options;

        mobs = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            if (entry.getValue().create(VoxelConstants.getMinecraft().level, EntitySpawnReason.LOAD) instanceof LivingEntity) {
                VoxelMapMobCategory category = VoxelMapMobCategory.forEntityType(entry.getValue());
                if ((category == VoxelMapMobCategory.HOSTILE && options.showHostiles) || (category == VoxelMapMobCategory.NEUTRAL && options.showNeutrals)) {
                    mobs.add(new MobItem(parentGui, entry.getValue(), entry.getKey().identifier()));
                }
            }
        });

        mobs.sort((mob1, mob2) -> {
            int dcat = mob1.category.compareTo(mob2.category);
            if (dcat != 0) {
                return dcat;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(mob1.nameString, mob2.nameString);
        });

        mobsFiltered = new ArrayList<>(mobs);
        mobsFiltered.forEach(x -> addEntry((MobItem) x));
    }

    @Override
    public void setSelected(MobItem entry) {
        super.setSelected(entry);
        if (getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().name));
        }

        parentGui.setSelectedMob(entry.id);
    }

    protected void updateFilter(String filterString) {
        setScrollAmount(0.0);
        clearEntries();

        mobsFiltered = new ArrayList<>(mobs);
        Iterator<?> iterator = mobsFiltered.iterator();

        while (iterator.hasNext()) {
            MobItem entry = (MobItem) iterator.next();
            if (!entry.nameString.toLowerCase().contains(filterString)) {
                if (entry.id.equals(parentGui.selectedMobId)) {
                    parentGui.setSelectedMob(null);
                }

                iterator.remove();
            }
        }

        mobsFiltered.forEach(x -> addEntry((MobItem) x));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public class MobItem extends AbstractSelectionList.Entry<MobItem> {
        private final GuiMobs parentGui;
        private final EntityType<?> type;
        private final Identifier id;
        private final Component name;
        private final String nameString;
        private final VoxelMapMobCategory category;
        private final GuiIconElement mobIcon;
        private final GuiIconElement mobToggle;
        private Sprite mobIconSprite;

        protected MobItem(GuiMobs mobsScreen, EntityType<?> type, Identifier id) {
            parentGui = mobsScreen;

            this.type = type;
            this.id = id;
            name = type.getDescription();
            nameString = name.getString();
            category = VoxelMapMobCategory.forEntityType(type);

            mobIcon = new GuiIconElement(getX() + 2, getY(), 18, 18, (element) -> {});
            mobToggle = new GuiIconElement(getX() + getWidth() - 20, getY(), 18, 18, (element) -> parentGui.toggleMobVisibility());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean isHostile = category == VoxelMapMobCategory.HOSTILE;
            boolean isNeutral = !isHostile;
            boolean isEnabled = parentGui.options.isMobEnabled(type);

            int red = isHostile ? 255 : 0;
            int green = isNeutral ? 255 : 0;
            int color = 0xFF000000 + (red << 16) + (green << 8);
            drawContext.drawCenteredString(parentGui.getFont(), name, parentGui.getWidth() / 2, getY() + 5, color);

            if (mobIconSprite == null) {
                Radar radar = VoxelConstants.getVoxelMapInstance().getFullRadar();
                if (radar != null) {
                    mobIconSprite = radar.getEntityMapImageManager().requestImageForMobType(type, true);
                }
            } else {
                int iconWidth = Math.min(18, mobIconSprite.getIconWidth() / 3);
                int iconHeight = Math.min(18, mobIconSprite.getIconHeight() / 3);
                mobIcon.setPosition(getX() + 2, getY());
                mobIcon.setIcon(mobIconSprite, iconWidth, iconHeight, 0xFFFFFFFF);
                mobIcon.render(drawContext, mouseX, mouseY, tickDelta);
            }

            mobToggle.setPosition(getX() + getWidth() - 20, getY());
            mobToggle.setIcon(isEnabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
            mobToggle.render(drawContext, mouseX, mouseY, tickDelta);

            if (mobIcon.isMouseOver(mouseX, mouseY)) {
                parentGui.setTooltip(isEnabled ? GuiListMobs.ENABLED : GuiListMobs.DISABLED);
            } else if (mobToggle.isMouseOver(mouseX, mouseY)) {
                parentGui.setTooltip(isEnabled ? GuiListMobs.TOOLTIP_DISABLE : GuiListMobs.TOOLTIP_ENABLE);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mouseY < getY() || mouseY > getBottom()) {
                return false;
            }

            setSelected(this);

            mobIcon.mouseClicked(mouseButtonEvent, doubleClick);
            mobToggle.mouseClicked(mouseButtonEvent, doubleClick);

            return true;
        }
    }
}
