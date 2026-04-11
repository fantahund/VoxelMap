package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
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
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.EasingUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapGuiGraphics;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final MapSettingsManager mapOptions;
    private final PersistentMapSettingsManager options;
    private final PersistentMap persistentMap;
    private final WaypointManager waypointManager;
    private final DimensionManager dimensionManager;
    private final Random generator = new Random();

    private final int SIDE_MARGIN = 10;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    // Mapping & Threading
    private final Object closedLock = new Object();
    private boolean closed;

    private CachedRegion[] regionsToDisplay = new CachedRegion[0];

    private boolean lastStill;
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);

    // Screen & Layouts
    protected String screenTitle = "World Map";

    private float scScale = 1.0F;
    private int top;
    private int bottom;

    private Component multiworldButtonName;
    private Component multiworldButtonNameRed;
    private PopupGuiButton buttonWaypoints;
    private PopupGuiButton buttonMultiworld;

    // Subworld Names
    private String subworldName = "";
    private String worldNameDisplay = "";
    private int worldNameDisplayLength;

    // Coordinate Transforms
    private boolean oldNorth;
    private int centerX;
    private int centerY;
    private float mapCenterX;
    private float mapCenterZ;
    private float mapPixelsX;
    private float mapPixelsY;
    private float guiToMap = 2.0F;
    private float mapToGui = 0.5F;
    private float rawMouseToMap = 1.0F;
    private float guiToRawMouse = 2.0F;

    // Input & Navigation
    private boolean skipMouseDetection;
    private boolean mouseCursorShown = true;
    private boolean leftMouseButtonDown;
    private long timeOfLastMouseInput;
    private int lastMouseX;
    private int lastMouseY;
    private float lastRawMouseX;
    private float lastRawMouseY;

    private long timeOfRelease;
    private float deltaX;
    private float deltaY;
    private float deltaXonRelease;
    private float deltaYonRelease;

    private float zoom;
    private float zoomStart;
    private float zoomGoal;
    private long timeOfZoom;
    private float zoomDirectX;
    private float zoomDirectY;

    private long timeAtLastTick;
    private long timeOfLastKeyboardInput;

    // Popup
    public boolean editClicked;
    public boolean deleteClicked;
    public boolean addClicked;
    private Waypoint newWaypoint;
    private Waypoint selectedWaypoint;
    private Waypoint hoverdWaypoint;

    // Resources
    private static boolean gotSkin;
    private final Identifier crosshairResource = Identifier.parse("textures/gui/sprites/hud/crosshair.png");
    private final Identifier voxelmapSkinLocation = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "worldmap/player_skin");
    private final Identifier caveButtonTexture = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/worldmap/cave_button.png");

    public GuiPersistentMap(Screen parentGui) {
        super(parentGui, Component.empty());

        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        zoom = options.zoom;
        zoomStart = options.zoom;
        zoomGoal = options.zoom;
        persistentMap.setLightMapArray(VoxelConstants.getVoxelMapInstance().getMap().getLightmapArray());

        if (!gotSkin) {
            getSkin();
        }
    }

    private void getSkin() {
        BufferedImage skinImage = ImageUtils.createBufferedImageFromIdentifier(VoxelConstants.getPlayer().getSkin().body().texturePath());

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

        DynamicTexture texture = new DynamicTexture(() -> "Voxelmap player", ImageUtils.nativeImageFromBufferedImage(skinImage));
        texture.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        minecraft.getTextureManager().register(voxelmapSkinLocation, texture);
    }

    @Override
    public void init() {
        if (minecraft.screen == this) {
            closed = false;
        }

        screenTitle = I18n.get("worldmap.title");

        scScale = (float) minecraft.getWindow().getGuiScale();
        top = 32;
        bottom = getHeight() - 32;

        int buttonCount = 5;
        int buttonSeparation = 4;
        int buttonWidth = (getWidth() - SIDE_MARGIN * 2 - buttonSeparation * (buttonCount - 1)) / buttonCount;

        addRenderableWidget(buttonWaypoints = new PopupGuiButton(SIDE_MARGIN, getHeight() - 26, buttonWidth, 20, Component.translatable("options.minimap.waypoints"), button -> minecraft.setScreen(new GuiWaypoints(this)), this));

        multiworldButtonName = Component.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.worldmap.multiworld");
        multiworldButtonNameRed = multiworldButtonName.copy().withStyle(ChatFormatting.RED);
        if (!minecraft.hasSingleplayerServer() && !waypointManager.receivedAutoSubworldName()) {
            addRenderableWidget(buttonMultiworld = new PopupGuiButton(SIDE_MARGIN + (buttonWidth + buttonSeparation), getHeight() - 26, buttonWidth, 20, multiworldButtonName, button -> minecraft.setScreen(new GuiSubworldsSelect(this)), this));
        }

        addRenderableWidget(new PopupGuiButton(SIDE_MARGIN + 3 * (buttonWidth + buttonSeparation), getHeight() - 26, buttonWidth, 20, Component.translatable("menu.options"), button -> minecraft.setScreen(new GuiMinimapOptions(this)), this));
        addRenderableWidget(new PopupGuiButton(SIDE_MARGIN + 4 * (buttonWidth + buttonSeparation), getHeight() - 26, buttonWidth, 20, Component.translatable("gui.done"), button -> onClose(), this));

        oldNorth = mapOptions.oldNorth;
        centerX = getWidth() / 2;
        centerY = (bottom - top) / 2;
        mapPixelsX = getWindowWidth();
        mapPixelsY = getWindowHeight() - (int) (64.0F * scScale);

        leftMouseButtonDown = false;
        lastStill = false;
        timeAtLastTick = System.currentTimeMillis();

        buildWorldName();

        centerAt(options.mapX, options.mapZ);
    }

    private void centerAt(int x, int z) {
        if (oldNorth) {
            mapCenterX = -z;
            mapCenterZ = x;
        } else {
            mapCenterX = x;
            mapCenterZ = z;
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
        String subworldName = waypointManager.getCurrentSubworldDescriptor(true);
        this.subworldName = subworldName;
        if ((subworldName == null || subworldName.isEmpty()) && waypointManager.isMultiworld()) {
            subworldName = "???";
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            worldNameBuilder.append(" - ").append(subworldName);
        }

        worldNameDisplay = worldNameBuilder.toString();
        worldNameDisplayLength = getFont().width(worldNameDisplay);

        int maxWorldNameDisplayLength;
        for (maxWorldNameDisplayLength = getWidth() / 2 - getFont().width(screenTitle) / 2 - SIDE_MARGIN * 2; worldNameDisplayLength > maxWorldNameDisplayLength
                && worldName.get().length() > 5; worldNameDisplayLength = getFont().width(worldNameDisplay)) {
            worldName.set(worldName.get().substring(0, worldName.get().length() - 1));
            worldNameBuilder = new StringBuilder(worldName.get());
            worldNameBuilder.append("...");
            if (subworldName != null && !subworldName.isEmpty()) {
                worldNameBuilder.append(" - ").append(subworldName);
            }

            worldNameDisplay = worldNameBuilder.toString();
        }

        if (subworldName != null && !subworldName.isEmpty()) {
            while (worldNameDisplayLength > maxWorldNameDisplayLength && subworldName.length() > 5) {
                worldNameBuilder = new StringBuilder(worldName.get());
                worldNameBuilder.append("...");
                subworldName = subworldName.substring(0, subworldName.length() - 1);
                worldNameBuilder.append(" - ").append(subworldName);
                worldNameDisplay = worldNameBuilder.toString();
                worldNameDisplayLength = getFont().width(worldNameDisplay);
            }
        }
    }

    private float clampZoom(float zoom) {
        return Math.min(Math.max(zoom, options.minZoom), options.maxZoom);
    }

    private int getWindowWidth() {
        return minecraft.getWindow().getWidth();
    }

    private int getWindowHeight() {
        return minecraft.getWindow().getHeight();
    }

    private double getRawMouseX() {
        return minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()) * minecraft.getWindow().getGuiScale();
    }

    private double getRawMouseY() {
        return minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()) * minecraft.getWindow().getGuiScale();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        timeOfLastMouseInput = System.currentTimeMillis();
        switchToMouseInput();

        float rawMouseX = (float) getRawMouseX();
        float rawMouseY = (float) getRawMouseY();
        if (amount != 0.0) {
            if (amount > 0.0) {
                zoomGoal *= 1.26F;
            } else if (amount < 0.0) {
                zoomGoal /= 1.26F;
            }

            zoomStart = zoom;
            zoomGoal = clampZoom(zoomGoal);
            timeOfZoom = System.currentTimeMillis();
            zoomDirectX = rawMouseX;
            zoomDirectY = rawMouseY;
        }

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        selectedWaypoint = getHoveredWaypoint();
        if (mouseButtonEvent.button() == 1 && (selectedWaypoint != null || (lastMouseY > top && lastMouseY < bottom))) {
            timeOfLastKeyboardInput = 0L;

            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (mapOptions.worldmapAllowed) {
                createPopup((int) mouseX, (int) mouseY, (int) getRawMouseX(), (int) getRawMouseY());
            }
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean jumpPressed = minecraft.options.keyJump.matches(keyEvent);
        boolean sneakPressed = minecraft.options.keyShift.matches(keyEvent);

        if (jumpPressed || sneakPressed) {
            if (jumpPressed) {
                zoomGoal /= 1.26F;
            }
            if (sneakPressed) {
                zoomGoal *= 1.26F;
            }

            zoomStart = zoom;
            zoomGoal = clampZoom(zoomGoal);
            timeOfZoom = System.currentTimeMillis();
            zoomDirectX = getWindowWidth() / 2.0F;
            zoomDirectY = getWindowHeight() / 2.0F;

            switchToKeyboardInput();
        }

        clearPopups();

        if (mapOptions.keyBindMenu.matches(keyEvent)) {
            keyEvent = new KeyEvent(GLFW.GLFW_KEY_ESCAPE, -1, -1);
        }

        return super.keyPressed(keyEvent);
    }

    private void switchToMouseInput() {
        timeOfLastKeyboardInput = 0L;
        if (!mouseCursorShown) {
            GLFW.glfwSetInputMode(minecraft.getWindow().handle(), 208897, 212993);
        }

        mouseCursorShown = true;
    }

    private void switchToKeyboardInput() {
        timeOfLastKeyboardInput = System.currentTimeMillis();
        mouseCursorShown = false;
        GLFW.glfwSetInputMode(minecraft.getWindow().handle(), 208897, 212995);
    }

    private void handleInputAndNavigation(int mouseX, int mouseY, float rawMouseX, float rawMouseY) {
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            timeOfLastMouseInput = System.currentTimeMillis();
            switchToMouseInput();

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        zoomGoal = clampZoom(zoomGoal);
        if (zoom != zoomGoal) {
            float lastZoom = zoom;

            long elapsed = System.currentTimeMillis() - timeOfZoom;
            if (elapsed < 700.0F) {
                zoom = EasingUtils.easeOutExpo(zoomStart, zoomGoal, elapsed, 700.0F);
            } else {
                zoom = zoomGoal;
            }

            float mouseOffsetX = (centerX * guiToRawMouse) - zoomDirectX;
            float mouseOffsetZ = ((top + centerY) * guiToRawMouse) - zoomDirectY;
            float zoomRatio = zoom / lastZoom;
            float scaledZoom = zoom * getWindowScale();

            float moveFactor = (1.0F - zoomRatio) / scaledZoom;

            mapCenterX += mouseOffsetX * moveFactor;
            mapCenterZ += mouseOffsetZ * moveFactor;
        }

        options.zoom = zoomGoal;

        float scaledZoom = zoom * getWindowScale();

        guiToMap = scScale / scaledZoom;
        mapToGui = 1.0F / guiToMap;
        rawMouseToMap = 1.0F / scaledZoom;
        guiToRawMouse = scScale;

        if (isMouseDown(GLFW.GLFW_MOUSE_BUTTON_1)) {
            if (leftMouseButtonDown) {
                deltaX = (lastRawMouseX - rawMouseX) * rawMouseToMap;
                deltaY = (lastRawMouseY - rawMouseY) * rawMouseToMap;
                deltaXonRelease = deltaX;
                deltaYonRelease = deltaY;
                timeOfRelease = System.currentTimeMillis();
            } else if (overPopup(mouseX, mouseY)) {
                deltaX = 0.0F;
                deltaY = 0.0F;
                leftMouseButtonDown = true;
            }

            lastRawMouseX = rawMouseX;
            lastRawMouseY = rawMouseY;
        } else {
            long elapsed = System.currentTimeMillis() - timeOfRelease;
            if (elapsed < 700.0F) {
                deltaX = EasingUtils.easeOutExpo(deltaXonRelease, 0.0F, elapsed, 700.0F);
                deltaY = EasingUtils.easeOutExpo(deltaYonRelease, 0.0F, elapsed, 700.0F);
            } else {
                deltaX = 0.0F;
                deltaY = 0.0F;
                deltaXonRelease = 0.0F;
                deltaYonRelease = 0.0F;
            }

            leftMouseButtonDown = false;
        }

        long elapsed = System.currentTimeMillis() - timeAtLastTick;
        timeAtLastTick = System.currentTimeMillis();

        float moveSpeed = isKeyDown(minecraft.options.keySprint) ? 10.0F : 5.0F;
        float moveStep = (moveSpeed / scaledZoom) * (elapsed / 12.0F);

        if (isKeyDown(minecraft.options.keyUp)) {
            deltaY -= moveStep;
            switchToKeyboardInput();
        }

        if (isKeyDown(minecraft.options.keyDown)) {
            deltaY += moveStep;
            switchToKeyboardInput();
        }

        if (isKeyDown(minecraft.options.keyLeft)) {
            deltaX -= moveStep;
            switchToKeyboardInput();
        }

        if (isKeyDown(minecraft.options.keyRight)) {
            deltaX += moveStep;
            switchToKeyboardInput();
        }

        mapCenterX += deltaX;
        mapCenterZ += deltaY;

        if (oldNorth) {
            options.mapX = (int) mapCenterZ;
            options.mapZ = -((int) mapCenterX);
        } else {
            options.mapX = (int) mapCenterX;
            options.mapZ = (int) mapCenterZ;
        }

        centerX = getWidth() / 2;
        centerY = (bottom - top) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        buttonWaypoints.active = mapOptions.waypointsAllowed;

        guiGraphics.fill(0, 0, getWidth(), getHeight(), 0xFF000000);

        float rawMouseX = (float) getRawMouseX();
        float rawMouseY = (float) getRawMouseY();

        handleInputAndNavigation(mouseX, mouseY, rawMouseX, rawMouseY);

        int regionLeft;
        int regionRight;
        int regionTop;
        int regionBottom;
        if (oldNorth) {
            regionLeft = (int) Math.floor((mapCenterZ - centerY * guiToMap) / 256.0F);
            regionRight = (int) Math.floor((mapCenterZ + centerY * guiToMap) / 256.0F);
            regionTop = (int) Math.floor((-mapCenterX - centerX * guiToMap) / 256.0F);
            regionBottom = (int) Math.floor((-mapCenterX + centerX * guiToMap) / 256.0F);
        } else {
            regionLeft = (int) Math.floor((mapCenterX - centerX * guiToMap) / 256.0F);
            regionRight = (int) Math.floor((mapCenterX + centerX * guiToMap) / 256.0F);
            regionTop = (int) Math.floor((mapCenterZ - centerY * guiToMap) / 256.0F);
            regionBottom = (int) Math.floor((mapCenterZ + centerY * guiToMap) / 256.0F);
        }

        synchronized (closedLock) {
            if (closed) {
                return;
            }

            regionsToDisplay = persistentMap.getRegions(regionLeft - 1, regionRight + 1, regionTop - 1, regionBottom + 1);
        }

        BackgroundImageInfo bgInfo = waypointManager.getBackgroundImageInfo();
        if (bgInfo != null) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, bgInfo.getImageLocation(), bgInfo.left, bgInfo.top + 32, 0, 0, bgInfo.width, bgInfo.height, bgInfo.width, bgInfo.height);
        }

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(centerX - mapCenterX * mapToGui, top + centerY - mapCenterZ * mapToGui);
        if (oldNorth) {
            guiGraphics.pose().rotate(90.0F * Mth.DEG_TO_RAD);
        }

        float cursorCoordZ = 0.0F;
        float cursorCoordX = 0.0F;
        guiGraphics.pose().scale(mapToGui, mapToGui);
        if (mapOptions.worldmapAllowed) {
            for (CachedRegion region : regionsToDisplay) {
                Identifier resource = region.getTextureLocation(zoom);
                if (resource != null) {
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, resource, region.getX() * 256, region.getZ() * 256, 0, 0, region.getWidth(), region.getWidth(), region.getWidth(), region.getWidth());
                }
            }

            if (mapOptions.worldBorder) {
                WorldBorder worldBorder = minecraft.level.getWorldBorder();
                float scale = 1.0F / scScale / mapToGui;

                float x1 = (float) (worldBorder.getMinX());
                float z1 = (float) (worldBorder.getMinZ());
                float x2 = (float) (worldBorder.getMaxX());
                float z2 = (float) (worldBorder.getMaxZ());

                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z1 - scale, x2 + scale, z1 + scale, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000);
                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z2 - scale, x2 + scale, z2 + scale, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000);

                VoxelMapGuiGraphics.fillGradient(guiGraphics, x1 - scale, z1 - scale, x1 + scale, z2 + scale, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000);
                VoxelMapGuiGraphics.fillGradient(guiGraphics, x2 - scale, z1 - scale, x2 + scale, z2 + scale, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000, 0xFFFF0000);
            }

            float cursorX;
            float cursorY;
            if (mouseCursorShown) {
                cursorX = rawMouseX;
                cursorY = rawMouseY - top * guiToRawMouse;
            } else {
                cursorX = getWindowWidth() / 2.0F;
                cursorY = getWindowHeight() / 2.0F - top * guiToRawMouse;
            }

            if (oldNorth) {
                cursorCoordX = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
                cursorCoordZ = -(cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap));
            } else {
                cursorCoordX = cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap);
                cursorCoordZ = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
            }

            if (oldNorth) {
                guiGraphics.pose().rotate(-90.0F * Mth.DEG_TO_RAD);
            }

            guiGraphics.pose().scale(guiToMap, guiToMap);
            guiGraphics.pose().translate(-(centerX - (mapCenterX * mapToGui)), -((top + centerY) - (mapCenterZ * mapToGui)));

            if (mapOptions.biomeOverlay != 0) {
                drawBiomeOverlay(guiGraphics, regionLeft, regionRight, regionTop, regionBottom);
            }
        }
        guiGraphics.pose().popMatrix();

        if (options.showDistantWaypoints) {
            overlayBackground(guiGraphics, 0, top);
            overlayBackground(guiGraphics, bottom, getHeight());
        }

        Waypoint currentlyHovered = null;
        if (mapOptions.waypointsAllowed && options.showWaypoints) {
            TextureAtlas textureAtlas = waypointManager.getTextureAtlas();

            for (Waypoint waypoint : waypointManager.getWaypoints()) {
                if (!waypoint.inWorld || !waypoint.inDimension) continue;

                boolean isHighlighted = waypointManager.isHighlightedWaypoint(waypoint);
                boolean isHovered = drawWaypoint(guiGraphics, waypoint, textureAtlas, null, isHighlighted, -1, mouseX, mouseY);
                if (isHovered) {
                    currentlyHovered = waypoint;
                }
            }

            Waypoint highlightedPoint = waypointManager.getHighlightedWaypoint();
            if (highlightedPoint != null) {
                boolean isHovered = drawWaypoint(guiGraphics, highlightedPoint, textureAtlas, textureAtlas.getAtlasSprite("marker/target"), true, 0xFFFF0000, mouseX, mouseY);
                if (isHovered) {
                    currentlyHovered = highlightedPoint;
                }
            }
        }
        hoverdWaypoint = currentlyHovered;

        if (!options.showDistantWaypoints) {
            overlayBackground(guiGraphics, 0, top);
            overlayBackground(guiGraphics, bottom, getHeight());
        }

        if (gotSkin) {
            float playerX = (float) GameVariableAccessShim.xCoordDouble();
            float playerZ = (float) GameVariableAccessShim.zCoordDouble();
            drawPlayer(guiGraphics, voxelmapSkinLocation, playerX, playerZ, mouseX, mouseY);
        }

        if (System.currentTimeMillis() - timeOfLastKeyboardInput < 2000L) {
            guiGraphics.blit(RenderPipelines.CROSSHAIR, crosshairResource, getWidth() / 2 - 8, getHeight() / 2 - 8, 0, 0, 15, 15, 15, 15);
        } else {
            switchToMouseInput();
        }

        if (mapOptions.worldmapAllowed) {
            guiGraphics.drawCenteredString(getFont(), screenTitle, getWidth() / 2, 16, 0xFFFFFFFF);
            int x = (int) Math.floor(cursorCoordX);
            int z = (int) Math.floor(cursorCoordZ);
            if (options.showCoordinates) {
                guiGraphics.drawString(getFont(), "X: " + x, SIDE_MARGIN, 16, 0xFFFFFFFF);
                guiGraphics.drawString(getFont(), "Z: " + z, SIDE_MARGIN + 64, 16, 0xFFFFFFFF);
            }

            if (!Objects.equals(subworldName, waypointManager.getCurrentSubworldDescriptor(true))) {
                buildWorldName();
            }

            guiGraphics.drawString(getFont(), worldNameDisplay, getWidth() - SIDE_MARGIN - worldNameDisplayLength, 16, 0xFFFFFFFF);
            if (buttonMultiworld != null) {
                boolean warning = (subworldName == null || subworldName.isEmpty()) && waypointManager.isMultiworld();
                Component message = (warning && (System.currentTimeMillis() / 1000L % 2L) == 0L) ? multiworldButtonNameRed : multiworldButtonName;

                buttonMultiworld.setMessage(message);
            }
        } else {
            guiGraphics.drawString(getFont(), Component.translatable("worldmap.disabled"), SIDE_MARGIN, 16, 0xFFFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    private boolean isMouseDown(int keyCode) {
        return !skipMouseDetection && GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), keyCode) == GLFW.GLFW_TRUE;
    }

    private boolean isKeyDown(KeyMapping keyMapping) {
        return isKeyDown(keyMapping.key.getValue());
    }

    private boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(minecraft.getWindow().handle(), keyCode) == GLFW.GLFW_TRUE;
    }

    private float getWindowScale() {
        return Math.max(1.0F, getWindowWidth() / 1600.0F);
    }

    private boolean drawPlayer(GuiGraphics guiGraphics, Identifier skin, float playerX, float playerZ, int mouseX, int mouseY) {
        float headWidth = ICON_WIDTH * 0.75F;
        float headHeight = ICON_HEIGHT * 0.75F;

        int x = getWidth() / 2;
        int y = getHeight() / 2;
        int borderX = x - 4;
        int borderY = y - top;

        double wayX = mapCenterX - (oldNorth ? -playerZ : playerX);
        double wayY = mapCenterZ - (oldNorth ? playerX : playerZ);
        float locate = (float) Math.atan2(wayX, wayY);
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

        double dispX = hypot * Math.sin(locate);
        double dispY = hypot * Math.cos(locate);
        boolean far = Math.abs(dispX) > borderX || Math.abs(dispY) > borderY;
        if (far) {
            hypot *= (float) Math.min(borderX / Math.abs(dispX), borderY / Math.abs(dispY));
        }

        guiGraphics.pose().pushMatrix();

        if (far) {
            guiGraphics.pose().translate(x, y);
            guiGraphics.pose().rotate(-locate);
            guiGraphics.pose().translate(0.0F, -hypot);
            guiGraphics.pose().rotate(locate);
            guiGraphics.pose().translate(-x, -y);
        } else {
            guiGraphics.pose().rotate(-locate);
            guiGraphics.pose().translate(0.0F, -hypot);
            guiGraphics.pose().rotate(locate);
        }

        Vector2f guiVector = guiGraphics.pose().transformPosition(new Vector2f(x, y));
        float screenX = guiVector.x();
        float screenY = guiVector.y();

        boolean isHovered = mouseX >= screenX - ICON_WIDTH / 2.0F && mouseX <= screenX + ICON_WIDTH / 2.0F
                && mouseY >= screenY - ICON_HEIGHT / 2.0F && mouseY <= screenY + ICON_HEIGHT / 2.0F;
        if (isHovered) {
            guiGraphics.requestCursor(CursorTypes.CROSSHAIR);
            if (options.showCoordinates) {
                Component tooltip = Component.literal("X: " + GameVariableAccessShim.xCoord() + ", Y: " + GameVariableAccessShim.yCoord() + ", Z: " + GameVariableAccessShim.zCoord());
                RenderUtils.drawTooltip(guiGraphics, Tooltip.create(tooltip), mouseX, mouseY);
            }
        }

        VoxelMapGuiGraphics.blitFloat(guiGraphics, RenderPipelines.GUI_TEXTURED, skin, x - headWidth / 2.0F, y - headHeight / 2.0F, headWidth, headHeight, 0, 1, 0, 1, 0xFFFFFFFF);

        guiGraphics.pose().popMatrix();

        return isHovered;
    }

    private boolean drawWaypoint(GuiGraphics guiGraphics, Waypoint waypoint, TextureAtlas textureAtlas, Sprite icon, boolean isHighlighted, int color, int mouseX, int mouseY) {
        float ptX = waypoint.getX() + 0.5F;
        float ptZ = waypoint.getZ() + 0.5F;

        int x = getWidth() / 2;
        int y = getHeight() / 2;

        int borderOffsetX = options.showDistantWaypoints ? -4 : ICON_WIDTH / 2;
        int borderOffsetY = options.showDistantWaypoints ? 0 : ICON_HEIGHT / 2;
        int borderX = x + borderOffsetX;
        int borderY = y - top + borderOffsetY;

        double wayX = mapCenterX - (oldNorth ? -ptZ : ptX);
        double wayY = mapCenterZ - (oldNorth ? ptX : ptZ);
        float locate = (float) Math.atan2(wayX, wayY);
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

        double dispX = hypot * Math.sin(locate);
        double dispY = hypot * Math.cos(locate);
        boolean far = Math.abs(dispX) > borderX || Math.abs(dispY) > borderY;
        if (far) {
            if (!options.showDistantWaypoints) {
                return false;
            }

            hypot *= (float) Math.min(borderX / Math.abs(dispX), borderY / Math.abs(dispY));
        }

        boolean uprightIcon = icon != null;

        String name = waypoint.name;
        if (waypointManager.isCoordinateHighlight(waypoint)) {
            name = "X:" + waypoint.getX() + ", Y:" + waypoint.getY() + ", Z:" + waypoint.getZ();
        }

        if (icon == null) {
            String iconLocation = (far ? "marker/" : "selectable/") + waypoint.imageSuffix;
            String fallbackLocation = far ? "marker/arrow" : WaypointManager.fallbackIconLocation;

            icon = textureAtlas.getAtlasSprite(iconLocation);
            if (icon == textureAtlas.getMissingImage()) {
                icon = textureAtlas.getAtlasSprite(fallbackLocation);
            }
        }

        guiGraphics.pose().pushMatrix();

        if (far) {
            guiGraphics.pose().translate(x, y);
            guiGraphics.pose().rotate(-locate);
            if (uprightIcon) {
                guiGraphics.pose().translate(0.0F, -hypot);
                guiGraphics.pose().rotate(locate);
                guiGraphics.pose().translate(-x, -y);
            } else {
                guiGraphics.pose().translate(-x, -y);
                guiGraphics.pose().translate(0.0F, -hypot);
            }
        } else {
            guiGraphics.pose().rotate(-locate);
            guiGraphics.pose().translate(0.0F, -hypot);
            guiGraphics.pose().rotate(locate);
        }

        Vector2f guiVector = guiGraphics.pose().transformPosition(new Vector2f(x, y));
        float screenX = guiVector.x();
        float screenY = guiVector.y();

        boolean isHovered = mouseX >= screenX - ICON_WIDTH / 2.0F && mouseX <= screenX + ICON_WIDTH / 2.0F
                && mouseY >= screenY - ICON_HEIGHT / 2.0F && mouseY <= screenY + ICON_HEIGHT / 2.0F;
        if (isHovered) {
            guiGraphics.requestCursor(CursorTypes.CROSSHAIR);
            if (options.showCoordinates) {
                Component tooltip = Component.literal("X: " + waypoint.getX() + ", Y: " + waypoint.getY() + ", Z: " + waypoint.getZ());
                RenderUtils.drawTooltip(guiGraphics, Tooltip.create(tooltip), mouseX, mouseY);
            }
        }

        int iconColor = color == -1 ? waypoint.getUnifiedColor(!waypoint.enabled && !isHighlighted && !isHovered ? 0.3F : 1.0F) : color;
        int textColor = !waypoint.enabled && !isHighlighted && !isHovered ? 0x55FFFFFF : 0xFFFFFFFF;

        icon.blit(guiGraphics, RenderPipelines.GUI_TEXTURED, x - ICON_WIDTH / 2.0F, y - ICON_HEIGHT / 2.0F, ICON_WIDTH, ICON_HEIGHT, iconColor);

        boolean textOverFrame = screenY + ICON_HEIGHT > bottom;
        if (options.showWaypointNames && !far && !textOverFrame) {
            guiGraphics.pose().pushMatrix();
            float fontScale = 1.0F;
            guiGraphics.pose().scale(fontScale, fontScale);
            writeCentered(guiGraphics, name, x / fontScale, (y + ICON_HEIGHT / 2.0F) / fontScale, textColor, true);
            guiGraphics.pose().popMatrix();
        }

        guiGraphics.pose().popMatrix();

        return isHovered;
    }

    private void drawBiomeOverlay(GuiGraphics guiGraphics, int regionLeft, int regionRight, int regionTop, int regionBottom) {
        float biomeScaleX = mapPixelsX / 760.0F;
        float biomeScaleY = mapPixelsY / 360.0F;

        boolean still = !leftMouseButtonDown
                && zoom == zoomGoal
                && deltaX == 0.0F
                && deltaY == 0.0F
                && ThreadManager.executorService.getActiveCount() == 0;

        if (still && !lastStill) {
            int column;
            if (oldNorth) {
                column = (int) Math.floor(Math.floor(mapCenterZ - centerY * guiToMap) / 256.0) - (regionLeft - 1);
            } else {
                column = (int) Math.floor(Math.floor(mapCenterX - centerX * guiToMap) / 256.0) - (regionLeft - 1);
            }

            for (int x = 0; x < biomeMapData.getWidth(); ++x) {
                for (int z = 0; z < biomeMapData.getHeight(); ++z) {
                    float floatMapX;
                    float floatMapZ;
                    if (oldNorth) {
                        floatMapX = z * biomeScaleY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
                        floatMapZ = -(x * biomeScaleX * rawMouseToMap + (mapCenterX - centerX * guiToMap));
                    } else {
                        floatMapX = x * biomeScaleX * rawMouseToMap + (mapCenterX - centerX * guiToMap);
                        floatMapZ = z * biomeScaleY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
                    }

                    int mapX = (int) Math.floor(floatMapX);
                    int mapZ = (int) Math.floor(floatMapZ);
                    int regionX = (int) Math.floor(mapX / 256.0F) - (regionLeft - 1);
                    int regionZ = (int) Math.floor(mapZ / 256.0F) - (regionTop - 1);

                    if (!oldNorth && regionX != column || oldNorth && regionZ != column) {
                        persistentMap.compress();
                    }

                    column = !oldNorth ? regionX : regionZ;
                    CachedRegion region = regionsToDisplay[regionZ * (regionRight + 1 - (regionLeft - 1) + 1) + regionX];
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

                    biomeMapData.setBiome(x, z, biome);
                }
            }

            persistentMap.compress();
            biomeMapData.segmentBiomes();
            biomeMapData.findCenterOfSegments(true);
        }

        lastStill = still;

        boolean displayStill = !leftMouseButtonDown
                && zoom == zoomGoal
                && deltaX == 0.0F
                && deltaY == 0.0F;

        if (displayStill) {
            int minimumSize = (int) (20.0F * scScale / biomeScaleX);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = biomeMapData.getBiomeLabels();
            for (AbstractMapData.BiomeLabel biomeLabel : labels) {
                if (biomeLabel.segmentSize > minimumSize) {
                    String label = biomeLabel.name;
                    float x = biomeLabel.x * biomeScaleX / scScale;
                    float z = biomeLabel.z * biomeScaleY / scScale;

                    writeCentered(guiGraphics, label, x, top + z - 3.0F, 0xFFFFFFFF, true);
                }
            }
        }
    }

    protected void overlayBackground(GuiGraphics guiGraphics, int startY, int endY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, VoxelConstants.getOptionsBackgroundTexture(), 0, startY, 0.0F, 0.0F, getWidth(), endY - startY, 32, 32, 0xFF404040);
        if (startY > getHeight() / 2) {
            guiGraphics.fillGradient(0, startY - 4, getWidth(), startY, 0x00000000, 0xFF000000);
        } else {
            guiGraphics.fillGradient(0, endY, getWidth(), endY + 4, 0xFF000000, 0x00000000);
        }
    }

    @Override
    public void tick() {
    }

    @Override
    public void removed() {
        synchronized (closedLock) {
            closed = true;
            persistentMap.getRegions(0, -1, 0, -1);
            regionsToDisplay = new CachedRegion[0];
        }
    }

    private void createPopup(int x, int y, int directX, int directY) {
        ArrayList<Popup.PopupEntry> entries = new ArrayList<>();
        float cursorX = directX;
        float cursorY = directY - top * guiToRawMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (oldNorth) {
            cursorCoordX = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
            cursorCoordZ = -(cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap));
        } else {
            cursorCoordX = cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap);
            cursorCoordZ = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
        }

        Popup.PopupEntry entry;
        if (selectedWaypoint != null && waypointManager.getWaypoints().contains(selectedWaypoint)) {
            entry = new Popup.PopupEntry(I18n.get("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(selectedWaypoint != waypointManager.getHighlightedWaypoint() ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.newWaypoint"), 0, true, mapOptions.waypointsAllowed);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(selectedWaypoint == null ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, mapOptions.waypointsAllowed);
        }
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.teleportTo"), 3, true, true);
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.share"), 2, true, true);
        entries.add(entry);

        createPopup(x, y, directX, directY, 60, entries);
        if (VoxelConstants.DEBUG) {
            persistentMap.debugLog((int) cursorCoordX, (int) cursorCoordZ);
        }
    }

    private Waypoint getHoveredWaypoint() {
        if (!mapOptions.waypointsAllowed) {
            return null;
        }

        return hoverdWaypoint;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.getClickedDirectX();
        int mouseDirectY = popup.getClickedDirectY();
        float cursorX = mouseDirectX;
        float cursorY = mouseDirectY - top * guiToRawMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (oldNorth) {
            cursorCoordX = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
            cursorCoordZ = -(cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap));
        } else {
            cursorCoordX = cursorX * rawMouseToMap + (mapCenterX - centerX * guiToMap);
            cursorCoordZ = cursorY * rawMouseToMap + (mapCenterZ - centerY * guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        int y = persistentMap.getHeightAt(x, z);
        editClicked = false;
        addClicked = false;
        deleteClicked = false;
        double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
        switch (action) {
            case 0 -> {
                if (selectedWaypoint != null) {
                    x = selectedWaypoint.getX();
                    z = selectedWaypoint.getZ();
                }
                addClicked = true;
                float r;
                float g;
                float b;
                if (waypointManager.getWaypoints().isEmpty()) {
                    r = 0.0F;
                    g = 1.0F;
                    b = 0.0F;
                } else {
                    r = generator.nextFloat();
                    g = generator.nextFloat();
                    b = generator.nextFloat();
                }
                TreeSet<DimensionContainer> dimensions = new TreeSet<>();
                dimensions.add(dimensionManager.getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                newWaypoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, r, g, b, "", waypointManager.getCurrentSubworldDescriptor(false), dimensions);
                minecraft.setScreen(new GuiAddWaypoint(this, newWaypoint, false));
            }
            case 1 -> {
                if (selectedWaypoint != null) {
                    waypointManager.setHighlightedWaypoint(selectedWaypoint, true);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    TreeSet<DimensionContainer> dimensions2 = new TreeSet<>();
                    dimensions2.add(dimensionManager.getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                    Waypoint fakePoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, 1.0F, 0.0F, 0.0F, "", waypointManager.getCurrentSubworldDescriptor(false), dimensions2);
                    waypointManager.setHighlightedWaypoint(fakePoint, true);
                }
            }
            case 2 -> {
                if (selectedWaypoint != null) {
                    CommandUtils.sendWaypoint(selectedWaypoint);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    CommandUtils.sendCoordinate(x, y, z);
                }
            }
            case 3 -> {
                if (selectedWaypoint == null) {
                    if (y < VoxelConstants.getPlayer().level().getMinY()) {
                        y = (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                    }
                    VoxelConstants.playerRunTeleportCommand(x, y, z);
                    break;
                }

                y = selectedWaypoint.getY() > VoxelConstants.getPlayer().level().getMinY() ? selectedWaypoint.getY() : (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                VoxelConstants.playerRunTeleportCommand(selectedWaypoint.getX(), y, selectedWaypoint.getZ());
            }
            case 4 -> {
                if (selectedWaypoint != null) {
                    editClicked = true;
                    minecraft.setScreen(new GuiAddWaypoint(this, selectedWaypoint, true));
                }
            }
            case 5 -> {
                if (selectedWaypoint != null) {
                    deleteClicked = true;
                    Component title = Component.translatable("minimap.waypoints.deleteConfirm");
                    Component explanation = Component.translatable("selectServer.deleteWarning", selectedWaypoint.name);
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
        return editClicked;
    }

    @Override
    public void accept(boolean accept) {
        if (deleteClicked) {
            deleteClicked = false;
            if (accept) {
                waypointManager.deleteWaypoint(selectedWaypoint);
                selectedWaypoint = null;
            }
        }

        if (editClicked) {
            editClicked = false;
            if (accept) {
                waypointManager.saveWaypoints();
            }
        }

        if (addClicked) {
            addClicked = false;
            if (accept) {
                waypointManager.addWaypoint(newWaypoint);
            }
        }

        minecraft.setScreen(this);
    }

    private int textWidth(String string) {
        return minecraft.font.width(string);
    }

    private int textWidth(Component text) {
        return minecraft.font.width(text);
    }

    private void write(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        write(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    private void write(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        drawContext.drawString(minecraft.font, text, (int) x, (int) y, color, shadow);
    }

    private void writeCentered(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        writeCentered(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    private void writeCentered(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        drawContext.drawString(minecraft.font, text, (int) x - (textWidth(text) / 2), (int) y, color, shadow);
    }
}
