package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelContants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Random;
import java.util.TreeSet;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    private final Screen parentScreen;
    private final IVoxelMap master;
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
    protected Waypoint highlightedWaypoint;
    protected Waypoint newWaypoint = null;
    private final Random generator = new Random();
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
        this.screenTitle = Text.translatable("minimap.waypoints.title");
        VoxelContants.getMinecraft().keyboard.setRepeatEvents(true);
        this.waypointList = new GuiSlotWaypoints(this);
        this.addDrawableChild(this.buttonSortName = new ButtonWidget(this.getWidth() / 2 - 154, 34, 77, 20, Text.translatable("minimap.waypoints.sortbyname"), button -> this.sortClicked(2)));
        this.addDrawableChild(this.buttonSortDistance = new ButtonWidget(this.getWidth() / 2 - 77, 34, 77, 20, Text.translatable("minimap.waypoints.sortbydistance"), button -> this.sortClicked(3)));
        this.addDrawableChild(this.buttonSortCreated = new ButtonWidget(this.getWidth() / 2, 34, 77, 20, Text.translatable("minimap.waypoints.sortbycreated"), button -> this.sortClicked(1)));
        this.addDrawableChild(this.buttonSortColor = new ButtonWidget(this.getWidth() / 2 + 77, 34, 77, 20, Text.translatable("minimap.waypoints.sortbycolor"), button -> this.sortClicked(4)));
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 80, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(this.buttonEdit = new ButtonWidget(this.getWidth() / 2 - 154, this.getHeight() - 52, 74, 20, Text.translatable("selectServer.edit"), button -> this.editWaypoint(this.selectedWaypoint)));
        this.addDrawableChild(this.buttonDelete = new ButtonWidget(this.getWidth() / 2 - 76, this.getHeight() - 52, 74, 20, Text.translatable("selectServer.delete"), button -> this.deleteClicked()));
        this.addDrawableChild(this.buttonHighlight = new ButtonWidget(this.getWidth() / 2 + 2, this.getHeight() - 52, 74, 20, Text.translatable("minimap.waypoints.highlight"), button -> this.setHighlightedWaypoint()));
        this.addDrawableChild(this.buttonTeleport = new ButtonWidget(this.getWidth() / 2 + 80, this.getHeight() - 52, 74, 20, Text.translatable("minimap.waypoints.teleportto"), button -> this.teleportClicked()));
        this.addDrawableChild(this.buttonShare = new ButtonWidget(this.getWidth() / 2 - 154, this.getHeight() - 28, 74, 20, Text.translatable("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(this.selectedWaypoint)));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 - 76, this.getHeight() - 28, 74, 20, Text.translatable("minimap.waypoints.newwaypoint"), button -> this.addWaypoint()));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 2, this.getHeight() - 28, 74, 20, Text.translatable("menu.options"), button -> VoxelContants.getMinecraft().setScreen(new GuiWaypointsOptions(this, this.options))));
        this.addDrawableChild(new ButtonWidget(this.getWidth() / 2 + 80, this.getHeight() - 28, 74, 20, Text.translatable("gui.done"), button -> VoxelContants.getMinecraft().setScreen(this.parentScreen)));
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
            this.buttonSortName.setMessage(Text.literal(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbyname") + " " + arrow));
        } else {
            this.buttonSortName.setMessage(Text.translatable("minimap.waypoints.sortbyname"));
        }

        if (sortKey == 3) {
            this.buttonSortDistance.setMessage(Text.literal(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbydistance") + " " + arrow));
        } else {
            this.buttonSortDistance.setMessage(Text.translatable("minimap.waypoints.sortbydistance"));
        }

        if (sortKey == 1) {
            this.buttonSortCreated.setMessage(Text.literal(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbycreated") + " " + arrow));
        } else {
            this.buttonSortCreated.setMessage(Text.translatable("minimap.waypoints.sortbycreated"));
        }

        if (sortKey == 4) {
            this.buttonSortColor.setMessage(Text.literal(arrow + " " + I18nUtils.getString("minimap.waypoints.sortbycolor") + " " + arrow));
        } else {
            this.buttonSortColor.setMessage(Text.translatable("minimap.waypoints.sortbycolor"));
        }

    }

    private void deleteClicked() {
        String var2 = this.selectedWaypoint.name;
        if (var2 != null) {
            this.deleteClicked = true;
            Text title = Text.translatable("minimap.waypoints.deleteconfirm");
            Text explanation = Text.translatable("selectServer.deleteWarning", var2);
            Text affirm = Text.translatable("selectServer.deleteButton");
            Text deny = Text.translatable("gui.cancel");
            ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
            VoxelContants.getMinecraft().setScreen(confirmScreen);
        }

    }

    private void teleportClicked() {
        boolean mp = !this.client.isIntegratedServerRunning();
        int y = this.selectedWaypoint.getY() > VoxelContants.getMinecraft().world.getBottomY() ? this.selectedWaypoint.getY() : (!VoxelContants.getMinecraft().player.world.getDimension().hasCeiling() ? VoxelContants.getMinecraft().world.getTopY() : 64);
        VoxelContants.getMinecraft().player.sendCommand("tp " + VoxelContants.getMinecraft().player.getName().getString() + " " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
        if (mp) {
            VoxelContants.getMinecraft().player.sendCommand("tppos " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
        }

        VoxelContants.getMinecraft().setScreen(null);
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

            VoxelContants.getMinecraft().setScreen(this);
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (par1) {
                this.waypointManager.saveWaypoints();
            }

            VoxelContants.getMinecraft().setScreen(this);
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (par1) {
                this.waypointManager.addWaypoint(this.newWaypoint);
                this.setSelectedWaypoint(this.newWaypoint);
            }

            VoxelContants.getMinecraft().setScreen(this);
        }

    }

    protected void setSelectedWaypoint(Waypoint waypoint) {
        this.selectedWaypoint = waypoint;
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonEdit.active = isSomethingSelected;
        this.buttonDelete.active = isSomethingSelected;
        this.buttonHighlight.active = isSomethingSelected;
        this.buttonHighlight.setMessage(Text.translatable(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
        this.buttonShare.active = isSomethingSelected;
        this.buttonTeleport.active = isSomethingSelected && this.canTeleport();
    }

    protected void setHighlightedWaypoint() {
        this.waypointManager.setHighlightedWaypoint(this.selectedWaypoint, true);
        this.highlightedWaypoint = this.waypointManager.getHighlightedWaypoint();
        boolean isSomethingSelected = this.selectedWaypoint != null;
        this.buttonHighlight.setMessage(Text.translatable(isSomethingSelected && this.selectedWaypoint == this.highlightedWaypoint ? "minimap.waypoints.removehighlight" : "minimap.waypoints.highlight"));
    }

    protected void editWaypoint(Waypoint waypoint) {
        this.editClicked = true;
        VoxelContants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, waypoint, true));
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

        TreeSet<DimensionContainer> dimensions = new TreeSet<>();
        dimensions.add(AbstractVoxelMap.getInstance().getDimensionManager().getDimensionContainerByWorld(VoxelContants.getMinecraft().world));
        double dimensionScale = VoxelContants.getMinecraft().player.world.getDimension().coordinateScale();
        this.newWaypoint = new Waypoint("", (int) ((double) GameVariableAccessShim.xCoord() * dimensionScale), (int) ((double) GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
        VoxelContants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, this.newWaypoint, false));
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

    static void setTooltip(GuiWaypoints par0GuiWaypoints, Text par1Str) {
        par0GuiWaypoints.tooltip = par1Str;
    }

    public boolean canTeleport() {
        boolean allowed;
        boolean singlePlayer = VoxelContants.getMinecraft().isIntegratedServerRunning();
        if (singlePlayer) {
            try {
                allowed = VoxelContants.getMinecraft().getServer().getPlayerManager().isOperator(VoxelContants.getMinecraft().player.getGameProfile());
            } catch (Exception var4) {
                allowed = VoxelContants.getMinecraft().getServer().getSaveProperties().areCommandsAllowed();
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
