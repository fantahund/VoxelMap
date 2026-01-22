package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiIconElement;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Iterator;

class GuiSlotMobs extends AbstractSelectionList<GuiSlotMobs.MobItem> {
    private final ArrayList<MobItem> mobs;
    private ArrayList<Entry<?>> mobsFiltered;
    final GuiMobs parentGui;
    static final Component ENABLED = Component.translatable("options.minimap.mobs.enabled");
    static final Component DISABLED = Component.translatable("options.minimap.mobs.disabled");
    static final Component TOOLTIP_ENABLE = Component.translatable("options.minimap.mobs.enableTooltip");
    static final Component TOOLTIP_DISABLE = Component.translatable("options.minimap.mobs.disableTooltip");

    GuiSlotMobs(GuiMobs par1GuiMobs) {
        super(VoxelConstants.getMinecraft(), par1GuiMobs.getWidth(), par1GuiMobs.getHeight() - 110, 40, 18);

        this.parentGui = par1GuiMobs;
        // RadarSettingsManager options = this.parentGui.options;
        this.mobs = new ArrayList<>();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            if (entry.getValue().create(Minecraft.getInstance().level, EntitySpawnReason.LOAD) instanceof LivingEntity) {
                this.mobs.add(new MobItem(this.parentGui, entry.getValue(), entry.getKey().identifier()));
            }
        });

        this.mobs.sort((mob1, mob2) -> {
            int dcat = mob1.category.compareTo(mob2.category);
            if (dcat != 0) {
                return dcat;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(mob1.nameString, mob2.nameString);
        });
        this.mobsFiltered = new ArrayList<>(this.mobs);
        this.mobsFiltered.forEach(x -> addEntry((MobItem) x));
    }

    @Override
    public void setSelected(MobItem entry) {
        super.setSelected(entry);
        if (this.getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayChatQueued(Component.translatable("narrator.select", this.getSelected().name));
        }

        this.parentGui.setSelectedMob(entry.id);
    }

    // FIXME 1.21.9
    // @Override
    // protected boolean isSelectedItem(int index) {
    // return ((MobItem) this.mobsFiltered.get(index)).id.equals(this.parentGui.selectedMobId);
    // }

    protected void updateFilter(String filterString) {
        this.clearEntries();
        this.mobsFiltered = new ArrayList<>(this.mobs);
        Iterator<?> iterator = this.mobsFiltered.iterator();

        while (iterator.hasNext()) {
            String mobName = ((MobItem) iterator.next()).nameString;
            if (!mobName.toLowerCase().contains(filterString)) {
                if (mobName.equals(this.parentGui.selectedMobId)) {
                    this.parentGui.setSelectedMob(null);
                }

                iterator.remove();
            }
        }

        this.mobsFiltered.forEach(x -> this.addEntry((MobItem) x));
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
        private final MobCategory category;
        private final GuiIconElement mobIcon;
        private final GuiIconElement mobToggle;

        protected MobItem(GuiMobs mobsScreen, EntityType<?> type, Identifier id) {
            this.type = type;
            this.parentGui = mobsScreen;
            this.id = id;
            this.name = type.getDescription();
            this.nameString = name.getString();
            this.category = MobCategory.forEntityType(type);
            this.mobIcon = new GuiIconElement(this.getX() + 2, this.getY(), 18, 18, false, (element) -> {});
            this.mobToggle = new GuiIconElement(this.getX() + this.getWidth() - 20, this.getY(), 18, 18, true, (element) -> this.parentGui.toggleMobVisibility());
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean isHostile = category == MobCategory.HOSTILE;
            boolean isNeutral = !isHostile;
            boolean isEnabled = VoxelMap.radarOptions.isMobEnabled(type);

            int red = isHostile ? 255 : 0;
            int green = isNeutral ? 255 : 0;
            int color = 0xFF000000 + (red << 16) + (green << 8);
            drawContext.drawCenteredString(this.parentGui.getFont(), this.name, this.parentGui.getWidth() / 2, getY() + 5, color);

            this.mobIcon.setPosition(this.getX() + 2, this.getY());
            Sprite sprite = VoxelConstants.getVoxelMapInstance().getNotSimpleRadar().getEntityMapImageManager().requestImageForMobType(type, true);
            if (sprite != null) {
                int width = Math.min(18, sprite.getIconWidth() / 3);
                int height = Math.min(18, sprite.getIconHeight() / 3);
                this.mobIcon.setIconSize(width, height);
                this.mobIcon.render(drawContext, mouseX, mouseY, RenderPipelines.GUI_TEXTURED, sprite, 0xFFFFFFFF);
            }

            Identifier toggleIcon = isEnabled ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture();
            this.mobToggle.setPosition(this.getX() + this.getWidth() - 20, this.getY());
            this.mobToggle.render(drawContext, mouseX, mouseY, RenderPipelines.GUI_TEXTURED, toggleIcon, 0xFFFFFFFF);

            if (this.mobIcon.getHovered(mouseX, mouseY)) {
                GuiMobs.setTooltip(this.parentGui, isEnabled ? GuiSlotMobs.ENABLED : GuiSlotMobs.DISABLED);
            } else if (this.mobToggle.getHovered(mouseX, mouseY)) {
                GuiMobs.setTooltip(this.parentGui, isEnabled ? GuiSlotMobs.TOOLTIP_ENABLE : GuiSlotMobs.TOOLTIP_DISABLE);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mouseY < GuiSlotMobs.this.getY() || mouseY > GuiSlotMobs.this.getBottom()) {
                return false;
            }

            GuiSlotMobs.this.setSelected(this);

            this.mobIcon.mouseClicked(mouseButtonEvent, doubleClick);
            this.mobToggle.mouseClicked(mouseButtonEvent, doubleClick);

            return true;
        }
    }
}
