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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.Consumer;

public class GuiMinimapOptions extends GuiScreenMinimap {
    protected String screenTitle = "Minimap Options";
    private final MapSettingsManager options;
    private final RadarSettingsManager radarOptions;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;

    private final ArrayList<AbstractWidget> optionButtons = new ArrayList<>();
    private int pageIndex = 0;
    private String pageState = "";
    private int tabIndex = 0;
    private int lastTabIndex = 0;
    private Button nextPageButton;
    private Button prevPageButton;

    private static final EnumOptionsMinimap[] generalOptions = { EnumOptionsMinimap.HIDE_MINIMAP, EnumOptionsMinimap.UPDATE_NOTIFIER, EnumOptionsMinimap.SHOW_BIOME, EnumOptionsMinimap.SHOW_COORDS, EnumOptionsMinimap.LOCATION, EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUARE_MAP, EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.IN_GAME_WAYPOINTS, EnumOptionsMinimap.CAVE_MODE, EnumOptionsMinimap.MOVE_MAP_DOWN_WHILE_STATUS_EFFECT, EnumOptionsMinimap.MOVE_SCOREBOARD_DOWN };
    private static final EnumOptionsMinimap[] performanceOptions = { EnumOptionsMinimap.DYNAMIC_LIGHTING, EnumOptionsMinimap.TERRAIN_DEPTH, EnumOptionsMinimap.WATER_TRANSPARENCY, EnumOptionsMinimap.BLOCK_TRANSPARENCY, EnumOptionsMinimap.BIOMES, EnumOptionsMinimap.FILTERING, EnumOptionsMinimap.CHUNK_GRID, EnumOptionsMinimap.BIOME_OVERLAY, EnumOptionsMinimap.SLIME_CHUNKS, EnumOptionsMinimap.WORLD_BORDER, EnumOptionsMinimap.TELEPORT_COMMAND };
    private static final EnumOptionsMinimap[] radarFullOptions = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_MOB_NAMES, EnumOptionsMinimap.SHOW_PLAYER_NAMES, EnumOptionsMinimap.SHOW_MOB_HELMETS, EnumOptionsMinimap.SHOW_PLAYER_HELMETS, EnumOptionsMinimap.RADAR_FILTERING, EnumOptionsMinimap.RADAR_OUTLINES };
    private static final EnumOptionsMinimap[] radarSimpleOptions = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_FACING };

    // Performance Tab
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;

    // Radar Tab
    private Button mobListButton;

    public GuiMinimapOptions(Screen parent) {
        this.lastScreen = parent;

        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.radarOptions = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void init() {
        this.screenTitle = I18n.get("options.minimap.title");

        this.tabNavigationBar = TabNavigationBar.builder(this.tabManager, this.width).addTabs(new Tab[] {
                new OptionsTab(Component.translatable("stat.generalButton"), 0),
                new OptionsTab(Component.translatable("options.minimap.tab.detailsPerformance"), 1),
                new OptionsTab(Component.translatable("options.minimap.tab.radar"), 2),
                new OptionsTab(Component.translatable("controls.title"), 3),
                new OptionsTab(Component.translatable("options.minimap.tab.worldmap"), 4)}).build();

        this.tabNavigationBar.setFocused(true);
        this.tabNavigationBar.selectTab(this.tabIndex, false);
        this.tabNavigationBar.arrangeElements();
        this.addRenderableWidget(this.tabNavigationBar);

        int tabBottom = this.tabNavigationBar.getRectangle().bottom();
        ScreenRectangle screenRect = new ScreenRectangle(0, tabBottom, this.width, this.height - this.layout.getFooterHeight() - tabBottom);
        this.tabManager.setTabArea(screenRect);
        this.layout.setHeaderHeight(tabBottom);
        this.layout.addToFooter(new Button.Builder(Component.translatable("gui.done"), button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.layout.arrangeElements();

        this.nextPageButton = new Button.Builder(Component.literal(">"), button -> {
            this.pageIndex++;
            this.replaceButtons();
        }).bounds(this.width / 2 + 140, this.height / 6 + 120, 40, 20).build();
        this.addRenderableWidget(this.nextPageButton);

        this.prevPageButton = new Button.Builder(Component.literal("<"), button -> {
            this.pageIndex--;
            this.replaceButtons();
        }).bounds(this.width / 2 - 180, this.height / 6 + 120, 40, 20).build();
        this.addRenderableWidget(this.prevPageButton);

        this.replaceButtons();
    }

    public void replaceButtons() {
        for (GuiEventListener widget : this.optionButtons) {
            this.removeWidget(widget);
        }
        this.optionButtons.clear();

        EnumOptionsMinimap[] relevantOptions = null;
        switch (this.tabIndex) {
            case 0 -> relevantOptions = generalOptions;
            case 1 -> relevantOptions = performanceOptions;
            case 2 -> {
                if (this.radarOptions.radarMode == 2) {
                    relevantOptions = radarFullOptions;
                } else {
                    relevantOptions = radarSimpleOptions;
                }
            }
            case 3 -> minecraft.setScreen(new GuiMinimapControls(this));
            case 4 -> minecraft.setScreen(new GuiPersistentMapOptions(this));
        }

        if (relevantOptions == null) {
            this.tabIndex = this.lastTabIndex;
            return;
        }
        this.lastTabIndex = this.tabIndex;

        int itemCount = 10;
        int pageCount = (relevantOptions.length - 1) / itemCount;
        if (this.pageIndex > pageCount) {
            this.pageIndex = 0;
        }
        if (this.pageIndex < 0) {
            this.pageIndex = pageCount;
        }
        this.pageState = "[ " + (this.pageIndex + 1) + " / " + (pageCount + 1) + " ]";
        int pageStart = itemCount * this.pageIndex;
        int pageEnd = Math.min(itemCount * (this.pageIndex + 1), relevantOptions.length);

        this.nextPageButton.active = pageCount > 0;
        this.prevPageButton.active = pageCount > 0;

        // Menu Buttons
        for (int i = pageStart; i < pageEnd; i++) {
            EnumOptionsMinimap option = relevantOptions[i];
            int buttonX = this.getWidth() / 2 - 155 + (i - pageStart) % 2 * 160;
            int buttonY = this.getHeight() / 6 + 24 * ((i - pageStart) >> 1);

            // List / Toggle
            if (option.isBoolean() || option.isList()) {
                StringBuilder text = new StringBuilder().append(this.getKeyText(option));
                if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOMES) && !this.options.multicore && this.getOptionBooleanValue(option)) {
                    text.append("§c").append(text);
                }

                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(buttonX, buttonY, option, Component.literal(text.toString()), this::optionClicked);
                this.addOptionButton(optionButton);

                if (option == EnumOptionsMinimap.SLIME_CHUNKS) {
                    this.slimeChunksButton = optionButton;
                }
            }

            // Text Field
            if (option == EnumOptionsMinimap.TELEPORT_COMMAND) {
                String buttonTeleportText = I18n.get("options.minimap.teleportCommand") + ": " + VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand;
                this.teleportCommandButton = new GuiButtonText(this.getFont(), buttonX, buttonY, 150, 20, Component.literal(buttonTeleportText), button -> this.teleportCommandButton.setEditing(true));
                this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
                this.teleportCommandButton.active = VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand == null;
                this.addOptionButton(teleportCommandButton);
            }
        }

        int additionalButtonX = this.width / 2 - 75;
        int additionalButtonY = this.height / 6 + 144;

        // Additional Buttons
        if (relevantOptions == performanceOptions) {
            String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
            if (worldSeedDisplay.isEmpty()) {
                worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
            }

            String buttonSeedText = I18n.get("options.minimap.worldSeed") + ": " + worldSeedDisplay;
            this.worldSeedButton = new GuiButtonText(this.getFont(), additionalButtonX, additionalButtonY, 150, 20, Component.literal(buttonSeedText), button -> this.worldSeedButton.setEditing(true));
            this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
            this.worldSeedButton.active = !VoxelConstants.getMinecraft().hasSingleplayerServer();
            this.addOptionButton(this.worldSeedButton);
        }

        if (relevantOptions == radarFullOptions) {
            this.mobListButton = new Button.Builder(Component.translatable("options.minimap.radar.selectMobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, this.radarOptions))).bounds(additionalButtonX, additionalButtonY, 150, 20).build();
            this.addOptionButton(this.mobListButton);
        }

        this.setButtonsActive();

    }

    private void addOptionButton(AbstractWidget widget) {
        this.optionButtons.add(widget);
        this.addRenderableWidget(widget);
    }

    private void optionClicked(Button button) {
        if (!(button instanceof GuiOptionButtonMinimap button2)) {
            return;
        }
        EnumOptionsMinimap option = button2.returnEnumOptions();
        this.setOptionValue(option);

        String prefix = "";
        switch (option) {
            case OLD_NORTH -> VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(this.options.oldNorth);
            case WATER_TRANSPARENCY, BLOCK_TRANSPARENCY, BIOMES -> {
                if (!this.options.multicore && option.isBoolean() && this.getOptionBooleanValue(option)) {
                    prefix = "§c";
                }
            }
            case RADAR_MODE -> this.replaceButtons();
        }

        button2.setMessage(Component.literal(prefix + this.getKeyText(option)));
        this.setButtonsActive();
    }

    private void setButtonsActive() {
        for (GuiEventListener button : this.children()) {
            if (!(button instanceof GuiOptionButtonMinimap button2)){
                continue;
            }
            EnumOptionsMinimap option = button2.returnEnumOptions();

            boolean radarBlocked = !this.radarOptions.radarAllowed && !this.radarOptions.radarPlayersAllowed && !this.radarOptions.radarMobsAllowed;
            if (ArrayUtils.contains(radarFullOptions, option) || ArrayUtils.contains(radarSimpleOptions, option)) {
                button2.active = this.radarOptions.showRadar && !radarBlocked;
                if (this.mobListButton != null) {
                    this.mobListButton.active = this.radarOptions.showRadar && !radarBlocked;
                }
            }

            switch (option) {
                case HIDE_MINIMAP -> button2.active = this.options.minimapAllowed;
                case IN_GAME_WAYPOINTS -> button2.active = this.options.waypointsAllowed;
                case CAVE_MODE -> button2.active = this.options.cavesAllowed;
                case SLIME_CHUNKS -> button2.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
                case SHOW_RADAR -> button2.active = !radarBlocked;
                case SHOW_PLAYERS -> button2.active = button2.active && this.radarOptions.radarPlayersAllowed;
                case SHOW_MOBS -> button2.active = button2.active && this.radarOptions.radarMobsAllowed;
                case SHOW_PLAYER_HELMETS, SHOW_PLAYER_NAMES -> button2.active = button2.active && this.radarOptions.showPlayers && this.radarOptions.radarPlayersAllowed;
                case SHOW_MOB_HELMETS, SHOW_MOB_NAMES -> button2.active = button2.active && (this.radarOptions.showNeutrals || this.radarOptions.showHostiles) && this.radarOptions.radarMobsAllowed;
            }
        }
    }


    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, this.height - this.layout.getFooterHeight() - 2, 0.0F, 0.0F, this.width, 2, 32, 2);

        drawContext.drawCenteredString(this.font, this.pageState, this.width / 2, this.height / 6 + 126, 0xFFFFFFFF);
    }

    @Override
    public void renderMenuBackground(GuiGraphics drawContext) {
        drawContext.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, this.width, this.layout.getHeaderHeight(), 16, 16);
        this.renderMenuBackground(drawContext, 0, this.layout.getHeaderHeight(), this.width, this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        boolean bl = super.mouseClicked(mouseButtonEvent, doubleClick);
        if (this.tabManager.getCurrentTab() instanceof OptionsTab tab) {
            if (tab.index() != this.tabIndex) {
                this.tabIndex = tab.index();
                this.replaceButtons();
            }
        }
        return bl;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (this.worldSeedButton != null) {
                this.worldSeedButton.keyPressed(keyEvent);
            }
            if (this.teleportCommandButton != null) {
                this.teleportCommandButton.keyPressed(keyEvent);
            }
        }

        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (this.worldSeedButton != null && this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton != null && this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return super.keyPressed(keyEvent);
    }

    @Override
    public boolean charTyped(CharacterEvent characterEvent) {
        boolean OK = super.charTyped(characterEvent);
        if (characterEvent.codepoint() == '\r') {
            if (this.worldSeedButton != null && this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton != null && this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return OK;
    }

    private record OptionsTab(Component title, int index) implements Tab {
        @Override
        public Component getTabTitle() {
            return this.title;
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
        if (this.worldSeedButton != null) {
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
        }
        if (this.slimeChunksButton != null) {
            this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
        }

    }

    private void newTeleportCommand() {
        if (teleportCommandButton != null) {
            String newTeleportCommand = this.teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : this.teleportCommandButton.getText();
            VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand = newTeleportCommand;

            String buttonText = I18n.get("options.minimap.teleportCommand") + ": " + newTeleportCommand;
            this.teleportCommandButton.setMessage(Component.literal(buttonText));
            this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
        }

    }

    private void setOptionValue(EnumOptionsMinimap option) {
        try {
            this.options.setOptionValue(option);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.radarOptions.setOptionValue(option);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void setOptionFloatValue(EnumOptionsMinimap option, float value) {
        try {
            this.options.setOptionFloatValue(option, value);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            this.radarOptions.setOptionFloatValue(option, value);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String getKeyText(EnumOptionsMinimap option) {
        try {
            return this.options.getKeyText(option);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return this.radarOptions.getKeyText(option);
        } catch (IllegalArgumentException ignored) {
        }

        return "error";
    }

    private boolean getOptionBooleanValue(EnumOptionsMinimap option) {
        try {
            return this.options.getOptionBooleanValue(option);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return this.radarOptions.getOptionBooleanValue(option);
        } catch (IllegalArgumentException ignored) {
        }

        return false;
    }

    private String getOptionListValue(EnumOptionsMinimap option) {
        try {
            return this.options.getOptionListValue(option);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return this.options.getOptionListValue(option);
        } catch (IllegalArgumentException ignored) {
        }

        return "error";
    }

    private float getOptionFloatValue(EnumOptionsMinimap option) {
        try {
            return this.options.getOptionFloatValue(option);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            return this.radarOptions.getOptionFloatValue(option);
        } catch (IllegalArgumentException ignored) {
        }

        return 0.0F;
    }
}
