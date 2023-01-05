package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
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
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

public class GuiWaypoints extends GuiScreenMinimap implements IGuiWaypoints {
    private final Screen parentScreen;
    private final VoxelMap master;
    protected final MapSettingsManager options;
    protected final WaypointManager waypointManager;
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

    public GuiWaypoints(Screen parentScreen, VoxelMap master) {
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
        this.waypointList = new GuiSlotWaypoints(this);
        this.addDrawableChild(this.buttonSortName = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.sortbyname"), button -> this.sortClicked(2)).dimensions(this.getWidth() / 2 - 154, 34, 77, 20).build());
        this.addDrawableChild(this.buttonSortDistance = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.sortbydistance"), button -> this.sortClicked(3)).dimensions(this.getWidth() / 2 - 77, 34, 77, 20).build());
        this.addDrawableChild(this.buttonSortCreated = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.sortbycreated"), button -> this.sortClicked(1)).dimensions(this.getWidth() / 2, 34, 77, 20).build());
        this.addDrawableChild(this.buttonSortColor = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.sortbycolor"), button -> this.sortClicked(4)).dimensions(this.getWidth() / 2 + 77, 34, 77, 20).build());
        int filterStringWidth = this.getFontRenderer().getWidth(I18nUtils.getString("minimap.waypoints.filter") + ":");
        this.filter = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 153 + filterStringWidth + 5, this.getHeight() - 80, 305 - filterStringWidth - 5, 20, null);
        this.filter.setMaxLength(35);
        this.addDrawableChild(this.filter);
        this.addDrawableChild(this.buttonEdit = new ButtonWidget.Builder(Text.translatable("selectServer.edit"), button -> this.editWaypoint(this.selectedWaypoint)).dimensions(this.getWidth() / 2 - 154, this.getHeight() - 52, 74, 20).build());
        this.addDrawableChild(this.buttonDelete = new ButtonWidget.Builder(Text.translatable("selectServer.delete"), button -> this.deleteClicked()).dimensions(this.getWidth() / 2 - 76, this.getHeight() - 52, 74, 20).build());
        this.addDrawableChild(this.buttonHighlight = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.highlight"), button -> this.setHighlightedWaypoint()).dimensions(this.getWidth() / 2 + 2, this.getHeight() - 52, 74, 20).build());
        this.addDrawableChild(this.buttonTeleport = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.teleportto"), button -> this.teleportClicked()).dimensions(this.getWidth() / 2 + 80, this.getHeight() - 52, 74, 20).build());
        this.addDrawableChild(this.buttonShare = new ButtonWidget.Builder(Text.translatable("minimap.waypoints.share"), button -> CommandUtils.sendWaypoint(this.selectedWaypoint)).dimensions(this.getWidth() / 2 - 154, this.getHeight() - 28, 74, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("minimap.waypoints.newwaypoint"), button -> this.addWaypoint()).dimensions(this.getWidth() / 2 - 76, this.getHeight() - 28, 74, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("menu.options"), button -> VoxelConstants.getMinecraft().setScreen(new GuiWaypointsOptions(this, this.options))).dimensions(this.getWidth() / 2 + 2, this.getHeight() - 28, 74, 20).build());
        this.addDrawableChild(new ButtonWidget.Builder(Text.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).dimensions(this.getWidth() / 2 + 80, this.getHeight() - 28, 74, 20).build());
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
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        }

    }

    private void teleportClicked() {
        int y = this.selectedWaypoint.getY() > VoxelConstants.getMinecraft().world.getBottomY() ? this.selectedWaypoint.getY() : (!VoxelConstants.getPlayer().world.getDimension().hasCeiling() ? VoxelConstants.getMinecraft().world.getTopY() : 64);
        VoxelConstants.getPlayer().networkHandler.sendCommand("tp " + VoxelConstants.getPlayer().getName().getString() + " " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());

        VoxelConstants.getMinecraft().setScreen(null);
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

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (par1) {
                this.waypointManager.saveWaypoints();
            }

            VoxelConstants.getMinecraft().setScreen(this);
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (par1) {
                this.waypointManager.addWaypoint(this.newWaypoint);
                this.setSelectedWaypoint(this.newWaypoint);
            }

            VoxelConstants.getMinecraft().setScreen(this);
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
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, waypoint, true));
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
        dimensions.add(VoxelMap.getInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getMinecraft().world));
        double dimensionScale = VoxelConstants.getPlayer().world.getDimension().coordinateScale();
        this.newWaypoint = new Waypoint("", (int) ((double) GameVariableAccessShim.xCoord() * dimensionScale), (int) ((double) GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
        VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, this.newWaypoint, false));
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
        Optional<IntegratedServer> integratedServer = VoxelConstants.getIntegratedServer();

        if (integratedServer.isEmpty()) return true;

        try {
            return integratedServer.get().getPlayerManager().isOperator(VoxelConstants.getPlayer().getGameProfile());
        } catch (Exception exception) {
            return integratedServer.get().getSaveProperties().areCommandsAllowed();
        }
    }

    @Override
    public void removed() {

        if (changedSort) super.removed();
    }
}
