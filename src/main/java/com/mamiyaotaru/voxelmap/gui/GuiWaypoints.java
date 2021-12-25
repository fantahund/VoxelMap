package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.Random;
import java.util.TreeSet;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    private final Screen parentScreen;
    private IVoxelMap master;
    protected final MapSettingsManager options;
    protected final IWaypointManager waypointManager;
    protected Text screenTitle;
    private GuiSlotWaypoints waypointList;
    private ButtonWidget buttonEdit;
    private boolean editClicked = false;
    private ButtonWidget buttonDelete;
    private boolean deleteClicked = false;
    private ButtonWidget buttonHighlight;
    private ButtonWidget buttonShare;
    private ButtonWidget buttonTeleport;
    private ButtonWidget buttonSortName;
    private ButtonWidget buttonSortCreated;
    private ButtonWidget buttonSortDistance;
    private ButtonWidget buttonSortColor;
    protected TextFieldWidget filter;
    private boolean addClicked = false;
    private Text tooltip = null;
    protected Waypoint selectedWaypoint = null;
    protected Waypoint highlightedWaypoint = null;
    protected Waypoint newWaypoint = null;
    private Random generator = new Random();
    private boolean changedSort = false;

    public GuiWaypoints(Screen parentScreen, IVoxelMap master) {
        this.master = master;
        this.parentScreen = parentScreen;
        this.options = master.getMapOptions();
        this.waypointManager = master.getWaypointManager();
        this.highlightedWaypoint = this.waypointManager.getHighlightedWaypoint();
    }

    public void tick() {
        this.filter.tick();
    }

    public void init() {
        this.screenTitle = new TranslatableText("minimap.waypoints.title");
        this.getMinecraft().keyboard.setRepeatEvents(true);
        this.waypointList = new GuiSlotWaypoints(this);
        this.addDrawableChild(this.buttonSortName = new ButtonWidget(this.getWidth() / 2 - 154, 34, 77, 20, new TranslatableText("minimap.waypoints.sortbyname"), button -> this.sortClicked(2)));
        this.addDrawableChild(this.buttonSortDistance = new ButtonWidget(this.getWidth() / 2 - 77, 34, 77, 20, new TranslatableText("minimap.waypoints.sortbydistance"), button -> this.sortClicked(3)));
        this.addDrawableChild(this.buttonSortCreated = new ButtonWidget(this.getWidth() / 2, 34, 77, 20, new TranslatableText("minimap.waypoints.sortbycreated"), button -> this.sortClicked(1)));
        this.addDrawableChild(this.buttonSortColor = new ButtonWidget(this.getWidth() / 2 + 77, 34, 77, 20, new TranslatableText("minimap.waypoints.sortbycolor"), button -> this.sortClicked(4)));
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 80, 305 - filterStringWidth - 5, 20, (Text) null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(this.buttonEdit = new ButtonWidget(this.getWidth() / 2 - 154, this.getHeight() - 52, 74, 20, new TranslatableText("selectServer.edit"), button -> this.editWaypoint(this.selectedWaypoint)));
        this.addDrawableChild(this.buttonDelete = new ButtonWidget(this.getWidth() / 2 - 76, this.getHeight() - 52, 74, 20, new TranslatableText("selectServer.delete"), button -> this.deleteClicked()));
        this.addDrawableChild(this.buttonHighlight = new ButtonWidget(this.getWidth() / 2 + 2, this.getHeight() - 52, 74, 20, new TranslatableText("minimap.waypoints.highlight"), button -> this.setHighlightedWaypoint()));
        this.addDrawableChild(this.buttonTeleport = new ButtonWidget(this.getWidth() / 2 + 80, this.getHeight() - 52, 74, 20, new TranslatableText("minimap.waypoints.teleportto"), button -> this.teleportClicked()));
        this.addDrawableChild(this.buttonShare = new ButtonWidget(this.getWidth() / 2 - 154, this.getHeight() - 28, 74, 20, new TranslatableText("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(this.selectedWaypoint)));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 76, this.getHeight() - 28, 74, 20, new TranslatableText("minimap.waypoints.newwaypoint"), button -> this.addWaypoint()));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 2, this.getHeight() - 28, 74, 20, new TranslatableText("menu.options"), button -> this.getMinecraft().setScreen(new GuiWaypointsOptions(this, this.options))));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 80, this.getHeight() - 28, 74, 20, new TranslatableText("gui.done"), button -> this.getMinecraft().setScreen(this.parentScreen)));
        this.setFocused(this.filter);
        this.filter.setTextFieldFocused(true);
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonEdit.active = isSomethingSelected;
        this.buttonDelete.active = isSomethingSelected;
        this.buttonHighlight.active = isSomethingSelected;
        this.buttonShare.active = isSomethingSelected;
        this.buttonTeleport.active = isSomethingSelected && this.canTeleport();
        this.sort();
    }

    private void sort() {
        int sortKey = Math.abs(this.options.sort);
        boolean ascending = this.options.sort > 0;
        this.waypointList.sortBy(sortKey, ascending);
        String arrow = ascending ? "↑" : "↓";
        if (sortKey == 2) {
            this.buttonSortName.setMessage(new LiteralText(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbyname") + " " + arrow));
        } else {
            this.buttonSortName.setMessage(new TranslatableText("minimap.waypoints.sortbyname"));
        }

        if (sortKey == 3) {
            this.buttonSortDistance.setMessage(new LiteralText(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbydistance") + " " + arrow));
        } else {
            this.buttonSortDistance.setMessage(new TranslatableText("minimap.waypoints.sortbydistance"));
        }

        if (sortKey == 1) {
            this.buttonSortCreated.setMessage(new LiteralText(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbycreated") + " " + arrow));
        } else {
            this.buttonSortCreated.setMessage(new TranslatableText("minimap.waypoints.sortbycreated"));
        }

        if (sortKey == 4) {
            this.buttonSortColor.setMessage(new LiteralText(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbycolor") + " " + arrow));
        } else {
            this.buttonSortColor.setMessage(new TranslatableText("minimap.waypoints.sortbycolor"));
        }

    }

    private void deleteClicked() {
        String var2 = this.selectedWaypoint.name;
        if (var2 != null) {
            this.deleteClicked = true;
            TranslatableText title = new TranslatableText("minimap.waypoints.deleteconfirm");
            TranslatableText explanation = new TranslatableText("selectServer.deleteWarning", new Object[]{var2});
            TranslatableText affirm = new TranslatableText("selectServer.deleteButton");
            TranslatableText deny = new TranslatableText("gui.cancel");
            ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
            this.getMinecraft().setScreen(confirmScreen);
        }

    }

    private void teleportClicked() {
        boolean mp = !this.client.isIntegratedServerRunning();
        int y = this.selectedWaypoint.getY() > 0 ? this.selectedWaypoint.getY() : (!this.options.game.player.world.getDimension().hasCeiling() ? 255 : 64);
        this.options.game.player.sendChatMessage("/tp " + this.options.game.player.getName().getString() + " " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
        if (mp) {
            this.options.game.player.sendChatMessage("/tppos " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
        }

        this.getMinecraft().setScreen((Screen) null);
    }

    protected void sortClicked(int id) {
        this.options.setSort(id);
        this.changedSort = true;
        this.sort();
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean OK = super.keyPressed(keysm, scancode, b);
        if (this.filter.isFocused()) {
            this.waypointList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean charTyped(char character, int keycode) {
        boolean OK = super.charTyped(character, keycode);
        if (this.filter.isFocused()) {
            this.waypointList.updateFilter(this.filter.getText().toLowerCase());
        }

        return OK;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.waypointList.mouseClicked(mouseX, mouseY, mouseButton);
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.waypointList.mouseReleased(mouseX, mouseY, mouseButton);
        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseEvent, double deltaX, double deltaY) {
        return this.waypointList.mouseDragged(mouseX, mouseY, mouseEvent, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.waypointList.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean isEditing() {
        return this.editClicked;
    }

    public void accept(boolean par1) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (par1) {
                this.waypointManager.deleteWaypoint(this.selectedWaypoint);
                this.selectedWaypoint = null;
            }

            this.getMinecraft().setScreen(this);
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (par1) {
                this.waypointManager.saveWaypoints();
            }

            this.getMinecraft().setScreen(this);
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (par1) {
                this.waypointManager.addWaypoint(this.newWaypoint);
                this.setSelectedWaypoint(this.newWaypoint);
            }

            this.getMinecraft().setScreen(this);
        }

    }

    protected void setSelectedWaypoint(Waypoint waypoint) {
        this.selectedWaypoint = waypoint;
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonEdit.active = isSomethingSelected;
        this.buttonDelete.active = isSomethingSelected;
        this.buttonHighlight.active = isSomethingSelected;
        this.buttonHighlight.setMessage(new TranslatableText(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
        this.buttonShare.active = isSomethingSelected;
        this.buttonTeleport.active = isSomethingSelected && this.canTeleport();
    }

    protected void setHighlightedWaypoint() {
        this.waypointManager.setHighlightedWaypoint(this.selectedWaypoint, true);
        this.highlightedWaypoint = this.waypointManager.getHighlightedWaypoint();
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonHighlight.setMessage(new TranslatableText(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
    }

    protected void editWaypoint(Waypoint waypoint) {
        this.editClicked = true;
        this.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, waypoint, true));
    }

    protected void addWaypoint() {
        this.addClicked = true;
        float r;
        float g;
        float b;
        if (this.waypointManager.getWaypoints().size() == 0) {
            r = 0.0F;
            g = 1.0F;
            b = 0.0F;
        } else {
            r = this.generator.nextFloat();
            g = this.generator.nextFloat();
            b = this.generator.nextFloat();
        }

        TreeSet dimensions = new TreeSet();
        dimensions.add(AbstractVoxelMap.getInstance().getDimensionManager().getDimensionContainerByWorld(this.getMinecraft().world));
        double dimensionScale = this.options.game.player.world.getDimension().getCoordinateScale();
        this.newWaypoint = new Waypoint("", (int) ((double) GameVariableAccessShim.xCoord() * dimensionScale), (int) ((double) GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
        this.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, this.newWaypoint, false));
    }

    protected void toggleWaypointVisibility() {
        this.selectedWaypoint.enabled = !this.selectedWaypoint.enabled;
        this.waypointManager.saveWaypoints();
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawMap(matrixStack);
        this.tooltip = null;
        this.waypointList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("minimap.waypoints.filter") + ":", this.getWidth() / 2 - 153, this.getHeight() - 75, 10526880);
        this.filter.render(matrixStack, mouseX, mouseY, partialTicks);
        if (this.tooltip != null) {
            this.renderTooltip(matrixStack, this.tooltip, mouseX, mouseY);
        }

    }

    static Text setTooltip(GuiWaypoints par0GuiWaypoints, Text par1Str) {
        return par0GuiWaypoints.tooltip = par1Str;
    }

    public boolean canTeleport() {
        boolean allowed = false;
        boolean singlePlayer = this.options.game.isIntegratedServerRunning();
        if (singlePlayer) {
            try {
                allowed = this.getMinecraft().getServer().getPlayerManager().isOperator(this.getMinecraft().player.getGameProfile());
            } catch (Exception var4) {
                allowed = this.getMinecraft().getServer().getSaveProperties().areCommandsAllowed();
            }
        } else {
            allowed = true;
        }

        return allowed;
    }

    @Override
    public void removed() {
        this.client.keyboard.setRepeatEvents(false);
        if (this.changedSort) {
            super.removed();
        }

    }
}
