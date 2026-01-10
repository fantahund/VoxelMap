package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.GridLayout;
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

public class GuiMinimapOptions extends GuiScreenMinimap {
    private final MapSettingsManager options;
    private final RadarSettingsManager radarOptions;
    protected String screenTitle = "Minimap Options";

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;

    // Performance Tab
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;

    private int lastTabIndex = 0;

    public GuiMinimapOptions(Screen parent) {
        this.lastScreen = parent;

        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.radarOptions = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void init() {
        this.screenTitle = I18n.get("options.minimap.title");

        this.clearWidgets();

        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width).addTabs(new Tab[] {
                new GeneralTab(Component.translatable("stat.generalButton"), this, 0),
                new PerformanceTab(Component.translatable("options.minimap.tab.detailsPerformance"), this, 1),
                new RadarTab(Component.translatable("options.minimap.tab.radar"), this, 2),
                new RedirectionTab(Component.translatable("controls.title"), this, new GuiMinimapControls(this)),
                new RedirectionTab(Component.translatable("options.minimap.tab.worldmap"), this, new GuiPersistentMapOptions(this))}).build();

        this.tabNavigationBar.selectTab(this.lastTabIndex, false);
        this.tabNavigationBar.arrangeElements();
        this.addRenderableWidget(this.tabNavigationBar);

        int tabBottom = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle screenRect = new ScreenRectangle(0, tabBottom, this.width, this.height - this.layout.getFooterHeight() - tabBottom);
        this.tabManager.setTabArea(screenRect);
        this.layout.addToFooter(new Button.Builder(Component.translatable("gui.done"), button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.setHeaderHeight(tabBottom);
        this.layout.arrangeElements();

    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);
    }

    @Override
    public void renderMenuBackground(GuiGraphics drawContext) {
        drawContext.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(drawContext, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();

        if (this.worldSeedButton != null && this.teleportCommandButton != null && this.slimeChunksButton != null) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                this.worldSeedButton.keyPressed(keyEvent);
                this.teleportCommandButton.keyPressed(keyEvent);
            }

            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                if (this.worldSeedButton.isEditing()) {
                    this.newSeed();
                } else if (this.teleportCommandButton.isEditing()) {
                    this.newTeleportCommand();
                }

            }
        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        if (this.worldSeedButton != null && this.teleportCommandButton != null && this.slimeChunksButton != null) {
            if (characterEvent.codepoint() == '\r') {
                if (this.worldSeedButton.isEditing()) {
                    this.newSeed();
                } else if (this.teleportCommandButton.isEditing()) {
                    this.newTeleportCommand();
                }

            }
        }

        return OK;
    }

    private void newSeed() {
        String newSeed = this.worldSeedButton.getText();
        VoxelConstants.getVoxelMapInstance().setWorldSeed(newSeed);
        String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (worldSeedDisplay.isEmpty()) {
            worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
        }

        String buttonText = I18n.get("options.minimap.worldSeed") + ": " + worldSeedDisplay;
        this.worldSeedButton.setMessage(Component.literal(buttonText));
        this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
        VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
    }

    private void newTeleportCommand() {
        String newTeleportCommand = this.teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : this.teleportCommandButton.getText();
        VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand = newTeleportCommand;

        String buttonText = I18n.get("options.minimap.teleportCommand") + ": " + newTeleportCommand;
        this.teleportCommandButton.setMessage(Component.literal(buttonText));
        this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
    }

    private class BasicTabEntry extends GridLayoutTab {
        protected GuiMinimapOptions parent;
        private final int tabIndex;

