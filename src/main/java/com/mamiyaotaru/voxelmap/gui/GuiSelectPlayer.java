package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    private final Screen parentScreen;
    protected Text screenTitle = new LiteralText("players");
    private boolean sharingWaypoint = true;
    private GuiButtonRowListPlayers playerList;
    protected boolean allClicked = false;
    protected TextFieldWidget message;
    protected TextFieldWidget filter;
    private Text tooltip = null;
    private String locInfo;
    private final int maxMessageLength = 78;
    final MutableText SHARE_MESSAGE = (new TranslatableText("minimap.waypointshare.sharemessage")).append(":");
    final TranslatableText SHARE_WITH = new TranslatableText("minimap.waypointshare.sharewith");
    final TranslatableText SHARE_WAYPOINT = new TranslatableText("minimap.waypointshare.title");
    final TranslatableText SHARE_COORDINATES = new TranslatableText("minimap.waypointshare.titlecoordinate");

    public GuiSelectPlayer(Screen parentScreen, IVoxelMap master, String locInfo, boolean sharingWaypoint) {
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
        this.getMinecraft().keyboard.setRepeatEvents(true);
        this.playerList = new GuiButtonRowListPlayers(this);
        int messageStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypointshare.sharemessage") + ":");
        this.message = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + messageStringWidth + 5, 34, 305 - messageStringWidth - 5, 20, (Text) null);
        this.message.setMaxLength(78);
        this.addDrawableChild(this.message);
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 55, 305 - filterStringWidth - 5, 20, (Text) null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, this.height - 27, 150, 20, new TranslatableText("gui.cancel"), button -> this.getMinecraft().setScreen(this.parentScreen)));
        this.setFocused(this.filter);
        this.filter.setTextFieldFocused(true);
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean OK = super.keyPressed(keysm, scancode, b);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char character, int keycode) {
        boolean OK = super.charTyped(character, keycode);
        if (this.filter.isFocused()) {
            this.playerList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.playerList.mouseClicked(mouseX, mouseY, mouseButton);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.playerList.mouseReleased(mouseX, mouseY, mouseButton);
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseEvent, double deltaX, double deltaY) {
        return this.playerList.mouseDragged(mouseX, mouseY, mouseEvent, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.playerList.mouseScrolled(mouseX, mouseY, amount);
    }

    public void accept(boolean par1) {
        if (this.allClicked) {
            this.allClicked = false;
            if (par1) {
                String combined = this.message.getText() + " " + this.locInfo;
                if (combined.length() > 100) {
                    this.client.player.sendChatMessage(this.message.getText());
                    this.client.player.sendChatMessage(this.locInfo);
                } else {
                    this.client.player.sendChatMessage(combined);
                }

                this.getMinecraft().setScreen(this.parentScreen);
            } else {
                this.getMinecraft().setScreen(this);
            }
        }

    }

    protected void sendMessageToPlayer(String name) {
        String combined = "/msg " + name + " " + this.message.getText() + " " + this.locInfo;
        if (combined.length() > 100) {
            this.getMinecraft().player.sendChatMessage("/msg " + name + " " + this.message.getText());
            this.getMinecraft().player.sendChatMessage("/msg " + name + " " + this.locInfo);
        } else {
            this.getMinecraft().player.sendChatMessage(combined);
        }

        this.getMinecraft().setScreen(this.parentScreen);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.tooltip = null;
        this.playerList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawTextWithShadow(matrixStack, this.getFontRenderer(), this.SHARE_MESSAGE, this.getWidth() / 2 - 153, 39, 10526880);
        this.message.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.SHARE_WITH, this.getWidth() / 2, 75, 16777215);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 50, 10526880);
        this.filter.render(matrixStack, mouseX, mouseY, partialTicks);
        if (this.tooltip != null) {
            this.renderTooltip(matrixStack, this.tooltip, mouseX, mouseY);
        }

    }

    static Text setTooltip(GuiSelectPlayer par0GuiWaypoints, Text par1Str) {
        return par0GuiWaypoints.tooltip = par1Str;
    }

    @Override
    public void removed() {
        this.getMinecraft().keyboard.setRepeatEvents(false);
    }
}
