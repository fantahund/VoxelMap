package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class GuiListPlayers extends AbstractSelectionList<GuiListPlayers.Row> {
    private final ArrayList<PlayerInfo> players;
    private ArrayList<PlayerInfo> playersFiltered;
    private final GuiSelectPlayer parentGui;
    private final Row everyoneRow;

    static final Component EVERYONE = Component.translatable("minimap.waypointShare.all");
    static final Component CONFIRM_TITLE = Component.translatable("minimap.waypointShare.shareWithEveryone");
    static final Component CONFIRM_EXPLANATION = Component.translatable("minimap.waypointShare.shareWithEveryone2");
    static final Component CONFIRM_AFFIRM = Component.translatable("gui.yes");
    static final Component CONFIRM_DENY = Component.translatable("gui.cancel");

    public GuiListPlayers(GuiSelectPlayer parentGui) {
        super(VoxelConstants.getMinecraft(), parentGui.getWidth(), parentGui.getHeight() - 65 + 4 - 89, 89, 25);
        this.parentGui = parentGui;

        ClientPacketListener connection = VoxelConstants.getPlayer().connection;
        players = new ArrayList<>(connection.getOnlinePlayers());
        sort();

        Button everyoneButton = new Button.Builder(EVERYONE, button -> {}).bounds(parentGui.getWidth() / 2 - 75, 0, 150, 20).build();
        everyoneButton.setTooltip(Tooltip.create(Component.translatable("minimap.waypointShare.shareWithName", EVERYONE)));
        everyoneRow = new Row(everyoneButton, -1, null, -1);

        updateFilter("");
    }

    private Component getPlayerName(PlayerInfo ScoreboardEntryIn) {
        return Component.literal(ScoreboardEntryIn.getProfile().name());
    }

    private Button createButtonFor(int x, int y, PlayerInfo ScoreboardEntry) {
        if (ScoreboardEntry == null) {
            return null;
        } else {
            Component name = getPlayerName(ScoreboardEntry);
            Button btn = new Button.Builder(name, button -> {}).bounds(x, y, 150, 20).build();
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

            Button button1 = createButtonFor(parentGui.getWidth() / 2 - 155, 0, ScoreboardEntry1);
            Button button2 = createButtonFor(parentGui.getWidth() / 2 - 155 + 160, 0, ScoreboardEntry2);

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
    public void updateWidgetNarration(NarrationElementOutput builder) {
    }

    public class Row extends AbstractSelectionList.Entry<Row> {
        private final LinkedHashMap<Button, Integer> buttonAndIds = new LinkedHashMap<>();;

        public Row(Button primaryButton, int primaryId, Button secondaryButton, int secondaryId) {
            buttonAndIds.put(primaryButton, primaryId);
            buttonAndIds.put(secondaryButton, secondaryId);
        }

        @Override
        public void renderContent(GuiGraphics drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            buttonAndIds.forEach((button, id) -> {
                if (button != null) {
                    button.setY(getY());
                    button.render(drawContext, mouseX, mouseY, tickDelta);
                    if (id != -1) {
                        drawIconForButton(drawContext, button, id);
                    }
                }
            });
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
        public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
            for (Map.Entry<Button, Integer> entry : buttonAndIds.entrySet()) {
                Button button = entry.getKey();
                Integer id = entry.getValue();
                if (button != null && button.mouseClicked(mouseButtonEvent, doubleClick)) {
                    buttonClicked(id);
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
            for (Map.Entry<Button, Integer> entry : buttonAndIds.entrySet()) {
                Button button = entry.getKey();
                if (button != null && button.mouseReleased(mouseButtonEvent)) {
                    return true;
                }
            }

            return false;
        }
    }
}
