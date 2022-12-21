package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.IGuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiScreen;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.AbstractVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IPersistentMap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.util.BiomeMapData;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Random generator = new Random();
    private final IVoxelMap master;
    private final IPersistentMap persistentMap;
    private final IWaypointManager waypointManager;
    private final Screen parent;
    private final MapSettingsManager mapOptions;
    private final PersistentMapSettingsManager options;
    protected String screenTitle = "World Map";
    protected String worldNameDisplay = "";
    protected int worldNameDisplayLength = 0;
    protected int maxWorldNameDisplayLength = 0;
    private String subworldName = "";
    private PopupGuiButton buttonMultiworld;
    private int top;
    private int bottom;
    private boolean oldNorth = false;
    private boolean lastStill = false;
    private boolean editingCoordinates = false;
    private boolean lastEditingCoordinates = false;
    private TextFieldWidget coordinates;
    int centerX = 0;
    int centerY = 0;
    float mapCenterX = 0.0F;
    float mapCenterZ = 0.0F;
    float deltaX = 0.0F;
    float deltaY = 0.0F;
    float deltaXonRelease = 0.0F;
    float deltaYonRelease = 0.0F;
    long timeOfRelease = 0L;
    boolean mouseCursorShown = true;
    long timeAtLastTick = 0L;
    long timeOfLastKBInput = 0L;
    long timeOfLastMouseInput = 0L;
    float lastMouseX = 0.0F;
    float lastMouseY = 0.0F;
    protected int mouseX;
    protected int mouseY;
    boolean leftMouseButtonDown = false;
    float zoom;
    float zoomStart;
    float zoomGoal;
    long timeOfZoom = 0L;
    float zoomDirectX = 0.0F;
    float zoomDirectY = 0.0F;
    private float scScale = 1.0F;
    private float guiToMap = 2.0F;
    private float mapToGui = 0.5F;
    private float mouseDirectToMap = 1.0F;
    private float guiToDirectMouse = 2.0F;
    private static int playerGLID = 0;
    private static boolean gotSkin = false;
    private static int skinTries = 0;
    private boolean closed = false;
    private CachedRegion[] regions = new CachedRegion[0];
    BackgroundImageInfo backGroundImageInfo = null;
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);
    private float mapPixelsX = 0.0F;
    private float mapPixelsY = 0.0F;
    private final Object closedLock = new Object();
    private final KeyBinding keyBindForward = new KeyBinding("key.forward.fake", 17, "key.categories.movement");
    private final KeyBinding keyBindLeft = new KeyBinding("key.left.fake", 30, "key.categories.movement");
    private final KeyBinding keyBindBack = new KeyBinding("key.back.fake", 31, "key.categories.movement");
    private final KeyBinding keyBindRight = new KeyBinding("key.right.fake", 32, "key.categories.movement");
    private final KeyBinding keyBindSprint = new KeyBinding("key.sprint.fake", 29, "key.categories.movement");
    private final InputUtil.Key forwardCode;
    private final InputUtil.Key leftCode;
    private final InputUtil.Key backCode;
    private final InputUtil.Key rightCode;
    private final InputUtil.Key sprintCode;
    final InputUtil.Key nullInput = InputUtil.fromTranslationKey("key.keyboard.unknown");
    private Text multiworldButtonName = null;
    private MutableText multiworldButtonNameRed = null;
    int sideMargin = 10;
    int buttonCount = 5;
    int buttonSeparation = 4;
    int buttonWidth = 66;
    public boolean editClicked = false;
    public boolean deleteClicked = false;
    public boolean addClicked = false;
    Waypoint newWaypoint;
    Waypoint selectedWaypoint;

    public GuiPersistentMap(Screen parent, IVoxelMap master) {
        this.parent = parent;
        this.master = master;
        this.waypointManager = master.getWaypointManager();
        mapOptions = master.getMapOptions();
        this.persistentMap = master.getPersistentMap();
        this.options = master.getPersistentMapOptions();
        this.zoom = this.options.zoom;
        this.zoomStart = this.options.zoom;
        this.zoomGoal = this.options.zoom;
        this.persistentMap.setLightMapArray(master.getMap().getLightmapArray());
        if (!gotSkin && skinTries < 5) {
            this.getSkin();
        }

        this.forwardCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.forwardKey.getBoundKeyTranslationKey());
        this.leftCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.leftKey.getBoundKeyTranslationKey());
        this.backCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.backKey.getBoundKeyTranslationKey());
        this.rightCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.rightKey.getBoundKeyTranslationKey());
        this.sprintCode = InputUtil.fromTranslationKey(VoxelConstants.getMinecraft().options.sprintKey.getBoundKeyTranslationKey());
    }

    private void getSkin() {
        Identifier skinLocation = VoxelConstants.getMinecraft().player.getSkinTexture();
        PlayerSkinTexture imageData = null;

        try {
            if (skinLocation != DefaultSkinHelper.getTexture(VoxelConstants.getMinecraft().player.getUuid())) {
                AbstractClientPlayerEntity.loadSkin(skinLocation, VoxelConstants.getMinecraft().player.getName().getString());
                imageData = (PlayerSkinTexture) VoxelConstants.getMinecraft().getTextureManager().getTexture(skinLocation);
            }
        } catch (Exception ignored) {}

        if (imageData != null) {
            gotSkin = true;
            GLUtils.disp(imageData.getGlId());
        } else {
            ++skinTries;
            GLUtils.img(skinLocation);
        }

        BufferedImage skinImage = ImageUtils.createBufferedImageFromCurrentGLImage();
        boolean showHat = VoxelConstants.getMinecraft().player.isPartVisible(PlayerModelPart.HAT);
        if (showHat) {
            skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
        } else {
            skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
        }

        float scale = (float) skinImage.getWidth() / 8.0F;
        skinImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(skinImage, 2.0F / scale)), true, 1);
        if (playerGLID != 0) {
            GLUtils.glah(playerGLID);
        }

        playerGLID = GLUtils.tex(skinImage);
    }

    public void init() {
        this.passEvents = true;
        this.oldNorth = mapOptions.oldNorth;
        this.centerAt(this.options.mapX, this.options.mapZ);
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(true);
        if (VoxelConstants.getMinecraft().currentScreen == this) {
            this.closed = false;
        }

        this.screenTitle = I18nUtils.getString("worldmap.title");
        this.buildWorldName();
        this.leftMouseButtonDown = false;
        this.sideMargin = 10;
        this.buttonCount = 5;
        this.buttonSeparation = 4;
        this.buttonWidth = (this.width - this.sideMargin * 2 - this.buttonSeparation * (this.buttonCount - 1)) / this.buttonCount;
        this.addDrawableChild(new PopupGuiButton(this.sideMargin, this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("options.minimap.waypoints"), buttonWidget_1 -> VoxelConstants.getMinecraft().setScreen(new GuiWaypoints(this, this.master)), this));
        this.multiworldButtonName = Text.translatable(VoxelConstants.getMinecraft().isConnectedToRealms() ? "menu.online" : "options.worldmap.multiworld");
        this.multiworldButtonNameRed = (Text.translatable(VoxelConstants.getMinecraft().isConnectedToRealms() ? "menu.online" : "options.worldmap.multiworld")).formatted(Formatting.RED);
        if (!VoxelConstants.getMinecraft().isIntegratedServerRunning() && !this.master.getWaypointManager().receivedAutoSubworldName()) {
            this.addDrawableChild(this.buttonMultiworld = new PopupGuiButton(this.sideMargin + (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, this.multiworldButtonName, buttonWidget_1 -> VoxelConstants.getMinecraft().setScreen(new GuiSubworldsSelect(this, this.master)), this));
        }

        this.addDrawableChild(new PopupGuiButton(this.sideMargin + 3 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("menu.options"), null, this) {
            public void onPress() {
                VoxelConstants.getMinecraft().setScreen(new GuiMinimapOptions(GuiPersistentMap.this, GuiPersistentMap.this.master));
            }
        });
        this.addDrawableChild(new PopupGuiButton(this.sideMargin + 4 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Text.translatable("gui.done"), null, this) {
            public void onPress() {
                VoxelConstants.getMinecraft().setScreen(GuiPersistentMap.this.parent);
            }
        });
        this.coordinates = new TextFieldWidget(this.getFontRenderer(), this.sideMargin, 10, 140, 20, null);
        this.top = 32;
        this.bottom = this.getHeight() - 32;
        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        this.scScale = (float) VoxelConstants.getMinecraft().getWindow().getScaleFactor();
        this.mapPixelsX = (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth();
        this.mapPixelsY = (float) (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - (int) (64.0F * this.scScale));
        this.lastStill = false;
        this.timeAtLastTick = System.currentTimeMillis();
        this.keyBindForward.setBoundKey(this.forwardCode);
        this.keyBindLeft.setBoundKey(this.leftCode);
        this.keyBindBack.setBoundKey(this.backCode);
        this.keyBindRight.setBoundKey(this.rightCode);
        this.keyBindSprint.setBoundKey(this.sprintCode);
        VoxelConstants.getMinecraft().options.forwardKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.leftKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.backKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.rightKey.setBoundKey(this.nullInput);
        VoxelConstants.getMinecraft().options.sprintKey.setBoundKey(this.nullInput);
        KeyBinding.updateKeysByCode();
    }

    private void centerAt(int x, int z) {
        if (this.oldNorth) {
            this.mapCenterX = (float) (-z);
            this.mapCenterZ = (float) x;
        } else {
            this.mapCenterX = (float) x;
            this.mapCenterZ = (float) z;
        }

    }

    private void buildWorldName() {
        String worldName = "";
        if (VoxelConstants.getMinecraft().isIntegratedServerRunning()) {
            worldName = VoxelConstants.getMinecraft().getServer().getSaveProperties().getLevelName();
            if (worldName == null || worldName.equals("")) {
                worldName = "Singleplayer World";
            }
        } else {
            ServerInfo serverData = VoxelConstants.getMinecraft().getCurrentServerEntry();
            if (serverData != null) {
                worldName = serverData.name;
            }

            if (worldName == null || worldName.equals("")) {
                worldName = "Multiplayer Server";
            }

            if (this.client.isConnectedToRealms()) {
                worldName = "Realms";
            }
        }

        StringBuilder worldNameBuilder = (new StringBuilder("§r")).append(worldName);
        String subworldName = this.master.getWaypointManager().getCurrentSubworldDescriptor(true);
        this.subworldName = subworldName;
        if ((subworldName == null || subworldName.equals("")) && this.master.getWaypointManager().isMultiworld()) {
            subworldName = "???";
        }

        if (subworldName != null && !subworldName.equals("")) {
            worldNameBuilder.append(" - ").append(subworldName);
        }

        this.worldNameDisplay = worldNameBuilder.toString();
        this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay);

        for (this.maxWorldNameDisplayLength = this.getWidth() / 2 - this.getFontRenderer().getWidth(this.screenTitle) / 2 - this.sideMargin * 2; this.worldNameDisplayLength > this.maxWorldNameDisplayLength && worldName.length() > 5; this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay)) {
            worldName = worldName.substring(0, worldName.length() - 1);
            worldNameBuilder = new StringBuilder(worldName);
            worldNameBuilder.append("...");
            if (subworldName != null && !subworldName.equals("")) {
                worldNameBuilder.append(" - ").append(subworldName);
            }

            this.worldNameDisplay = worldNameBuilder.toString();
        }

        if (subworldName != null && !subworldName.equals("")) {
            while (this.worldNameDisplayLength > this.maxWorldNameDisplayLength && subworldName.length() > 5) {
                worldNameBuilder = new StringBuilder(worldName);
                worldNameBuilder.append("...");
                subworldName = subworldName.substring(0, subworldName.length() - 1);
                worldNameBuilder.append(" - ").append(subworldName);
                this.worldNameDisplay = worldNameBuilder.toString();
                this.worldNameDisplayLength = this.getFontRenderer().getWidth(this.worldNameDisplay);
            }
        }

    }

    private float bindZoom(float zoom) {
        zoom = Math.max(this.options.minZoom, zoom);
        return Math.min(this.options.maxZoom, zoom);
    }

    private float easeOut(float elapsedTime, float startValue, float finalDelta, float totalTime) {
        float value;
        if (elapsedTime == totalTime) {
            value = startValue + finalDelta;
        } else {
            value = finalDelta * (-((float) Math.pow(2.0, -10.0F * elapsedTime / totalTime)) + 1.0F) + startValue;
        }

        return value;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double mouseRoll) {
        this.timeOfLastMouseInput = System.currentTimeMillis();
        this.switchToMouseInput();
        float mouseDirectX = (float) VoxelConstants.getMinecraft().mouse.getX();
        float mouseDirectY = (float) VoxelConstants.getMinecraft().mouse.getY();
        if (mouseRoll != 0.0) {
            if (mouseRoll > 0.0) {
                this.zoomGoal *= 1.26F;
            } else if (mouseRoll < 0.0) {
                this.zoomGoal /= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = mouseDirectX;
            this.zoomDirectY = mouseDirectY;
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (mouseY > (double) this.top && mouseY < (double) this.bottom && mouseButton == 1) {
            this.timeOfLastKBInput = 0L;
            int mouseDirectX = (int) VoxelConstants.getMinecraft().mouse.getX();
            int mouseDirectY = (int) VoxelConstants.getMinecraft().mouse.getY();
            this.createPopup((int) mouseX, (int) mouseY, mouseDirectX, mouseDirectY);
        }

        return super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.popupOpen()) {
            this.coordinates.mouseClicked(mouseX, mouseY, mouseButton);
            this.editingCoordinates = this.coordinates.isFocused();
            if (this.editingCoordinates && !this.lastEditingCoordinates) {
                int x;
                int z;
                if (this.oldNorth) {
                    x = (int) Math.floor(this.mapCenterZ);
                    z = -((int) Math.floor(this.mapCenterX));
                } else {
                    x = (int) Math.floor(this.mapCenterX);
                    z = (int) Math.floor(this.mapCenterZ);
                }

                this.coordinates.setText(x + ", " + z);
                this.coordinates.setEditableColor(16777215);
            }

            this.lastEditingCoordinates = this.editingCoordinates;
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton) || mouseButton == 1;
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        if (!this.editingCoordinates && (VoxelConstants.getMinecraft().options.jumpKey.matchesKey(keysm, scancode) || VoxelConstants.getMinecraft().options.sneakKey.matchesKey(keysm, scancode))) {
            if (VoxelConstants.getMinecraft().options.jumpKey.matchesKey(keysm, scancode)) {
                this.zoomGoal /= 1.26F;
            }

            if (VoxelConstants.getMinecraft().options.sneakKey.matchesKey(keysm, scancode)) {
                this.zoomGoal *= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = (float) (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 2);
            this.zoomDirectY = (float) (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / 2);
            this.switchToKeyboardInput();
        }

        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.keyPressed(keysm, scancode, b);
            boolean isGood = this.isAcceptable(this.coordinates.getText());
            this.coordinates.setEditableColor(isGood ? 16777215 : 16711680);
            if ((keysm == 257 || keysm == 335) && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getText().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }

            if (keysm == 258 && this.coordinates.isFocused()) {
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        if (this.master.getMapOptions().keyBindMenu.matchesKey(keysm, scancode)) {
            keysm = 256;
            scancode = -1;
            b = -1;
        }

        return super.keyPressed(keysm, scancode, b);
    }

    public boolean charTyped(char typedChar, int keyCode) {
        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.charTyped(typedChar, keyCode);
            boolean isGood = this.isAcceptable(this.coordinates.getText());
            this.coordinates.setEditableColor(isGood ? 16777215 : 16711680);
            if (typedChar == '\r' && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getText().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        if (this.master.getMapOptions().keyBindMenu.matchesKey(keyCode, -1)) {
            super.keyPressed(256, -1, -1);
        }

        return super.charTyped(typedChar, keyCode);
    }

    private boolean isAcceptable(String input) {
        try {
            String[] xz = this.coordinates.getText().split(",");
            Integer.valueOf(xz[0].trim());
            Integer.valueOf(xz[1].trim());
            return true;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException var3) {
            return false;
        }
    }

    private void switchToMouseInput() {
        this.timeOfLastKBInput = 0L;
        if (!this.mouseCursorShown) {
            GLFW.glfwSetInputMode(VoxelConstants.getMinecraft().getWindow().getHandle(), 208897, 212993);
        }

        this.mouseCursorShown = true;
    }

    private void switchToKeyboardInput() {
        this.timeOfLastKBInput = System.currentTimeMillis();
        this.mouseCursorShown = false;
        GLFW.glfwSetInputMode(VoxelConstants.getMinecraft().getWindow().getHandle(), 208897, 212995);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.zoomGoal = this.bindZoom(this.zoomGoal);
        if (this.mouseX != mouseX || this.mouseY != mouseY) {
            this.timeOfLastMouseInput = System.currentTimeMillis();
            this.switchToMouseInput();
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float mouseDirectX = (float) VoxelConstants.getMinecraft().mouse.getX();
        float mouseDirectY = (float) VoxelConstants.getMinecraft().mouse.getY();
        if (this.zoom != this.zoomGoal) {
            float previousZoom = this.zoom;
            long timeSinceZoom = System.currentTimeMillis() - this.timeOfZoom;
            if ((float) timeSinceZoom < 700.0F) {
                this.zoom = this.easeOut((float) timeSinceZoom, this.zoomStart, this.zoomGoal - this.zoomStart, 700.0F);
            } else {
                this.zoom = this.zoomGoal;
            }

            float scaledZoom = this.zoom;
            if (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() > 1600) {
                scaledZoom = this.zoom * (float) VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 1600.0F;
            }

            float zoomDelta = this.zoom / previousZoom;
            float zoomOffsetX = (float) this.centerX * this.guiToDirectMouse - this.zoomDirectX;
            float zoomOffsetY = (float) (this.top + this.centerY) * this.guiToDirectMouse - this.zoomDirectY;
            float zoomDeltaX = zoomOffsetX - zoomOffsetX * zoomDelta;
            float zoomDeltaY = zoomOffsetY - zoomOffsetY * zoomDelta;
            this.mapCenterX += zoomDeltaX / scaledZoom;
            this.mapCenterZ += zoomDeltaY / scaledZoom;
        }

        this.options.zoom = this.zoomGoal;
        float scaledZoom = this.zoom;
        if (VoxelConstants.getMinecraft().getWindow().getWidth() > 1600) {
            scaledZoom = this.zoom * (float) VoxelConstants.getMinecraft().getWindow().getWidth() / 1600.0F;
        }

        this.guiToMap = this.scScale / scaledZoom;
        this.mapToGui = 1.0F / this.scScale * scaledZoom;
        this.mouseDirectToMap = 1.0F / scaledZoom;
        this.guiToDirectMouse = this.scScale;
        this.renderBackground(matrixStack);
        if (VoxelConstants.getMinecraft().mouse.wasLeftButtonClicked()) {
            if (!this.leftMouseButtonDown && this.overPopup(mouseX, mouseY)) {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.leftMouseButtonDown = true;
            } else if (this.leftMouseButtonDown) {
                this.deltaX = (this.lastMouseX - mouseDirectX) * this.mouseDirectToMap;
                this.deltaY = (this.lastMouseY - mouseDirectY) * this.mouseDirectToMap;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.deltaXonRelease = this.deltaX;
                this.deltaYonRelease = this.deltaY;
                this.timeOfRelease = System.currentTimeMillis();
            }
        } else {
            long timeSinceRelease = System.currentTimeMillis() - this.timeOfRelease;
            if ((float) timeSinceRelease < 700.0F) {
                this.deltaX = this.deltaXonRelease * (float) Math.exp((float) (-timeSinceRelease) / 350.0F);
                this.deltaY = this.deltaYonRelease * (float) Math.exp((float) (-timeSinceRelease) / 350.0F);
            } else {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.deltaXonRelease = 0.0F;
                this.deltaYonRelease = 0.0F;
            }

            this.leftMouseButtonDown = false;
        }

        long timeSinceLastTick = System.currentTimeMillis() - this.timeAtLastTick;
        this.timeAtLastTick = System.currentTimeMillis();
        if (!this.editingCoordinates) {
            int kbDelta = 5;
            if (this.keyBindSprint.isPressed()) {
                kbDelta = 10;
            }

            if (this.keyBindForward.isPressed()) {
                this.deltaY -= (float) kbDelta / scaledZoom * (float) timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindBack.isPressed()) {
                this.deltaY += (float) kbDelta / scaledZoom * (float) timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindLeft.isPressed()) {
                this.deltaX -= (float) kbDelta / scaledZoom * (float) timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (this.keyBindRight.isPressed()) {
                this.deltaX += (float) kbDelta / scaledZoom * (float) timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }
        }

        this.mapCenterX += this.deltaX;
        this.mapCenterZ += this.deltaY;
        if (this.oldNorth) {
            this.options.mapX = (int) this.mapCenterZ;
            this.options.mapZ = -((int) this.mapCenterX);
        } else {
            this.options.mapX = (int) this.mapCenterX;
            this.options.mapZ = (int) this.mapCenterZ;
        }

        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        int left;
        int right;
        int top;
        int bottom;
        if (this.oldNorth) {
            left = (int) Math.floor((this.mapCenterZ - (float) this.centerY * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterZ + (float) this.centerY * this.guiToMap) / 256.0F);
            top = (int) Math.floor((-this.mapCenterX - (float) this.centerX * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((-this.mapCenterX + (float) this.centerX * this.guiToMap) / 256.0F);
        } else {
            left = (int) Math.floor((this.mapCenterX - (float) this.centerX * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterX + (float) this.centerX * this.guiToMap) / 256.0F);
            top = (int) Math.floor((this.mapCenterZ - (float) this.centerY * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((this.mapCenterZ + (float) this.centerY * this.guiToMap) / 256.0F);
        }

        synchronized (this.closedLock) {
            if (this.closed) {
                return;
            }

            this.regions = this.persistentMap.getRegions(left - 1, right + 1, top - 1, bottom + 1);
        }

        MatrixStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.push();
        GLShim.glColor3f(1.0F, 1.0F, 1.0F);
        modelViewMatrixStack.translate((float) this.centerX - this.mapCenterX * this.mapToGui, (float) (this.top + this.centerY) - this.mapCenterZ * this.mapToGui, 0.0);
        if (this.oldNorth) {
            modelViewMatrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        this.backGroundImageInfo = this.waypointManager.getBackgroundImageInfo();
        if (this.backGroundImageInfo != null) {
            GLUtils.disp2(this.backGroundImageInfo.glid);
            this.drawTexturedModalRect((float) this.backGroundImageInfo.left * this.mapToGui, (float) this.backGroundImageInfo.top * this.mapToGui, (float) this.backGroundImageInfo.width * this.mapToGui, (float) this.backGroundImageInfo.height * this.mapToGui);
        }

        for (CachedRegion region : this.regions) {
            int glid = region.getGLID();
            if (glid != 0) {
                GLUtils.disp2(glid);
                if (mapOptions.filtering) {
                    GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                    GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                } else {
                    GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                    GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                }

                this.drawTexturedModalRect((float) (region.getX() * 256) * this.mapToGui, (float) (region.getZ() * 256) * this.mapToGui, (float) region.getWidth() * this.mapToGui, (float) region.getWidth() * this.mapToGui);
            }
        }

        float cursorX;
        float cursorY;
        if (this.mouseCursorShown) {
            cursorX = mouseDirectX;
            cursorY = mouseDirectY - (float) this.top * this.guiToDirectMouse;
        } else {
            cursorX = (float) (VoxelConstants.getMinecraft().getWindow().getFramebufferWidth() / 2);
            cursorY = (float) (VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() - VoxelConstants.getMinecraft().getWindow().getFramebufferHeight() / 2) - (float) this.top * this.guiToDirectMouse;
        }

        float cursorCoordZ;
        float cursorCoordX;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        if (this.options.showWaypoints) {
            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                this.drawWaypoint(matrixStack, pt, cursorCoordX, cursorCoordZ, null, null, null, null);
            }

            if (this.waypointManager.getHighlightedWaypoint() != null) {
                this.drawWaypoint(matrixStack, this.waypointManager.getHighlightedWaypoint(), cursorCoordX, cursorCoordZ, this.master.getWaypointManager().getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/target.png"), 1.0F, 0.0F, 0.0F);
            }
        }

        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        GLUtils.disp2(playerGLID);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        float playerX = (float) GameVariableAccessShim.xCoordDouble();
        float playerZ = (float) GameVariableAccessShim.zCoordDouble();
        if (this.oldNorth) {
            modelViewMatrixStack.push();
            modelViewMatrixStack.translate(playerX * this.mapToGui, playerZ * this.mapToGui, 0.0);
            modelViewMatrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
            modelViewMatrixStack.translate(-(playerX * this.mapToGui), -(playerZ * this.mapToGui), 0.0);
            RenderSystem.applyModelViewMatrix();
        }

        this.drawTexturedModalRect(-10.0F / this.scScale + playerX * this.mapToGui, -10.0F / this.scScale + playerZ * this.mapToGui, 20.0F / this.scScale, 20.0F / this.scScale);
        if (this.oldNorth) {
            modelViewMatrixStack.pop();
        }

        if (this.oldNorth) {
            modelViewMatrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
        }

        modelViewMatrixStack.translate(-((float) this.centerX - this.mapCenterX * this.mapToGui), -((float) (this.top + this.centerY) - this.mapCenterZ * this.mapToGui), 0.0);
        RenderSystem.applyModelViewMatrix();
        if (mapOptions.biomeOverlay != 0) {
            float biomeScaleX = this.mapPixelsX / 760.0F;
            float biomeScaleY = this.mapPixelsY / 360.0F;
            boolean still = !this.leftMouseButtonDown;
            still = still && this.zoom == this.zoomGoal;
            still = still && this.deltaX == 0.0F && this.deltaY == 0.0F;
            still = still && ThreadManager.executorService.getActiveCount() == 0;
            if (still && !this.lastStill) {
                int column;
                if (this.oldNorth) {
                    column = (int) Math.floor(Math.floor(this.mapCenterZ - (float) this.centerY * this.guiToMap) / 256.0) - (left - 1);
                } else {
                    column = (int) Math.floor(Math.floor(this.mapCenterX - (float) this.centerX * this.guiToMap) / 256.0) - (left - 1);
                }

                for (int x = 0; x < this.biomeMapData.getWidth(); ++x) {
                    for (int z = 0; z < this.biomeMapData.getHeight(); ++z) {
                        float floatMapX;
                        float floatMapZ;
                        if (this.oldNorth) {
                            floatMapX = (float) z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
                            floatMapZ = -((float) x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap));
                        } else {
                            floatMapX = (float) x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap);
                            floatMapZ = (float) z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
                        }

                        int mapX = (int) Math.floor(floatMapX);
                        int mapZ = (int) Math.floor(floatMapZ);
                        int regionX = (int) Math.floor((float) mapX / 256.0F) - (left - 1);
                        int regionZ = (int) Math.floor((float) mapZ / 256.0F) - (top - 1);
                        if (!this.oldNorth && regionX != column || this.oldNorth && regionZ != column) {
                            this.persistentMap.compress();
                        }

                        column = !this.oldNorth ? regionX : regionZ;
                        CachedRegion region = this.regions[regionZ * (right + 1 - (left - 1) + 1) + regionX];
                        int id = -1;
                        if (region.getMapData() != null && region.isLoaded() && !region.isEmpty()) {
                            int inRegionX = mapX - region.getX() * region.getWidth();
                            int inRegionZ = mapZ - region.getZ() * region.getWidth();
                            int height = region.getMapData().getHeight(inRegionX, inRegionZ);
                            int light = region.getMapData().getLight(inRegionX, inRegionZ);
                            if (height != 0 || light != 0) {
                                id = region.getMapData().getBiomeID(inRegionX, inRegionZ);
                            }
                        }

                        this.biomeMapData.setBiomeID(x, z, id);
                    }
                }

                this.persistentMap.compress();
                this.biomeMapData.segmentBiomes();
                this.biomeMapData.findCenterOfSegments(true);
            }

            this.lastStill = still;
            boolean displayStill = !this.leftMouseButtonDown;
            displayStill = displayStill && this.zoom == this.zoomGoal;
            displayStill = displayStill && this.deltaX == 0.0F && this.deltaY == 0.0F;
            if (displayStill) {
                int minimumSize = (int) (20.0F * this.scScale / biomeScaleX);
                minimumSize *= minimumSize;
                ArrayList<AbstractMapData.BiomeLabel> labels = this.biomeMapData.getBiomeLabels();
                GLShim.glDisable(GL11.GL_DEPTH_TEST);

                for (AbstractMapData.BiomeLabel biomeLabel : labels) {
                    if (biomeLabel.segmentSize > minimumSize) {
                        int nameWidth = this.chkLen(biomeLabel.name);
                        float x = (float) biomeLabel.x * biomeScaleX / this.scScale;
                        float z = (float) biomeLabel.z * biomeScaleY / this.scScale;
                        this.write(matrixStack, biomeLabel.name, x - (float) (nameWidth / 2), (float) this.top + z - 3.0F, 16777215);
                    }
                }

                GLShim.glEnable(GL11.GL_DEPTH_TEST);
            }
        }

        modelViewMatrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        if (System.currentTimeMillis() - this.timeOfLastKBInput < 2000L) {
            int scWidth = VoxelConstants.getMinecraft().getWindow().getScaledWidth();
            int scHeight = VoxelConstants.getMinecraft().getWindow().getScaledHeight();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, GUI_ICONS_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(775, 769, 1, 0);
            this.drawTexture(matrixStack, scWidth / 2 - 7, scHeight / 2 - 7, 0, 0, 16, 16);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        } else {
            this.switchToMouseInput();
        }

        this.overlayBackground(0, this.top, 255, 255);
        this.overlayBackground(this.bottom, this.getHeight(), 255, 255);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 16, 16777215);
        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        if (this.master.getMapOptions().coords) {
            if (!this.editingCoordinates) {
                drawStringWithShadow(matrixStack, this.getFontRenderer(), "X: " + x, this.sideMargin, 16, 16777215);
                drawStringWithShadow(matrixStack, this.getFontRenderer(), "Z: " + z, this.sideMargin + 64, 16, 16777215);
            } else {
                this.coordinates.render(matrixStack, mouseX, mouseY, partialTicks);
            }
        }

        if (this.subworldName != null && !this.subworldName.equals(this.master.getWaypointManager().getCurrentSubworldDescriptor(true)) || this.master.getWaypointManager().getCurrentSubworldDescriptor(true) != null && !this.master.getWaypointManager().getCurrentSubworldDescriptor(true).equals(this.subworldName)) {
            this.buildWorldName();
        }

        drawStringWithShadow(matrixStack, this.getFontRenderer(), this.worldNameDisplay, this.getWidth() - this.sideMargin - this.worldNameDisplayLength, 16, 16777215);
        if (this.buttonMultiworld != null) {
            if ((this.subworldName == null || this.subworldName.equals("")) && this.master.getWaypointManager().isMultiworld()) {
                if ((int) (System.currentTimeMillis() / 1000L % 2L) == 0) {
                    this.buttonMultiworld.setMessage(this.multiworldButtonNameRed);
                } else {
                    this.buttonMultiworld.setMessage(this.multiworldButtonName);
                }
            } else {
                this.buttonMultiworld.setMessage(this.multiworldButtonName);
            }
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawWaypoint(MatrixStack matrixStack, Waypoint pt, float cursorCoordX, float cursorCoordZ, Sprite icon, Float r, Float g, Float b) {
        if (pt.inWorld && pt.inDimension && this.isOnScreen(pt.getX(), pt.getZ())) {
            String name = pt.name;
            if (r == null) {
                r = pt.red;
            }

            if (g == null) {
                g = pt.green;
            }

            if (b == null) {
                b = pt.blue;
            }

            float ptX = (float) pt.getX();
            float ptZ = (float) pt.getZ();
            if (this.backGroundImageInfo != null && this.backGroundImageInfo.isInRange((int) ptX, (int) ptZ) || this.persistentMap.isRegionLoaded((int) ptX, (int) ptZ)) {
                ptX += 0.5F;
                ptZ += 0.5F;
                boolean hover = cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
                boolean target = false;
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                TextureAtlas atlas = this.master.getWaypointManager().getTextureAtlas();
                GLUtils.disp2(atlas.getGlId());
                if (icon == null) {
                    icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
                    if (icon == atlas.getMissingImage()) {
                        icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                    }
                } else {
                    name = "";
                    target = true;
                }

                GLShim.glColor4f(r, g, b, !pt.enabled && !target && !hover ? 0.3F : 1.0F);
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GLShim.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                if (this.oldNorth) {
                    matrixStack.push();
                    matrixStack.translate(ptX * this.mapToGui, ptZ * this.mapToGui, 0.0);
                    matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
                    matrixStack.translate(-(ptX * this.mapToGui), -(ptZ * this.mapToGui), 0.0);
                    RenderSystem.applyModelViewMatrix();
                }

                this.drawTexturedModalRect(-16.0F / this.scScale + ptX * this.mapToGui, -16.0F / this.scScale + ptZ * this.mapToGui, icon, 32.0F / this.scScale, 32.0F / this.scScale);
                if (this.oldNorth) {
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                }

                if (mapOptions.biomeOverlay == 0 && this.options.showWaypointNames || target || hover) {
                    float fontScale = 2.0F / this.scScale;
                    int m = this.chkLen(name) / 2;
                    matrixStack.push();
                    matrixStack.scale(fontScale, fontScale, 1.0F);
                    if (this.oldNorth) {
                        matrixStack.translate(ptX * this.mapToGui / fontScale, ptZ * this.mapToGui / fontScale, 0.0);
                        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
                        matrixStack.translate(-(ptX * this.mapToGui / fontScale), -(ptZ * this.mapToGui / fontScale), 0.0);
                        RenderSystem.applyModelViewMatrix();
                    }

                    this.write(matrixStack, name, ptX * this.mapToGui / fontScale - (float) m, ptZ * this.mapToGui / fontScale + 16.0F / this.scScale / fontScale, !pt.enabled && !target && !hover ? 1442840575 : 16777215);
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                    GLShim.glEnable(GL11.GL_BLEND);
                }
            }
        }

    }

    private boolean isOnScreen(int x, int z) {
        int left;
        int right;
        int top;
        int bottom;
        if (this.oldNorth) {
            left = (int) Math.floor((double) this.mapCenterZ - (double) ((float) this.centerY * this.guiToMap) * 1.1);
            right = (int) Math.floor((double) this.mapCenterZ + (double) ((float) this.centerY * this.guiToMap) * 1.1);
            top = (int) Math.floor((double) (-this.mapCenterX) - (double) ((float) this.centerX * this.guiToMap) * 1.1);
            bottom = (int) Math.floor((double) (-this.mapCenterX) + (double) ((float) this.centerX * this.guiToMap) * 1.1);
        } else {
            left = (int) Math.floor((double) this.mapCenterX - (double) ((float) this.centerX * this.guiToMap) * 1.1);
            right = (int) Math.floor((double) this.mapCenterX + (double) ((float) this.centerX * this.guiToMap) * 1.1);
            top = (int) Math.floor((double) this.mapCenterZ - (double) ((float) this.centerY * this.guiToMap) * 1.1);
            bottom = (int) Math.floor((double) this.mapCenterZ + (double) ((float) this.centerY * this.guiToMap) * 1.1);
        }

        return x > left && x < right && z > top && z < bottom;
    }

    public void renderBackground(MatrixStack matrixStack) {
        fill(matrixStack, 0, 0, this.getWidth(), this.getHeight(), -16777216);
    }

    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Screen.OPTIONS_BACKGROUND_TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        vertexBuffer.vertex(0.0, endY, 0.0).texture(0.0F, (float) endY / 32.0F).color(64, 64, 64, endAlpha).next();
        vertexBuffer.vertex(this.getWidth(), endY, 0.0).texture((float) this.width / 32.0F, (float) endY / 32.0F).color(64, 64, 64, endAlpha).next();
        vertexBuffer.vertex(this.getWidth(), startY, 0.0).texture((float) this.width / 32.0F, (float) startY / 32.0F).color(64, 64, 64, startAlpha).next();
        vertexBuffer.vertex(0.0, startY, 0.0).texture(0.0F, (float) startY / 32.0F).color(64, 64, 64, startAlpha).next();
        tessellator.draw();
    }

    public void tick() {
        this.coordinates.tick();
    }

    @Override
    public void removed() {
        VoxelConstants.getMinecraft().options.forwardKey.setBoundKey(this.forwardCode);
        VoxelConstants.getMinecraft().options.leftKey.setBoundKey(this.leftCode);
        VoxelConstants.getMinecraft().options.backKey.setBoundKey(this.backCode);
        VoxelConstants.getMinecraft().options.rightKey.setBoundKey(this.rightCode);
        VoxelConstants.getMinecraft().options.sprintKey.setBoundKey(this.sprintCode);
        this.keyBindForward.setBoundKey(this.nullInput);
        this.keyBindLeft.setBoundKey(this.nullInput);
        this.keyBindBack.setBoundKey(this.nullInput);
        this.keyBindRight.setBoundKey(this.nullInput);
        this.keyBindSprint.setBoundKey(this.nullInput);
        KeyBinding.updateKeysByCode();
        KeyBinding.unpressAll();
        VoxelConstants.getMinecraft().keyboard.setRepeatEvents(false);
        synchronized (this.closedLock) {
            this.closed = true;
            this.persistentMap.getRegions(0, -1, 0, -1);
            this.regions = new CachedRegion[0];
        }
    }

    public void drawTexturedModalRect(float x, float y, float width, float height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexBuffer.vertex(x + 0.0F, y + height, this.getZOffset()).texture(0.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + height, this.getZOffset()).texture(1.0F, 1.0F).next();
        vertexBuffer.vertex(x + width, y + 0.0F, this.getZOffset()).texture(1.0F, 0.0F).next();
        vertexBuffer.vertex(x + 0.0F, y + 0.0F, this.getZOffset()).texture(0.0F, 0.0F).next();
        tessellator.draw();
    }

    public void drawTexturedModalRect(Sprite icon, float x, float y) {
        float width = (float) icon.getIconWidth() / this.scScale;
        float height = (float) icon.getIconHeight() / this.scScale;
        this.drawTexturedModalRect(x, y, icon, width, height);
    }

    public void drawTexturedModalRect(float xCoord, float yCoord, Sprite icon, float widthIn, float heightIn) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexBuffer = tessellator.getBuffer();
        vertexBuffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        vertexBuffer.vertex(xCoord + 0.0F, yCoord + heightIn, this.getZOffset()).texture(icon.getMinU(), icon.getMaxV()).next();
        vertexBuffer.vertex(xCoord + widthIn, yCoord + heightIn, this.getZOffset()).texture(icon.getMaxU(), icon.getMaxV()).next();
        vertexBuffer.vertex(xCoord + widthIn, yCoord + 0.0F, this.getZOffset()).texture(icon.getMaxU(), icon.getMinV()).next();
        vertexBuffer.vertex(xCoord + 0.0F, yCoord + 0.0F, this.getZOffset()).texture(icon.getMinU(), icon.getMinV()).next();
        tessellator.draw();
    }

    private void createPopup(int mouseX, int mouseY, int mouseDirectX, int mouseDirectY) {
        ArrayList<Popup.PopupEntry> entries = new ArrayList<>();
        float cursorX = (float) mouseDirectX;
        float cursorY = (float) mouseDirectY - (float) this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        boolean canTeleport = this.canTeleport();
        canTeleport = canTeleport && (this.persistentMap.isGroundAt(x, z) || this.backGroundImageInfo != null && this.backGroundImageInfo.isGroundAt(x, z));
        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        if (hovered != null && this.waypointManager.getWaypoints().contains(hovered)) {
            Popup.PopupEntry entry = new Popup.PopupEntry(I18nUtils.getString("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString(hovered != this.waypointManager.getHighlightedWaypoint() ? "minimap.waypoints.highlight" : "minimap.waypoints.removehighlight"), 1, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString("minimap.waypoints.teleportto"), 3, true, canTeleport);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString("minimap.waypoints.share"), 2, true, true);
            entries.add(entry);
        } else {
            Popup.PopupEntry entry = new Popup.PopupEntry(I18nUtils.getString("minimap.waypoints.newwaypoint"), 0, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString(hovered == null ? "minimap.waypoints.highlight" : "minimap.waypoints.removehighlight"), 1, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString("minimap.waypoints.teleportto"), 3, true, canTeleport);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18nUtils.getString("minimap.waypoints.share"), 2, true, true);
            entries.add(entry);
        }

        this.createPopup(mouseX, mouseY, mouseDirectX, mouseDirectY, entries);
    }

    private Waypoint getHovered(float cursorCoordX, float cursorCoordZ) {
        Waypoint waypoint = null;

        for (Waypoint pt : this.waypointManager.getWaypoints()) {
            float ptX = (float) pt.getX() + 0.5F;
            float ptZ = (float) pt.getZ() + 0.5F;
            boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
            if (hover) {
                waypoint = pt;
            }
        }

        if (waypoint == null) {
            Waypoint pt = this.waypointManager.getHighlightedWaypoint();
            if (pt != null) {
                float ptX = (float) pt.getX() + 0.5F;
                float ptZ = (float) pt.getZ() + 0.5F;
                boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
                if (hover) {
                    waypoint = pt;
                }
            }
        }

        return waypoint;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.clickedDirectX;
        int mouseDirectY = popup.clickedDirectY;
        float cursorX = (float) mouseDirectX;
        float cursorY = (float) mouseDirectY - (float) this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - (float) this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - (float) this.centerY * this.guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        int y = this.persistentMap.getHeightAt(x, z);
        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        this.editClicked = false;
        this.addClicked = false;
        this.deleteClicked = false;
        double dimensionScale = VoxelConstants.getMinecraft().player.world.getDimension().coordinateScale();
        switch (action) {
            case 0 -> {
                if (hovered != null) {
                    x = hovered.getX();
                    z = hovered.getZ();
                }
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
                dimensions.add(AbstractVoxelMap.getInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getMinecraft().world));
                this.newWaypoint = new Waypoint("", (int) ((double) x * dimensionScale), (int) ((double) z * dimensionScale), y, true, r, g, b, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, this.newWaypoint, false));
            }
            case 1 -> {
                if (hovered != null) {
                    this.waypointManager.setHighlightedWaypoint(hovered, true);
                } else {
                    y = y > VoxelConstants.getMinecraft().player.world.getBottomY() ? y : 64;
                    TreeSet<DimensionContainer> dimensions2 = new TreeSet<>();
                    dimensions2.add(AbstractVoxelMap.getInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getMinecraft().world));
                    Waypoint fakePoint = new Waypoint("", (int) ((double) x * dimensionScale), (int) ((double) z * dimensionScale), y, true, 1.0F, 0.0F, 0.0F, "", this.master.getWaypointManager().getCurrentSubworldDescriptor(false), dimensions2);
                    this.waypointManager.setHighlightedWaypoint(fakePoint, true);
                }
            }
            case 2 -> {
                if (hovered != null) {
                    CommandUtils.sendWaypoint(hovered);
                } else {
                    y = y > VoxelConstants.getMinecraft().player.world.getBottomY() ? y : 64;
                    CommandUtils.sendCoordinate(x, y, z);
                }
            }
            case 3 -> {
                if (hovered != null) {
                    this.selectedWaypoint = hovered;
                    boolean mp = !VoxelConstants.getMinecraft().isInSingleplayer();
                    y = this.selectedWaypoint.getY() > VoxelConstants.getMinecraft().world.getBottomY() ? this.selectedWaypoint.getY() : (!VoxelConstants.getMinecraft().player.world.getDimension().hasCeiling() ? VoxelConstants.getMinecraft().world.getTopY() : 64);
                    VoxelConstants.getMinecraft().player.sendCommand("tp " + VoxelConstants.getMinecraft().player.getName().getString() + " " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
                    if (mp) {
                        VoxelConstants.getMinecraft().player.sendCommand("tppos " + this.selectedWaypoint.getX() + " " + y + " " + this.selectedWaypoint.getZ());
                    } else {
                        VoxelConstants.getMinecraft().setScreen(null);
                    }
                } else {
                    if (y == 0) {
                        y = !VoxelConstants.getMinecraft().player.world.getDimension().hasCeiling() ? VoxelConstants.getMinecraft().world.getTopY() : 64;
                    }

                    VoxelConstants.getMinecraft().player.sendCommand("tp " + VoxelConstants.getMinecraft().player.getName().getString() + " " + x + " " + y + " " + z);
                    if (!VoxelConstants.getMinecraft().isInSingleplayer()) {
                        VoxelConstants.getMinecraft().player.sendCommand("tppos " + x + " " + y + " " + z);
                    }
                }
            }
            case 4 -> {
                if (hovered != null) {
                    this.editClicked = true;
                    this.selectedWaypoint = hovered;
                    VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(this, this.master, hovered, true));
                }
            }
            case 5 -> {
                if (hovered != null) {
                    this.deleteClicked = true;
                    this.selectedWaypoint = hovered;
                    Text title = Text.translatable("minimap.waypoints.deleteconfirm");
                    Text explanation = Text.translatable("selectServer.deleteWarning", this.selectedWaypoint.name);
                    Text affirm = Text.translatable("selectServer.deleteButton");
                    Text deny = Text.translatable("gui.cancel");
                    ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
                    VoxelConstants.getMinecraft().setScreen(confirmScreen);
                }
            }
            default -> VoxelConstants.getLogger().warn("unimplemented command");
        }

    }

    @Override
    public boolean isEditing() {
        return this.editClicked;
    }

    public void accept(boolean confirm) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (confirm) {
                this.waypointManager.deleteWaypoint(this.selectedWaypoint);
                this.selectedWaypoint = null;
            }
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (confirm) {
                this.waypointManager.saveWaypoints();
            }
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (confirm) {
                this.waypointManager.addWaypoint(this.newWaypoint);
            }
        }

        VoxelConstants.getMinecraft().setScreen(this);
    }

    public boolean canTeleport() {
        boolean allowed;
        boolean singlePlayer = VoxelConstants.getMinecraft().isInSingleplayer();
        if (singlePlayer) {
            try {
                allowed = VoxelConstants.getMinecraft().getServer().getPlayerManager().isOperator(VoxelConstants.getMinecraft().player.getGameProfile());
            } catch (Exception var4) {
                allowed = VoxelConstants.getMinecraft().getServer().getSaveProperties().areCommandsAllowed();
            }
        } else {
            allowed = true;
        }

        return allowed;
    }

    private int chkLen(String string) {
        return this.getFontRenderer().getWidth(string);
    }

    private void write(MatrixStack matrixStack, String string, float x, float y, int color) {
        this.getFontRenderer().drawWithShadow(matrixStack, string, x, y, color);
    }
}
