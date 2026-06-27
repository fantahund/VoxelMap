package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiColorPickerContainer;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiIconButton;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiListMinimap;
import com.mamiyaotaru.voxelmap.gui.widgets.Popup;
import com.mamiyaotaru.voxelmap.gui.widgets.PopupGuiButton;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.WaypointOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.render.RenderUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.function.Consumer;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    private final MapOptions mapOptions;
    private final WaypointOptions options;
    private final WaypointManager waypointManager;
    private final DimensionManager dimensionManager;
    private final IGuiWaypoints parentGui;

    private PopupGuiButton doneButton;
    private DimensionList dimensionList;
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
    private TreeSet<DimensionContainer> dimensions;

    private DimensionContainer selectedDimension;

    public GuiAddWaypoint(IGuiWaypoints parentGui, Waypoint waypoint, boolean editing) {
        super((Screen) parentGui, Component.translatable(editing ? "minimap.waypoints.edit" : "minimap.waypoints.new"));
        this.parentGui = parentGui;
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        options = VoxelConstants.getVoxelMapInstance().getWaypointOptions();
        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();

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

        dimensionList = new DimensionList(getWidth() / 2, getHeight() / 6 + 90, 101, 64);
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

        boolean simpleMode = mapOptions.colorPickerMode.get() == OptionEnumMinimap.ColorPickerMode.SIMPLE;
        colorPicker = new GuiColorPickerContainer(getWidth() / 2, getHeight() / 2, 200, 140, simpleMode, picker -> {});
        colorPicker.setColor(ARGB.colorFromFloat(1.0F, red, green, blue));
        colorPickerModeButton = new PopupGuiButton(0, 0, 50, 15, Component.literal(mapOptions.colorPickerMode.getValueString()), this::updateColorPickerMode, this);
        colorPickerModeButton.setTooltip(Tooltip.create(Component.translatable(mapOptions.colorPickerMode.getKey())));
        popupDoneButton = new PopupGuiButton(getWidth() / 2 - 155, getHeight() - 26, 150, 20, Component.translatable("gui.done"), button -> closePopupAndApplyChanges(), this);
        popupCancelButton = new PopupGuiButton(getWidth() / 2 + 5, getHeight() - 26, 150, 20, Component.translatable("gui.cancel"), button -> closePopupAndCancelChanges(), this);
    }

    @Override
    public void removed() {
    }

    private void updateColorPickerMode(Button button) {
        mapOptions.colorPickerMode.cycle();
        colorPicker.updateMode(mapOptions.colorPickerMode.get() == OptionEnumMinimap.ColorPickerMode.SIMPLE);

        button.setMessage(Component.literal(mapOptions.colorPickerMode.getValueString()));
    }

    private void cancelWaypoint() {
        if (parentGui != null) {
            parentGui.accept(false);
            return;
        }

        onClose();
    }

    private void acceptWaypoint() {
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
        if (popupOpen()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePopupAndCancelChanges();
            }
            handlePopupEvents(widget -> widget.keyPressed(keyEvent));
            return false;
        }
        boolean pressed = super.keyPressed(keyEvent);
        boolean acceptable = isWaypointAcceptable();
        doneButton.active = acceptable;
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && acceptable) {
            acceptWaypoint();
        }
        return pressed;
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        if (popupOpen()) {
            handlePopupEvents(widget -> widget.keyReleased(keyEvent));
            return false;
        }
        return super.keyReleased(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        if (popupOpen()) {
            handlePopupEvents(widget -> widget.charTyped(characterEvent));
            return false;
        }
        boolean typed = super.charTyped(characterEvent);
        doneButton.active = isWaypointAcceptable();
        return typed;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        double mouseX = mouseButtonEvent.x();
        double mouseY = mouseButtonEvent.y();
        int button = mouseButtonEvent.button();
        if (popupOpen()) {
            if (choosingIcon && button == 0) {
                Sprite pickedIcon = pickIcon((int) mouseX, (int) mouseY);
                if (pickedIcon != null) {
                    pickedSuffix = WaypointManager.toSimpleName(pickedIcon.getIconName().toString()).replace("selectable/", "");
                }
            }
            handlePopupEvents(widget -> widget.mouseClicked(mouseButtonEvent, doubleClick));
            return false;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        if (popupOpen()) {
            handlePopupEvents(widget -> widget.mouseReleased(mouseButtonEvent));
            return false;
        }
        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        if (popupOpen()) {
            handlePopupEvents(widget -> widget.mouseDragged(mouseButtonEvent, deltaX, deltaY));
            return false;
        }
        return super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (popupOpen()) {
            handlePopupEvents(widget -> widget.mouseScrolled(mouseX, mouseY, horizontalAmount, amount));
            return false;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
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
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        if (!popupOpen()) {
            super.extractBlurredBackground(graphics);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, popupOpen() ? 0 : mouseX, popupOpen() ? 0 : mouseY, delta);

        graphics.text(getFont(), I18n.get("minimap.waypoints.name"), getWidth() / 2 - 100, getHeight() / 6, 0xFFFFFFFF);
        graphics.text(getFont(), "X", getWidth() / 2 - 100, getHeight() / 6 + 41, 0xFFFFFFFF);
        graphics.text(getFont(), "Y", getWidth() / 2 - 28, getHeight() / 6 + 41, 0xFFFFFFFF);
        graphics.text(getFont(), "Z", getWidth() / 2 + 44, getHeight() / 6 + 41, 0xFFFFFFFF);

        buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        int buttonListY = getHeight() / 6 + 88;
        int color = ARGB.colorFromFloat(1.0F, red, green, blue);

        graphics.fill(getWidth() / 2 - 25, buttonListY + 24 + 5, getWidth() / 2 - 25 + 16, buttonListY + 24 + 5 + 10, color);

        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
        Sprite icon = chooser.getAtlasSprite("selectable/" + suffix);
        if (icon == chooser.getMissingImage()) {
            icon = chooser.getAtlasSprite(WaypointManager.FALLBACK_ICON_NAME);
        }
        icon.blit(graphics, RenderPipelines.GUI_TEXTURED, getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);

        if (popupOpen()) {
            graphics.nextStratum();
            graphics.blurBeforeThisStratum();

            extractTransparentBackground(graphics);

            if (choosingColor) {
                // render title
                graphics.centeredText(getFont(), I18n.get("minimap.waypoints.colorPicker.title"), getWidth() / 2, 20, 0xFFFFFFFF);

                // render color picker background
                int x0 = colorPicker.getX() - (colorPicker.getWidth() / 2) - 30;
                int y0 = colorPicker.getY() - (colorPicker.getHeight() / 2) - 10;
                int x1 = colorPicker.getWidth() + 60;
                int y1 = colorPicker.getHeight() + 30;
                TooltipRenderUtil.extractTooltipBackground(graphics, x0, y0, x1, y1, null);

                // render color picker
                colorPicker.extractRenderState(graphics, mouseX, mouseY, delta);
                int pickerColor = colorPicker.getColor();
                int red = ARGB.red(pickerColor);
                int green = ARGB.green(pickerColor);
                int blue = ARGB.blue(pickerColor);

                // render color text
                String text = "R: " + red + ", G: " + green + ", B: " + blue + " (#" + String.format("%06X", pickerColor & 0xFFFFFF) + ")";
                int textX = (getWidth() - colorPicker.getWidth()) / 2;
                int textY = (getHeight() + colorPicker.getHeight()) / 2 + 10;
                int textWidth = getFont().width(text);
                graphics.fill(textX - 2, textY - 1, textX + textWidth + 2, textY + 9, pickerColor);
                graphics.fill(textX - 1, textY, textX + textWidth + 1, textY + 8, ARGB.black(0.15F));
                graphics.text(getFont(), text, textX, textY, 0xFFFFFFFF, false);

                // render color picker mode button
                int buttonX = (getWidth() + colorPicker.getWidth()) / 2 - colorPickerModeButton.getWidth() + 18;
                int buttonY = (getHeight() + colorPicker.getHeight()) / 2 + 6;
                colorPickerModeButton.setPosition(buttonX, buttonY);
                colorPickerModeButton.extractRenderState(graphics, mouseX, mouseY, delta);
            }

            if (choosingIcon) {
                // render title
                graphics.centeredText(getFont(), I18n.get("minimap.waypoints.iconPicker.title"), getWidth() / 2, 20, 0xFFFFFFFF);

                // render icon picker
                int pickerX = (getWidth() - chooser.getWidth()) / 2;
                int pickerY = (getHeight() - chooser.getHeight()) / 2;
                for (Sprite pickerIcon : chooser.getUploadedSprites()) {
                    pickerIcon.blit(graphics, RenderPipelines.GUI_TEXTURED, pickerIcon.getOriginX() + pickerX, pickerIcon.getOriginY() + pickerY, pickerIcon.getIconWidth(), pickerIcon.getIconHeight(), 0xBFFFFFFF);
                }

                // render selected icon
                Sprite currentIcon = chooser.getAtlasSprite("selectable/" + pickedSuffix);
                if (currentIcon == chooser.getMissingImage()) {
                    currentIcon = chooser.getAtlasSprite(WaypointManager.FALLBACK_ICON_NAME);
                }
                int iconX = currentIcon.getOriginX() + pickerX;
                int iconY = currentIcon.getOriginY() + pickerY;
                currentIcon.blit(graphics, RenderPipelines.GUI_TEXTURED, iconX, iconY, currentIcon.getIconWidth(), currentIcon.getIconHeight(), color);

                // render hovered icon
                Sprite hoveredIcon = pickIcon(mouseX, mouseY);
                if (hoveredIcon != null) {
                    int iconColor = currentIcon.getIconName().equals(hoveredIcon.getIconName()) ? color : 0xFFFFFFFF;
                    int iconWidth = (int) (hoveredIcon.getIconWidth() * 1.25F);
                    int iconHeight = (int) (hoveredIcon.getIconHeight() * 1.25F);
                    iconX = (hoveredIcon.getOriginX() + pickerX) - ((iconWidth - hoveredIcon.getIconWidth()) / 2);
                    iconY = (hoveredIcon.getOriginY() + pickerY) - ((iconHeight - hoveredIcon.getIconHeight()) / 2);

                    hoveredIcon.blit(graphics, RenderPipelines.GUI_TEXTURED, iconX, iconY, iconWidth, iconHeight, iconColor);

                    Component tooltip = Component.translatable("minimap.waypoints.icon." + WaypointManager.toSimpleName(hoveredIcon.getIconName().toString()).replace("selectable/", ""));
                    RenderUtils.drawTooltip(graphics, Tooltip.create(tooltip), mouseX, mouseY);
                }
            }

            popupDoneButton.extractRenderState(graphics, mouseX, mouseY, delta);
            popupCancelButton.extractRenderState(graphics, mouseX, mouseY, delta);
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

    class DimensionList extends GuiListMinimap<DimensionList.Entry> {
        private static final Tooltip TOOLTIP_APPLIES = Tooltip.create(Component.translatable("minimap.waypoints.dimension.applies"));
        private static final Tooltip TOOLTIP_NOT_APPLIES = Tooltip.create(Component.translatable("minimap.waypoints.dimension.notApplies"));

        private final ArrayList<Entry> dimensionItems;

        public DimensionList(int x, int y, int width, int height) {
            super(x, y, width, height, 18);

            dimensionItems = new ArrayList<>();
            Entry first = null;
            for (DimensionContainer dim : dimensionManager.getDimensions()) {
                Entry item = new Entry(dim);
                dimensionItems.add(item);
                if (dim.equals(dimensions.first())) {
                    first = item;
                }
            }

            dimensionItems.forEach(this::addEntry);

            if (first != null) {
                scrollToEntry(first);
            }
        }

        @Override
        public int getRowWidth() {
            return 100;
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            if (getSelected() != null) {
                GameNarrator narratorManager = new GameNarrator(VoxelConstants.getMinecraft());
                narratorManager.sayChatQueued(Component.translatable("narrator.select", getSelected().dim.name));
            }

            setSelectedDimension(entry.dim);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

        public class Entry extends GuiListMinimap.Entry<Entry> {
            private final DimensionContainer dim;
            private final GuiIconButton dimToggle;

            public Entry(DimensionContainer dim) {
                super(DimensionList.this);
                this.dim = dim;
                addWidget(dimToggle = new GuiIconButton(getX() + getWidth() - 20, getY(), 18, 18, element -> toggleDimensionSelected()));
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                super.extractContent(graphics, mouseX, mouseY, hovered, tickDelta);

                graphics.centeredText(getFont(), dim.getDisplayName(), (GuiAddWaypoint.this.getWidth() + getWidth()) / 2, getY() + 5, 0xFFFFFFFF);

                dimToggle.setPosition(getX() + getWidth() - 20, getY());
                dimToggle.setIcon(dimensions.contains(dim) ? VoxelConstants.getCheckMarkerTexture() : VoxelConstants.getCrossMarkerTexture(), 0xFFFFFFFF);
                dimToggle.setTooltip(dimensions.contains(dim) ? TOOLTIP_APPLIES : TOOLTIP_NOT_APPLIES);
            }
        }
    }
}
