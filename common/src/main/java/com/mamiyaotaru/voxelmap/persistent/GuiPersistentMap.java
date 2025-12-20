package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.IGuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiScreen;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.util.BiomeMapData;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.EasingUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
// TODO: 1.20.1 Port - RenderPipelines doesn't exist in 1.20.1
// import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Random generator = new Random();
    private final PersistentMap persistentMap;
    private final WaypointManager waypointManager;
    private final Screen parent;
    private final MapSettingsManager mapOptions;
    private final PersistentMapSettingsManager options;
    protected String screenTitle = "World Map";
    protected String worldNameDisplay = "";
    protected int worldNameDisplayLength;
    protected int maxWorldNameDisplayLength;
    private String subworldName = "";
    private PopupGuiButton buttonMultiworld;
    private int top;
    private int bottom;
    private boolean oldNorth;
    private boolean lastStill;
    private boolean editingCoordinates;
    private boolean lastEditingCoordinates;
    private EditBox coordinates;
    int centerX;
    int centerY;
    float mapCenterX;
    float mapCenterZ;
    float deltaX;
    float deltaY;
    float deltaXonRelease;
    float deltaYonRelease;
    long timeOfRelease;
    boolean mouseCursorShown = true;
    long timeAtLastTick;
    long timeOfLastKBInput;
    long timeOfLastMouseInput;
    float lastMouseX;
    float lastMouseY;
    protected int mouseX;
    protected int mouseY;
    boolean leftMouseButtonDown;
    float zoom;
    float zoomStart;
    float zoomGoal;
    long timeOfZoom;
    float zoomDirectX;
    float zoomDirectY;
    private float scScale = 1.0F;
    private float guiToMap = 2.0F;
    private float mapToGui = 0.5F;
    private float mouseDirectToMap = 1.0F;
    private float guiToDirectMouse = 2.0F;
    private static boolean gotSkin;
    private boolean closed;
    private CachedRegion[] regions = new CachedRegion[0];
    BackgroundImageInfo backGroundImageInfo;
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);
    private float mapPixelsX;
    private float mapPixelsY;
    private final Object closedLock = new Object();
    private Component multiworldButtonName;
    private MutableComponent multiworldButtonNameRed;
    int sideMargin = 10;
    int buttonCount = 5;
    int buttonSeparation = 4;
    int buttonWidth = 66;
    public boolean editClicked;
    public boolean deleteClicked;
    public boolean addClicked;
    Waypoint newWaypoint;
    Waypoint selectedWaypoint;
    public boolean passEvents;
    private PopupGuiButton buttonWaypoints;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final ResourceLocation voxelmapSkinLocation = new ResourceLocation("voxelmap", "persistentmap/playerskin");
    private final ResourceLocation crosshairResource = new ResourceLocation("textures/gui/sprites/hud/crosshair.png");
    private boolean currentDragging;
    private boolean keySprintPressed;
    private boolean keyUpPressed;
    private boolean keyDownPressed;
    private boolean keyLeftPressed;
    private boolean keyRightPressed;
    private int iconsWidth = 16;
    private int iconsHeight = 16;

    public GuiPersistentMap(Screen parent) {
        this.parent = parent;
        this.setParentScreen(this.parent);

        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        this.zoom = this.options.zoom;
        this.zoomStart = this.options.zoom;
        this.zoomGoal = this.options.zoom;
        this.persistentMap.setLightMapArray(VoxelConstants.getVoxelMapInstance().getMap().getLightmapArray());
        if (!gotSkin) {
            this.getSkin();
        }

    }

    private void getSkin() {
        BufferedImage skinImage = ImageUtils.createBufferedImageFromIdentifier(VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(VoxelConstants.getPlayer().getGameProfile()).texture());

        if (skinImage == null) {
            if (VoxelConstants.DEBUG) {
                VoxelConstants.getLogger().warn("Got no player skin!");
            }
            return;
        }

        gotSkin = true;

        boolean showHat = VoxelConstants.getPlayer().isModelPartShown(PlayerModelPart.HAT);
        if (showHat) {
            skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
        } else {
            skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
        }

        float scale = skinImage.getWidth() / 8.0F;
        skinImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(skinImage, 2.0F / scale)), true, 1);

        DynamicTexture texture = new DynamicTexture(ImageUtils.nativeImageFromBufferedImage(skinImage));
        minecraft.getTextureManager().register(voxelmapSkinLocation, texture);
    }

    @Override
    public void init() {
        this.passEvents = true;
        this.oldNorth = mapOptions.oldNorth;
        this.centerAt(this.options.mapX, this.options.mapZ);
        if (minecraft.screen == this) {
            this.closed = false;
        }

        this.screenTitle = I18n.get("worldmap.title");
        this.buildWorldName();
        this.leftMouseButtonDown = false;
        this.sideMargin = 10;
        this.buttonCount = 5;
        this.buttonSeparation = 4;
        this.buttonWidth = (this.width - this.sideMargin * 2 - this.buttonSeparation * (this.buttonCount - 1)) / this.buttonCount;
        this.buttonWaypoints = new PopupGuiButton(this.sideMargin, this.getHeight() - 28, this.buttonWidth, 20, Component.translatable("options.minimap.waypoints"), button -> minecraft.setScreen(new GuiWaypoints(this)), this);
        this.addRenderableWidget(this.buttonWaypoints);
        this.multiworldButtonName = Component.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.worldmap.multiworld");
        this.multiworldButtonNameRed = (Component.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.worldmap.multiworld")).withStyle(ChatFormatting.RED);
        if (!minecraft.hasSingleplayerServer() && !VoxelConstants.getVoxelMapInstance().getWaypointManager().receivedAutoSubworldName()) {
            this.addRenderableWidget(this.buttonMultiworld = new PopupGuiButton(this.sideMargin + (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, this.multiworldButtonName, button -> minecraft.setScreen(new GuiSubworldsSelect(this)), this));
        }

        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 3 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Component.translatable("menu.options"), button -> minecraft.setScreen(new GuiMinimapOptions(this)), this));
        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 4 * (this.buttonWidth + this.buttonSeparation), this.getHeight() - 28, this.buttonWidth, 20, Component.translatable("gui.done"), button -> minecraft.setScreen(parent), this));
        this.coordinates = new EditBox(this.getFont(), this.sideMargin, 10, 140, 20, null);
        this.top = 32;
        this.bottom = this.getHeight() - 32;
        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        this.scScale = (float) minecraft.getWindow().getGuiScale();
        this.mapPixelsX = minecraft.getWindow().getWidth();
        this.mapPixelsY = (minecraft.getWindow().getHeight() - (int) (64.0F * this.scScale));
        this.lastStill = false;
        this.timeAtLastTick = System.currentTimeMillis();
    }

    @Override
    public void added() {
        currentDragging = false;
        super.added();
    }

    private void centerAt(int x, int z) {
        if (this.oldNorth) {
            this.mapCenterX = (-z);
            this.mapCenterZ = x;
        } else {
            this.mapCenterX = x;
            this.mapCenterZ = z;
        }

    }

    private void buildWorldName() {
        final AtomicReference<String> worldName = new AtomicReference<>();

        VoxelConstants.getIntegratedServer().ifPresentOrElse(integratedServer -> {
            worldName.set(integratedServer.getWorldData().getLevelName());

            if (worldName.get() == null || worldName.get().isBlank()) {
                worldName.set("Singleplayer World");
            }
        }, () -> {
            ServerData info = minecraft.getCurrentServer();

            if (info != null) {
                worldName.set(info.name);
            }
            if (worldName.get() == null || worldName.get().isBlank()) {
                worldName.set("Multiplayer Server");
            }
            if (VoxelConstants.isRealmServer()) {
                worldName.set("Realms");
            }
        });

        StringBuilder worldNameBuilder = (new StringBuilder("§r")).append(worldName.get());
        String subworldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true);
        this.subworldName = subworldName;
        if ((subworldName == null || subworldName.isEmpty()) && VoxelConstants.getVoxelMapInstance().getWaypointManager().isMultiworld()) {
            subworldName = "???";
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            worldNameBuilder.append(" - ").append(subworldName);
        }

        this.worldNameDisplay = worldNameBuilder.toString();
        this.worldNameDisplayLength = this.getFont().width(this.worldNameDisplay);

        for (this.maxWorldNameDisplayLength = this.getWidth() / 2 - this.getFont().width(this.screenTitle) / 2 - this.sideMargin * 2; this.worldNameDisplayLength > this.maxWorldNameDisplayLength
                && worldName.get().length() > 5; this.worldNameDisplayLength = this.getFont().width(this.worldNameDisplay)) {
            worldName.set(worldName.get().substring(0, worldName.get().length() - 1));
            worldNameBuilder = new StringBuilder(worldName.get());
            worldNameBuilder.append("...");
            if (subworldName != null && !subworldName.isEmpty()) {
                worldNameBuilder.append(" - ").append(subworldName);
            }

            this.worldNameDisplay = worldNameBuilder.toString();
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            while (this.worldNameDisplayLength > this.maxWorldNameDisplayLength && subworldName.length() > 5) {
                worldNameBuilder = new StringBuilder(worldName.get());
                worldNameBuilder.append("...");
                subworldName = subworldName.substring(0, subworldName.length() - 1);
                worldNameBuilder.append(" - ").append(subworldName);
                this.worldNameDisplay = worldNameBuilder.toString();
                this.worldNameDisplayLength = this.getFont().width(this.worldNameDisplay);
            }
        }

    }

    private float bindZoom(float zoom) {
        zoom = Math.max(this.options.minZoom, zoom);
        return Math.min(this.options.maxZoom, zoom);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        this.timeOfLastMouseInput = System.currentTimeMillis();
        this.switchToMouseInput();
        float mouseDirectX = (float) minecraft.mouseHandler.xpos();
        float mouseDirectY = (float) minecraft.mouseHandler.ypos();
        if (amount != 0.0) {
            if (amount > 0.0) {
                this.zoomGoal *= 1.26F;
            } else if (amount < 0.0) {
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

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        currentDragging = false;
        if (mouseY > this.top && mouseY < this.bottom && mouseButtonEvent.button() == 1) {
            this.timeOfLastKBInput = 0L;
            int mouseDirectX = (int) minecraft.mouseHandler.xpos();
            int mouseDirectY = (int) minecraft.mouseHandler.ypos();
            if (VoxelMap.mapOptions.worldmapAllowed) {
                this.createPopup((int) mouseButtonEvent.x(), (int) mouseButtonEvent.y(), mouseDirectX, mouseDirectY);
            }
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        if (this.popupOpen()) {
            this.coordinates.mouseClicked(mouseButtonEvent, doubleClick);
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

                this.coordinates.setValue(x + ", " + z);
                this.coordinates.setTextColor(0xFFFFFF);
            }

            this.lastEditingCoordinates = this.editingCoordinates;
        }
        if (mouseButtonEvent.button() == 0) {
            currentDragging = true;
        }
        return super.mouseClicked(mouseButtonEvent, doubleClick) || mouseButtonEvent.button() == 1;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.editingCoordinates && (minecraft.options.keyJump.matches(keyEvent) || minecraft.options.keyShift.matches(keyEvent))) {
            if (minecraft.options.keyJump.matches(keyEvent)) {
                this.zoomGoal /= 1.26F;
            }

            if (minecraft.options.keyShift.matches(keyEvent)) {
                this.zoomGoal *= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = (minecraft.getWindow().getWidth() / 2f);
            this.zoomDirectY = (minecraft.getWindow().getHeight() - minecraft.getWindow().getHeight() / 2f);
            this.switchToKeyboardInput();
        }

        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.keyPressed(keyEvent);
            boolean isGood = this.isAcceptable(this.coordinates.getValue());
            this.coordinates.setTextColor(isGood ? 0xFFFFFF : 0xFF0000);
            if ((keyEvent.key() == 257 || keyEvent.key() == 335) && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getValue().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }

            if (keyEvent.key() == 258 && this.coordinates.isFocused()) {
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        if (VoxelConstants.getVoxelMapInstance().getMapOptions().keyBindMenu.matches(keyEvent)) {
            keyEvent = new KeyEvent(GLFW.GLFW_KEY_ESCAPE, -1, -1);
        }

        keySprintPressed = minecraft.options.keySprint.matches(keyEvent) || keySprintPressed;
        keyUpPressed = minecraft.options.keyUp.matches(keyEvent) || keyUpPressed;
        keyDownPressed = minecraft.options.keyDown.matches(keyEvent) || keyDownPressed;
        keyLeftPressed = minecraft.options.keyLeft.matches(keyEvent) || keyLeftPressed;
        keyRightPressed = minecraft.options.keyRight.matches(keyEvent) || keyRightPressed;

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
        keySprintPressed = !minecraft.options.keySprint.matches(keyEvent) && keySprintPressed;
        keyUpPressed = !minecraft.options.keyUp.matches(keyEvent) && keyUpPressed;
        keyDownPressed = !minecraft.options.keyDown.matches(keyEvent) && keyDownPressed;
        keyLeftPressed = !minecraft.options.keyLeft.matches(keyEvent) && keyLeftPressed;
        keyRightPressed = !minecraft.options.keyRight.matches(keyEvent) && keyRightPressed;

        return super.keyReleased(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        this.clearPopups();
        if (this.editingCoordinates) {
            this.coordinates.charTyped(characterEvent);
            boolean isGood = this.isAcceptable(this.coordinates.getValue());
            this.coordinates.setTextColor(isGood ? 0xFFFFFF : 0xFF0000);
            if (characterEvent.codepoint() == '\r' && this.coordinates.isFocused() && isGood) {
                String[] xz = this.coordinates.getValue().split(",");
                this.centerAt(Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim()));
                this.editingCoordinates = false;
                this.lastEditingCoordinates = false;
                this.switchToKeyboardInput();
            }
        }

        // FIXME 1.21.9
        // if (VoxelConstants.getVoxelMapInstance().getMapOptions().keyBindMenu.matches(modifiers, -1)) {
        // super.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, -1, -1));
        // }

        return super.charTyped(characterEvent);
    }

    private boolean isAcceptable(String input) {
        try {
            String[] xz = this.coordinates.getValue().split(",");
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
            GLFW.glfwSetInputMode(minecraft.getWindow().handle(), 208897, 212993);
        }

        this.mouseCursorShown = true;
    }

    private void switchToKeyboardInput() {
        this.timeOfLastKBInput = System.currentTimeMillis();
        this.mouseCursorShown = false;
        GLFW.glfwSetInputMode(minecraft.getWindow().handle(), 208897, 212995);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.pose().pushMatrix();
        this.buttonWaypoints.active = VoxelMap.mapOptions.waypointsAllowed;
        this.zoomGoal = this.bindZoom(this.zoomGoal);
        if (this.mouseX != mouseX || this.mouseY != mouseY) {
            this.timeOfLastMouseInput = System.currentTimeMillis();
            this.switchToMouseInput();
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float mouseDirectX = (float) minecraft.mouseHandler.xpos();
        float mouseDirectY = (float) minecraft.mouseHandler.ypos();
        if (this.zoom != this.zoomGoal) {
            float previousZoom = this.zoom;
            long timeSinceZoom = System.currentTimeMillis() - this.timeOfZoom;
            if (timeSinceZoom < 700.0F) {
                this.zoom = EasingUtils.easeOutExpo(this.zoomStart, this.zoomGoal, timeSinceZoom, 700.0F);
            } else {
                this.zoom = this.zoomGoal;
            }

            float scaledZoom = this.zoom;
            if (minecraft.getWindow().getWidth() > 1600) {
                scaledZoom = this.zoom * minecraft.getWindow().getWidth() / 1600.0F;
            }

            float zoomDelta = this.zoom / previousZoom;
            float zoomOffsetX = this.centerX * this.guiToDirectMouse - this.zoomDirectX;
            float zoomOffsetY = (this.top + this.centerY) * this.guiToDirectMouse - this.zoomDirectY;
            float zoomDeltaX = zoomOffsetX - zoomOffsetX * zoomDelta;
            float zoomDeltaY = zoomOffsetY - zoomOffsetY * zoomDelta;
            this.mapCenterX += zoomDeltaX / scaledZoom;
            this.mapCenterZ += zoomDeltaY / scaledZoom;
        }

        this.options.zoom = this.zoomGoal;
        float scaledZoom = this.zoom;
        if (minecraft.getWindow().getScreenWidth() > 1600) {
            scaledZoom = this.zoom * minecraft.getWindow().getScreenWidth() / 1600.0F;
        }

        this.guiToMap = this.scScale / scaledZoom;
        this.mapToGui = 1.0F / this.scScale * scaledZoom;
        this.mouseDirectToMap = 1.0F / scaledZoom;
        this.guiToDirectMouse = this.scScale;
        this.renderBackground(guiGraphics);
        if (currentDragging) {
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
            if (timeSinceRelease < 700.0F) {
                this.deltaX = EasingUtils.easeOutExpo(this.deltaXonRelease, 0.0F, timeSinceRelease, 700.0F);
                this.deltaY = EasingUtils.easeOutExpo(this.deltaYonRelease, 0.0F, timeSinceRelease, 700.0F);
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
            if (keySprintPressed) {
                kbDelta = 10;
            }

            if (keyUpPressed) {
                this.deltaY -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (keyDownPressed) {
                this.deltaY += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (keyLeftPressed) {
                this.deltaX -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
                this.switchToKeyboardInput();
            }

            if (keyRightPressed) {
                this.deltaX += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
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
            left = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
            top = (int) Math.floor((-this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((-this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
        } else {
            left = (int) Math.floor((this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
            top = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
        }

        synchronized (this.closedLock) {
            if (this.closed) {
                return;
            }

            this.regions = this.persistentMap.getRegions(left - 1, right + 1, top - 1, bottom + 1);
        }

        this.backGroundImageInfo = this.waypointManager.getBackgroundImageInfo();
        if (this.backGroundImageInfo != null) {
            // TODO: 1.20.1 Port - RenderPipelines.GUI_TEXTURED doesn't exist, using null
            guiGraphics.blitSprite(null, backGroundImageInfo.getImageLocation(), backGroundImageInfo.left, backGroundImageInfo.top + 32, 0, 0, backGroundImageInfo.width, backGroundImageInfo.height, backGroundImageInfo.width, backGroundImageInfo.height);
        }

        guiGraphics.pose().translate(this.centerX - this.mapCenterX * this.mapToGui, (this.top + this.centerY) - this.mapCenterZ * this.mapToGui);
        if (this.oldNorth) {
            guiGraphics.pose().rotate(90.0F * Mth.DEG_TO_RAD);
        }

        float cursorCoordZ = 0.0f;
        float cursorCoordX = 0.0f;
        guiGraphics.pose().scale(this.mapToGui, this.mapToGui);
        if (VoxelMap.mapOptions.worldmapAllowed) {
            for (CachedRegion region : this.regions) {
                Identifier resource = region.getTextureLocation();
                if (resource != null) {
                    guiGraphics.blit(VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, resource, region.getX() * 256, region.getZ() * 256, 0, 0, region.getWidth(), region.getWidth(), region.getWidth(), region.getWidth());
                }
            }

            if (VoxelMap.mapOptions.worldborder) {
                WorldBorder worldBorder = minecraft.level.getWorldBorder();
                float scale = 1.0f / (float) minecraft.getWindow().getGuiScale() / mapToGui;

                float x1 = (float) (worldBorder.getMinX());
                float z1 = (float) (worldBorder.getMinZ());
                float x2 = (float) (worldBorder.getMaxX());
                float z2 = (float) (worldBorder.getMaxZ());

                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z1 - scale, x2 + scale, z1 + scale, 0xffff0000, 0xffff0000, 0xffff0000, 0xffff0000);
                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z2 - scale, x2 + scale, z2 + scale, 0xffff0000, 0xffff0000, 0xffff0000, 0xffff0000);

                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z1 - scale, x1 + scale, z2 + scale, 0xffff0000, 0xffff0000, 0xffff0000, 0xffff0000);
                VoxelMapGuiGraphics.fillGradient(guiGraphics, x2 - scale, z1 - scale, x2 + scale, z2 + scale, 0xffff0000, 0xffff0000, 0xffff0000, 0xffff0000);
            }

            float cursorX;
            float cursorY;
            if (this.mouseCursorShown) {
                cursorX = mouseDirectX;
                cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
            } else {
                cursorX = (minecraft.getWindow().getWidth() / 2f);
                cursorY = (minecraft.getWindow().getHeight() - minecraft.getWindow().getHeight() / 2f) - this.top * this.guiToDirectMouse;
            }

            if (this.oldNorth) {
                cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
            } else {
                cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
                cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            }

            if (this.oldNorth) {
                guiGraphics.pose().rotate(-90.0F * Mth.DEG_TO_RAD);
            }

            guiGraphics.pose().scale(this.guiToMap, this.guiToMap);
            guiGraphics.pose().translate(-(this.centerX - this.mapCenterX * this.mapToGui), -((this.top + this.centerY) - this.mapCenterZ * this.mapToGui));
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
                        column = (int) Math.floor(Math.floor(this.mapCenterZ - this.centerY * this.guiToMap) / 256.0) - (left - 1);
                    } else {
                        column = (int) Math.floor(Math.floor(this.mapCenterX - this.centerX * this.guiToMap) / 256.0) - (left - 1);
                    }

                    for (int x = 0; x < this.biomeMapData.getWidth(); ++x) {
                        for (int z = 0; z < this.biomeMapData.getHeight(); ++z) {
                            float floatMapX;
                            float floatMapZ;
                            if (this.oldNorth) {
                                floatMapX = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                                floatMapZ = -(x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
                            } else {
                                floatMapX = x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
                                floatMapZ = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                            }

                            int mapX = (int) Math.floor(floatMapX);
                            int mapZ = (int) Math.floor(floatMapZ);
                            int regionX = (int) Math.floor(mapX / 256.0F) - (left - 1);
                            int regionZ = (int) Math.floor(mapZ / 256.0F) - (top - 1);
                            if (!this.oldNorth && regionX != column || this.oldNorth && regionZ != column) {
                                this.persistentMap.compress();
                            }

                            column = !this.oldNorth ? regionX : regionZ;
                            CachedRegion region = this.regions[regionZ * (right + 1 - (left - 1) + 1) + regionX];
                            Biome biome = null;
                            if (region.getMapData() != null && region.isLoaded() && !region.isEmpty()) {
                                int inRegionX = mapX - region.getX() * region.getWidth();
                                int inRegionZ = mapZ - region.getZ() * region.getWidth();
                                int height = region.getMapData().getHeight(inRegionX, inRegionZ);
                                int light = region.getMapData().getLight(inRegionX, inRegionZ);
                                if (height != Short.MIN_VALUE || light != 0) {
                                    biome = region.getMapData().getBiome(inRegionX, inRegionZ);
                                }
                            }

                            this.biomeMapData.setBiome(x, z, biome);
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
                    for (AbstractMapData.BiomeLabel biomeLabel : labels) {
                        if (biomeLabel.segmentSize > minimumSize) {
                            String label = biomeLabel.name; // + " (" + biomeLabel.x + "," + biomeLabel.z + ")";
                            int nameWidth = this.chkLen(label);
                            float x = biomeLabel.x * biomeScaleX / this.scScale;
                            float z = biomeLabel.z * biomeScaleY / this.scScale;

                            this.write(guiGraphics, label, x - (nameWidth / 2f), this.top + z - 3.0F, 0xFFFFFFFF);
                        }
                    }
                }
            }
        }
        guiGraphics.pose().popMatrix();

        if (VoxelMap.mapOptions.waypointsAllowed && this.options.showWaypoints) {
            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                this.drawWaypoint(guiGraphics, pt, cursorCoordX, cursorCoordZ, null);
            }

            if (this.waypointManager.getHighlightedWaypoint() != null) {
                this.drawWaypoint(guiGraphics, this.waypointManager.getHighlightedWaypoint(), cursorCoordX, cursorCoordZ, VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/target.png"));
            }
        }

        if (gotSkin) {
            float playerX = (float) GameVariableAccessShim.xCoordDouble();
            float playerZ = (float) GameVariableAccessShim.zCoordDouble();

            float width = iconsWidth * 0.75F;
            float height = iconsHeight * 0.75F;

            boolean hover = cursorCoordX >= playerX - width / 2 * guiToMap && cursorCoordX <= playerX + width / 2 * guiToMap && cursorCoordZ >= playerZ - height / 2 * guiToMap && cursorCoordZ <= playerZ + height / 2 * guiToMap;
            if (hover) {
                guiGraphics.requestCursor(CursorTypes.CROSSHAIR);
                renderTooltip(guiGraphics, Component.literal("X: " + GameVariableAccessShim.xCoord() + ", Y: " + GameVariableAccessShim.yCoord() + ", Z: " + GameVariableAccessShim.zCoord()), this.mouseX, this.mouseY);
            }

            int x = this.width / 2;
            int y = this.height / 2;

            double wayX = this.mapCenterX - (this.oldNorth ? -playerZ : playerX);
            double wayY = this.mapCenterZ - (this.oldNorth ? playerX : playerZ);
            float locate = (float) Math.atan2(wayX, wayY);
            float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x, y);
            guiGraphics.pose().rotate(-locate);
            guiGraphics.pose().translate(0.0F, -hypot);
            guiGraphics.pose().rotate(locate);
            guiGraphics.pose().translate(-x, -y);

            VoxelMapGuiGraphics.blitFloat(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, voxelmapSkinLocation, x - width / 2, y - height / 2, width, height, 0, 1, 0, 1, 0xFFFFFFFF);

            guiGraphics.pose().popMatrix();
        }

        if (System.currentTimeMillis() - this.timeOfLastKBInput < 2000L) {
            int scWidth = minecraft.getWindow().getGuiScaledWidth();
            int scHeight = minecraft.getWindow().getGuiScaledHeight();
            // TODO: 1.20.1 Port - RenderPipelines.CROSSHAIR doesn't exist, using null
            guiGraphics.blit(null, crosshairResource, scWidth / 2 - 8, scHeight / 2 - 8, 0, 0, 15, 15, 15, 15);
        } else {
            this.switchToMouseInput();
        }

        this.overlayBackground(guiGraphics, 0, this.top, 255, 255);
        this.overlayBackground(guiGraphics, this.bottom, this.getHeight(), 255, 255);
        if (VoxelMap.mapOptions.worldmapAllowed) {
            guiGraphics.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 16, 0xFFFFFFFF);
            int x = (int) Math.floor(cursorCoordX);
            int z = (int) Math.floor(cursorCoordZ);
            if (VoxelConstants.getVoxelMapInstance().getMapOptions().coords) {
                if (!this.editingCoordinates) {
                    guiGraphics.drawString(this.getFont(), "X: " + x, this.sideMargin, 16, 0xFFFFFFFF);
                    guiGraphics.drawString(this.getFont(), "Z: " + z, this.sideMargin + 64, 16, 0xFFFFFFFF);
                } else {
                    this.coordinates.render(guiGraphics, mouseX, mouseY, delta);
                }
            }

            if (this.subworldName != null && !this.subworldName.equals(VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true))
                    || VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true) != null && !VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(true).equals(this.subworldName)) {
                this.buildWorldName();
            }

            guiGraphics.drawString(this.getFont(), this.worldNameDisplay, this.getWidth() - this.sideMargin - this.worldNameDisplayLength, 16, 0xFFFFFF);
            if (this.buttonMultiworld != null) {
                if ((this.subworldName == null || this.subworldName.isEmpty()) && VoxelConstants.getVoxelMapInstance().getWaypointManager().isMultiworld()) {
                    if ((int) (System.currentTimeMillis() / 1000L % 2L) == 0) {
                        this.buttonMultiworld.setMessage(this.multiworldButtonNameRed);
                    } else {
                        this.buttonMultiworld.setMessage(this.multiworldButtonName);
                    }
                } else {
                    this.buttonMultiworld.setMessage(this.multiworldButtonName);
                }
            }
        } else {
            guiGraphics.drawString(this.getFont(), Component.translatable("worldmap.disabled"), this.sideMargin, 16, 0xFFFFFFFF);
        }
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // nothing
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private void drawWaypoint(GuiGraphics guiGraphics, Waypoint pt, float cursorCoordX, float cursorCoordZ, Sprite icon) {
        if (!pt.inWorld || !pt.inDimension) {
            return;
        }

        float ptX = pt.getX() + 0.5F;
        float ptZ = pt.getZ() + 0.5F;

        String name = pt.name;

        boolean hover = pt.inWorld && pt.inDimension && cursorCoordX >= ptX - iconsWidth / 2 * guiToMap && cursorCoordX <= ptX + iconsWidth / 2 * guiToMap && cursorCoordZ >= ptZ - iconsHeight / 2 * guiToMap && cursorCoordZ <= ptZ + iconsHeight / 2 * guiToMap;
        if (hover) {
            guiGraphics.requestCursor(CursorTypes.CROSSHAIR);
            renderTooltip(guiGraphics, Component.literal("X: " + pt.getX() + ", Y: " + pt.getY() + ", Z: " + pt.getZ()), this.mouseX, this.mouseY);
        }

        boolean target = false;
        TextureAtlas atlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        if (icon == null) {
            icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
            if (icon == atlas.getMissingImage()) {
                icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
            }
        } else {
            if (name.isEmpty()) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            }
            target = true;
        }

        int x = this.width / 2;
        int y = this.height / 2;

        double wayX = this.mapCenterX - (this.oldNorth ? -ptZ : ptX);
        double wayY = this.mapCenterZ - (this.oldNorth ? ptX : ptZ);
        float locate = (float) Math.atan2(wayX, wayY);
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().rotate(-locate);
        guiGraphics.pose().translate(0.0F, -hypot);
        guiGraphics.pose().rotate(locate);
        guiGraphics.pose().translate(-x, -y);

        int color = pt.getUnifiedColor(!pt.enabled && !target && !hover ? 0.3F : 1.0F);

        icon.blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - iconsWidth / 2, y - iconsHeight / 2, iconsWidth, iconsHeight, color);

        if (mapOptions.biomeOverlay == 0 && this.options.showWaypointNames || target || hover) {
            float fontScale = 1.0F;
            int halfWidth = this.chkLen(name) / 2;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().scale(fontScale, fontScale);
            this.write(guiGraphics, name, x / fontScale - halfWidth, y / fontScale + iconsHeight / 2, !pt.enabled && !target && !hover ? 0x55FFFFFF : 0xFFFFFFFF);
            guiGraphics.pose().popMatrix();
        }

        guiGraphics.pose().popMatrix();
    }

    //private boolean isOnScreen(int x, int z) {
    //    int left;
    //    int right;
    //    int top;
    //    int bottom;
    //    if (this.oldNorth) {
    //        left = (int) Math.floor(this.mapCenterZ - (this.centerY * this.guiToMap) * 1.1);
    //        right = (int) Math.floor(this.mapCenterZ + (this.centerY * this.guiToMap) * 1.1);
    //        top = (int) Math.floor((-this.mapCenterX) - (this.centerX * this.guiToMap) * 1.1);
    //        bottom = (int) Math.floor((-this.mapCenterX) + (this.centerX * this.guiToMap) * 1.1);
    //    } else {
    //        left = (int) Math.floor(this.mapCenterX - (this.centerX * this.guiToMap) * 1.1);
    //        right = (int) Math.floor(this.mapCenterX + (this.centerX * this.guiToMap) * 1.1);
    //        top = (int) Math.floor(this.mapCenterZ - (this.centerY * this.guiToMap) * 1.1);
    //        bottom = (int) Math.floor(this.mapCenterZ + (this.centerY * this.guiToMap) * 1.1);
    //    }
    //
    //    return x > left && x < right && z > top && z < bottom;
    //}

    public void renderBackground(GuiGraphics drawContext) {
        drawContext.fill(0, 0, this.getWidth(), this.getHeight(), 0xff000000);
    }

    protected void overlayBackground(GuiGraphics guiGraphics, int startY, int endY, int startAlpha, int endAlpha) {
        int colorBase = 0x404040;
        int colorStart = (startAlpha << 24) | colorBase;
        int colorEnd = (endAlpha << 24) | colorBase;
        float renderedTextureSize = 32.0F;
        // TODO: 1.20.1 Port - RenderPipelines.GUI_TEXTURED doesn't exist, using null
        VoxelMapGuiGraphics.blitFloatGradient(guiGraphics, null, VoxelConstants.getOptionsBackgroundTexture(), 0, startY, this.getWidth(), endY, 0, this.width / renderedTextureSize, 0, endY / renderedTextureSize, colorStart, colorEnd);
    }

    @Override
    public void tick() {
    }

    @Override
    public void removed() {
        synchronized (this.closedLock) {
            this.closed = true;
            this.persistentMap.getRegions(0, -1, 0, -1);
            this.regions = new CachedRegion[0];
        }
    }

    private void createPopup(int x, int y, int directX, int directY) {
        ArrayList<Popup.PopupEntry> entries = new ArrayList<>();
        float cursorX = directX;
        float cursorY = directY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        Popup.PopupEntry entry;
        if (hovered != null && this.waypointManager.getWaypoints().contains(hovered)) {
            entry = new Popup.PopupEntry(I18n.get("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(hovered != this.waypointManager.getHighlightedWaypoint() ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.newWaypoint"), 0, true, VoxelMap.mapOptions.waypointsAllowed);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(hovered == null ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, VoxelMap.mapOptions.waypointsAllowed);
        }
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.teleportTo"), 3, true, true);
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.share"), 2, true, true);
        entries.add(entry);

        this.createPopup(x, y, directX, directY, 60, entries);
        if (VoxelConstants.DEBUG) {
            persistentMap.debugLog((int) cursorCoordX, (int) cursorCoordZ);
        }
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    private Waypoint getHovered(float cursorCoordX, float cursorCoordZ) {
        if (!VoxelMap.mapOptions.waypointsAllowed) {
            return null;
        }
        Waypoint waypoint = null;

        for (Waypoint pt : this.waypointManager.getWaypoints()) {
            float ptX = pt.getX() + 0.5F;
            float ptZ = pt.getZ() + 0.5F;
            boolean hover = pt.inWorld && pt.inDimension && cursorCoordX >= ptX - iconsWidth / 2 * guiToMap && cursorCoordX <= ptX + iconsWidth / 2 * guiToMap && cursorCoordZ >= ptZ - iconsHeight / 2 * guiToMap && cursorCoordZ <= ptZ + iconsHeight / 2 * guiToMap;
            if (hover) {
                waypoint = pt;
            }
        }

        if (waypoint == null) {
            Waypoint pt = this.waypointManager.getHighlightedWaypoint();
            if (pt != null) {
                float ptX = pt.getX() + 0.5F;
                float ptZ = pt.getZ() + 0.5F;
                boolean hover = pt.inWorld && pt.inDimension && cursorCoordX >= ptX - iconsWidth / 2 * guiToMap && cursorCoordX <= ptX + iconsWidth / 2 * guiToMap && cursorCoordZ >= ptZ - iconsHeight / 2 * guiToMap && cursorCoordZ <= ptZ + iconsHeight / 2 * guiToMap;
                if (hover) {
                    waypoint = pt;
                }
            }
        }

        return waypoint;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.getClickedDirectX();
        int mouseDirectY = popup.getClickedDirectY();
        float cursorX = mouseDirectX;
        float cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        int y = this.persistentMap.getHeightAt(x, z);
        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        this.editClicked = false;
        this.addClicked = false;
        this.deleteClicked = false;
        double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
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
                if (this.waypointManager.getWaypoints().isEmpty()) {
                    r = 0.0F;
                    g = 1.0F;
                    b = 0.0F;
                } else {
                    r = this.generator.nextFloat();
                    g = this.generator.nextFloat();
                    b = this.generator.nextFloat();
                }
                TreeSet<DimensionContainer> dimensions = new TreeSet<>();
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                this.newWaypoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, r, g, b, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                minecraft.setScreen(new GuiAddWaypoint(this, this.newWaypoint, false));
            }
            case 1 -> {
                if (hovered != null) {
                    this.waypointManager.setHighlightedWaypoint(hovered, true);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    TreeSet<DimensionContainer> dimensions2 = new TreeSet<>();
                    dimensions2.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                    Waypoint fakePoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, 1.0F, 0.0F, 0.0F, "", VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions2);
                    this.waypointManager.setHighlightedWaypoint(fakePoint, true);
                }
            }
            case 2 -> {
                if (hovered != null) {
                    CommandUtils.sendWaypoint(hovered);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    CommandUtils.sendCoordinate(x, y, z);
                }
            }
            case 3 -> {
                if (hovered == null) {
                    if (y < VoxelConstants.getPlayer().level().getMinY()) {
                        y = (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                    }
                    VoxelConstants.playerRunTeleportCommand(x, y, z);
                    break;
                }

                this.selectedWaypoint = hovered;
                y = selectedWaypoint.getY() > VoxelConstants.getPlayer().level().getMinY() ? selectedWaypoint.getY() : (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                VoxelConstants.playerRunTeleportCommand(selectedWaypoint.getX(), y, selectedWaypoint.getZ());
            }
            case 4 -> {
                if (hovered != null) {
                    this.editClicked = true;
                    this.selectedWaypoint = hovered;
                    minecraft.setScreen(new GuiAddWaypoint(this, hovered, true));
                }
            }
            case 5 -> {
                if (hovered != null) {
                    this.deleteClicked = true;
                    this.selectedWaypoint = hovered;
                    Component title = Component.translatable("minimap.waypoints.deleteConfirm");
                    Component explanation = Component.translatable("selectServer.deleteWarning", this.selectedWaypoint.name);
                    Component affirm = Component.translatable("selectServer.deleteButton");
                    Component deny = Component.translatable("gui.cancel");
                    ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
                    minecraft.setScreen(confirmScreen);
                }
            }
            default -> VoxelConstants.getLogger().warn("unimplemented command");
        }

    }

    @Override
    public boolean isEditing() {
        return this.editClicked;
    }

    @Override
    public void accept(boolean b) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (b) {
                this.waypointManager.deleteWaypoint(this.selectedWaypoint);
                this.selectedWaypoint = null;
            }
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (b) {
                this.waypointManager.saveWaypoints();
            }
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (b) {
                this.waypointManager.addWaypoint(this.newWaypoint);
            }
        }

        minecraft.setScreen(this);
    }

    private int chkLen(String string) {
        return this.getFont().width(string);
    }

    private void write(GuiGraphics drawContext, String string, float x, float y, int color) {
        drawContext.drawString(this.font, string, (int) x, (int) y, color);
    }
}
