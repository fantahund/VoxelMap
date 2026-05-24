package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.IGuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.PopupGuiScreen;
import com.mamiyaotaru.voxelmap.gui.widgets.Popup;
import com.mamiyaotaru.voxelmap.gui.widgets.PopupGuiButton;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.PersistentMapOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.render.RenderUtils;
import com.mamiyaotaru.voxelmap.render.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.textures.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BiomeMapData;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.EasingUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final MapPermissionsManager permissions;
    private final MapOptions mapOptions;
    private final PersistentMapOptions options;
    private final PersistentMap persistentMap;
    private final WaypointManager waypointManager;
    private final DimensionManager dimensionManager;
    private final Random generator = new Random();

    // Mapping & Threading
    private final Object closedLock = new Object();
    private boolean closed;
    private boolean lastStill;
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);
    private CachedRegion[] regionsToDisplay = new CachedRegion[0];

    // UI & Layouts
    private final CachedOrthoProjectionMatrixBuffer mapProjection;
    private static final float MAP_IMAGE_DEPTH = 0.0F;
    private static final float MAP_TEXT_DEPTH = 100.0F;
    private static final float MAP_ICON_DEPTH = 200.0F;
    private static final float MAP_OVERLAY_DEPTH = 300.0F;

    protected String screenTitle = "World Map";
    private float scScale = 1.0F;
    private int top;
    private int bottom;

    private static final int SIDE_MARGIN = 10;
    private static final int ICON_WIDTH = 16;
    private static final int ICON_HEIGHT = 16;

    private Component multiworldButtonName;
    private Component multiworldButtonNameRed;
    private PopupGuiButton buttonWaypoints;
    private PopupGuiButton buttonMultiworld;
    private Sprite playerSkin;
    private final Identifier crosshairResource = Identifier.parse("textures/gui/sprites/hud/crosshair.png");
    private final Identifier caveButtonTexture = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/worldmap/cave_button.png");

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
    private float mouseDirectToMap = 1.0F;
    private float guiToDirectMouse = 2.0F;

    // Input & Navigation
    private boolean skipMouseDetection;
    private boolean mouseCursorShown = true;
    private boolean leftMouseButtonDown;
    private long timeOfLastMouseInput;
    private int lastMouseX;
    private int lastMouseY;
    private float lastMouseDirectX;
    private float lastMouseDirectY;

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

    public GuiPersistentMap(Screen parentGui) {
        super(parentGui, Component.empty());

        waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();
        permissions = VoxelConstants.getVoxelMapInstance().getPermissionsManager();
        options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        persistentMap.setLightMapArray(VoxelConstants.getVoxelMapInstance().getMap().getLightmapArray());

        mapProjection = new CachedOrthoProjectionMatrixBuffer("VoxelMap WorldMap Projection", 1000.0F, 21000.0F, true);
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

        oldNorth = mapOptions.oldNorth.get();
        centerX = getWidth() / 2;
        centerY = (bottom - top) / 2;
        mapPixelsX = getWindowWidth();
        mapPixelsY = getWindowHeight() - (int) (64.0F * scScale);

        zoom = options.getZoom();
        zoomStart = options.getZoom();
        zoomGoal = options.getZoom();

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

    private float bindZoom(float zoom) {
        return Math.min(Math.max(zoom, options.minZoomExponent.get()), options.maxZoomExponent.get());
    }

    private int getWindowWidth() {
        return minecraft.getWindow().getWidth();
    }

    private int getWindowHeight() {
        return minecraft.getWindow().getHeight();
    }

    private double getMouseDirectX() {
        return minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()) * minecraft.getWindow().getGuiScale();
    }

    private double getMouseDirectY() {
        return minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()) * minecraft.getWindow().getGuiScale();
    }

    private void zoom(double amount, float mouseDirectX, float mouseDirectY) {
        float lastZoom = zoom;
        float nextZoom = options.zoomExponent.get();
        if (amount > 0.0) {
            nextZoom += 1.0F / 3.0F;
        } else if (amount < 0.0) {
            nextZoom -= 1.0F / 3.0F;
        }
        nextZoom = bindZoom(nextZoom);
        options.zoomExponent.set(nextZoom);

        zoomStart = lastZoom;
        zoomGoal = options.getZoom();
        timeOfZoom = System.currentTimeMillis();
        zoomDirectX = mouseDirectX;
        zoomDirectY = mouseDirectY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        timeOfLastMouseInput = System.currentTimeMillis();
        switchToMouseInput();
        zoom(amount, (float) getMouseDirectX(), (float) getMouseDirectY());

        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        selectedWaypoint = getHoveredWaypoint();
        if (mouseButtonEvent.button() == 1 && (selectedWaypoint != null || (lastMouseY > top && lastMouseY < bottom))) {
            timeOfLastKeyboardInput = 0L;

            double mouseX = mouseButtonEvent.x();
            double mouseY = mouseButtonEvent.y();
            if (permissions.getBoolean(MapPermissionsManager.WORLDMAP_ALLOWED)) {
                createPopup((int) mouseX, (int) mouseY, (int) getMouseDirectX(), (int) getMouseDirectY());
            }
        }

        return super.mouseReleased(mouseButtonEvent);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        boolean jumpPressed = minecraft.options.keyJump.matches(keyEvent);
        boolean sneakPressed = minecraft.options.keyShift.matches(keyEvent);

        if (jumpPressed || sneakPressed) {
            double amount = 0.0;
            if (jumpPressed) {
                amount = -1.0;
            }
            if (sneakPressed) {
                amount = 1.0;
            }
            zoom(amount, getWindowWidth() / 2.0F, getWindowHeight() / 2.0F);

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

    private void handleInputAndNavigation(int mouseX, int mouseY, float mouseDirectX, float mouseDirectY) {
        if (mouseX != lastMouseX || mouseY != lastMouseY) {
            timeOfLastMouseInput = System.currentTimeMillis();
            switchToMouseInput();

            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }

        if (zoom != zoomGoal) {
            float lastZoom = zoom;

            long elapsed = System.currentTimeMillis() - timeOfZoom;
            if (elapsed < 700.0F) {
                zoom = EasingUtils.easeOutExpo(zoomStart, zoomGoal, elapsed, 700.0F);
            } else {
                zoom = zoomGoal;
            }

            float mouseOffsetX = (centerX * guiToDirectMouse) - zoomDirectX;
            float mouseOffsetZ = ((top + centerY) * guiToDirectMouse) - zoomDirectY;
            float zoomRatio = zoom / lastZoom;
            float scaledZoom = zoom * getWindowScale();

            float moveFactor = (1.0F - zoomRatio) / scaledZoom;

            mapCenterX += mouseOffsetX * moveFactor;
            mapCenterZ += mouseOffsetZ * moveFactor;
        }

        float scaledZoom = zoom * getWindowScale();

        guiToMap = scScale / scaledZoom;
        mapToGui = 1.0F / guiToMap;
        mouseDirectToMap = 1.0F / scaledZoom;
        guiToDirectMouse = scScale;

        if (isMouseDown(GLFW.GLFW_MOUSE_BUTTON_1)) {
            if (leftMouseButtonDown) {
                deltaX = (lastMouseDirectX - mouseDirectX) * mouseDirectToMap;
                deltaY = (lastMouseDirectY - mouseDirectY) * mouseDirectToMap;
                deltaXonRelease = deltaX;
                deltaYonRelease = deltaY;
                timeOfRelease = System.currentTimeMillis();
            } else if (overPopup(mouseX, mouseY)) {
                deltaX = 0.0F;
                deltaY = 0.0F;
                leftMouseButtonDown = true;
            }

            lastMouseDirectX = mouseDirectX;
            lastMouseDirectY = mouseDirectY;
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
        buttonWaypoints.active = permissions.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED);

        guiGraphics.fill(0, 0, getWidth(), getHeight(), 0xFF000000);

        float mouseDirectX = (float) getMouseDirectX();
        float mouseDirectY = (float) getMouseDirectY();

        handleInputAndNavigation(mouseX, mouseY, mouseDirectX, mouseDirectY);

        float cursorX;
        float cursorY;
        if (mouseCursorShown) {
            cursorX = mouseDirectX;
            cursorY = mouseDirectY - top * guiToDirectMouse;
        } else {
            cursorX = getWindowWidth() / 2.0F;
            cursorY = getWindowHeight() / 2.0F - top * guiToDirectMouse;
        }

        float cursorCoordX;
        float cursorCoordZ;
        if (oldNorth) {
            cursorCoordX = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
            cursorCoordZ = -(cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap));
        } else {
            cursorCoordX = cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap);
            cursorCoordZ = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
        }

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

        RenderUtils.setProjectionMatrix(mapProjection.getBuffer(RenderUtils.getGuiWidth(), RenderUtils.getGuiHeight()), ProjectionType.ORTHOGRAPHIC, -2000.0F);
        RenderUtils.setRenderTarget(RenderUtils.getFullscreenRenderTarget(), true);
        Matrix4fStack matrixStack = RenderUtils.getRenderMatrixStack();
        matrixStack.pushMatrix();
        matrixStack.translate(centerX - mapCenterX * mapToGui, top + centerY - mapCenterZ * mapToGui, 0.0F);
        if (oldNorth) {
            matrixStack.rotate(Axis.ZP.rotationDegrees(90.0F));
        }
        matrixStack.scale(mapToGui, mapToGui, 1.0F);

        if (permissions.getBoolean(MapPermissionsManager.WORLDMAP_ALLOWED)) {
            for (CachedRegion region : regionsToDisplay) {
                if (region != null) {
                    AbstractTexture image = region.getMapImage(zoom);
                    if (image != null) {
                        RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, image);
                        RenderUtils.drawTexturedModalRect(matrixStack, region.getX() * 256.0F, region.getZ() * 256.0F, MAP_IMAGE_DEPTH, region.getWidth(), region.getWidth(), 0xFFFFFFFF);
                        RenderUtils.endBatch();
                    }
                }
            }

            if (mapOptions.worldBorder.get()) {
                WorldBorder worldBorder = minecraft.level.getWorldBorder();
                float scale = 1.0F / scScale / mapToGui;

                float x1 = (float) (worldBorder.getMinX());
                float z1 = (float) (worldBorder.getMinZ());
                float x2 = (float) (worldBorder.getMaxX());
                float z2 = (float) (worldBorder.getMaxZ());

                RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, TextureSetup.noTexture());
                RenderUtils.drawTexturedModalRect(matrixStack, x1 - scale, z1 - scale, MAP_IMAGE_DEPTH, x2 + scale, z1 + scale, 0xFFFF0000);
                RenderUtils.drawTexturedModalRect(matrixStack, x1 - scale, z2 - scale, MAP_IMAGE_DEPTH, x2 + scale, z2 + scale, 0xFFFF0000);
                RenderUtils.drawTexturedModalRect(matrixStack, x1 - scale, z1 - scale, MAP_IMAGE_DEPTH, x1 + scale, z2 + scale, 0xFFFF0000);
                RenderUtils.drawTexturedModalRect(matrixStack, x2 - scale, z1 - scale, MAP_IMAGE_DEPTH, x2 + scale, z2 + scale, 0xFFFF0000);
                RenderUtils.endBatch();
            }

            if (oldNorth) {
                matrixStack.rotate(Axis.ZP.rotationDegrees(-90.0F));
            }

            matrixStack.scale(guiToMap, guiToMap, 1.0F);
            matrixStack.translate(-(centerX - mapCenterX * mapToGui), -(top + centerY - mapCenterZ * mapToGui), 0.0F);

            if (mapOptions.biomeOverlay.get() != OptionEnumMinimap.BiomeOverlay.OFF) {
                drawBiomeOverlay(matrixStack, regionLeft, regionRight, regionTop, regionBottom);
            }
        }
        matrixStack.popMatrix();

        overlayBackground(matrixStack, 0, top);
        overlayBackground(matrixStack, bottom, getHeight());

        Waypoint currentlyHovered = null;
        if (permissions.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED) && options.showWaypoints.get()) {
            TextureAtlas textureAtlas = waypointManager.getTextureAtlas();
            RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, textureAtlas);

            for (Waypoint waypoint : waypointManager.getWaypoints()) {
                if (!waypoint.inWorld || !waypoint.inDimension) continue;

                boolean isHighlighted = waypointManager.isHighlightedWaypoint(waypoint);
                boolean isHovered = drawWaypoint(matrixStack, waypoint, textureAtlas, null, isHighlighted, -1, mouseX, mouseY);
                if (isHovered) {
                    currentlyHovered = waypoint;
                }
            }

            Waypoint highlightedPoint = waypointManager.getHighlightedWaypoint();
            if (highlightedPoint != null) {
                boolean isHovered = drawWaypoint(matrixStack, highlightedPoint, textureAtlas, textureAtlas.getAtlasSprite("marker/target"), true, 0xFFFF0000, mouseX, mouseY);
                if (isHovered) {
                    currentlyHovered = highlightedPoint;
                }
            }

            RenderUtils.endBatch();

            if (currentlyHovered != null) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
                if (options.showCoordinates.get()) {
                    Tooltip tooltip = Tooltip.create(Component.literal("X: " + currentlyHovered.getX() + ", Y: " + currentlyHovered.getY() + ", Z: " + currentlyHovered.getZ()));
                    RenderUtils.drawTooltip(guiGraphics, tooltip, mouseX, mouseY);
                }
            }
        }
        hoverdWaypoint = currentlyHovered;

        if (playerSkin == null) {
            playerSkin = VoxelConstants.getVoxelMapInstance().getEntityMapImageManager().requestImageForMob(VoxelConstants.getPlayer(), true);
        } else {
            float playerX = (float) GameVariableAccessShim.xCoordDouble();
            float playerZ = (float) GameVariableAccessShim.zCoordDouble();
            boolean isHovered = drawPlayer(matrixStack, playerSkin, playerX, playerZ, mouseX, mouseY);

            if (isHovered) {
                guiGraphics.requestCursor(CursorTypes.CROSSHAIR);
                if (options.showCoordinates.get()) {
                    Component tooltip = Component.literal("X: " + GameVariableAccessShim.xCoord() + ", Y: " + GameVariableAccessShim.yCoord() + ", Z: " + GameVariableAccessShim.zCoord());
                    RenderUtils.drawTooltip(guiGraphics, Tooltip.create(tooltip), mouseX, mouseY);
                }
            }
        }

        RenderUtils.restoreRenderTarget();
        RenderUtils.restoreProjectionMatrix();

        RenderUtils.blitRenderTarget(guiGraphics, RenderUtils.getFullscreenRenderTarget());

        if (System.currentTimeMillis() - timeOfLastKeyboardInput < 2000L) {
            guiGraphics.blit(RenderPipelines.CROSSHAIR, crosshairResource, (getWidth() - 15) / 2, (getHeight() - 15) / 2, 0.0F, 0.0F, 15, 15, 15, 15);
        } else {
            switchToMouseInput();
        }

        if (permissions.getBoolean(MapPermissionsManager.WORLDMAP_ALLOWED)) {
            guiGraphics.drawCenteredString(getFont(), screenTitle, getWidth() / 2, 16, 0xFFFFFFFF);
            int x = (int) Math.floor(cursorCoordX);
            int z = (int) Math.floor(cursorCoordZ);
            if (options.showCoordinates.get()) {
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

    private boolean drawPlayer(Matrix4fStack matrixStack, Sprite skin, float playerX, float playerZ, int mouseX, int mouseY) {
        float headWidth = ICON_WIDTH * 0.75F;
        float headHeight = ICON_HEIGHT * 0.75F;

        int x = getWidth() / 2;
        int y = getHeight() / 2;
        int borderX = x - 4;
        int borderY = y - top;

        double wayX = mapCenterX - (oldNorth ? -playerZ : playerX);
        double wayY = mapCenterZ - (oldNorth ? playerX : playerZ);
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

        double dispX = hypot * Math.sin(Math.toRadians(locate));
        double dispY = hypot * Math.cos(Math.toRadians(locate));
        boolean far = Math.abs(dispX) > borderX || Math.abs(dispY) > borderY;
        if (far) {
            hypot *= (float) Math.min(borderX / Math.abs(dispX), borderY / Math.abs(dispY));
        }

        matrixStack.pushMatrix();

        if (far) {
            matrixStack.translate(x, y, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
            matrixStack.translate(0.0F, -hypot, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
            matrixStack.translate(-x, -y, 0.0F);
        } else {
            matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
            matrixStack.translate(0.0F, -hypot, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
        }

        Vector3f guiVector = matrixStack.transformPosition(new Vector3f(x, y, 0.0F));
        float screenX = guiVector.x();
        float screenY = guiVector.y();

        RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, skin.getIdentifier());
        RenderUtils.drawSpriteRect(matrixStack, skin, x - headWidth / 2.0F, y - headHeight / 2.0F, MAP_OVERLAY_DEPTH, headWidth, headHeight, 0xFFFFFFFF);
        RenderUtils.endBatch();

        matrixStack.popMatrix();

        return mouseX >= screenX - ICON_WIDTH / 2.0F && mouseX <= screenX + ICON_WIDTH / 2.0F && mouseY >= screenY - ICON_HEIGHT / 2.0F && mouseY <= screenY + ICON_HEIGHT / 2.0F;
    }

    private boolean drawWaypoint(Matrix4fStack matrixStack, Waypoint waypoint, TextureAtlas textureAtlas, Sprite icon, boolean isHighlighted, int color, int mouseX, int mouseY) {
        float ptX = waypoint.getX() + 0.5F;
        float ptZ = waypoint.getZ() + 0.5F;

        int x = getWidth() / 2;
        int y = getHeight() / 2;

        int borderOffsetX = options.showDistantWaypoints.get() ? -4 : ICON_WIDTH / 2;
        int borderOffsetY = options.showDistantWaypoints.get() ? 0 : ICON_HEIGHT / 2;
        int borderX = x + borderOffsetX;
        int borderY = y - top + borderOffsetY;

        double wayX = mapCenterX - (oldNorth ? -ptZ : ptX);
        double wayY = mapCenterZ - (oldNorth ? ptX : ptZ);
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY) * mapToGui;

        double dispX = hypot * Math.sin(Math.toRadians(locate));
        double dispY = hypot * Math.cos(Math.toRadians(locate));
        boolean far = Math.abs(dispX) > borderX || Math.abs(dispY) > borderY;
        if (far) {
            if (!options.showDistantWaypoints.get()) {
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
            String fallbackLocation = far ? "marker/arrow" : WaypointManager.FALLBACK_ICON_NAME;

            icon = textureAtlas.getAtlasSprite(iconLocation);
            if (icon == textureAtlas.getMissingImage()) {
                icon = textureAtlas.getAtlasSprite(fallbackLocation);
            }
        }

        matrixStack.pushMatrix();

        if (far) {
            matrixStack.translate(x, y, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
            if (uprightIcon) {
                matrixStack.translate(0.0F, -hypot, 0.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
                matrixStack.translate(-x, -y, 0.0F);
            } else {
                matrixStack.translate(-x, -y, 0.0F);
                matrixStack.translate(0.0F, -hypot, 0.0F);
            }
        } else {
            matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
            matrixStack.translate(0.0F, -hypot, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
        }

        Vector3f guiVector = matrixStack.transformPosition(new Vector3f(x, y, 0.0F));
        float screenX = guiVector.x();
        float screenY = guiVector.y();

        boolean isHovered = mouseX >= screenX - ICON_WIDTH / 2.0F && mouseX <= screenX + ICON_WIDTH / 2.0F  && mouseY >= screenY - ICON_HEIGHT / 2.0F && mouseY <= screenY + ICON_HEIGHT / 2.0F;

        float iconDepth = options.showDistantWaypoints.get() ? MAP_OVERLAY_DEPTH : MAP_ICON_DEPTH;
        int iconColor = color == -1 ? waypoint.getUnifiedColor(!waypoint.enabled && !isHighlighted && !isHovered ? 0.3F : 1.0F) : color;
        int textColor = !waypoint.enabled && !isHighlighted && !isHovered ? 0x55FFFFFF : 0xFFFFFFFF;
        RenderUtils.drawSpriteRect(matrixStack, icon, x - ICON_WIDTH / 2.0F, y - ICON_HEIGHT / 2.0F, iconDepth, ICON_WIDTH, ICON_HEIGHT, iconColor);
        if (!far && options.showWaypointNames.get()) {
            RenderUtils.drawCenteredString(matrixStack, name, x, y + ICON_HEIGHT / 2.0F, MAP_TEXT_DEPTH, textColor, true);
        }

        matrixStack.popMatrix();

        return isHovered;
    }

    private void drawBiomeOverlay(Matrix4fStack matrixStack, int regionLeft, int regionRight, int regionTop, int regionBottom) {
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
                        floatMapX = z * biomeScaleY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
                        floatMapZ = -(x * biomeScaleX * mouseDirectToMap + (mapCenterX - centerX * guiToMap));
                    } else {
                        floatMapX = x * biomeScaleX * mouseDirectToMap + (mapCenterX - centerX * guiToMap);
                        floatMapZ = z * biomeScaleY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
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

                    RenderUtils.drawCenteredString(matrixStack, label, x, top + z - 3.0F, 0.0F, 0xFFFFFFFF, true);
                }
            }
        }
    }

    protected void overlayBackground(Matrix4fStack matrixStack, int startY, int endY) {
        RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, VoxelConstants.getOptionsBackgroundTexture());
        RenderUtils.drawTexturedModalRect(matrixStack, 0.0F, startY, MAP_OVERLAY_DEPTH, getWidth(), 32.0F, 0.0F, getWidth() / 32.0F, 0.0F, (endY - startY) / 32.0F, 0xFF404040);

        boolean isTop = startY < getHeight() / 2;
        int color0 = isTop ? 0xFF000000 : 0x00000000;
        int color1 = isTop ? 0x00000000 : 0xFF000000;
        float y0 = isTop ? endY : startY - 4.0F;
        float y1 = isTop ? endY + 4.0F : startY;

        RenderUtils.vertexBuffer().addVertex(matrixStack, 0.0F, y0, MAP_OVERLAY_DEPTH).setUv(0.0F, 0.0F).setColor(color0);
        RenderUtils.vertexBuffer().addVertex(matrixStack, 0.0F, y1, MAP_OVERLAY_DEPTH).setUv(0.0F, 1.0F).setColor(color1);
        RenderUtils.vertexBuffer().addVertex(matrixStack, 0.0F + getWidth(), y1, MAP_OVERLAY_DEPTH).setUv(1.0F, 1.0F).setColor(color1);
        RenderUtils.vertexBuffer().addVertex(matrixStack, 0.0F + getWidth(), y0, MAP_OVERLAY_DEPTH).setUv(1.0F, 0.0F).setColor(color0);

        RenderUtils.endBatch();
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
        float cursorY = directY - top * guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (oldNorth) {
            cursorCoordX = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
            cursorCoordZ = -(cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap));
        } else {
            cursorCoordX = cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap);
            cursorCoordZ = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
        }

        Popup.PopupEntry entry;
        if (selectedWaypoint != null && waypointManager.getWaypoints().contains(selectedWaypoint)) {
            entry = new Popup.PopupEntry(I18n.get("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(selectedWaypoint != waypointManager.getHighlightedWaypoint() ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.get("minimap.waypoints.newWaypoint"), 0, true, permissions.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED));
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(selectedWaypoint == null ? "minimap.waypoints.highlight" : "minimap.waypoints.removeHighlight"), 1, true, permissions.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED));
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
        return permissions.getBoolean(MapPermissionsManager.WAYPOINTS_ALLOWED) ? hoverdWaypoint : null;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.getClickedDirectX();
        int mouseDirectY = popup.getClickedDirectY();
        float cursorX = mouseDirectX;
        float cursorY = mouseDirectY - top * guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (oldNorth) {
            cursorCoordX = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
            cursorCoordZ = -(cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap));
        } else {
            cursorCoordX = cursorX * mouseDirectToMap + (mapCenterX - centerX * guiToMap);
            cursorCoordZ = cursorY * mouseDirectToMap + (mapCenterZ - centerY * guiToMap);
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
}
