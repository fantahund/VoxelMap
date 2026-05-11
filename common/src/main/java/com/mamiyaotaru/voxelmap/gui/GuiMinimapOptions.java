package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;
import com.mamiyaotaru.voxelmap.options.fields.StringField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class GuiMinimapOptions extends GuiOptionsScreenMinimap {
    private static final int ITEMS_PER_PAGE = 10;

    private static final int TAB_GENERIC = 0;
    private static final int TAB_PERFORMANCE = 1;
    private static final int TAB_RADAR = 2;
    private static final int TAB_WORLDMAP = 3;
    private static final int TAB_CONTROLS = 4;

    private final VoxelMap voxelMap = VoxelConstants.getVoxelMapInstance();

    // Tab & Page Management
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);
    private TabNavigationBar tabNavigationBar;
    private int currentTab;
    private int lastTab;
    private int pageIndex;
    private String pageInfo = "";
    private Button nextPageButton;
    private Button prevPageButton;

    public GuiMinimapOptions(Screen parentGui) {
        super(parentGui, Component.empty());
    }

    @Override
    public void init() {
        Tab[] allTabs = new Tab[] {
                new OptionsTab(TAB_GENERIC, Component.translatable("stat.generalButton")),
                new OptionsTab(TAB_PERFORMANCE, Component.translatable("options.minimap.tab.detailsPerformance")),
                new OptionsTab(TAB_RADAR, Component.translatable("options.minimap.tab.radar")),
                new OptionsTab(TAB_WORLDMAP, Component.translatable("options.minimap.tab.worldmap")),
                new OptionsTab(TAB_CONTROLS, Component.translatable("controls.title"))
        };
        tabNavigationBar = TabNavigationBar.builder(tabManager, getWidth()).addTabs(allTabs).build();
        tabNavigationBar.selectTab(currentTab, false);
        tabNavigationBar.arrangeElements();
        setFocused(tabNavigationBar);
        addRenderableWidget(tabNavigationBar);

        int tabBottom = tabNavigationBar.getRectangle().bottom();
        ScreenRectangle screenRect = new ScreenRectangle(0, tabBottom, getWidth(), getHeight() - layout.getFooterHeight() - tabBottom);
        tabManager.setTabArea(screenRect);
        layout.setHeaderHeight(tabBottom);
        layout.addToFooter(new Button.Builder(Component.translatable("gui.done"), button -> onClose()).width(200).build());
        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();

        addRenderableWidget(nextPageButton = new Button.Builder(Component.literal(">"), button -> switchPage(1)).bounds(getWidth() / 2 + 140, getHeight() / 6 + 120, 40, 20).build());
        addRenderableWidget(prevPageButton = new Button.Builder(Component.literal("<"), button -> switchPage(-1)).bounds(getWidth() / 2 - 180, getHeight() / 6 + 120, 40, 20).build());

        rebuildOptionWidgets();
    }

    private OptionField<?>[] getRelevantOptions(int tabId) {
        switch (tabId) {
            case TAB_GENERIC -> {
                return new OptionField[]{mapOptions.hide, mapOptions.updateNotifier, mapOptions.showBiomeInfo, mapOptions.showCoordInfo, mapOptions.mapCorner, mapOptions.sizeModifier, mapOptions.squareMap, mapOptions.rotates, mapOptions.inGameWaypoints, mapOptions.showCaves, mapOptions.moveMapBelowStatusEffectIcons, mapOptions.moveScoreboardBelowMap};
            }
            case TAB_PERFORMANCE -> {
                return new OptionField[]{mapOptions.dynamicLighting, mapOptions.terrainDepth, mapOptions.waterTransparency, mapOptions.blockTransparency, mapOptions.biomeShading, mapOptions.biomeOverlay, mapOptions.chunkGrid, mapOptions.slimeChunks, mapOptions.worldBorder, mapOptions.filtering, mapOptions.teleportCommand};
            }
            case TAB_RADAR -> {
                switch (radarOptions.radarMode.get()) {
                    case SIMPLE -> {
                        return new OptionField[]{radarOptions.showRadar, radarOptions.radarMode, radarOptions.showMobs, radarOptions.showPlayers, radarOptions.showFacing, radarOptions.showElevation, radarOptions.hideSneaking, radarOptions.hideInvisible};
                    }
                    case FULL -> {
                        return new OptionField[]{radarOptions.showRadar, radarOptions.radarMode, radarOptions.showMobs, radarOptions.showPlayers, radarOptions.showMobNames, radarOptions.showPlayerNames, radarOptions.showMobHelmets, radarOptions.showPlayerHelmets, radarOptions.filtering, radarOptions.outlines, radarOptions.showFullNames, radarOptions.showElevation, radarOptions.hideSneaking, radarOptions.hideInvisible, radarOptions.compatRendering};
                    }
                }
            }
            case TAB_WORLDMAP -> {
                return new OptionField[]{persistentMapOptions.showCoordinates, persistentMapOptions.showCaves, persistentMapOptions.showWaypoints, persistentMapOptions.showWaypointNames, persistentMapOptions.showDistantWaypoints, persistentMapOptions.minZoomExponent, persistentMapOptions.maxZoomExponent, persistentMapOptions.cacheSize};
            }
        }
        return null;
    }

    private void switchPage(int i) {
        pageIndex += i;
        rebuildOptionWidgets();
    }

    @Override
    public void setupOptionWidgets() {
        OptionField<?>[] relevantOptions = getRelevantOptions(currentTab);

        if (currentTab == TAB_CONTROLS) {
            minecraft.setScreen(new GuiMinimapControls(this));
        }

        if (relevantOptions == null) {
            currentTab = lastTab;
            return;
        }

        if (currentTab != lastTab) {
            lastTab = currentTab;
            pageIndex = 0;
        }

        int pageCount = (relevantOptions.length - 1) / ITEMS_PER_PAGE;
        if (pageIndex > pageCount) {
            pageIndex = 0;
        }
        if (pageIndex < 0) {
            pageIndex = pageCount;
        }
        pageInfo = "[ " + (pageIndex + 1) + " / " + (pageCount + 1) + " ]";
        int pageStart = ITEMS_PER_PAGE * pageIndex;
        int pageEnd = Math.min(ITEMS_PER_PAGE * (pageIndex + 1), relevantOptions.length);

        nextPageButton.active = pageCount > 0;
        prevPageButton.active = pageCount > 0;

        // Menu Buttons
        for (int i = pageStart; i < pageEnd; i++) {
            OptionField<?> option = relevantOptions[i];

            int widgetX = getWidth() / 2 - 155 + (i - pageStart) % 2 * 160;
            int widgetY = getHeight() / 6 + 24 * ((i - pageStart) >> 1);
            AbstractWidget widget = createOptionWidget(option, widgetX, widgetY, 150, 20);

            addOptionWidget(widget);
        }

        // Additional Buttons
        int widgetX = getWidth() / 2 - 75;
        int widgetY = getHeight() / 6 + 144;

        switch (currentTab) {
            case TAB_PERFORMANCE -> {
                String seed = voxelMap.getWorldSeed();

                StringField worldSeedField;
                (worldSeedField = new StringField("World Seed", "options.minimap.worldSeed", seed, StringField.PATTERN_NONE)).withListener(this::updateWorldSeed).withFormat(this::formatWorldSeed);
                worldSeedField.setActive(!minecraft.hasSingleplayerServer());
                AbstractWidget widget = createOptionWidget(worldSeedField, widgetX, widgetY, 150, 20);

                addOptionWidget(widget);
            }
            case TAB_RADAR -> {
                AbstractWidget mobListButton = new Button.Builder(Component.translatable("options.minimap.radar.selectMobs"), x -> minecraft.setScreen(new GuiMobs(this))).bounds(widgetX, widgetY, 150, 20).build();
                mobListButton.active = VoxelConstants.getVoxelMapInstance().getPermissionsManager().anyAllowed(MapPermissionsManager.RADAR_ALLOWED, MapPermissionsManager.RADAR_MOBS_ALLOWED, MapPermissionsManager.RADAR_PLAYERS_ALLOWED);

                addOptionWidget(mobListButton);
            }
        }
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (tabManager.getCurrentTab() instanceof OptionsTab tab) {
            int tabId = tab.id();
            if (currentTab != tabId) {
                currentTab = tabId;
                rebuildOptionWidgets();
            }
        }

        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.blit(RenderPipelines.GUI_TEXTURED, Screen.FOOTER_SEPARATOR, 0, height - layout.getFooterHeight() - 2, 0.0F, 0.0F, width, 2, 32, 2);
        drawContext.drawCenteredString(font, pageInfo, width / 2, height / 6 + 126, 0xFFFFFFFF);
    }

    @Override
    public void renderMenuBackground(GuiGraphics drawContext) {
        drawContext.blit(RenderPipelines.GUI_TEXTURED, CreateWorldScreen.TAB_HEADER_BACKGROUND, 0, 0, 0.0F, 0.0F, width, layout.getHeaderHeight(), 16, 16);
        renderMenuBackground(drawContext, 0, layout.getHeaderHeight(), width, height);
    }

    private String formatWorldSeed(String value) {
        return value.isEmpty() ? I18n.get("selectWorld.versionUnknown") : value;
    }

    private void updateWorldSeed(String value) {
        voxelMap.setWorldSeed(value);
        voxelMap.getMap().forceFullRender(true);
    }

    private record OptionsTab(int id, Component title) implements Tab {
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
}
