package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
    private Text tooltip = null;
    protected String selectedMobId = null;

    public GuiMobs(Screen parentScreen, RadarSettingsManager options) {
        this.parentScreen = parentScreen;
        this.options = options;
    }

    public void tick() {
        this.filter.tick();
    }

    public void init() {
        this.screenTitle = Text.translatable("options.minimap.mobs.title");
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(true);
        this.mobsList = new GuiSlotMobs(this);
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 56, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(this.buttonEnable = new ButtonWidget(this.getWidth() / 2 - 154, this.getHeight() - 28, 100, 20, Text.translatable("options.minimap.mobs.enable"), button -> this.setMobEnabled(this.selectedMobId, true)));
        this.addDrawableChild(this.buttonDisable = new ButtonWidget(this.getWidth() / 2 - 50, this.getHeight() - 28, 100, 20, Text.translatable("options.minimap.mobs.disable"), button -> this.setMobEnabled(this.selectedMobId, false)));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 4 + 50, this.getHeight() - 28, 100, 20, Text.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)));
        this.setFocused(this.filter);
        this.filter.setTextFieldFocused(true);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected;
        this.buttonDisable.active = isSomethingSelected;
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean OK = super.keyPressed(keysm, scancode, b);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char character, int keycode) {
        boolean OK = super.charTyped(character, keycode);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.mobsList.mouseClicked(mouseX, mouseY, mouseButton);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.mobsList.mouseReleased(mouseX, mouseY, mouseButton);
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseEvent, double deltaX, double deltaY) {
        return this.mobsList.mouseDragged(mouseX, mouseY, mouseEvent, deltaX, deltaY);
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

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialticks) {
        super.drawMap(matrixStack);
        this.tooltip = null;
        this.mobsList.render(matrixStack, mouseX, mouseY, partialticks);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected && !this.isMobEnabled(this.selectedMobId);
        this.buttonDisable.active = isSomethingSelected && this.isMobEnabled(this.selectedMobId);
        super.render(matrixStack, mouseX, mouseY, partialticks);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 51, 10526880);
        this.filter.render(matrixStack, mouseX, mouseY, partialticks);
        if (this.tooltip != null) {
            this.renderTooltip(matrixStack, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiMobs par0GuiWaypoints, Text par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    @Override
    public void removed() {
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(false);
        super.removed();
    }
}
