package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiSlotMinimap;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;

class GuiSlotMobs extends GuiSlotMinimap {
    private final ArrayList<MobItem> mobs;
    private ArrayList<Entry<?>> mobsFiltered;
    final GuiMobs parentGui;
    final Text ENABLE = Text.translatable("options.minimap.mobs.enable");
    final Text DISABLE = Text.translatable("options.minimap.mobs.disable");
    final Text ENABLED = Text.translatable("options.minimap.mobs.enabled");
    final Text DISABLED = Text.translatable("options.minimap.mobs.disabled");
    final Identifier visibleIconIdentifier = new Identifier("textures/mob_effect/night_vision.png");
    final Identifier invisibleIconIdentifier = new Identifier("textures/mob_effect/blindness.png");

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

        final Collator collator = I18nUtils.getLocaleAwareCollator();
        this.mobs.sort((mob1, mob2) -> collator.compare(mob1.name, mob2.name));
        this.mobsFiltered = new ArrayList<>(this.mobs);
        this.mobsFiltered.forEach(this::addEntry);
    }

    private static String getTranslatedName(String name) {
        if (!name.contains(".")) {
            name = "entity.minecraft." + name.toLowerCase();
        }

        name = I18nUtils.getString(name);
        name = name.replaceAll("^entity.minecraft.", "");
        name = name.replace("_", " ");
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        return TextUtils.scrubCodes(name);
    }

    public void setSelected(MobItem item) {
        super.setSelected(item);
        if (this.getSelectedOrNull() instanceof MobItem) {
            NarratorManager narratorManager = new NarratorManager(VoxelConstants.getMinecraft());
            narratorManager.narrate((Text.translatable("narrator.select", ((MobItem) this.getSelectedOrNull()).name)).getString());
        }

        this.parentGui.setSelectedMob(item.id);
    }

    protected boolean isSelectedEntry(int par1) {
        return ((MobItem) this.mobsFiltered.get(par1)).id.equals(this.parentGui.selectedMobId);
    }

    protected int getMaxPosition() {
        return this.getEntryCount() * this.itemHeight;
    }

    public void renderBackground(MatrixStack matrixStack) {
        this.parentGui.renderBackground(matrixStack);
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

    public class MobItem extends EntryListWidget.Entry<MobItem> {
        private final GuiMobs parentGui;
        private final String id;
        private final String name;

        protected MobItem(GuiMobs mobsScreen, String id) {
            this.parentGui = mobsScreen;
            this.id = id;
            this.name = GuiSlotMobs.getTranslatedName(id);
        }

        public void render(MatrixStack matrixStack, int slotIndex, int slotYPos, int leftEdge, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean mouseOver, float partialTicks) {
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
            DrawableHelper.drawCenteredText(matrixStack, this.parentGui.getFontRenderer(), this.name, this.parentGui.getWidth() / 2, slotYPos + 3, color);
            byte padding = 3;
            if (mouseX >= leftEdge - padding && mouseY >= slotYPos && mouseX <= leftEdge + 215 + padding && mouseY <= slotYPos + GuiSlotMobs.this.itemHeight) {
                Text tooltip;
                if (mouseX >= leftEdge + 215 - 16 - padding && mouseX <= leftEdge + 215 + padding) {
                    tooltip = isEnabled ? GuiSlotMobs.this.DISABLE : GuiSlotMobs.this.ENABLE;
                } else {
                    tooltip = isEnabled ? GuiSlotMobs.this.ENABLED : GuiSlotMobs.this.DISABLED;
                }

                GuiMobs.setTooltip(this.parentGui, tooltip);
            }

            GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GLUtils.img2(isEnabled ? GuiSlotMobs.this.visibleIconIdentifier : GuiSlotMobs.this.invisibleIconIdentifier);
            DrawableHelper.drawTexture(matrixStack, leftEdge + 198, slotYPos - 2, GuiSlotMobs.this.getZOffset(), 0.0F, 0.0F, 18, 18, 18, 18);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int mouseEvent) {
            GuiSlotMobs.this.setSelected(this);
            int leftEdge = this.parentGui.getWidth() / 2 - 92 - 16;
            byte padding = 3;
            int width = 215;
            if (mouseX >= (double) (leftEdge + width - 16 - padding) && mouseX <= (double) (leftEdge + width + padding)) {
                this.parentGui.toggleMobVisibility();
            } else if (GuiSlotMobs.this.doubleclick) {
                this.parentGui.toggleMobVisibility();
            }

            return true;
        }
    }
}
