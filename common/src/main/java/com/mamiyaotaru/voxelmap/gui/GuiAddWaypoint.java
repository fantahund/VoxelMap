package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.IPopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import java.util.HashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.ColorRGBA;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    private static final ResourceLocation PICKER = ResourceLocation.parse("voxelmap:images/colorpicker.png");
    private static final ResourceLocation TARGET = ResourceLocation.parse("voxelmap:images/waypoints/target.png");
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
    private final int colorPickerWidth = 200;
    private final int colorPickerHeight = 200;
    private final float red;
    private final float green;
    private final float blue;
    private final String suffix;
    private final boolean enabled;
    private final boolean editing;

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.parentGui = par1GuiScreen;
        this.setParentScreen(this.parentGui);

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
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbycolor") + ":     "), button -> this.choosingColor = true, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbyicon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() - 28, 150, 20, Component.translatable("gui.done"), button -> this.acceptWaypoint(), this);
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() - 28, 150, 20, Component.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
        this.doneButton.active = !this.waypointName.getValue().isEmpty();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
        this.addRenderableWidget(dimensionList);
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

        VoxelConstants.getMinecraft().setScreen(null);
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
            VoxelConstants.getMinecraft().setScreen(null);

            return;
        }

        waypointManager.addWaypoint(waypoint);
        VoxelConstants.getMinecraft().setScreen(null);
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
            if ((keyCode == 257 || keyCode == 335) && acceptable) {
                this.acceptWaypoint();
            }
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

        if (this.choosingColor && button == 0) {
            int pickedColor = pickColor(colorPickerWidth, colorPickerHeight, (int) mouseX, (int) mouseY);
            if (pickedColor != -1) {
                this.waypoint.red = ARGB.red(pickedColor) / 255.0F;
                this.waypoint.green = ARGB.green(pickedColor) / 255.0F;
                this.waypoint.blue = ARGB.blue(pickedColor) / 255.0F;

                this.choosingColor = false;
            }
        }

        if (this.choosingIcon && button == 0) {
            TextureAtlas chooser = waypointManager.getTextureAtlasChooser();

            Sprite pickedIcon = pickIcon((int) mouseX, (int) mouseY);
            if (pickedIcon != chooser.getMissingImage()) {
                this.waypoint.imageSuffix = ((String) pickedIcon.getIconName()).replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");

                this.choosingIcon = false;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        return !this.popupOpen() && super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double deltaX, double deltaY) {
        return !this.popupOpen() && super.mouseDragged(mouseButtonEvent, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return !this.popupOpen() && super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
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
        drawContext.drawString(this.getFont(), I18n.get("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), I18n.get("Y"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        drawContext.drawString(this.getFont(), I18n.get("Z"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 0xFFFFFFFF);
        super.render(drawContext, this.popupOpen() ? 0 : mouseX, this.popupOpen() ? 0 : mouseY, delta);

        int buttonListY = this.getHeight() / 6 + 88;
        int color = this.waypoint.getUnifiedColor();

        drawContext.fill(this.getWidth() / 2 - 25, buttonListY + 24 + 5, this.getWidth() / 2 - 25 + 16, buttonListY + 24 + 5 + 10, color);
        waypointManager.getTextureAtlasChooser().getAtlasSprite("voxelmap:images/waypoints/waypoint" + this.waypoint.imageSuffix + ".png").blit(drawContext, RenderPipelines.GUI_TEXTURED, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);

        if (this.choosingColor || this.choosingIcon) {
            drawContext.fill(0, 0, getWidth(), getHeight(), 0xBF000000);

            if (this.choosingColor) {
                int anchorX = this.getWidth() / 2 - colorPickerWidth / 2;
                int anchorY = this.getHeight() / 2 - colorPickerHeight / 2;

                drawContext.blit(RenderPipelines.GUI_TEXTURED, PICKER, anchorX, anchorY, 0f, 0f, colorPickerWidth, colorPickerHeight, colorPickerWidth, colorPickerHeight);

                int pickedColor = pickColor(colorPickerWidth, colorPickerHeight, mouseX, mouseY);
                if (pickedColor != -1) {
                    int red = ARGB.red(pickedColor);
                    int green = ARGB.green(pickedColor);
                    int blue = ARGB.blue(pickedColor);
                    drawContext.blit(RenderPipelines.GUI_TEXTURED, TARGET, mouseX - 8, mouseY - 8, 0f, 0f, 16, 16, 16, 16);
                    drawContext.drawCenteredString(this.getFont(), "R: " + red + ", G: " + green + ", B: " + blue, this.getWidth() / 2, this.getHeight() / 2 + colorPickerHeight / 2 + 8, pickedColor);
                }
            }

            if (this.choosingIcon) {
                TextureAtlas chooser = waypointManager.getTextureAtlasChooser();

                int anchorX = (int) (this.getWidth() / 2.0F - chooser.getWidth() / 2.0F);
                int anchorY = (int) (this.getHeight() / 2.0F - chooser.getHeight() / 2.0F);

                drawContext.blit(RenderPipelines.GUI_TEXTURED, WaypointManager.resourceTextureAtlasWaypointChooser, anchorX, anchorY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xFFC8C8C8);

                Sprite pickedIcon = pickIcon(mouseX, mouseY);
                if (pickedIcon != chooser.getMissingImage()) {
                    int iconPreviewX = pickedIcon.getOriginX() + anchorX;
                    int iconPreviewY = pickedIcon.getOriginY() + anchorY;
                    pickedIcon.blit(drawContext, RenderPipelines.GUI_TEXTURED, iconPreviewX - 4, iconPreviewY - 4, 40, 40, color);

                    this.tooltip = Component.translatable(((String) (pickedIcon.getIconName())).replace("voxelmap:images/waypoints/", "minimap.waypoints.").replace(".png", ""));
                }
            }
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    private int pickColor(int width, int height, int mouseX, int mouseY) {
        int anchorX = (int) (this.getWidth() / 2.0F - width / 2.0F);
        int anchorY = (int) (this.getHeight() / 2.0F - height / 2.0F);
        if (mouseX >= anchorX && mouseX <= anchorX + width && mouseY >= anchorY && mouseY <= anchorY + height){
            int pickPointX = (int) ((mouseX - anchorX) / (float) width * 255.0F);
            int pickPointY = (int) ((mouseY - anchorY) / (float) height * 255.0F);

            return this.colorManager.getColorPicker().getRGB(pickPointX, pickPointY);
        }

        return -1;
    }

    private Sprite pickIcon(int mouseX, int mouseY) {
        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();

        int anchorX = (int) (this.getWidth() / 2.0F - chooser.getWidth() / 2.0F);
        int anchorY = (int) (this.getHeight() / 2.0F - chooser.getHeight() / 2.0F);

        return chooser.getIconAt(mouseX - anchorX, mouseY - anchorY);
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
