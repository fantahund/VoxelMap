package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Consumer;

public class GuiMinimapOptions extends GuiScreenMinimap {
    protected String screenTitle = "Minimap Options";
    private final VoxelMap voxelMap = VoxelConstants.getVoxelMapInstance();
    private final MapSettingsManager mapOptions;
    private final RadarSettingsManager radarOptions;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;

    private final ArrayList<AbstractWidget> optionButtons = new ArrayList<>();
    private int pageIndex = 0;
    private String pageInfo = "";
    private int tabIndex = 0;
    private int lastTabIndex = 0;
    private Button nextPageButton;
    private Button prevPageButton;

    private static final EnumOptionsMinimap[] GENERAL_OPTIONS = { EnumOptionsMinimap.HIDE_MINIMAP, EnumOptionsMinimap.UPDATE_NOTIFIER, EnumOptionsMinimap.SHOW_BIOME, EnumOptionsMinimap.SHOW_COORDS, EnumOptionsMinimap.LOCATION, EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUARE_MAP, EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.IN_GAME_WAYPOINTS, EnumOptionsMinimap.CAVE_MODE, EnumOptionsMinimap.MOVE_MAP_BELOW_STATUS_EFFECT_ICONS, EnumOptionsMinimap.MOVE_SCOREBOARD_BELOW_MAP};
    private static final EnumOptionsMinimap[] PERFORMANCE_OPTIONS = { EnumOptionsMinimap.DYNAMIC_LIGHTING, EnumOptionsMinimap.TERRAIN_DEPTH, EnumOptionsMinimap.WATER_TRANSPARENCY, EnumOptionsMinimap.BLOCK_TRANSPARENCY, EnumOptionsMinimap.BIOMES, EnumOptionsMinimap.BIOME_OVERLAY, EnumOptionsMinimap.CHUNK_GRID, EnumOptionsMinimap.SLIME_CHUNKS, EnumOptionsMinimap.WORLD_BORDER,  EnumOptionsMinimap.FILTERING, EnumOptionsMinimap.TELEPORT_COMMAND };
    private static final EnumOptionsMinimap[] RADAR_FULL_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_MOB_NAMES, EnumOptionsMinimap.SHOW_PLAYER_NAMES, EnumOptionsMinimap.SHOW_MOB_HELMETS, EnumOptionsMinimap.SHOW_PLAYER_HELMETS, EnumOptionsMinimap.RADAR_FILTERING, EnumOptionsMinimap.RADAR_OUTLINES, EnumOptionsMinimap.SHOW_ENTITY_ELEVATION, EnumOptionsMinimap.HIDE_SNEAKING_PLAYERS };
    private static final EnumOptionsMinimap[] RADAR_SIMPLE_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_FACING, EnumOptionsMinimap.SHOW_ENTITY_ELEVATION, EnumOptionsMinimap.HIDE_SNEAKING_PLAYERS };

    // Performance Tab
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;

    // Radar Tab
    private Button mobListButton;

    public GuiMinimapOptions(Screen parent) {
        lastScreen = parent;
        mapOptions = voxelMap.getMapOptions();
        radarOptions = voxelMap.getRadarOptions();
    }

    @Override
    public void init() {
        screenTitle = I18n.get("options.minimap.title");

        tabNavigationBar = TabNavigationBar.builder(tabManager, width).addTabs(
                new OptionsTab(Component.translatable("stat.generalButton"), 0),
                new OptionsTab(Component.translatable("options.minimap.tab.detailsPerformance"), 1),
                new OptionsTab(Component.translatable("options.minimap.tab.radar"), 2),
                new OptionsTab(Component.translatable("controls.title"), 3),
                new OptionsTab(Component.translatable("options.minimap.tab.worldmap"), 4)).build();

        tabNavigationBar.selectTab(tabIndex, false);
        tabNavigationBar.arrangeElements();
        setFocused(tabNavigationBar);
        addRenderableWidget(tabNavigationBar);

        int tabBottom = tabNavigationBar.getRectangle().bottom();
        ScreenRectangle screenRect = new ScreenRectangle(0, tabBottom, width, height - layout.getFooterHeight() - tabBottom);
        tabManager.setTabArea(screenRect);
        layout.setHeaderHeight(tabBottom);
        layout.addToFooter(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).width(200).build());
        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();

        nextPageButton = new Button.Builder(Component.literal(">"), button -> {
            pageIndex++;
            replaceButtons();
        }).bounds(width / 2 + 140, height / 6 + 120, 40, 20).build();
        addRenderableWidget(nextPageButton);

        prevPageButton = new Button.Builder(Component.literal("<"), button -> {
            pageIndex--;
            replaceButtons();
        }).bounds(width / 2 - 180, height / 6 + 120, 40, 20).build();
        addRenderableWidget(prevPageButton);

        replaceButtons();
    }

    private void handleTabChange() {
        if (tabManager.getCurrentTab() instanceof OptionsTab tab) {
            if (tab.index() != tabIndex) {
                tabIndex = tab.index();
                replaceButtons();
            }
        }
    }

    public void replaceButtons() {
        for (GuiEventListener widget : optionButtons) {
            removeWidget(widget);
        }
        optionButtons.clear();

        EnumOptionsMinimap[] relevantOptions = null;
        switch (tabIndex) {
            case 0 -> relevantOptions = GENERAL_OPTIONS;
            case 1 -> relevantOptions = PERFORMANCE_OPTIONS;
            case 2 -> {
                if (radarOptions.radarMode == 2) {
                    relevantOptions = RADAR_FULL_OPTIONS;
                } else {
                    relevantOptions = RADAR_SIMPLE_OPTIONS;
                }
            }
            case 3 -> minecraft.setScreen(new GuiMinimapControls(this));
            case 4 -> minecraft.setScreen(new GuiPersistentMapOptions(this));
        }

        if (relevantOptions == null) {
            tabIndex = lastTabIndex;
            return;
        }
        if (tabIndex != lastTabIndex) {
            pageIndex = 0;
        }
        lastTabIndex = tabIndex;

        int itemCount = 10;
        int pageCount = (relevantOptions.length - 1) / itemCount;
        if (pageIndex > pageCount) {
            pageIndex = 0;
        }
        if (pageIndex < 0) {
            pageIndex = pageCount;
        }
        pageInfo = "[ " + (pageIndex + 1) + " / " + (pageCount + 1) + " ]";
        int pageStart = itemCount * pageIndex;
        int pageEnd = Math.min(itemCount * (pageIndex + 1), relevantOptions.length);

        nextPageButton.active = pageCount > 0;
        prevPageButton.active = pageCount > 0;

        // Menu Buttons
        for (int i = pageStart; i < pageEnd; i++) {
            EnumOptionsMinimap option = relevantOptions[i];

            ISettingsManager settingsManager = getSettingsManager(option);
            if (settingsManager == null) continue;

            int buttonX = getWidth() / 2 - 155 + (i - pageStart) % 2 * 160;
            int buttonY = getHeight() / 6 + 24 * ((i - pageStart) >> 1);

            // List / Toggle
            if (option.isBoolean() || option.isList()) {
                StringBuilder text = new StringBuilder().append(settingsManager.getKeyText(option));
                if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !mapOptions.multicore && settingsManager.getBooleanValue(option)) {
                    text.append("§c").append(text);
                }

                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(buttonX, buttonY, option, Component.literal(text.toString()), this::optionClicked);
                addOptionButton(optionButton);

                if (option == EnumOptionsMinimap.SLIME_CHUNKS) {
                    slimeChunksButton = optionButton;
                }
            }

            // Text Field
            if (option == EnumOptionsMinimap.TELEPORT_COMMAND) {
                String buttonTeleportText = I18n.get("options.minimap.teleportCommand") + ": " + mapOptions.teleportCommand;
                teleportCommandButton = new GuiButtonText(getFont(), buttonX, buttonY, 150, 20, Component.literal(buttonTeleportText), button -> teleportCommandButton.setEditing(true));
                teleportCommandButton.setText(mapOptions.teleportCommand);
                teleportCommandButton.active = mapOptions.serverTeleportCommand == null;
                addOptionButton(teleportCommandButton);
            }
        }

        int additionalButtonX = width / 2 - 75;
        int additionalButtonY = height / 6 + 144;

        // Additional Buttons
        if (relevantOptions == PERFORMANCE_OPTIONS) {
            String worldSeedDisplay = voxelMap.getWorldSeed();
            if (worldSeedDisplay.isEmpty()) {
                worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
            }

            String buttonSeedText = I18n.get("options.minimap.worldSeed") + ": " + worldSeedDisplay;
            worldSeedButton = new GuiButtonText(getFont(), additionalButtonX, additionalButtonY, 150, 20, Component.literal(buttonSeedText), button -> worldSeedButton.setEditing(true));
            worldSeedButton.setText(voxelMap.getWorldSeed());
            worldSeedButton.active = !minecraft.hasSingleplayerServer();
            addOptionButton(worldSeedButton);
        }

        if (relevantOptions == RADAR_FULL_OPTIONS) {
            mobListButton = new Button.Builder(Component.translatable("options.minimap.radar.selectMobs"), x -> minecraft.setScreen(new GuiMobs(this, radarOptions))).bounds(additionalButtonX, additionalButtonY, 150, 20).build();
            addOptionButton(mobListButton);
        }

        setButtonsActive();

    }

    private void addOptionButton(AbstractWidget widget) {
        optionButtons.add(widget);
        addRenderableWidget(widget);
    }

    private void optionClicked(Button button) {
        if (!(button instanceof GuiOptionButtonMinimap button2)) {
            return;
        }
        EnumOptionsMinimap option = button2.returnEnumOptions();

        ISettingsManager settingsManager = getSettingsManager(option);
        if (settingsManager == null) return;

        MapSettingsManager.updateBooleanOrListValue(settingsManager, option);

        String prefix = "";
        switch (option) {
            case OLD_NORTH -> voxelMap.getWaypointManager().setOldNorth(mapOptions.oldNorth);
            case WATER_TRANSPARENCY, BLOCK_TRANSPARENCY, BIOMES -> {
                if (!mapOptions.multicore && option.isBoolean() && settingsManager.getBooleanValue(option)) {
                    prefix = "§c";
                }
            }
            case RADAR_MODE -> replaceButtons();
        }

        button2.setMessage(Component.literal(prefix + settingsManager.getKeyText(option)));
        setButtonsActive();
    }

    private void setButtonsActive() {
        for (GuiEventListener button : children()) {
            if (!(button instanceof GuiOptionButtonMinimap button2)){
                continue;
            }
            EnumOptionsMinimap option = button2.returnEnumOptions();

            boolean radarBlocked = !radarOptions.radarAllowed && !radarOptions.radarPlayersAllowed && !radarOptions.radarMobsAllowed;

            if (containsOption(option, RADAR_FULL_OPTIONS) || containsOption(option, RADAR_SIMPLE_OPTIONS)) {
                button2.active = radarOptions.showRadar && !radarBlocked;
                if (mobListButton != null) {
                    mobListButton.active = radarOptions.showRadar && !radarBlocked;
                }
            }

            switch (option) {
                case HIDE_MINIMAP -> button2.active = mapOptions.minimapAllowed;
                case IN_GAME_WAYPOINTS -> button2.active = mapOptions.waypointsAllowed;
                case CAVE_MODE -> button2.active = mapOptions.cavesAllowed;
                case SLIME_CHUNKS -> button2.active = minecraft.hasSingleplayerServer() || !voxelMap.getWorldSeed().isEmpty();
                case SHOW_RADAR -> button2.active = !radarBlocked;
                case SHOW_PLAYERS -> button2.active = button2.active && radarOptions.radarPlayersAllowed;
                case SHOW_MOBS -> button2.active = button2.active && radarOptions.radarMobsAllowed;
                case SHOW_PLAYER_HELMETS, SHOW_PLAYER_NAMES -> button2.active = button2.active && radarOptions.showPlayers && radarOptions.radarPlayersAllowed;
                case SHOW_MOB_HELMETS, SHOW_MOB_NAMES -> button2.active = button2.active && (radarOptions.showNeutrals || radarOptions.showHostiles) && radarOptions.radarMobsAllowed;
            }
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
        drawContext.drawCenteredString(font, pageInfo, width / 2, height / 6 + 126, 0xFFFFFFFF);
        handleTabChange();
    }

    @Override
    public void renderMenuBackground(GuiGraphics drawContext) {
        drawContext.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, layout.getHeaderHeight(), 16, 16);
        renderMenuBackground(drawContext, 0, layout.getHeaderHeight(), width, height);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (worldSeedButton != null) {
                worldSeedButton.keyPressed(keyEvent);
            }
            if (teleportCommandButton != null) {
                teleportCommandButton.keyPressed(keyEvent);
            }
        }

        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (worldSeedButton != null && worldSeedButton.isEditing()) {
                newSeed();
            } else if (teleportCommandButton != null && teleportCommandButton.isEditing()) {
                newTeleportCommand();
            }

        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        if (characterEvent.codepoint() == '\r') {
            if (worldSeedButton != null && worldSeedButton.isEditing()) {
                newSeed();
            } else if (teleportCommandButton != null && teleportCommandButton.isEditing()) {
                newTeleportCommand();
            }

        }

        return OK;
    }

    private record OptionsTab(Component title, int index) implements Tab {
        @Override
        public Component getTabTitle() {
            return title;
        }

        @Override
        public Component getTabExtraNarration() {
            return Component.empty();
        }

        @Override
        public void visitChildren(Consumer<AbstractWidget> consumer) {
        }

        @Override
        public void doLayout(ScreenRectangle screenRectangle) {
        }

    }

    private void newSeed() {
        if (worldSeedButton != null) {
            String newSeed = worldSeedButton.getText();
            voxelMap.setWorldSeed(newSeed);
            String worldSeedDisplay = voxelMap.getWorldSeed();
            if (worldSeedDisplay.isEmpty()) {
                worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
            }

            String buttonText = I18n.get("options.minimap.worldSeed") + ": " + worldSeedDisplay;
            worldSeedButton.setMessage(Component.literal(buttonText));
            worldSeedButton.setText(voxelMap.getWorldSeed());
            voxelMap.getMap().forceFullRender(true);
        }
        if (slimeChunksButton != null) {
            slimeChunksButton.active = minecraft.hasSingleplayerServer() || !voxelMap.getWorldSeed().isEmpty();
        }

    }

    private void newTeleportCommand() {
        if (teleportCommandButton != null) {
            String newTeleportCommand = teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : teleportCommandButton.getText();
            mapOptions.teleportCommand = newTeleportCommand;

            String buttonText = I18n.get("options.minimap.teleportCommand") + ": " + newTeleportCommand;
            teleportCommandButton.setMessage(Component.literal(buttonText));
            teleportCommandButton.setText(mapOptions.teleportCommand);
        }

    }

    private ISettingsManager getSettingsManager(EnumOptionsMinimap option) {
        if (containsOption(option, GENERAL_OPTIONS) || containsOption(option, PERFORMANCE_OPTIONS)) {
            return mapOptions;
        }
        if (containsOption(option, RADAR_SIMPLE_OPTIONS) || containsOption(option, RADAR_FULL_OPTIONS)) {
            return radarOptions;
        }

        return null;
    }

    private boolean containsOption(EnumOptionsMinimap option, EnumOptionsMinimap[] optionArray) {
        for (EnumOptionsMinimap x : optionArray) {
            if (x == option) return true;
        }
        return false;
    }
}
