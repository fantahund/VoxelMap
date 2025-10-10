package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parentScreen;
    protected Component screenTitle = Component.literal("players");
    private final boolean sharingWaypoint;
    private GuiButtonRowListPlayers playerList;
    protected boolean allClicked;
    protected EditBox message;
    protected EditBox filter;
    private Component tooltip;
    private final String locInfo;
    static final MutableComponent SHARE_MESSAGE = (Component.translatable("minimap.waypointshare.sharemessage")).append(":");
    static final Component SHARE_WITH = Component.translatable("minimap.waypointshare.sharewith");
    static final Component SHARE_WAYPOINT = Component.translatable("minimap.waypointshare.title");
    static final Component SHARE_COORDINATES = Component.translatable("minimap.waypointshare.titlecoordinate");

    public GuiSelectPlayer(Screen parentScreen, String locInfo, boolean sharingWaypoint) {
        this.parentScreen = parentScreen;
        this.setParentScreen(this.parentScreen);

        this.locInfo = locInfo;
        this.sharingWaypoint = sharingWaypoint;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.screenTitle = this.sharingWaypoint ? SHARE_WAYPOINT : SHARE_COORDINATES;
        this.playerList = new GuiButtonRowListPlayers(this);
        int messageStringWidth = this.getFont().width(I18n.get("minimap.waypointshare.sharemessage") + ":");
        this.message = new EditBox(this.getFont(), this.getWidth() / 2 - 153 + messageStringWidth + 5, 34, 305 - messageStringWidth - 5, 20, null);
        this.message.setMaxLength(78);
        this.addRenderableWidget(this.message);
        int filterStringWidth = this.getFont().width(I18n.get("minimap.waypoints.filter") + ":");
        this.filter = new EditBox(this.getFont(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 55, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addRenderableWidget(this.filter);
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.width / 2 - 100, this.height - 27, 150, 20).build());
        this.setFocused(this.filter);
        this.filter.setFocused(true);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean OK = super.keyPressed(keyEvent);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getValue().toLowerCase());
        }

        return OK;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        this.playerList.mouseClicked(mouseButtonEvent, doubleClick);
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        this.playerList.mouseReleased(mouseButtonEvent);
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        return this.playerList.mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return this.playerList.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    @Override
    public void accept(boolean b) {
        if (this.allClicked) {
            this.allClicked = false;
            if (b) {
                String combined = this.message.getValue() + " " + this.locInfo;
                if (combined.length() > 256) {
                    VoxelConstants.getPlayer().connection.sendChat(this.message.getValue());
                    VoxelConstants.getPlayer().connection.sendChat(this.locInfo);
                } else {
                    VoxelConstants.getPlayer().connection.sendChat(combined);
                }

                VoxelConstants.getMinecraft().setScreen(this.parentScreen);
            } else {
                VoxelConstants.getMinecraft().setScreen(this);
            }
        }

    }

    protected void sendMessageToPlayer(String name) {
        String combined = "msg " + name + " " + this.message.getValue() + " " + this.locInfo;
        if (combined.length() > 256) {
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + this.message.getValue());
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + this.locInfo);
        } else {
            VoxelConstants.getPlayer().connection.sendCommand(combined);
        }

        VoxelConstants.getMinecraft().setScreen(this.parentScreen);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.playerList.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 0xFFFFFFFF);
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawString(this.getFont(), SHARE_MESSAGE, this.getWidth() / 2 - 153, 39, 0xFFA0A0A0);
        this.message.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFont(), SHARE_WITH, this.getWidth() / 2, 75, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), I18n.get("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 50, 0xFFA0A0A0);
        this.filter.render(drawContext, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiSelectPlayer par0GuiWaypoints, Component par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    @Override
    public void removed() {
    }
}
