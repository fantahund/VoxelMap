package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiColorPickerContainer;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.IPopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.TreeSet;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    private final MapSettingsManager mapOptions;
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
    private final Waypoint waypoint;
    private boolean choosingColor;
    private boolean choosingIcon;
    private final boolean editing;

    private GuiColorPickerContainer colorPicker;
    private PopupGuiButton colorPickerModeButton;
    private String pickedIconSuffix;
    private PopupGuiButton popupDoneButton;
    private PopupGuiButton popupCancelButton;

    private float red;
    private float green;
    private float blue;
    private String suffix;
    private boolean enabled;
    protected TreeSet<DimensionContainer> dimensions;

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.parentGui = par1GuiScreen;
        this.lastScreen = (Screen) par1GuiScreen;

        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypoint = par2Waypoint;
        this.editing = editing;

        this.red = this.waypoint.red;
        this.green = this.waypoint.green;
        this.blue = this.waypoint.blue;
        this.suffix = this.waypoint.imageSuffix;
        this.enabled = this.waypoint.enabled;
        this.dimensions = new TreeSet<>(this.waypoint.dimensions);

        this.pickedIconSuffix = this.suffix;

    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, Component.empty());
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        this.waypointX.setMaxLength(128);
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointY = new EditBox(this.getFont(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        this.waypointY.setMaxLength(128);
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.waypointZ = new EditBox(this.getFont(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointY);
        this.addRenderableWidget(this.waypointZ);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Component.empty(), button -> this.enabled = !this.enabled, this));
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
        this.popupDoneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() - 28, 150, 20, Component.translatable("gui.done"), button -> this.closePopupAndApplyChanges(), this);
        this.popupCancelButton = new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() - 28, 150, 20, Component.translatable("gui.cancel"), button -> this.closePopupAndCancelChanges(), this);
        boolean simpleMode = this.mapOptions.colorPickerMode == 0;
        this.colorPicker = new GuiColorPickerContainer(this.getWidth() / 2, this.getHeight() / 2, 200, 140, simpleMode, picker -> {});
        this.colorPicker.setColor(ARGB.colorFromFloat(1.0F, this.red, this.green, this.blue));
        this.colorPickerModeButton = new PopupGuiButton(0, 0, 50, 15, this.getColorPickerModeText(this.mapOptions.colorPickerMode), this::updateColorPickerMode, this);
    }

    @Override
    public void removed() {
    }

    private Component getColorPickerModeText(int mode) {
        if (mode == 0) {
            return Component.translatable("options.minimap.colorPickerMode.full");
        } else {
            if (mode == 1) {
                return Component.translatable("options.minimap.colorPickerMode.simple");
            }

            return Component.literal("error");
        }
    }

    private void updateColorPickerMode(Button button) {
        this.mapOptions.setOptionValue(EnumOptionsMinimap.COLOR_PICKER_MODE);
        this.colorPicker.updateMode(this.mapOptions.colorPickerMode == 0);

        button.setMessage(this.getColorPickerModeText(this.mapOptions.colorPickerMode));
    }

    protected void cancelWaypoint() {
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
        waypoint.red = red;
        waypoint.green = green;
        waypoint.blue = blue;
        waypoint.imageSuffix = suffix;
        waypoint.enabled = enabled;
        waypoint.dimensions.clear();
        waypoint.dimensions.addAll(dimensions);

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

    private void closePopupAndApplyChanges() {
        if (this.choosingColor) {
            this.choosingColor = false;
            int color = colorPicker.getColor();
            this.red = ARGB.redFloat(color);
            this.green = ARGB.greenFloat(color);
            this.blue = ARGB.blueFloat(color);
        }

        if (this.choosingIcon) {
            this.choosingIcon = false;
            this.suffix = this.pickedIconSuffix;
        }
    }

    private void closePopupAndCancelChanges() {
        if (this.choosingColor) {
            this.choosingColor = false;
            this.colorPicker.setColor(ARGB.colorFromFloat(1.0F, this.red, this.green, this.blue));
        }

        if (this.choosingIcon) {
            this.choosingIcon = false;
            this.pickedIconSuffix = this.suffix;
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
            this.closePopupAndCancelChanges();
        }

        this.popupDoneButton.keyPressed(keyEvent);
        this.popupCancelButton.keyPressed(keyEvent);

        if (this.choosingColor) {
            this.colorPickerModeButton.keyPressed(keyEvent);
        }

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

        if (this.choosingColor) {
            this.colorPickerModeButton.charTyped(characterEvent);
        }

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

        this.popupDoneButton.mouseClicked(mouseButtonEvent, doubleClick);
        this.popupCancelButton.mouseClicked(mouseButtonEvent, doubleClick);

        if (this.choosingColor) {
            this.colorPicker.mouseClicked(mouseButtonEvent, doubleClick);
            this.colorPickerModeButton.mouseClicked(mouseButtonEvent, doubleClick);
        }

        if (this.choosingIcon && button == 0) {
            Sprite pickedIcon = pickIcon((int) mouseX, (int) mouseY);
            if (pickedIcon != null) {
                this.pickedIconSuffix = WaypointManager.toSimpleName(pickedIcon.getIconName().toString()).replace("selectable/", "");
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (!this.popupOpen()) {
            return super.mouseReleased(mouseButtonEvent);
        }

        this.popupDoneButton.mouseReleased(mouseButtonEvent);
        this.popupCancelButton.mouseReleased(mouseButtonEvent);

        if (this.choosingColor) {
            this.colorPicker.mouseReleased(mouseButtonEvent);
            this.colorPickerModeButton.mouseReleased(mouseButtonEvent);
        }

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (!this.popupOpen()) {
            return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }

        this.popupDoneButton.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        this.popupCancelButton.mouseDragged(mouseButtonEvent, deltaX, deltaY);

        if (this.choosingColor) {
            this.colorPicker.mouseDragged(mouseButtonEvent, deltaX, deltaY);
            this.colorPickerModeButton.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (!this.popupOpen()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        }

        this.popupDoneButton.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        this.popupCancelButton.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);

        if (this.choosingColor) {
            this.colorPickerModeButton.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        }

        return false;
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        return this.popupOpen();
    }

    @Override
    public boolean popupOpen() {
        return this.choosingColor || this.choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics guiGraphics) {
        if (!this.popupOpen()) {
            super.renderBlurredBackground(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (this.enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        drawContext.drawCenteredString(this.getFont(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("minimap.waypoints.new") : I18n.get("minimap.waypoints.edit"), this.getWidth() / 2, 20, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), I18n.get("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "X", this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "Y", this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), "Z", this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        super.render(drawContext, this.popupOpen() ? 0 : mouseX, this.popupOpen() ? 0 : mouseY, delta);

        int buttonListY = this.getHeight() / 6 + 88;
        int color = ARGB.colorFromFloat(1.0F, this.red, this.green, this.blue);

        drawContext.fill(this.getWidth() / 2 - 25, buttonListY + 24 + 5, this.getWidth() / 2 - 25 + 16, buttonListY + 24 + 5 + 10, color);

        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
        Sprite icon = chooser.getAtlasSprite("selectable/" + this.suffix);
        if (icon == chooser.getMissingImage()) {
            icon = chooser.getAtlasSprite(WaypointManager.fallbackIconLocation);
        }
        icon.blit(drawContext, RenderPipelines.GUI_TEXTURED, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);

        if (this.popupOpen()) {
            drawContext.nextStratum();
            drawContext.blurBeforeThisStratum();

            this.renderTransparentBackground(drawContext);

            if (this.choosingColor) {
                // render title
                drawContext.drawCenteredString(this.getFont(), I18n.get("minimap.waypoints.colorPicker.title"), this.getWidth() / 2, 20, 0xFFFFFFFF);

                // render color picker background
                int x0 = this.colorPicker.getX() - (this.colorPicker.getWidth() / 2) - 30;
                int y0 = this.colorPicker.getY() - (this.colorPicker.getHeight() / 2) - 10;
                int x1 = this.colorPicker.getWidth() + 60;
                int y1 = this.colorPicker.getHeight() + 30;
                TooltipRenderUtil.renderTooltipBackground(drawContext, x0, y0, x1, y1, null);

                // render color picker
                this.colorPicker.render(drawContext, mouseX, mouseY, delta);
                int pickerColor = this.colorPicker.getColor();
                int red = ARGB.red(pickerColor);
                int green = ARGB.green(pickerColor);
                int blue = ARGB.blue(pickerColor);

                // render color text
                String text = "R: " + red + ", G: " + green + ", B: " + blue + " (#" + String.format("%06X", pickerColor & 0xFFFFFF) + ")";
                int textX = this.getWidth() / 2 - (this.colorPicker.getWidth() / 2);
                int textY = this.getHeight() / 2 + (this.colorPicker.getHeight() / 2) + 10;
                int textWidth = this.getFont().width(text);
                drawContext.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, pickerColor);
                drawContext.fill(textX - 1, textY, textX + textWidth + 1, textY + 8, ARGB.black(0.15F));
                drawContext.drawString(this.getFont(), text, textX, textY, 0xFFFFFFFF, false);

                // render color picker mode button
                int buttonX = this.getWidth() / 2 + (this.colorPicker.getWidth() / 2) - this.colorPickerModeButton.getWidth() + 18;
                int buttonY = this.getHeight() / 2 + (this.colorPicker.getHeight() / 2) + 6;
                this.colorPickerModeButton.setPosition(buttonX, buttonY);
                this.colorPickerModeButton.render(drawContext, mouseX, mouseY, delta);
            }

            if (this.choosingIcon) {
                // render title
                drawContext.drawCenteredString(this.getFont(), I18n.get("minimap.waypoints.iconPicker.title"), this.getWidth() / 2, 20, 0xFFFFFFFF);

                // render icon picker
                int pickerX = (this.getWidth() - chooser.getWidth()) / 2;
                int pickerY = (this.getHeight() - chooser.getHeight()) / 2;
                drawContext.blit(RenderPipelines.GUI_TEXTURED, chooser.getIdentifier(), pickerX, pickerY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xBFFFFFFF);

                // render selected icon
                Sprite currentIcon = chooser.getAtlasSprite("selectable/" + this.pickedIconSuffix);
                if (currentIcon == chooser.getMissingImage()) {
                    currentIcon = chooser.getAtlasSprite(WaypointManager.fallbackIconLocation);
                }
                int iconX = currentIcon.getOriginX() + pickerX;
                int iconY = currentIcon.getOriginY() + pickerY;
                currentIcon.blit(drawContext, RenderPipelines.GUI_TEXTURED, iconX, iconY, currentIcon.getIconWidth(), currentIcon.getIconHeight(), color);

                // render hovered icon
                Sprite hoveredIcon = pickIcon(mouseX, mouseY);
                if (hoveredIcon != null) {
                    int iconColor = currentIcon.getIconName().equals(hoveredIcon.getIconName()) ? color : 0xFFFFFFFF;
                    int iconWidth = (int) (hoveredIcon.getIconWidth() * 1.25F);
                    int iconHeight = (int) (hoveredIcon.getIconHeight() * 1.25F);
                    iconX = (hoveredIcon.getOriginX() + pickerX) - ((iconWidth - hoveredIcon.getIconWidth()) / 2);
                    iconY = (hoveredIcon.getOriginY() + pickerY) - ((iconHeight - hoveredIcon.getIconHeight()) / 2);

                    hoveredIcon.blit(drawContext, RenderPipelines.GUI_TEXTURED, iconX, iconY, iconWidth, iconHeight, iconColor);

                    this.tooltip = Component.translatable("minimap.waypoints.icon." + WaypointManager.toSimpleName(hoveredIcon.getIconName().toString()).replace("selectable/", ""));
                }
            }

            this.popupDoneButton.render(drawContext, mouseX, mouseY, delta);
            this.popupCancelButton.render(drawContext, mouseX, mouseY, delta);
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
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
        DimensionContainer currentDimension = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level());
        if (this.dimensions.size() > 1 && this.dimensions.contains(this.selectedDimension) && this.selectedDimension != currentDimension) {
            this.dimensions.remove(this.selectedDimension);
        } else {
            this.dimensions.add(this.selectedDimension);
        }

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }
}
