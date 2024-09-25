package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiSlotMinimap;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

class GuiSlotMobs extends GuiSlotMinimap {
    private final ArrayList<MobItem> mobs;
    private ArrayList<Entry<?>> mobsFiltered;
    final GuiMobs parentGui;
    static final Component ENABLE = Component.translatable("options.minimap.mobs.enable");
    static final Component DISABLE = Component.translatable("options.minimap.mobs.disable");
    static final Component ENABLED = Component.translatable("options.minimap.mobs.enabled");
    static final Component DISABLED = Component.translatable("options.minimap.mobs.disabled");
    final ResourceLocation visibleIconIdentifier = ResourceLocation.parse("textures/mob_effect/night_vision.png");
    final ResourceLocation invisibleIconIdentifier = ResourceLocation.parse("textures/mob_effect/blindness.png");

    GuiSlotMobs(GuiMobs par1GuiMobs) {
        super (par1GuiMobs.getWidth(), par1GuiMobs.getHeight(), 32, par1GuiMobs.getHeight() - 65 + 4, 18);

        this.parentGui = par1GuiMobs;
        RadarSettingsManager options = this.parentGui.options;
        this.mobs = new ArrayList<>();

        for (EnumMobs mob : EnumMobs.values()) {
            if (mob.isTopLevelUnit && (mob.isHostile && options.showHostiles || mob.isNeutral && options.showNeutrals)) {
                this.mobs.add(new MobItem(this.parentGui, mob.id));
            }
        }

        for (CustomMob mob : CustomMobsManager.mobs) {
            if (mob.isHostile && options.showHostiles || mob.isNeutral && options.showNeutrals) {
                this.mobs.add(new MobItem(this.parentGui, mob.id));
            }
        }

        this.mobs.sort((mob1, mob2) -> String.CASE_INSENSITIVE_ORDER.compare(mob1.name, mob2.name));
        this.mobsFiltered = new ArrayList<>(this.mobs);
        this.mobsFiltered.forEach(this::addEntry);
    }

    private static String getTranslatedName(String name) {
        if (!name.contains(".")) {
            name = "entity.minecraft." + name.toLowerCase();
        }

        name = I18n.get(name);
        name = name.replaceAll("^entity.minecraft.", "");
        name = name.replace("_", " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return TextUtils.scrubCodes(name);
    }

    public void setSelected(MobItem entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof MobItem) {
            GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
            narratorManager.sayNow((Component.translatable("narrator.select", ((MobItem) this.getSelected()).name)).getString());
        }

        this.parentGui.setSelectedMob(entry.id);
    }

    protected boolean isSelectedItem(int index) {
        return ((MobItem) this.mobsFiltered.get(index)).id.equals(this.parentGui.selectedMobId);
    }

    protected int getMaxPosition() {
        return this.getItemCount() * this.itemHeight;
    }

    protected void updateFilter(String filterString) {
        this.clearEntries();
        this.mobsFiltered = new ArrayList<>(this.mobs);
        Iterator<?> iterator = this.mobsFiltered.iterator();

        while (iterator.hasNext()) {
            String mobName = ((MobItem) iterator.next()).name;
            if (!mobName.toLowerCase().contains(filterString)) {
                if (mobName.equals(this.parentGui.selectedMobId)) {
                    this.parentGui.setSelectedMob(null);
                }

                iterator.remove();
            }
        }

        this.mobsFiltered.forEach(this::addEntry);
    }

    public class MobItem extends AbstractSelectionList.Entry<MobItem> {
        private final GuiMobs parentGui;
        private final String id;
        private final String name;

        protected MobItem(GuiMobs mobsScreen, String id) {
            this.parentGui = mobsScreen;
            this.id = id;
            this.name = GuiSlotMobs.getTranslatedName(id);
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean isHostile = false;
            boolean isNeutral = false;
            boolean isEnabled = true;
            EnumMobs mob = EnumMobs.getMobByName(this.id);
            if (mob != null) {
                isHostile = mob.isHostile;
                isNeutral = mob.isNeutral;
                isEnabled = mob.enabled;
            } else {
                CustomMob customMob = CustomMobsManager.getCustomMobByType(this.id);
                if (customMob != null) {
                    isHostile = customMob.isHostile;
                    isNeutral = customMob.isNeutral;
                    isEnabled = customMob.enabled;
                }
            }

            int red = isHostile ? 255 : 0;
            int green = isNeutral ? 255 : 0;
            int color = -16777216 + (red << 16) + (green << 8);
            drawContext.drawCenteredString(this.parentGui.getFontRenderer(), this.name, this.parentGui.getWidth() / 2, y + 3, color);
            byte padding = 3;
            if (mouseX >= x - padding && mouseY >= y && mouseX <= x + 215 + padding && mouseY <= y + GuiSlotMobs.this.itemHeight) {
                Component tooltip;
                if (mouseX >= x + 215 - 16 - padding && mouseX <= x + 215 + padding) {
                    tooltip = isEnabled ? GuiSlotMobs.DISABLE : GuiSlotMobs.ENABLE;
                } else {
                    tooltip = isEnabled ? GuiSlotMobs.ENABLED : GuiSlotMobs.DISABLED;
                }

                GuiMobs.setTooltip(this.parentGui, tooltip);
            }

            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGL.Utils.img2(isEnabled ? GuiSlotMobs.this.visibleIconIdentifier : GuiSlotMobs.this.invisibleIconIdentifier);
            drawContext.blit(isEnabled ? GuiSlotMobs.this.visibleIconIdentifier : GuiSlotMobs.this.invisibleIconIdentifier, x + 198, y - 2, 0, 0.0F, 0.0F, 18, 18, 18, 18);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GuiSlotMobs.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2 - 92 - 16;
            byte padding = 3;
            int width = 215;
            if (mouseX >= (leftEdge + width - 16 - padding) && mouseX <= (leftEdge + width + padding)) {
                this.parentGui.toggleMobVisibility();
            } else if (GuiSlotMobs.this.doubleclick) {
                this.parentGui.toggleMobVisibility();
            }

            return true;
        }
    }
}
