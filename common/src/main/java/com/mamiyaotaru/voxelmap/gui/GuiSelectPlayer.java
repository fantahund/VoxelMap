package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.widgets.GuiListMinimap;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.Optional;

public class GuiSelectPlayer extends GuiScreenMinimap implements BooleanConsumer {
    private static final Component SHARE_WAYPOINT = Component.translatable("minimap.waypointShare.title");
    private static final Component SHARE_COORDINATES = Component.translatable("minimap.waypointShare.titleCoordinate");
    private static final Component EVERYONE = Component.translatable("minimap.waypointShare.all");
    private static final Component CONFIRM_TITLE = Component.translatable("minimap.waypointShare.shareWithEveryone");
    private static final Component CONFIRM_EXPLANATION = Component.translatable("minimap.waypointShare.shareWithEveryone2");
    private static final Component CONFIRM_AFFIRM = Component.translatable("gui.yes");
    private static final Component CONFIRM_DENY = Component.translatable("gui.cancel");

    private final String locInfo;
    private final boolean sharingWaypoint;

    private PlayerList playerList;
    private EditBox filter;
    private EditBox message;
    private Button sendButton;
    private String selectedPlyer = "";

    public GuiSelectPlayer(Screen parentGui, String locInfo, boolean sharingWaypoint) {
        super(parentGui, sharingWaypoint ? SHARE_WAYPOINT : SHARE_COORDINATES);
        this.locInfo = locInfo;
        this.sharingWaypoint = sharingWaypoint;
    }

    @Override
    public void tick() {
    }

