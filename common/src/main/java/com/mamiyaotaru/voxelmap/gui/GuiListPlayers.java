package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiListMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class GuiListPlayers extends GuiListMinimap<GuiListPlayers.Row> {
    private final ArrayList<PlayerInfo> players;
    private ArrayList<PlayerInfo> playersFiltered;
    private final GuiSelectPlayer parentGui;
    private final Row everyoneRow;

    static final Component EVERYONE = Component.translatable("minimap.waypointShare.all");
    static final Component CONFIRM_TITLE = Component.translatable("minimap.waypointShare.shareWithEveryone");
    static final Component CONFIRM_EXPLANATION = Component.translatable("minimap.waypointShare.shareWithEveryone2");
    static final Component CONFIRM_AFFIRM = Component.translatable("gui.yes");
    static final Component CONFIRM_DENY = Component.translatable("gui.cancel");

    public GuiListPlayers(GuiSelectPlayer parentGui, int x, int y, int width, int height) {
        super(x, y, width, height, 25);
        this.parentGui = parentGui;

        ClientPacketListener connection = VoxelConstants.getPlayer().connection;
        players = new ArrayList<>(connection.getOnlinePlayers());
        sort();

        Button everyoneButton = new Button.Builder(EVERYONE, button -> buttonClicked(-1)).bounds(parentGui.getWidth() / 2 - 75, 0, 150, 20).build();
        everyoneButton.setTooltip(Tooltip.create(Component.translatable("minimap.waypointShare.shareWithName", EVERYONE)));
        everyoneRow = new Row(everyoneButton, -1, null, -1);

        updateFilter("");
    }

    private Component getPlayerName(PlayerInfo ScoreboardEntryIn) {
        return Component.literal(ScoreboardEntryIn.getProfile().name());
    }

    private Button createButtonFor(int x, int y, PlayerInfo ScoreboardEntry, int id) {
        if (ScoreboardEntry == null) {
            return null;
        } else {
            Component name = getPlayerName(ScoreboardEntry);
            Button btn = new Button.Builder(name, button -> buttonClicked(id)).bounds(x, y, 150, 20).build();
            btn.setTooltip(Tooltip.create(Component.translatable("minimap.waypointShare.shareWithName", name)));
            return btn;
        }
    }

    @Override
    public int getRowWidth() {
        return 400;
    }

    @Override
    protected int scrollBarX() {
        return super.scrollBarX() + 32;
    }

    protected void sort() {
        players.sort((player1, player2) -> {
            String name1 = getPlayerName(player1).getString();
            String name2 = getPlayerName(player2).getString();
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        });
    }

    protected void updateFilter(String filterString) {
        playersFiltered = new ArrayList<>(players);
        Iterator<?> iterator = playersFiltered.iterator();

        while (iterator.hasNext()) {
            PlayerInfo ScoreboardEntry = (PlayerInfo) iterator.next();
            String name = getPlayerName(ScoreboardEntry).getString();
            if (!name.toLowerCase().contains(filterString)) {
                iterator.remove();
            }
        }

        setScrollAmount(0.0);
        clearEntries();
        addEntry(everyoneRow);

        for (int i = 0; i < playersFiltered.size(); i += 2) {
            PlayerInfo ScoreboardEntry1 = playersFiltered.get(i);
            PlayerInfo ScoreboardEntry2 = i < playersFiltered.size() - 1 ? playersFiltered.get(i + 1) : null;

            Button button1 = createButtonFor(parentGui.getWidth() / 2 - 155, 0, ScoreboardEntry1, i);
            Button button2 = createButtonFor(parentGui.getWidth() / 2 - 155 + 160, 0, ScoreboardEntry2, i + 1);

            addEntry(new Row(button1, i, button2, i + 1));
        }
    }

    public void buttonClicked(int id) {
        if (id == -1) {
            parentGui.allClicked = true;
            ConfirmScreen confirmScreen = new ConfirmScreen(parentGui, CONFIRM_TITLE, CONFIRM_EXPLANATION, CONFIRM_AFFIRM, CONFIRM_DENY);
            minecraft.setScreen(confirmScreen);
        } else {
            PlayerInfo ScoreboardEntry = playersFiltered.get(id);
            String name = getPlayerName(ScoreboardEntry).getString();
            parentGui.sendMessageToPlayer(name);
        }

    }

    @Override
    protected void renderSelection(GuiGraphics guiGraphics, Row entry, int color) {
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
    }

    public class Row extends GuiListMinimap.Entry<Row> {
        private final Button button1;
        private final Button button2;
        private final int id1;
        private final int id2;

        public Row(Button primaryButton, int primaryId, Button secondaryButton, int secondaryId) {
            super(GuiListPlayers.this);

            button1 = primaryButton;
            button2 = secondaryButton;
            id1 = primaryId;
            id2 = secondaryId;

            if (button1 != null) {
                addWidget(button1);
            }
            if (button2 != null) {
                addWidget(button2);
            }
        }

        private void drawIconForButton(GuiGraphics drawContext, Button button, int id) {
            GameProfile gameProfile = playersFiltered.get(id).getProfile();
            Player player = VoxelConstants.getPlayer().level().getPlayerByUUID(gameProfile.id());
            Optional<PlayerSkin> optionalSkin = VoxelConstants.getMinecraft().getSkinManager().get(gameProfile).getNow(Optional.empty());
            if (optionalSkin.isPresent()) {
                Identifier skinIdentifier = optionalSkin.get().body().texturePath();
                drawContext.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 8.0F, 8.0F, 8, 8, 8, 8, 64, 64);
                if (player != null && player.isModelPartShown(PlayerModelPart.HAT)) {
                    drawContext.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 40.0F, 8.0F, 8, 8, 8, 8, 64, 64);
                }
            }
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            if (button1 != null) {
                button1.setY(getY());
                button1.render(drawContext, mouseX, mouseY, tickDelta);
                if (id1 != -1) {
                    drawIconForButton(drawContext, button1, id1);
                }
            }
            if (button2 != null) {
                button2.setY(getY());
                button2.render(drawContext, mouseX, mouseY, tickDelta);
                if (id2 != -1) {
                    drawIconForButton(drawContext, button2, id2);
                }
            }
        }
    }
}