        public BasicTabEntry(Component component, GuiMinimapOptions parent, int tabIndex) {
            super(component);
            this.parent = parent;
            this.tabIndex = tabIndex;
            this.layout.addChild(new PlainTextButton(0, 0, 0, 0, Component.empty(), btn -> {}, VoxelConstants.getMinecraft().font) {
                @Override
                public void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
                    BasicTabEntry.this.render(guiGraphics, mouseX, mouseY, delta);
                }
            }, 1, 1);
        }

        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            if (this.tabIndex != -1) {
                this.parent.lastTabIndex = this.tabIndex;
            }
            this.layout.setY(this.parent.height / 6);
        }
    }

    private class GeneralTab extends BasicTabEntry {
        private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.SHOW_COORDS, EnumOptionsMinimap.HIDE_MINIMAP, EnumOptionsMinimap.LOCATION, EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUARE_MAP, EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.IN_GAME_WAYPOINTS, EnumOptionsMinimap.CAVE_MODE, EnumOptionsMinimap.MOVE_MAP_DOWN_WHILE_STATUS_EFFECT, EnumOptionsMinimap.MOVE_SCOREBOARD_DOWN };
        private final MapSettingsManager options;

        public GeneralTab(Component component, GuiMinimapOptions parent, int tabIndex) {
            super(component, parent, tabIndex);
            this.options = parent.options;
            this.parent = parent;
            this.layout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
            GridLayout.RowHelper row = this.layout.createRowHelper(2);
            for (EnumOptionsMinimap option : relevantOptions) {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(0, 0, option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
                row.addChild(optionButton);

                switch (option) {
                    case HIDE_MINIMAP -> optionButton.active = this.options.minimapAllowed;
                    case IN_GAME_WAYPOINTS -> optionButton.active = this.options.waypointsAllowed;
                    case CAVE_MODE -> optionButton.active = this.options.cavesAllowed;
                }
            }
            this.layout.arrangeElements();
        }

        private void optionClicked(Button button) {
            EnumOptionsMinimap option = ((GuiOptionButtonMinimap) button).returnEnumOptions();
            this.options.setOptionValue(option);
            button.setMessage(Component.literal(this.options.getKeyText(option)));
            if (option == EnumOptionsMinimap.OLD_NORTH) {
                VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(this.options.oldNorth);
            }

        }
    }

    private class PerformanceTab extends BasicTabEntry {
        private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.DYNAMIC_LIGHTING, EnumOptionsMinimap.TERRAIN_DEPTH, EnumOptionsMinimap.WATER_TRANSPARENCY, EnumOptionsMinimap.BLOCK_TRANSPARENCY, EnumOptionsMinimap.BIOMES, EnumOptionsMinimap.FILTERING, EnumOptionsMinimap.CHUNK_GRID, EnumOptionsMinimap.BIOME_OVERLAY, EnumOptionsMinimap.SLIME_CHUNKS, EnumOptionsMinimap.WORLD_BORDER };
        private final MapSettingsManager options;

        public PerformanceTab(Component component, GuiMinimapOptions parent, int tabIndex) {
            super(component, parent, tabIndex);
            this.options = parent.options;
            this.parent = parent;
            this.layout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
            GridLayout.RowHelper row = this.layout.createRowHelper(2);
            for (EnumOptionsMinimap option : relevantOptions) {
                StringBuilder text = new StringBuilder().append(this.options.getKeyText(option));
                if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !this.options.multicore && this.options.getOptionBooleanValue(option)) {
                    text.append("§c").append(text);
                }

                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(0, 0, option, Component.literal(text.toString()), this::optionClicked);
                row.addChild(optionButton);
                if (optionButton.returnEnumOptions() == EnumOptionsMinimap.SLIME_CHUNKS) {
                    this.parent.slimeChunksButton = optionButton;
                    this.parent.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
                }
            }
            String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
            if (worldSeedDisplay.isEmpty()) {
                worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
            }

            String buttonSeedText = I18n.get("options.minimap.worldSeed") + ": " + worldSeedDisplay;
            this.parent.worldSeedButton = new GuiButtonText(VoxelConstants.getMinecraft().font, 0, 0, 150, 20, Component.literal(buttonSeedText), button -> this.parent.worldSeedButton.setEditing(true));
            this.parent.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
            this.parent.worldSeedButton.active = !VoxelConstants.getMinecraft().hasSingleplayerServer();
            row.addChild(this.parent.worldSeedButton);

            String buttonTeleportText = I18n.get("options.minimap.teleportCommand") + ": " + VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand;
            this.parent.teleportCommandButton = new GuiButtonText(VoxelConstants.getMinecraft().font, 0, 0, 150, 20, Component.literal(buttonTeleportText), button -> this.parent.teleportCommandButton.setEditing(true));
            this.parent.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
            this.parent.teleportCommandButton.active = VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand == null;
            row.addChild(this.parent.teleportCommandButton);

            this.layout.arrangeElements();
        }

        private void optionClicked(Button button) {
            EnumOptionsMinimap option = ((GuiOptionButtonMinimap) button).returnEnumOptions();
            this.options.setOptionValue(option);
            String perfBomb = "";
            if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !this.options.multicore && this.options.getOptionBooleanValue(option)) {
                perfBomb = "§c";
            }

            button.setMessage(Component.literal(perfBomb + this.options.getKeyText(option)));
        }
    }

    private class RadarTab extends BasicTabEntry {
        private static final EnumOptionsMinimap[] relevantOptions_RadarFull = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_MOB_NAMES, EnumOptionsMinimap.SHOW_PLAYER_NAMES, EnumOptionsMinimap.SHOW_MOB_HELMETS, EnumOptionsMinimap.SHOW_PLAYER_HELMETS, EnumOptionsMinimap.RADAR_FILTERING, EnumOptionsMinimap.RADAR_OUTLINES };
        private static final EnumOptionsMinimap[] relevantOptions_RadarSimple = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_FACING };
        private final RadarSettingsManager options;

        public RadarTab(Component component, GuiMinimapOptions parent, int tabIndex) {
            super(component, parent, tabIndex);
            this.options = parent.radarOptions;
            this.parent = parent;
            this.layout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
            GridLayout.RowHelper row = this.layout.createRowHelper(2);
            EnumOptionsMinimap[] relevantOptions = this.options.radarMode == 2 ? relevantOptions_RadarFull : relevantOptions_RadarSimple;
            for (EnumOptionsMinimap option : relevantOptions) {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(0, 0, option, Component.literal(this.options.getKeyText(option)), this::optionClicked);

                row.addChild(optionButton);
            }
            if (this.options.radarMode == 2) {
                row.addChild(new Button.Builder(Component.translatable("options.minimap.radar.selectMobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this.parent, this.options))).width(150).build());
            }
            this.iterateButtonOptions();
            this.layout.arrangeElements();
        }

        private void optionClicked(Button button) {
            EnumOptionsMinimap option = ((GuiOptionButtonMinimap) button).returnEnumOptions();
            this.options.setOptionValue(option);

            if (option == EnumOptionsMinimap.RADAR_MODE) {
                this.parent.init();
                return;
            }

            button.setMessage(Component.literal(this.options.getKeyText(option)));

            this.iterateButtonOptions();
        }

        private void iterateButtonOptions() {
            this.layout.visitWidgets(widget -> {
                widget.active = this.options.showRadar;
                if (widget instanceof GuiOptionButtonMinimap button) {
                    EnumOptionsMinimap option = button.returnEnumOptions();
                    switch (option) {
                        case SHOW_RADAR -> button.active = this.options.radarAllowed;
                        case SHOW_PLAYERS -> button.active = button.active && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                        case SHOW_MOBS -> button.active = button.active && (this.options.radarAllowed || this.options.radarMobsAllowed);
                        case SHOW_PLAYER_HELMETS, SHOW_PLAYER_NAMES -> {
                            button.active = button.active && this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed);
                            // TODO: remove this code after radar helmet icon implementation
                            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYER_HELMETS) {
                                button.active = false;
                                button.setTooltip(Tooltip.create(Component.translatable("minimap.ui.workInProgress")));
                            }
                        }
                        case SHOW_MOB_HELMETS, SHOW_MOB_NAMES -> {
                            button.active = button.active && (this.options.showNeutrals || this.options.showHostiles) && (this.options.radarAllowed || this.options.radarMobsAllowed);
                            // TODO: remove this code after radar helmet icon implementation
                            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_HELMETS) {
                                button.active = false;
                                button.setTooltip(Tooltip.create(Component.translatable("minimap.ui.workInProgress")));
                            }
                        }
                    }
                }
            });
        }
    }

    private class RedirectionTab extends BasicTabEntry {
        private final Screen targetScreen;
        private boolean switched = false;

        public RedirectionTab(Component component, GuiMinimapOptions parent, Screen targetScreen) {
            super(component, parent, -1);
            this.targetScreen = targetScreen;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
            super.render(guiGraphics, mouseX, mouseY, delta);
            if (!this.switched) {
                this.switched = true;
                minecraft.setScreen(this.targetScreen);
            }
        }
    }
}
