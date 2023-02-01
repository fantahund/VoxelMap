package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class GuiMobs extends GuiScreenMinimap {
    private final Screen parentScreen;
    protected final RadarSettingsManager options;
    protected Text screenTitle;
    private GuiSlotMobs mobsList;
    private ButtonWidget buttonEnable;
    private ButtonWidget buttonDisable;
    protected TextFieldWidget filter;
    private Text tooltip;
    protected String selectedMobId;

    public GuiMobs(Screen parentScreen, RadarSettingsManager options) {
        this.parentScreen = parentScreen;
        this.options = options;
    }

    public void tick() {
        this.filter.tick();
    }

    public void init() {
        this.screenTitle = Text.translatable("options.minimap.mobs.title");
        this.mobsList = new GuiSlotMobs(this);
        int filterStringWidth = this.getFontRenderer().getWidth(I18n.translate("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 56, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(this.buttonEnable = new ButtonWidget.Builder(Text.translatable("options.minimap.mobs.enable"), button -> this.setMobEnabled(this.selectedMobId, true)).dimensions(this.getWidth() / 2 - 154, this.getHeight() - 28, 100, 20).build());
        this.addDrawableChild(this.buttonDisable = new ButtonWidget.Builder(Text.translatable("options.minimap.mobs.disable"), button -> this.setMobEnabled(this.selectedMobId, false)).dimensions(this.getWidth() / 2 - 50, this.getHeight() - 28, 100, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).dimensions(this.getWidth() / 2 + 4 + 50, this.getHeight() - 28, 100, 20).build());
        this.setFocused(this.filter);
        this.filter.setFocused(true);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected;
        this.buttonDisable.active = isSomethingSelected;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.mobsList.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.mobsList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.mobsList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.mobsList.mouseScrolled(mouseX, mouseY, amount);
    }

    protected void setSelectedMob(String id) {
        this.selectedMobId = id;
    }

    private boolean isMobEnabled(String mobId) {
        EnumMobs mob = EnumMobs.getMobByName(mobId);
        if (mob != null) {
            return mob.enabled;
        } else {
            CustomMob customMob = CustomMobsManager.getCustomMobByType(mobId);
            return customMob != null && customMob.enabled;
        }
    }

    private void setMobEnabled(String mobId, boolean enabled) {
        for (EnumMobs mob : EnumMobs.values()) {
            if (mob.id.equals(mobId)) {
                mob.enabled = enabled;
            }
        }

        for (CustomMob mob : CustomMobsManager.mobs) {
            if (mob.id.equals(mobId)) {
                mob.enabled = enabled;
            }
        }

    }

    protected void toggleMobVisibility() {
        EnumMobs mob = EnumMobs.getMobByName(this.selectedMobId);
        if (mob != null) {
            this.setMobEnabled(this.selectedMobId, !mob.enabled);
        } else {
            CustomMob customMob = CustomMobsManager.getCustomMobByType(this.selectedMobId);
            if (customMob != null) {
                this.setMobEnabled(this.selectedMobId, !customMob.enabled);
            }
        }

    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.drawMap(matrices);
        this.tooltip = null;
        this.mobsList.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected && !this.isMobEnabled(this.selectedMobId);
        this.buttonDisable.active = isSomethingSelected && this.isMobEnabled(this.selectedMobId);
        super.render(matrices, mouseX, mouseY, delta);
        drawStringWithShadow(matrices, this.getFontRenderer(), I18n.translate("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 51, 10526880);
        this.filter.render(matrices, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiMobs par0GuiWaypoints, Text par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }
}
