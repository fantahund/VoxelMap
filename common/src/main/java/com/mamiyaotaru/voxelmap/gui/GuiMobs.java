package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiMobs extends GuiScreenMinimap {
    private final RadarSettingsManager options;
    private GuiListMobs mobsList;
    private Button buttonEnable;
    private Button buttonDisable;
    protected EditBox filter;
    protected Identifier selectedMobId;

    public GuiMobs(Screen parentGui) {
        super(parentGui, Component.translatable("options.minimap.mobs.title"));
        options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        mobsList = new GuiListMobs(this, 0, 40, getWidth(), getHeight() - 110);
        int filterStringWidth = getFont().width(I18n.get("minimap.waypoints.filter") + ":");
        filter = new EditBox(getFont(), getWidth() / 2 - 153 + filterStringWidth + 5, getHeight() - 54, 305 - filterStringWidth - 5, 20, Component.empty());
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);

        addRenderableWidget(mobsList);
        addRenderableWidget(filter);
        setFocused(filter);
        addRenderableWidget(buttonEnable = new Button.Builder(Component.translatable("options.minimap.mobs.enable"), button -> setMobEnabled(selectedMobId, true)).bounds(getWidth() / 2 - 154, getHeight() - 26, 100, 20).build());
        addRenderableWidget(buttonDisable = new Button.Builder(Component.translatable("options.minimap.mobs.disable"), button -> setMobEnabled(selectedMobId, false)).bounds(getWidth() / 2 - 50, getHeight() - 26, 100, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).bounds(getWidth() / 2 + 4 + 50, getHeight() - 26, 100, 20).build());

        boolean isSomethingSelected = selectedMobId != null;
        buttonEnable.active = isSomethingSelected;
        buttonDisable.active = isSomethingSelected;
    }

    private void filterUpdated(String string) {
        mobsList.updateFilter(string.toLowerCase());
    }

    protected void setSelectedMob(Identifier id) {
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

    protected void toggleMobVisibility() {
        setMobEnabled(selectedMobId, !isMobEnabled(selectedMobId));
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.drawString(getFont(), I18n.get("minimap.waypoints.filter") + ":", getWidth() / 2 - 153, getHeight() - 49, 0xFFA0A0A0);

        boolean isSomethingSelected = selectedMobId != null;
        buttonEnable.active = isSomethingSelected && !isMobEnabled(selectedMobId);
        buttonDisable.active = isSomethingSelected && isMobEnabled(selectedMobId);
    }
}
