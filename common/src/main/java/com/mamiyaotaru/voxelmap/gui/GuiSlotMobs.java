package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
// TODO: 1.20.1 Port - RenderPipelines doesn't exist in 1.20.1
// import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    final ResourceLocation visibleIconIdentifier = new ResourceLocation("textures/gui/sprites/container/beacon/confirm.png");
    final ResourceLocation invisibleIconIdentifier = new ResourceLocation("textures/gui/sprites/container/beacon/cancel.png");

    GuiSlotMobs(GuiMobs par1GuiMobs) {
        super(VoxelConstants.getMinecraft(), par1GuiMobs.getWidth(), par1GuiMobs.getHeight() - 110, 40, 18);

        this.parentGui = par1GuiMobs;
        // RadarSettingsManager options = this.parentGui.options;
        this.mobs = new ArrayList<>();

        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            if (entry.getValue().create(Minecraft.getInstance().level, EntitySpawnReason.LOAD) instanceof LivingEntity) {
                this.mobs.add(new MobItem(this.parentGui, entry.getValue(), entry.getKey().location()));
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
        private final ResourceLocation id;
        private final Component name;
        private final String nameString;
        private final MobCategory category;

        protected MobItem(GuiMobs mobsScreen, EntityType<?> type, ResourceLocation id) {
            this.type = type;
            this.parentGui = mobsScreen;
            this.id = id;
            this.name = type.getDescription();
            this.nameString = name.getString();
            this.category = MobCategory.forEntityType(type);
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
            byte padding = 3;
            if (mouseX >= getX() - padding && mouseY >= getY() && mouseX <= getX() + 215 + padding && mouseY <= getY() + GuiSlotMobs.this.defaultEntryHeight) {
                Component tooltip;
                if (mouseX >= getX() + 215 - 16 - padding && mouseX <= getX() + 215 + padding) {
                    drawContext.requestCursor(CursorTypes.POINTING_HAND);
                    tooltip = isEnabled ? GuiSlotMobs.TOOLTIP_DISABLE : GuiSlotMobs.TOOLTIP_ENABLE;
                } else {
                    tooltip = isEnabled ? GuiSlotMobs.ENABLED : GuiSlotMobs.DISABLED;
                }

                GuiMobs.setTooltip(this.parentGui, tooltip);
            }
            Sprite sprite = VoxelConstants.getVoxelMapInstance().getNotSimpleRadar().getEntityMapImageManager().requestImageForMobType(type, true);
            if (sprite != null) {
                // TODO: 1.20.1 Port - RenderPipelines.GUI_TEXTURED doesn't exist, using null
                sprite.blit(drawContext, null, getX() + 2, getY(), 18, 18);
            }
            // TODO: 1.20.1 Port - RenderPipelines.GUI_TEXTURED doesn't exist, using null
            drawContext.blit(null, isEnabled ? GuiSlotMobs.this.visibleIconIdentifier : GuiSlotMobs.this.invisibleIconIdentifier, getX() + 198, getY(), 0.0F, 0.0F, 18, 18, 18, 18);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mouseY < GuiSlotMobs.this.getY() || mouseY > GuiSlotMobs.this.getBottom()) {
                return false;
            }

            GuiSlotMobs.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2 - 92 - 16;
            byte padding = 3;
            int width = 215;
            if (mouseX >= (leftEdge + width - 16 - padding) && mouseX <= (leftEdge + width + padding)) {
                this.parentGui.toggleMobVisibility();
            }

            return true;
        }
    }
}
