package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiHSVColorPicker;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.IPopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    final WaypointManager waypointManager;
    final ColorManager colorManager;
    private final IGuiWaypoints parentGui;
    private PopupGuiButton doneButton;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension;
    private Component tooltip;
    private EditBox waypointName;
    private EditBox waypointX;
    private EditBox waypointY;
    private EditBox waypointZ;
    private PopupGuiButton buttonEnabled;
    protected final Waypoint waypoint;
    private boolean choosingColor;
    private boolean choosingIcon;
    private final float red;
    private final float green;
    private final float blue;
    private final String suffix;
    private final boolean enabled;
    private final boolean editing;
    private GuiHSVColorPicker colorPicker;
    private PopupGuiButton popupDoneButton;
    private PopupGuiButton popupCancelButton;

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.parentGui = par1GuiScreen;
        this.lastScreen = (Screen) par1GuiScreen;

        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypoint = par2Waypoint;
        this.red = this.waypoint.red;
        this.green = this.waypoint.green;
        this.blue = this.waypoint.blue;
        this.suffix = this.waypoint.imageSuffix;
        this.enabled = this.waypoint.enabled;
        this.editing = editing;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointY = new EditBox(this.getFont(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.waypointZ = new EditBox(this.getFont(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointY);
        this.addRenderableWidget(this.waypointZ);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Component.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortByColor") + ":     "), button -> this.choosingColor = true, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortByIcon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() - 28, 150, 20, Component.translatable("gui.done"), button -> this.acceptWaypoint(), this);
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() - 28, 150, 20, Component.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
        this.doneButton.active = !this.waypointName.getValue().isEmpty();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
        this.addRenderableWidget(dimensionList);
        this.colorPicker = new GuiHSVColorPicker(this.getWidth() / 2, this.getHeight() / 2, 200, 70, 14, picker -> this.colorPicked(picker.getColor()));
        this.colorPicker.setColor(ARGB.colorFromFloat(1.0F, this.red, this.green, this.blue));
        this.popupDoneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() - 28, 150, 20, Component.translatable("gui.done"), button -> this.closePopup(true), this);
        this.popupCancelButton = new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() - 28, 150, 20, Component.translatable("gui.cancel"), button -> this.closePopup(false), this);
    }

    @Override
    public void removed() {
    }

    protected void cancelWaypoint() {
        waypoint.red = red;
        waypoint.green = green;
        waypoint.blue = blue;
        waypoint.imageSuffix = suffix;
        waypoint.enabled = enabled;

        if (parentGui != null) {
            parentGui.accept(false);
            return;
        }

        this.onClose();
    }

    protected void acceptWaypoint() {
        waypoint.name = waypointName.getValue();
        waypoint.setX(Integer.parseInt(waypointX.getValue()));
        waypoint.setY(Integer.parseInt(waypointY.getValue()));
        waypoint.setZ(Integer.parseInt(waypointZ.getValue()));

        if (parentGui != null) {
            parentGui.accept(true);

            return;
        }

        if (editing) {
            waypointManager.saveWaypoints();
            this.onClose();

            return;
        }

        waypointManager.addWaypoint(waypoint);
        this.onClose();
    }

    private void closePopup(boolean apply) {
        if (this.choosingColor) {
            this.choosingColor = false;
            if (!apply) {
                colorPicker.setColor(ARGB.colorFromFloat(1.0F, red, green, blue));
                waypoint.red = red;
                waypoint.green = green;
                waypoint.blue = blue;
            }
        }

        if (this.choosingIcon) {
            this.choosingIcon = false;
            if (!apply) {
                waypoint.imageSuffix = suffix;
            }
        }

    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        boolean OK = false;
        if (!this.popupOpen()) {
            OK = super.keyPressed(keyEvent);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointY.getValue());
                Integer.parseInt(this.waypointZ.getValue());
            } catch (NumberFormatException var7) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && acceptable) {
                this.acceptWaypoint();
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.closePopup(false);
        }

        this.popupDoneButton.keyPressed(keyEvent);
        this.popupCancelButton.keyPressed(keyEvent);

        return OK;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = false;
        if (!this.popupOpen()) {
            OK = super.charTyped(characterEvent);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointY.getValue());
                Integer.parseInt(this.waypointZ.getValue());
            } catch (NumberFormatException var6) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
        }

        this.popupDoneButton.charTyped(characterEvent);
        this.popupCancelButton.charTyped(characterEvent);

        return OK;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();

        if (!this.popupOpen()) {
            return super.mouseClicked(mouseButtonEvent, doubleClick);
        }

        if (this.choosingColor) {
            this.colorPicker.mouseClicked(mouseButtonEvent);
        }

        if (this.choosingIcon && button == 0) {
            Sprite pickedIcon = pickIcon((int) mouseX, (int) mouseY);
            if (pickedIcon != null) {
                this.waypoint.imageSuffix = WaypointManager.toSimpleName(pickedIcon.getIconName().toString()).replace("selectable/", "");
            }
        }

        this.popupDoneButton.mouseClicked(mouseButtonEvent, doubleClick);
        this.popupCancelButton.mouseClicked(mouseButtonEvent, doubleClick);

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (!this.popupOpen()) {
            return super.mouseReleased(mouseButtonEvent);
        }

        this.popupDoneButton.mouseReleased(mouseButtonEvent);
        this.popupCancelButton.mouseReleased(mouseButtonEvent);

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (!this.popupOpen()) {
            return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }

        if (this.choosingColor) {
            this.colorPicker.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }

        this.popupDoneButton.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        this.popupCancelButton.mouseDragged(mouseButtonEvent, deltaX, deltaY);

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (!this.popupOpen()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        }

        this.popupDoneButton.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        this.popupCancelButton.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);

        return false;
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        return this.choosingColor || this.choosingIcon;
    }

    @Override
    public boolean popupOpen() {
        return this.choosingColor || this.choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        drawContext.drawCenteredString(this.getFont(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("minimap.waypoints.new") : I18n.get("minimap.waypoints.edit"), this.getWidth() / 2, 20, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), I18n.get("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "X", this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "Y", this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "Z", this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        super.render(drawContext, this.popupOpen() ? 0 : mouseX, this.popupOpen() ? 0 : mouseY, delta);

        int buttonListY = this.getHeight() / 6 + 88;
        int color = this.waypoint.getUnifiedColor();

        drawContext.fill(this.getWidth() / 2 - 25, buttonListY + 24 + 5, this.getWidth() / 2 - 25 + 16, buttonListY + 24 + 5 + 10, color);

        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
        Sprite icon = chooser.getAtlasSprite("selectable/" + this.waypoint.imageSuffix);
        if (icon == chooser.getMissingImage()) {
            icon = chooser.getAtlasSprite(WaypointManager.fallbackIconLocation);
        }
        icon.blit(drawContext, RenderPipelines.GUI_TEXTURED, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);

        if (this.popupOpen()) {
            this.renderTransparentBackground(drawContext);

            if (this.choosingColor) {
                drawContext.drawCenteredString(this.getFont(), I18n.get("minimap.waypoints.colorPicker.title"), this.getWidth() / 2, 20, 0xFFFFFFFF);
                this.colorPicker.render(drawContext, mouseX, mouseY, delta);

                int pickerColor = this.colorPicker.getColor();
                int red = ARGB.red(pickerColor);
                int green = ARGB.green(pickerColor);
                int blue = ARGB.blue(pickerColor);
                String hex = String.format("%06X", pickerColor & 0xFFFFFF);
                drawContext.drawCenteredString(this.getFont(), "R: " + red + ", G: " + green + ", B: " + blue + " (#" + hex + ")", this.getWidth() / 2, this.getHeight() / 2 + 75, pickerColor);
            }

            if (this.choosingIcon) {
                drawContext.drawCenteredString(this.getFont(), I18n.get("minimap.waypoints.iconPicker.title"), this.getWidth() / 2, 20, 0xFFFFFFFF);

                int pickerX = (this.getWidth() - chooser.getWidth()) / 2;
                int pickerY = (this.getHeight() - chooser.getHeight()) / 2;

                drawContext.blit(RenderPipelines.GUI_TEXTURED, chooser.getIdentifier(), pickerX, pickerY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xFFC8C8C8);

                int iconX = icon.getOriginX() + pickerX;
                int iconY = icon.getOriginY() + pickerY;
                icon.blit(drawContext, RenderPipelines.GUI_TEXTURED, iconX, iconY, icon.getIconWidth(), icon.getIconHeight(), color);

                Sprite pickedIcon = pickIcon(mouseX, mouseY);
                if (pickedIcon != null) {
                    int iconColor = icon.getIconName().equals(pickedIcon.getIconName()) ? color : 0xFFFFFFFF;
                    int iconWidth = (int) (pickedIcon.getIconWidth() * 1.25F);
                    int iconHeight = (int) (pickedIcon.getIconHeight() * 1.25F);
                    iconX = (pickedIcon.getOriginX() + pickerX) - ((iconWidth - pickedIcon.getIconWidth()) / 2);
                    iconY = (pickedIcon.getOriginY() + pickerY) - ((iconHeight - pickedIcon.getIconHeight()) / 2);

                    pickedIcon.blit(drawContext, RenderPipelines.GUI_TEXTURED, iconX, iconY, iconWidth, iconHeight, iconColor);

                    this.tooltip = Component.translatable("minimap.waypoints.icon." + WaypointManager.toSimpleName(pickedIcon.getIconName().toString()).replace("selectable/", ""));
                }
            }

            this.popupDoneButton.render(drawContext, mouseX, mouseY, delta);
            this.popupCancelButton.render(drawContext, mouseX, mouseY, delta);
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    private void colorPicked(int color) {
        if (this.choosingColor) {
            this.waypoint.red = ARGB.redFloat(color);
            this.waypoint.green = ARGB.greenFloat(color);
            this.waypoint.blue = ARGB.blueFloat(color);
        }
    }

    private Sprite pickIcon(int mouseX, int mouseY) {
        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();

        int anchorX = (int) (this.getWidth() / 2.0F - chooser.getWidth() / 2.0F);
        int anchorY = (int) (this.getHeight() / 2.0F - chooser.getHeight() / 2.0F);

        Sprite icon = chooser.getIconAt(mouseX - anchorX, mouseY - anchorY);
        if (icon == chooser.getMissingImage()) {
            return null;
        }

        return icon;
    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level())) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else {
            this.waypoint.dimensions.add(this.selectedDimension);
        }

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }
}
