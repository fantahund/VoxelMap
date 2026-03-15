package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    protected Component screenTitle = Component.literal("players");
    private final boolean sharingWaypoint;
    private GuiListPlayers playerList;
    protected boolean allClicked;
    protected EditBox message;
    protected EditBox filter;
    private final String locInfo;

    private static final Component SHARE_MESSAGE = Component.translatable("minimap.waypointShare.shareMessage").append(":");
    private static final Component FILTER_MESSAGE = Component.translatable("minimap.waypoints.filter").append(":");

    private static final Component SHARE_WITH = Component.translatable("minimap.waypointShare.shareWith");
    private static final Component SHARE_WAYPOINT = Component.translatable("minimap.waypointShare.title");
    private static final Component SHARE_COORDINATES = Component.translatable("minimap.waypointShare.titleCoordinate");

    public GuiSelectPlayer(Screen parentScreen, String locInfo, boolean sharingWaypoint) {
        lastScreen = parentScreen;

        this.locInfo = locInfo;
        this.sharingWaypoint = sharingWaypoint;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        screenTitle = sharingWaypoint ? SHARE_WAYPOINT : SHARE_COORDINATES;

        playerList = new GuiListPlayers(this);

        int messageStringWidth = getFont().width(SHARE_MESSAGE);
        message = new EditBox(getFont(), getWidth() / 2 - 153 + messageStringWidth + 5, 34, 305 - messageStringWidth - 5, 20, Component.empty());
        message.setMaxLength(78);

        int filterStringWidth = getFont().width(FILTER_MESSAGE);
        filter = new EditBox(getFont(), getWidth() / 2 - 153 + filterStringWidth + 5, getHeight() - 54, 305 - filterStringWidth - 5, 20, Component.empty());
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);

        addRenderableWidget(playerList);
        addRenderableWidget(message);
        addRenderableWidget(filter);
        setFocused(filter);
        addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> onClose()).bounds(getWidth() / 2 - 100, getHeight() - 28, 200, 20).build());
    }

    private void filterUpdated(String string) {
        playerList.updateFilter(string.toLowerCase());
    }

    @Override
    public void accept(boolean b) {
        if (allClicked) {
            allClicked = false;
            if (b) {
                String combined = message.getValue() + " " + locInfo;
                if (combined.length() > 256) {
                    VoxelConstants.getPlayer().connection.sendChat(message.getValue());
                    VoxelConstants.getPlayer().connection.sendChat(locInfo);
                } else {
                    VoxelConstants.getPlayer().connection.sendChat(combined);
                }

                onClose();
            } else {
                VoxelConstants.getMinecraft().setScreen(this);
            }
        }

    }

    protected void sendMessageToPlayer(String name) {
        String combined = "msg " + name + " " + message.getValue() + " " + locInfo;
        if (combined.length() > 256) {
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + message.getValue());
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + locInfo);
        } else {
            VoxelConstants.getPlayer().connection.sendCommand(combined);
        }

        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(getFont(), screenTitle, getWidth() / 2, 20, 0xFFFFFFFF);
        graphics.text(getFont(), SHARE_MESSAGE, getWidth() / 2 - 153, 39, 0xFFA0A0A0);
        graphics.centeredText(getFont(), SHARE_WITH, getWidth() / 2, 75, 0xFFFFFFFF);
        graphics.text(getFont(), FILTER_MESSAGE, getWidth() / 2 - 153, getHeight() - 49, 0xFFA0A0A0);
    }

    @Override
    public void removed() {
    }
}
