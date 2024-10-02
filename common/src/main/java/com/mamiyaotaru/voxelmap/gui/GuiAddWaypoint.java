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
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    private EditBox waypointZ;
    private EditBox waypointY;
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
    private final ResourceLocation pickerResourceLocation = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/colorpicker.png");
    private final ResourceLocation blank = ResourceLocation.parse("textures/misc/white.png");

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
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
    }

    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointZ = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.waypointY = new EditBox(this.getFontRenderer(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointZ);
        this.addRenderableWidget(this.waypointY);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.buttonEnabled = new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY, 100, 20, Component.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbycolor") + ":     "), button -> this.choosingColor = true, this));
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20, Component.literal(I18n.get("minimap.waypoints.sortbyicon") + ":     "), button -> this.choosingIcon = true, this));
        this.doneButton = new PopupGuiButton(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20, Component.translatable("addServer.add"), button -> this.acceptWaypoint(), this);
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new PopupGuiButton(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20, Component.translatable("gui.cancel"), button -> this.cancelWaypoint(), this));
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
        waypoint.setZ(Integer.parseInt(waypointZ.getValue()));
        waypoint.setY(Integer.parseInt(waypointY.getValue()));

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

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.keyPressed(keyCode, scanCode, modifiers);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointZ.getValue());
                Integer.parseInt(this.waypointY.getValue());
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

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = false;
        if (this.popupOpen()) {
            OK = super.charTyped(chr, modifiers);
            boolean acceptable = !this.waypointName.getValue().isEmpty();

            try {
                Integer.parseInt(this.waypointX.getValue());
                Integer.parseInt(this.waypointZ.getValue());
                Integer.parseInt(this.waypointY.getValue());
            } catch (NumberFormatException var6) {
                acceptable = false;
            }

            this.doneButton.active = acceptable;
        }

        return OK;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.popupOpen()) {
            super.mouseClicked(mouseX, mouseY, button);
            this.waypointName.mouseClicked(mouseX, mouseY, button);
            this.waypointX.mouseClicked(mouseX, mouseY, button);
            this.waypointZ.mouseClicked(mouseX, mouseY, button);
            this.waypointY.mouseClicked(mouseX, mouseY, button);
        } else if (this.choosingColor) {
            if (mouseX >= (this.getWidth() / 2f - 128) && mouseX < (this.getWidth() / 2f + 128) && mouseY >= (this.getHeight() / 2f - 128) && mouseY < (this.getHeight() / 2f + 128)) {
                int color = this.colorManager.getColorPicker().getRGB((int) mouseX - (this.getWidth() / 2 - 128), (int) mouseY - (this.getHeight() / 2 - 128));
                this.waypoint.red = (color >> 16 & 0xFF) / 255.0F;
                this.waypoint.green = (color >> 8 & 0xFF) / 255.0F;
                this.waypoint.blue = (color & 0xFF) / 255.0F;
                this.choosingColor = false;
            }
        } else if (this.choosingIcon) {
            float scScale = (float) VoxelConstants.getMinecraft().getWindow().getGuiScale();
            TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
            float scale = scScale / 2.0F;
            float displayWidthFloat = chooser.getWidth() / scale;
            float displayHeightFloat = chooser.getHeight() / scale;
            if (displayWidthFloat > VoxelConstants.getMinecraft().getWindow().getWidth()) {
                float adj = displayWidthFloat / VoxelConstants.getMinecraft().getWindow().getWidth();
                scale *= adj;
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            if (displayHeightFloat > VoxelConstants.getMinecraft().getWindow().getHeight()) {
                float adj = displayHeightFloat / VoxelConstants.getMinecraft().getWindow().getHeight();
                scale *= adj;
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            int displayWidth = (int) displayWidthFloat;
            int displayHeight = (int) displayHeightFloat;
            if (mouseX >= (this.getWidth() / 2f - displayWidth / 2f) && mouseX < (this.getWidth() / 2f + displayWidth / 2f) && mouseY >= (this.getHeight() / 2f - displayHeight / 2f) && mouseY < (this.getHeight() / 2f + displayHeight / 2f)) {
                float x = ((float) mouseX - (this.getWidth() / 2f - displayWidth / 2f)) * scale;
                float y = ((float) mouseY - (this.getHeight() / 2f - displayHeight / 2f)) * scale;
                Sprite icon = chooser.getIconAt(x, y);
                if (icon != chooser.getMissingImage()) {
                    this.waypoint.imageSuffix = icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                    this.choosingIcon = false;
                }
            }
        }

        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseClicked(mouseX, mouseY, button);
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.popupOpen() && this.dimensionList != null) {
            this.dimensionList.mouseReleased(mouseX, mouseY, button);
        }

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        return !this.popupOpen() || this.dimensionList == null || this.dimensionList.mouseScrolled(mouseX, mouseY, 0, amount);
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public boolean popupOpen() {
        return !this.choosingColor && !this.choosingIcon;
    }

    @Override
    public void popupAction(Popup popup, int action) {
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        float scScale = (float) VoxelConstants.getMinecraft().getWindow().getGuiScale();
        this.tooltip = null;
        this.buttonEnabled.setMessage(Component.literal(I18n.get("minimap.waypoints.enabled") + " " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))));
        if (!this.choosingColor && !this.choosingIcon) {
            this.renderTransparentBackground(drawContext);
        }

        drawContext.drawCenteredString(this.getFontRenderer(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("minimap.waypoints.new") : I18n.get("minimap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("minimap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 10526880);
        drawContext.drawString(this.getFontRenderer(), I18n.get("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 10526880);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Z"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 10526880);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Y"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 10526880);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        super.render(drawContext, mouseX, mouseY, delta);
        OpenGL.glColor4f(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, this.blank);
        drawContext.blit(RenderType::guiTextured, this.blank, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10); //FIXME 1.21.2
        TextureAtlas chooser = this.waypointManager.getTextureAtlasChooser();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        OpenGL.Utils.disp2(chooser.getId());
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
        Sprite icon = chooser.getAtlasSprite("voxelmap:images/waypoints/waypoint" + this.waypoint.imageSuffix + ".png");
        this.drawTexturedModalRect((this.getWidth() / 2f - 25), (buttonListY + 48 + 2), icon, 16.0F, 16.0F);
        if (this.choosingColor || this.choosingIcon) {
            this.renderTransparentBackground(drawContext);
        }

        if (this.choosingColor) {
            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGL.Utils.img2(this.pickerResourceLocation);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            RenderSystem.disableDepthTest();
            drawContext.blit(RenderType::guiTextured, pickerResourceLocation, this.getWidth() / 2 - 128, this.getHeight() / 2 - 128, 0, 0, 256, 256); //FIXME 1.21.2
            RenderSystem.enableDepthTest();
        }

        if (this.choosingIcon) {
            float scale = scScale / 2.0F;
            float displayWidthFloat = chooser.getWidth() / scale;
            float displayHeightFloat = chooser.getHeight() / scale;
            if (displayWidthFloat > VoxelConstants.getMinecraft().getWindow().getWidth()) {
                float adj = displayWidthFloat / VoxelConstants.getMinecraft().getWindow().getWidth();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            if (displayHeightFloat > VoxelConstants.getMinecraft().getWindow().getHeight()) {
                float adj = displayHeightFloat / VoxelConstants.getMinecraft().getWindow().getHeight();
                displayWidthFloat /= adj;
                displayHeightFloat /= adj;
            }

            int displayWidth = (int) displayWidthFloat;
            int displayHeight = (int) displayHeightFloat;
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, this.blank);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            OpenGL.glColor4f(0.0F, 0.0F, 0.0F, 1.0F);
            drawContext.blit(RenderType::guiTextured, blank, this.getWidth() / 2 - displayWidth / 2 - 1, this.getHeight() / 2 - displayHeight / 2 - 1, 0, 0, displayWidth + 2, displayHeight + 2);
            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawContext.blit(RenderType::guiTextured, blank, this.getWidth() / 2 - displayWidth / 2, this.getHeight() / 2 - displayHeight / 2, 0, 0, displayWidth, displayHeight);
            OpenGL.glColor4f(this.waypoint.red, this.waypoint.green, this.waypoint.blue, 1.0F);
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            OpenGL.Utils.disp2(chooser.getId());
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);

            this.drawTexturedModalRect(this.getWidth() / 2f - displayWidth / 2f, this.getHeight() / 2f - displayHeight / 2f, displayWidth, displayHeight);
            if (mouseX >= this.getWidth() / 2 - displayWidth / 2 && mouseX <= this.getWidth() / 2 + displayWidth / 2 && mouseY >= this.getHeight() / 2 - displayHeight / 2 && mouseY <= this.getHeight() / 2 + displayHeight / 2) {
                float x = (mouseX - (this.getWidth() / 2f - displayWidth / 2f)) * scale;
                float y = (mouseY - (this.getHeight() / 2f - displayHeight / 2f)) * scale;
                icon = chooser.getIconAt(x, y);
                if (icon != chooser.getMissingImage()) {
                    this.tooltip = Component.literal(icon.getIconName().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", ""));
                }
            }
            RenderSystem.enableDepthTest();
            OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level())) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else
            this.waypoint.dimensions.add(this.selectedDimension);

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, Sprite icon, float widthIn, float heightIn) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vertexbuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertexbuffer.addVertex(xCoord + 0.0F, yCoord + heightIn, 0).setUv(icon.getMinU(), icon.getMaxV());
        vertexbuffer.addVertex(xCoord + widthIn, yCoord + heightIn, 0).setUv(icon.getMaxU(), icon.getMaxV());
        vertexbuffer.addVertex(xCoord + widthIn, yCoord + 0.0F, 0).setUv(icon.getMaxU(), icon.getMinV());
        vertexbuffer.addVertex(xCoord + 0.0F, yCoord + 0.0F, 0).setUv(icon.getMinU(), icon.getMinV());
        BufferUploader.drawWithShader(vertexbuffer.buildOrThrow());
    }

    public void drawTexturedModalRect(float x, float y, float width, float height) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertexBuffer.addVertex(x + 0.0F, y + height, 0).setUv(0.0F, 1.0F);
        vertexBuffer.addVertex(x + width, y + height, 0).setUv(1.0F, 1.0F);
        vertexBuffer.addVertex(x + width, y + 0.0F, 0).setUv(1.0F, 0.0F);
        vertexBuffer.addVertex(x + 0.0F, y + 0.0F, 0).setUv(0.0F, 0.0F);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
    }
}
