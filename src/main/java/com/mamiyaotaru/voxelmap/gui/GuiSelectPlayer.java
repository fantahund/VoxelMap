package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parentScreen;
    protected Text screenTitle = Text.literal("players");
    private final boolean sharingWaypoint;
    private GuiButtonRowListPlayers playerList;
    protected boolean allClicked = false;
    protected TextFieldWidget message;
    protected TextFieldWidget filter;
    private Text tooltip = null;
    private final String locInfo;
    static final MutableText SHARE_MESSAGE = (Text.translatable("minimap.waypointshare.sharemessage")).append(":");
    static final Text SHARE_WITH = Text.translatable("minimap.waypointshare.sharewith");
    static final Text SHARE_WAYPOINT = Text.translatable("minimap.waypointshare.title");
    static final Text SHARE_COORDINATES = Text.translatable("minimap.waypointshare.titlecoordinate");

    public GuiSelectPlayer(Screen parentScreen, String locInfo, boolean sharingWaypoint) {
        this.parentScreen = parentScreen;
        this.locInfo = locInfo;
        this.sharingWaypoint = sharingWaypoint;
    }

    public void tick() {
        this.message.tick();
        this.filter.tick();
    }

    public void init() {
        this.screenTitle = this.sharingWaypoint ? this.SHARE_WAYPOINT : this.SHARE_COORDINATES;
        this.playerList = new GuiButtonRowListPlayers(this);
        int messageStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypointshare.sharemessage") + ":");
        this.message = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + messageStringWidth + 5, 34, 305 - messageStringWidth - 5, 20, null);
        this.message.setMaxLength(78);
        this.addDrawableChild(this.message);
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 55, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).dimensions(this.width / 2 - 100, this.height - 27, 150, 20).build());
        this.setFocused(this.filter);
        this.filter.setTextFieldFocused(true);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = super.keyPressed(keyCode, scanCode, modifiers);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.playerList.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.playerList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.playerList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.playerList.mouseScrolled(mouseX, mouseY, amount);
    }

    public void accept(boolean b) {
        if (this.allClicked) {
            this.allClicked = false;
            if (b) {
                String combined = this.message.getText() + " " + this.locInfo;
                if (combined.length() > 100) {
                    VoxelConstants.getPlayer().sendMessage(Text.of(this.message.getText()));
                    VoxelConstants.getPlayer().sendMessage(Text.of(this.locInfo));
                } else {
                    VoxelConstants.getPlayer().sendMessage(Text.of(combined));
                }

                VoxelConstants.getMinecraft().setScreen(this.parentScreen);
            } else {
                VoxelConstants.getMinecraft().setScreen(this);
            }
        }

    }

    protected void sendMessageToPlayer(String name) {
        String combined = "msg " + name + " " + this.message.getText() + " " + this.locInfo;
        if (combined.length() > 100) {
            VoxelConstants.getPlayer().networkHandler.sendCommand("msg " + name + " " + this.message.getText());
            VoxelConstants.getPlayer().networkHandler.sendCommand("msg " + name + " " + this.locInfo);
        } else {
            VoxelConstants.getPlayer().networkHandler.sendCommand(combined);
        }

        VoxelConstants.getMinecraft().setScreen(this.parentScreen);
    }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.drawMap(matrices);
        this.tooltip = null;
        this.playerList.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrices, mouseX, mouseY, delta);
        drawTextWithShadow(matrices, this.getFontRenderer(), this.SHARE_MESSAGE, this.getWidth() / 2 - 153, 39, 10526880);
        this.message.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, this.getFontRenderer(), this.SHARE_WITH, this.getWidth() / 2, 75, 16777215);
        drawStringWithShadow(matrices, this.getFontRenderer(), I18nUtils.getString("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 50, 10526880);
        this.filter.render(matrices, mouseX, mouseY, delta);
        if (this.tooltip != null) {
            this.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
        }

    }

    static void setTooltip(GuiSelectPlayer par0GuiWaypoints, Text par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    @Override
    public void removed() {
    }
}
