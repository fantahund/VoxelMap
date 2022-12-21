package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.IPopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.interfaces.IColorManager;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

public class GuiAddWaypoint extends GuiScreenMinimap implements IPopupGuiScreen {
    final IVoxelMap master;
    final IWaypointManager waypointManager;
    final IColorManager colorManager;
    private final IGuiWaypoints parentGui;
    private PopupGuiButton doneButton;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension = null;
    private Text tooltip = null;
    private TextFieldWidget waypointName;
    private TextFieldWidget waypointX;
    private TextFieldWidget waypointZ;
    private TextFieldWidget waypointY;
    private PopupGuiButton buttonEnabled;
    protected final Waypoint waypoint;
    private boolean choosingColor = false;
    private boolean choosingIcon = false;
    private final float red;
    private final float green;
    private final float blue;
    private final String suffix;
    private final boolean enabled;
    private final boolean editing;
    private final Identifier pickerResourceLocation = new Identifier("voxelmap", "images/colorpicker.png");
    private final Identifier blank = new Identifier("textures/misc/white.png");

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, IVoxelMap master, Waypoint par2Waypoint, boolean editing) {
        this.master = master;
        this.waypointManager = master.getWaypointManager();
        this.colorManager = master.getColorManager();
        this.parentGui = par1GuiScreen;
        this.waypoint = par2Waypoint;
        this.red = this.waypoint.red;
        this.green = this.waypoint.green;
        this.blue = this.waypoint.blue;
        this.suffix = this.waypoint.imageSuffix;
        this.enabled = this.waypoint.enabled;
        this.editing = editing;
    }

    public void tick() {
        this.waypointName.tick();
        this.waypointX.tick();
        this.waypointY.tick();
        this.waypointZ.tick();
    }

    public void init() {
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(true);
        this.clearChildren();
        this.waypointName = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setText(this.waypoint.name);
        this.waypointX = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setText(this.waypoint.getX() + "");
        this.waypointZ = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setText(this.waypoint.getZ() + "");
        this.waypointY = new TextFieldWidget(this.getFontRenderer(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setText(this.waypoint.getY() + "");
        this.addDrawableChild(this.waypointName);
        this.addDrawableChild(this.waypointX);
        this.addDrawableChild(this.waypointZ);
        this.addDrawableChild(this.waypointY);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addDrawableChild(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Text.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled, this));
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Text.literal(I18nUtils.getString("minimap.waypoints.sortbycolor") + ":     "), button -> this.choosingColor = true, this));
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Text.literal(I18nUtils.getString("minimap.waypoints.sortbyicon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20, Text.translatable("addServer.add"), button -> this.acceptWaypoint(), this);
        this.addDrawableChild(this.doneButton);
        this.addDrawableChild(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20, Text.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
        this.doneButton.active = this.waypointName.getText().length() > 0;
        this.setFocused(this.waypointName);
        this.waypointName.setTextFieldFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
    }

    @Override
    public void removed() {
        if (VoxelConstants.getMinecraft() == null) {
            return;
        }
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(false);
    }

    protected void cancelWaypoint() {
        this.waypoint.red = this.red;
        this.waypoint.green = this.green;
        this.waypoint.blue = this.blue;
        this.waypoint.imageSuffix = this.suffix;
        this.waypoint.enabled = this.enabled;
        if (this.parentGui != null) {
            this.parentGui.accept(false);
        } else {
            if (VoxelConstants.getMinecraft() == null) {
                return;
            }
            VoxelConstants.getMinecraft().setScreen(null);
        }

    }

    protected void acceptWaypoint() {
        this.waypoint.name = this.waypointName.getText();
        this.waypoint.setX(Integer.parseInt(this.waypointX.getText()));
        this.waypoint.setZ(Integer.parseInt(this.waypointZ.getText()));
        this.waypoint.setY(Integer.parseInt(this.waypointY.getText()));
        if (this.parentGui != null) {
            this.parentGui.accept(true);
        } else {
            if (this.editing) {
                this.waypointManager.saveWaypoints();
            } else {
                this.waypointManager.addWaypoint(this.waypoint);
            }
            if (VoxelConstants.getMinecraft() == null) {
                return;
            }
            VoxelConstants.getMinecraft().setScreen(null);
        }

    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.keyPressed(keysm, scancode, b);
            boolean acceptable = this.waypointName.getText().length() > 0;

            try {
                Integer.parseInt(this.waypointX.getText());
                Integer.parseInt(this.waypointZ.getText());
                Integer.parseInt(this.waypointY.getText());
            } catch (NumberFormatException var7) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
            if ((keysm == 257 || keysm == 335) && acceptable) {
                this.acceptWaypoint();
            }
        }

        return OK;
    }

    public boolean charTyped(char character, int keycode) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.charTyped(character, keycode);
            boolean acceptable = this.waypointName.getText().length() > 0;

            try {
                Integer.parseInt(this.waypointX.getText());
                Integer.parseInt(this.waypointZ.getText());
                Integer.parseInt(this.waypointY.getText());
            } catch (NumberFormatException var6) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
        }

        return OK;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.popupOpen()) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            this.waypointName.mouseClicked(mouseX, mouseY, mouseButton);
            this.waypointX.mouseClicked(mouseX, mouseY, mouseButton);
            this.waypointZ.mouseClicked(mouseX, mouseY, mouseButton);
            this.waypointY.mouseClicked(mouseX, mouseY, mouseButton);
        } else if (this.choosingColor) {
            if (mouseX >= (double) (this.getWidth() / 2 - 128) && mouseX < (double) (this.getWidth() / 2 + 128) && mouseY >= (double) (this.getHeight() / 2 - 128) && mouseY < (double) (this.getHeight() / 2 + 128)) {
                int color = this.colorManager.getColorPicker().getRGB((int) mouseX - (this.getWidth() / 2 - 128), (int) mouseY - (this.getHeight() / 2 - 128));
                this.waypoint.red = (float) (color >> 16 & 0xFF) / 255.0F;
                this.waypoint.green = (float) (color >> 8 & 0xFF) / 255.0F;
                this.waypoint.blue = (float) (color & 0xFF) / 255.0F;
                this.choosingColor = false;
            }
        } else if (this.choosingIcon) {
            if (VoxelConstants.getMinecraft() != null) {
                float scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
                TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
                float scale = scScale / 2.0F;
                float displayWidthFloat = (float) chooser.getWidth() / scale;
                float displayHeightFloat = (float) chooser.getHeight() / scale;
                if (displayWidthFloat > (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth()) {
                    float adj = displayWidthFloat / (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
                    scale *= adj;
                    displayWidthFloat /= adj;
                    displayHeightFloat /= adj;
                }

                if (displayHeightFloat > (float) VoxelConstants.getMinecraft().getWindow().getFramebufferHeight()) {
                    float adj = displayHeightFloat / (float) VoxelConstants.getMinecraft().getWindow().getFramebufferHeight();
                    scale *= adj;
                    displayWidthFloat /= adj;
                    displayHeightFloat /= adj;
                }

                int displayWidth = (int) displayWidthFloat;
                int displayHeight = (int) displayHeightFloat;
                if (mouseX >= (double) (this.getWidth() / 2 - displayWidth / 2) && mouseX < (double) (this.getWidth() / 2 + displayWidth / 2) && mouseY >= (double) (this.getHeight() / 2 - displayHeight / 2) && mouseY < (double) (this.getHeight() / 2 + displayHeight / 2)) {
                    float x = ((float) mouseX - (float) (this.getWidth() / 2 - displayWidth / 2)) * scale;
                    float y = ((float) mouseY - (float) (this.getHeight() / 2 - displayHeight / 2)) * scale;
                    Sprite icon = chooser.getIconAt(x, y);
                    if (icon != chooser.getMissingImage()) {
                        this.waypoint.imageSuffix = icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                        this.choosingIcon = false;
                    }
                }
            }
        }

        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseReleased(mouseX, mouseY, mouseButton);
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int mouseEvent, double deltaX, double deltaY) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseDragged(mouseX, mouseY, mouseEvent, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean overPopup(int x, int y) {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public boolean popupOpen() {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (VoxelConstants.getMinecraft() == null) {
            return;
        }
        super.drawMap(matrixStack);
        float scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
        this.tooltip = null;
        this.buttonEnabled.setMessage(Text.literal(I18nUtils.getString("minimap.waypoints.enabled") + " " + (this.waypoint.enabled ? I18nUtils.getString("options.on") : I18nUtils.getString("options.off"))));
        if (!this.choosingColor && !this.choosingIcon) {
            this.renderBackground(matrixStack);
        }

        this.dimensionList.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredText(matrixStack, this.getFontRenderer(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18nUtils.getString("minimap.waypoints.new") : I18nUtils.getString("minimap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 10526880);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("Z"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 10526880);
        drawStringWithShadow(matrixStack, this.getFontRenderer(), I18nUtils.getString("Y"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 10526880);
        this.waypointName.render(matrixStack, mouseX, mouseY, partialTicks);
        this.waypointX.render(matrixStack, mouseX, mouseY, partialTicks);
        this.waypointZ.render(matrixStack, mouseX, mouseY, partialTicks);
        this.waypointY.render(matrixStack, mouseX, mouseY, partialTicks);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        GLShim.glColor4f(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
        GLShim.glDisable(GL11.GL_TEXTURE_2D);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, this.blank);
        this.drawTexture(matrixStack, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10);
        TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        GLUtils.disp2(chooser.getGlId());
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        Sprite icon = chooser.getAtlasSprite("voxelmap:images/waypoints/waypoint" + this.waypoint.imageSuffix + ".png");
        this.drawTexturedModalRect((float) (this.getWidth() / 2 - 25), (float) (buttonListY + 48 + 2), icon, 16.0F, 16.0F);
        if (this.choosingColor || this.choosingIcon) {
            this.renderBackground(matrixStack);
        }

        if (this.choosingColor) {
            GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GLUtils.img2(this.pickerResourceLocation);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            this.drawTexture(matrixStack, this.getWidth() / 2 - 128, this.getHeight() / 2 - 128, 0, 0, 256, 256);
        }

        if (this.choosingIcon) {
            float scale = scScale / 2.0F;
            float displayWidthFloat = (float) chooser.getWidth() / scale;
            float displayHeightFloat = (float) chooser.getHeight() / scale;
            if (displayWidthFloat > (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth()) {
                float adj = displayWidthFloat / (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            if (displayHeightFloat > (float) VoxelConstants.getMinecraft().getWindow().getFramebufferHeight()) {
                float adj = displayHeightFloat / (float) VoxelConstants.getMinecraft().getWindow().getFramebufferHeight();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            int displayWidth = (int) displayWidthFloat;
            int displayHeight = (int) displayHeightFloat;
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, this.blank);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GLShim.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
            this.drawTexture(matrixStack, this.getWidth() / 2 - displayWidth / 2 - 1, this.getHeight() / 2 - displayHeight / 2 - 1, 0, 0, displayWidth + 2, displayHeight + 2);
            GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexture(matrixStack, this.getWidth() / 2 - displayWidth / 2, this.getHeight() / 2 - displayHeight / 2, 0, 0, displayWidth, displayHeight);
            GLShim.glColor4f(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
            GLShim.glEnable(GL11.GL_BLEND);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            GLUtils.disp2(chooser.getGlId());
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            drawTexture(matrixStack, this.getWidth() / 2 - displayWidth / 2, this.getHeight() / 2 - displayHeight / 2, displayWidth, displayHeight, 0.0F, 0.0F, chooser.getWidth(), chooser.getHeight(), chooser.getImageWidth(), chooser.getImageHeight());
            if (mouseX >= this.getWidth() / 2 - displayWidth / 2 && mouseX <= this.getWidth() / 2 + displayWidth / 2 && mouseY >= this.getHeight() / 2 - displayHeight / 2 && mouseY <= this.getHeight() / 2 + displayHeight / 2) {
                float x = (float) (mouseX - (this.getWidth() / 2 - displayWidth / 2)) * scale;
                float y = (float) (mouseY - (this.getHeight() / 2 - displayHeight / 2)) * scale;
                icon = chooser.getIconAt(x, y);
                if (icon != chooser.getMissingImage()) {
                    this.tooltip = Text.literal(icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", ""));
                }
            }

            GLShim.glDisable(GL11.GL_BLEND);
            GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        }

        if (this.tooltip != null) {
            this.renderTooltip(matrixStack, this.tooltip, mouseX, mouseY);
        }

    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != this.master.getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getMinecraft().world)) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else
            this.waypoint.dimensions.add(this.selectedDimension);

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Text par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, Sprite icon, float widthIn, float heightIn) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexbuffer.vertex(xCoord + 0.0F, yCoord + heightIn, this.getZOffset()).texture(icon.getMinU(), icon.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord + heightIn, this.getZOffset()).texture(icon.getMaxU(), icon.getMaxV()).next();
        vertexbuffer.vertex(xCoord + widthIn, yCoord + 0.0F, this.getZOffset()).texture(icon.getMaxU(), icon.getMinV()).next();
        vertexbuffer.vertex(xCoord + 0.0F, yCoord + 0.0F, this.getZOffset()).texture(icon.getMinU(), icon.getMinV()).next();
        tessellator.draw();
    }
}
