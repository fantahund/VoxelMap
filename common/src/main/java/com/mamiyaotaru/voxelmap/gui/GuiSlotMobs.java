package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

class GuiSlotMobs extends AbstractSelectionList<GuiSlotMobs.MobItem> {
    private final ArrayList<MobItem> mobs;
    private ArrayList<Entry<?>> mobsFiltered;
    final GuiMobs parentGui;
    static final Component ENABLED = Component.translatable("options.minimap.mobs.enabled");
    static final Component DISABLED = Component.translatable("options.minimap.mobs.disabled");
    static final Component TOOLTIP_ENABLE = Component.translatable("options.minimap.mobs.enabletooltip");
    static final Component TOOLTIP_DISABLE = Component.translatable("options.minimap.mobs.disabletooltip");
    final ResourceLocation visibleIconIdentifier = ResourceLocation.parse("textures/gui/sprites/container/beacon/confirm.png");
    final ResourceLocation invisibleIconIdentifier = ResourceLocation.parse("textures/gui/sprites/container/beacon/cancel.png");

    GuiSlotMobs(GuiMobs par1GuiMobs) {
        super(VoxelConstants.getMinecraft(), par1GuiMobs.getWidth(), par1GuiMobs.getHeight() - 110, 40, 18);

        this.parentGui = par1GuiMobs;
        RadarSettingsManager options = this.parentGui.options;
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

    public void setSelected(MobItem entry) {
        super.setSelected(entry);
        if (this.getSelected() != null) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayNow((Component.translatable("narrator.select", this.getSelected().name)).getString());
        }

        this.parentGui.setSelectedMob(entry.id);
    }

    @Override
    protected boolean isSelectedItem(int index) {
        return ((MobItem) this.mobsFiltered.get(index)).id.equals(this.parentGui.selectedMobId);
    }

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
        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean isHostile = category == MobCategory.HOSTILE;
            boolean isNeutral = !isHostile;
            boolean isEnabled = VoxelMap.radarOptions.isMobEnabled(type);

            int red = isHostile ? 255 : 0;
            int green = isNeutral ? 255 : 0;
            int color = -16777216 + (red << 16) + (green << 8);
            drawContext.drawCenteredString(this.parentGui.getFontRenderer(), this.name, this.parentGui.getWidth() / 2, y + 3, color);
            byte padding = 3;
            if (mouseX >= x - padding && mouseY >= y && mouseX <= x + 215 + padding && mouseY <= y + GuiSlotMobs.this.itemHeight) {
                Component tooltip;
                if (mouseX >= x + 215 - 16 - padding && mouseX <= x + 215 + padding) {
                    tooltip = isEnabled ? GuiSlotMobs.TOOLTIP_DISABLE : GuiSlotMobs.TOOLTIP_ENABLE;
                } else {
                    tooltip = isEnabled ? GuiSlotMobs.ENABLED : GuiSlotMobs.DISABLED;
                }

                GuiMobs.setTooltip(this.parentGui, tooltip);
            }
            Sprite sprite = VoxelConstants.getVoxelMapInstance().getNotSimpleRadar().getEntityMapImageManager().requestImageForMobType(type);
            if (sprite != null) {
                sprite.blit(drawContext, RenderType::guiTextured, x + 20, y - 2, 18, 18);
            }
            drawContext.blit(RenderType::guiTextured, isEnabled ? GuiSlotMobs.this.visibleIconIdentifier : GuiSlotMobs.this.invisibleIconIdentifier, x + 198, y - 2, 0.0F, 0.0F, 18, 18, 18, 18);
            drawContext.flush();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
