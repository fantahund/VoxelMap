package com.mamiyaotaru.voxelmap.gui;

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
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
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
import java.util.function.Consumer;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    private final MapSettingsManager mapOptions;
    private final WaypointManager waypointManager;
    private final IGuiWaypoints parentGui;

    private PopupGuiButton doneButton;
    private GuiListDimensions dimensionList;
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
    private String pickedSuffix;
    private PopupGuiButton popupDoneButton;
    private PopupGuiButton popupCancelButton;

    private float red;
    private float green;
    private float blue;
    private String suffix;
    private boolean enabled;
    protected TreeSet<DimensionContainer> dimensions;

    private DimensionContainer selectedDimension;

    public GuiAddWaypoint(IGuiWaypoints parentGui, Waypoint waypoint, boolean editing) {
        super((Screen) parentGui, Component.translatable(editing ? "minimap.waypoints.edit" : "minimap.waypoints.new"));
        this.parentGui = parentGui;
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();

        this.waypoint = waypoint;
        this.editing = editing;

        red = waypoint.red;
        green = waypoint.green;
        blue = waypoint.blue;
        suffix = waypoint.imageSuffix;
        enabled = waypoint.enabled;
        dimensions = new TreeSet<>(waypoint.dimensions);

        pickedSuffix = suffix;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        clearWidgets();

        dimensionList = new GuiListDimensions(this, getWidth() / 2, getHeight() / 6 + 90, 101, 64);
        waypointName = new EditBox(getFont(), getWidth() / 2 - 100, getHeight() / 6 + 13, 200, 20, Component.empty());
        waypointName.setValue(waypoint.name);
        waypointX = new EditBox(getFont(), getWidth() / 2 - 100, getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        waypointX.setMaxLength(128);
        waypointX.setValue(String.valueOf(waypoint.getX()));
        waypointY = new EditBox(getFont(), getWidth() / 2 - 28, getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        waypointY.setMaxLength(128);
        waypointY.setValue(String.valueOf(waypoint.getY()));
        waypointZ = new EditBox(getFont(), getWidth() / 2 + 44, getHeight() / 6 + 41 + 13, 56, 20, Component.empty());
        waypointZ.setMaxLength(128);
        waypointZ.setValue(String.valueOf(waypoint.getZ()));

        addRenderableWidget(dimensionList);
        addRenderableWidget(waypointName);
        setFocused(waypointName);
        addRenderableWidget(waypointX);
        addRenderableWidget(waypointY);
        addRenderableWidget(waypointZ);


        int buttonListY = getHeight() / 6 + 82 + 6;
        addRenderableWidget(buttonEnabled = new PopupGuiButton(getWidth() / 2 - 101, buttonListY, 100, 20, Component.empty(), button -> enabled = !enabled, this));
        addRenderableWidget(new PopupGuiButton(getWidth() / 2 - 101, buttonListY + 24, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortByColor") + ":     "), button -> choosingColor = true, this));
        addRenderableWidget(new PopupGuiButton(getWidth() / 2 - 101, buttonListY + 48, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortByIcon") + ":     "), button -> choosingIcon = true, this));
        addRenderableWidget(doneButton = new PopupGuiButton(getWidth() / 2 - 155, getHeight() - 26, 150, 20, Component.translatable("gui.done"), button -> acceptWaypoint(), this));
        addRenderableWidget(new PopupGuiButton(getWidth() / 2 + 5, getHeight() - 26, 150, 20, Component.translatable("gui.cancel"), button -> cancelWaypoint(), this));
        doneButton.active = !waypointName.getValue().isEmpty();

        boolean simpleMode = mapOptions.colorPickerMode == 0;
        colorPicker = new GuiColorPickerContainer(getWidth() / 2, getHeight() / 2, 200, 140, simpleMode, picker -> {});
        colorPicker.setColor(ARGB.colorFromFloat(1.0F, red, green, blue));
        colorPickerModeButton = new PopupGuiButton(0, 0, 50, 15, Component.literal(mapOptions.getListValue(EnumOptionsMinimap.COLOR_PICKER_MODE)), this::updateColorPickerMode, this);
        colorPickerModeButton.setTooltip(Tooltip.create(Component.translatable("options.minimap.colorPickerMode")));
        popupDoneButton = new PopupGuiButton(getWidth() / 2 - 155, getHeight() - 26, 150, 20, Component.translatable("gui.done"), button -> closePopupAndApplyChanges(), this);
        popupCancelButton = new PopupGuiButton(getWidth() / 2 + 5, getHeight() - 26, 150, 20, Component.translatable("gui.cancel"), button -> closePopupAndCancelChanges(), this);
    }

    @Override
    public void removed() {
    }

    private void updateColorPickerMode(Button button) {
        mapOptions.cycleListValue(EnumOptionsMinimap.COLOR_PICKER_MODE);
        colorPicker.updateMode(mapOptions.colorPickerMode == 0);

        button.setMessage(Component.literal(mapOptions.getListValue(EnumOptionsMinimap.COLOR_PICKER_MODE)));
    }

    protected void cancelWaypoint() {
        if (parentGui != null) {
            parentGui.accept(false);
            return;
        }

        onClose();
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
            onClose();
            return;
        }

        waypointManager.addWaypoint(waypoint);
        onClose();
    }

    private void closePopupAndApplyChanges() {
        if (choosingColor) {
            choosingColor = false;
            int color = colorPicker.getColor();
            red = ARGB.redFloat(color);
            green = ARGB.greenFloat(color);
            blue = ARGB.blueFloat(color);
        }
        if (choosingIcon) {
            choosingIcon = false;
            suffix = pickedSuffix;
        }
    }

    private void closePopupAndCancelChanges() {
        if (choosingColor) {
            choosingColor = false;
            colorPicker.setColor(ARGB.colorFromFloat(1.0F, red, green, blue));
        }
        if (choosingIcon) {
            choosingIcon = false;
            pickedSuffix = suffix;
        }
    }

    private boolean isWaypointAcceptable() {
        if (popupOpen()) return false;

        try {
            Integer.parseInt(waypointX.getValue());
            Integer.parseInt(waypointY.getValue());
            Integer.parseInt(waypointZ.getValue());

            return !waypointName.getValue().isEmpty();
        } catch (NumberFormatException ignored) {
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        boolean pressed = false;
        if (!popupOpen()) {
            pressed = super.keyPressed(keyEvent);

            boolean acceptable = isWaypointAcceptable();
            doneButton.active = acceptable;
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && acceptable) {
                acceptWaypoint();
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closePopupAndCancelChanges();
        }

        handlePopupEvents(widget -> widget.keyPressed(keyEvent));

        return pressed;
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean pressed = false;
        if (!popupOpen()) {
            pressed = super.charTyped(characterEvent);
            doneButton.active = isWaypointAcceptable();
        }

        handlePopupEvents(widget -> widget.charTyped(characterEvent));

        return pressed;
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();

        if (!popupOpen()) {
            return super.mouseClicked(mouseButtonEvent, doubleClick);
        }

        handlePopupEvents(widget -> widget.mouseClicked(mouseButtonEvent, doubleClick));

        if (choosingIcon && button == 0) {
            Sprite pickedIcon = pickIcon((int) mouseX, (int) mouseY);
            if (pickedIcon != null) {
                pickedSuffix = WaypointManager.toSimpleName(pickedIcon.getIconName().toString()).replace("selectable/", "");
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (!popupOpen()) {
            return super.mouseReleased(mouseButtonEvent);
        }

        handlePopupEvents(widget -> widget.mouseReleased(mouseButtonEvent));

        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (!popupOpen()) {
            return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
        }

        handlePopupEvents(widget -> widget.mouseDragged(mouseButtonEvent, deltaX, deltaY));

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (!popupOpen()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        }

        handlePopupEvents(widget -> widget.mouseScrolled(mouseX, mouseY, horizontalAmount, amount));

        return false;
    }

    private void handlePopupEvents(Consumer<GuiEventListener> action) {
        action.accept(popupDoneButton);
        action.accept(popupCancelButton);

        if (choosingColor) {
            action.accept(colorPicker);
            action.accept(colorPickerModeButton);
        }
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        return popupOpen();
    }

    @Override
    public boolean popupOpen() {
        return choosingColor || choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    @Override
    protected void renderBlurredBackground(GuiGraphics guiGraphics) {
        if (!popupOpen()) {
            super.renderBlurredBackground(guiGraphics);
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, popupOpen() ? 0 : mouseX, popupOpen() ? 0 : mouseY, delta);

        drawContext.drawString(getFont(), I18n.get("minimap.waypoints.name"), getWidth() / 2 - 100, getHeight() / 6, 0xFFFFFFFF);
        drawContext.drawString(getFont(), "X", getWidth() / 2 - 100, getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(getFont(), "Y", getWidth() / 2 - 28, getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(getFont(), "Z", getWidth() / 2 + 44, getHeight() / 6 + 41, 0xFFFFFFFF);

        buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        int buttonListY = getHeight() / 6 + 88;
        int color = ARGB.colorFromFloat(1.0F, red, green, blue);

        drawContext.fill(getWidth() / 2 - 25, buttonListY + 24 + 5, getWidth() / 2 - 25 + 16, buttonListY + 24 + 5 + 10, color);

        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
        Sprite icon = chooser.getAtlasSprite("selectable/" + suffix);
        if (icon == chooser.getMissingImage()) {
            icon = chooser.getAtlasSprite(WaypointManager.fallbackIconLocation);
        }
        icon.blit(drawContext, RenderPipelines.GUI_TEXTURED, getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);

        if (popupOpen()) {
            drawContext.nextStratum();
            drawContext.blurBeforeThisStratum();

            renderTransparentBackground(drawContext);

            if (choosingColor) {
                // render title
                drawContext.drawCenteredString(getFont(), I18n.get("minimap.waypoints.colorPicker.title"), getWidth() / 2, 20, 0xFFFFFFFF);

                // render color picker background
                int x0 = colorPicker.getX() - (colorPicker.getWidth() / 2) - 30;
                int y0 = colorPicker.getY() - (colorPicker.getHeight() / 2) - 10;
                int x1 = colorPicker.getWidth() + 60;
                int y1 = colorPicker.getHeight() + 30;
                TooltipRenderUtil.renderTooltipBackground(drawContext, x0, y0, x1, y1, null);

                // render color picker
                colorPicker.render(drawContext, mouseX, mouseY, delta);
                int pickerColor = colorPicker.getColor();
                int red = ARGB.red(pickerColor);
                int green = ARGB.green(pickerColor);
                int blue = ARGB.blue(pickerColor);

                // render color text
                String text = "R: " + red + ", G: " + green + ", B: " + blue + " (#" + String.format("%06X", pickerColor & 0xFFFFFF) + ")";
                int textX = (getWidth() - colorPicker.getWidth()) / 2;
                int textY = (getHeight() + colorPicker.getHeight()) / 2 + 10;
                int textWidth = getFont().width(text);
                drawContext.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, pickerColor);
                drawContext.fill(textX - 1, textY, textX + textWidth + 1, textY + 8, ARGB.black(0.15F));
                drawContext.drawString(getFont(), text, textX, textY, 0xFFFFFFFF, false);

                // render color picker mode button
                int buttonX = (getWidth() + colorPicker.getWidth()) / 2 - colorPickerModeButton.getWidth() + 18;
                int buttonY = (getHeight() + colorPicker.getHeight()) / 2 + 6;
                colorPickerModeButton.setPosition(buttonX, buttonY);
                colorPickerModeButton.render(drawContext, mouseX, mouseY, delta);
            }

            if (choosingIcon) {
                // render title
                drawContext.drawCenteredString(getFont(), I18n.get("minimap.waypoints.iconPicker.title"), getWidth() / 2, 20, 0xFFFFFFFF);

                // render icon picker
                int pickerX = (getWidth() - chooser.getWidth()) / 2;
                int pickerY = (getHeight() - chooser.getHeight()) / 2;
                drawContext.blit(RenderPipelines.GUI_TEXTURED, chooser.getIdentifier(), pickerX, pickerY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xBFFFFFFF);

                // render selected icon
                Sprite currentIcon = chooser.getAtlasSprite("selectable/" + pickedSuffix);
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

                    Component tooltip = Component.translatable("minimap.waypoints.icon." + WaypointManager.toSimpleName(hoveredIcon.getIconName().toString()).replace("selectable/", ""));
                    RenderUtils.drawTooltip(drawContext, Tooltip.create(tooltip), mouseX, mouseY);
                }
            }

            popupDoneButton.render(drawContext, mouseX, mouseY, delta);
            popupCancelButton.render(drawContext, mouseX, mouseY, delta);
        }
    }

    private Sprite pickIcon(int mouseX, int mouseY) {
        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();

        int anchorX = (int) (getWidth() / 2.0F - chooser.getWidth() / 2.0F);
        int anchorY = (int) (getHeight() / 2.0F - chooser.getHeight() / 2.0F);

        Sprite icon = chooser.getIconAt(mouseX - anchorX, mouseY - anchorY);
        if (icon == chooser.getMissingImage()) {
            return null;
        }

        return icon;
    }

    public void setSelectedDimension(DimensionContainer dimension) {
        selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        DimensionContainer currentDimension = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level());
        if (dimensions.size() > 1 && dimensions.contains(selectedDimension) && selectedDimension != currentDimension) {
            dimensions.remove(selectedDimension);
        } else {
            dimensions.add(selectedDimension);
        }

    }
}
