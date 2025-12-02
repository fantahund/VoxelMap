package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiMobs extends GuiScreenMinimap {
    private final Screen parentScreen;
    protected final RadarSettingsManager options;
    protected Component screenTitle;
    private GuiSlotMobs mobsList;
    private Button buttonEnable;
    private Button buttonDisable;
    protected EditBox filter;
    private Component tooltip;
    protected Identifier selectedMobId;

    public GuiMobs(Screen parentScreen, RadarSettingsManager options) {
        this.parentScreen = parentScreen;
        this.setParentScreen(this.parentScreen);

        this.options = options;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.screenTitle = Component.translatable("options.minimap.mobs.title");
        this.mobsList = new GuiSlotMobs(this);
        int filterStringWidth = this.getFont().width(I18n.get("minimap.waypoints.filter") + ":");
        this.filter = new EditBox(this.getFont(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 56, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addRenderableWidget(this.filter);
        this.addRenderableWidget(this.buttonEnable = new Button.Builder(Component.translatable("options.minimap.mobs.enable"), button -> this.setMobEnabled(this.selectedMobId, true)).bounds(this.getWidth() / 2 - 154, this.getHeight() - 28, 100, 20).build());
        this.addRenderableWidget(this.buttonDisable = new Button.Builder(Component.translatable("options.minimap.mobs.disable"), button -> this.setMobEnabled(this.selectedMobId, false)).bounds(this.getWidth() / 2 - 50, this.getHeight() - 28, 100, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 + 4 + 50, this.getHeight() - 28, 100, 20).build());
        this.setFocused(this.filter);
        this.filter.setFocused(true);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected;
        this.buttonDisable.active = isSomethingSelected;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean OK = super.keyPressed(keyEvent);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        if (this.filter.isFocused()) {
            this.mobsList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        double mouseY = mouseButtonEvent.y();
        if (mouseY >= this.mobsList.getY() && mouseY < this.mobsList.getBottom()) {
            this.mobsList.mouseClicked(mouseButtonEvent, bl);
        }
        return super.mouseClicked(mouseButtonEvent, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        double mouseY = mouseButtonEvent.y();
        if (mouseY >= this.mobsList.getY() && mouseY < this.mobsList.getBottom()) {
            this.mobsList.mouseReleased(mouseButtonEvent);
        }
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double d, double e) {
        double mouseY = mouseButtonEvent.y();
        if (mouseY >= this.mobsList.getY() && mouseY < this.mobsList.getBottom()) {
            return this.mobsList.mouseDragged(mouseButtonEvent, d, e);
        }
        return super.mouseDragged(mouseButtonEvent, d, e);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (mouseY >= this.mobsList.getY() && mouseY < this.mobsList.getBottom()) {
            return this.mobsList.mouseScrolled(mouseX, mouseY, 0, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    protected void setSelectedMob(Identifier id) {
        this.selectedMobId = id;
    }

    private boolean isMobEnabled(Identifier mobId) {
        return !VoxelMap.radarOptions.hiddenMobs.contains(mobId);
    }

    private void setMobEnabled(Identifier mobId, boolean enabled) {
        if (enabled) {
            VoxelMap.radarOptions.hiddenMobs.remove(mobId);
        } else {
            VoxelMap.radarOptions.hiddenMobs.add(mobId);
        }
    }

    protected void toggleMobVisibility() {
        setMobEnabled(selectedMobId, !isMobEnabled(selectedMobId));
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.mobsList.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
        boolean isSomethingSelected = this.selectedMobId != null;
        this.buttonEnable.active = isSomethingSelected && !this.isMobEnabled(this.selectedMobId);
        this.buttonDisable.active = isSomethingSelected && this.isMobEnabled(this.selectedMobId);
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawString(this.getFont(), I18n.get("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 51, 0xFFA0A0A0);
        this.filter.render(drawContext, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiMobs par0GuiWaypoints, Component par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }
}