    @Override
    public void init() {
        playerList = new PlayerList(0, 40, getWidth(), getHeight() - 130);
        addRenderableWidget(playerList);

        filter = new EditBox(getFont(), getWidth() / 2 - 150, getHeight() - 78, 300, 20, Component.empty());
        filter.setHint(Component.translatable("gui.selectWorld.search").withStyle(ChatFormatting.GRAY));
        filter.setMaxLength(35);
        filter.setResponder(this::filterUpdated);
        setFocused(filter);
        addRenderableWidget(filter);

        message = new EditBox(getFont(), getWidth() / 2 - 150, getHeight() - 54, 300, 20, Component.empty());
        message.setHint(Component.translatable("minimap.waypointShare.shareMessage").withStyle(ChatFormatting.GRAY));
        message.setMaxLength(78);
        addRenderableWidget(message);

        addRenderableWidget(sendButton = new Button.Builder(Component.translatable("gui.done"), button -> sendClicked()).bounds(getWidth() / 2 - 155, getHeight() - 28, 150, 20).build());
        addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> onClose()).bounds(getWidth() / 2 + 5, getHeight() - 28, 150, 20).build());
    }

    private void filterUpdated(String string) {
        playerList.updateFilter(string.toLowerCase());
    }

    private void sendClicked() {
        if (selectedPlyer.isEmpty()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(GuiSelectPlayer.this, CONFIRM_TITLE, CONFIRM_EXPLANATION, CONFIRM_AFFIRM, CONFIRM_DENY);
            minecraft.setScreen(confirmScreen);
        } else {
            sendMessageToPlayer(selectedPlyer);
        }
    }

    @Override
    public void accept(boolean flag) {
        if (flag) {
            String combined = message.getValue() + " " + locInfo;
            if (combined.length() > 256) {
                VoxelConstants.getPlayer().connection.sendChat(message.getValue());
                VoxelConstants.getPlayer().connection.sendChat(locInfo);
            } else {
                VoxelConstants.getPlayer().connection.sendChat(combined);
            }

            onClose();
        } else {
            VoxelConstants.getMinecraft().setScreen(this);
        }
    }

    private void sendMessageToPlayer(String name) {
        String combined = "msg " + name + " " + message.getValue() + " " + locInfo;
        if (combined.length() > 256) {
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + message.getValue());
            VoxelConstants.getPlayer().connection.sendCommand("msg " + name + " " + locInfo);
        } else {
            VoxelConstants.getPlayer().connection.sendCommand(combined);
        }

        onClose();
    }

    @Override
    public void removed() {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        sendButton.setMessage(Component.translatable("minimap.waypointShare.shareWithName", selectedPlyer.isEmpty() ? EVERYONE : selectedPlyer));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    class PlayerList extends GuiListMinimap<PlayerList.Entry> {
        private final ArrayList<PlayerInfo> players;
        private final ArrayList<PlayerInfo> playersFiltered = new ArrayList<>();
        private final Entry everyoneRow;

        public PlayerList(int x, int y, int width, int height) {
            super(x, y, width, height, 25);

            ClientPacketListener connection = VoxelConstants.getPlayer().connection;
            players = new ArrayList<>(connection.getOnlinePlayers());
            players.sort((player1, player2) -> {
                String name1 = getPlayerName(player1).getString();
                String name2 = getPlayerName(player2).getString();
                return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
            });

            Button everyoneButton = new Button.Builder(EVERYONE, button -> buttonClicked(-1)).bounds(GuiSelectPlayer.this.getWidth() / 2 - 75, 0, 150, 20).build();
            everyoneRow = new Entry(everyoneButton, -1, null, -1);

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
                return new Button.Builder(name, button -> buttonClicked(id)).bounds(x, y, 150, 20).build();
            }
        }

        @Override
        public int getRowWidth() {
            return 400;
        }

        private void updateFilter(String filterString) {
            setScrollAmount(0.0);
            clearEntries();

            addEntry(everyoneRow);

            playersFiltered.clear();
            for (PlayerInfo player : players) {
                String name = getPlayerName(player).toString();
                if (name.toLowerCase().contains(filterString)) {
                    playersFiltered.add(player);
                }
            }

            for (int i = 0; i < playersFiltered.size(); i += 2) {
                PlayerInfo player1 = playersFiltered.get(i);
                PlayerInfo player2 = i < playersFiltered.size() - 1 ? playersFiltered.get(i + 1) : null;

                Button button1 = createButtonFor(getWidth() / 2 - 155, 0, player1, i);
                Button button2 = createButtonFor(getWidth() / 2 - 155 + 160, 0, player2, i + 1);

                addEntry(new Entry(button1, i, button2, i + 1));
            }
        }

        public void buttonClicked(int id) {
            if (id == -1) {
                selectedPlyer = "";
            } else {
                PlayerInfo ScoreboardEntry = playersFiltered.get(id);
                selectedPlyer = getPlayerName(ScoreboardEntry).getString();
            }
        }

        @Override
        protected void extractSelection(GuiGraphicsExtractor graphics, Entry entry, int color) {
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput builder) {
        }

        public class Entry extends GuiListMinimap.Entry<Entry> {
            private final Button button1;
            private final Button button2;
            private final int id1;
            private final int id2;

            public Entry(Button primaryButton, int primaryId, Button secondaryButton, int secondaryId) {
                super(PlayerList.this);

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

            private void drawIconForButton(GuiGraphicsExtractor graphics, Button button, int id) {
                GameProfile gameProfile = playersFiltered.get(id).getProfile();
                Player player = VoxelConstants.getPlayer().level().getPlayerByUUID(gameProfile.id());
                Optional<PlayerSkin> optionalSkin = VoxelConstants.getMinecraft().getSkinManager().get(gameProfile).getNow(Optional.empty());
                if (optionalSkin.isPresent()) {
                    Identifier skinIdentifier = optionalSkin.get().body().texturePath();
                    graphics.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 8.0F, 8.0F, 8, 8, 8, 8, 64, 64);
                    if (player != null && player.isModelPartShown(PlayerModelPart.HAT)) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 40.0F, 8.0F, 8, 8, 8, 8, 64, 64);
                    }
                }
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (button1 != null) {
                    button1.setY(getY());
                    button1.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                    if (id1 != -1) {
                        drawIconForButton(graphics, button1, id1);
                    }
                }
                if (button2 != null) {
                    button2.setY(getY());
                    button2.extractRenderState(graphics, mouseX, mouseY, tickDelta);
                    if (id2 != -1) {
                        drawIconForButton(graphics, button2, id2);
                    }
                }
            }
        }
    }
}
