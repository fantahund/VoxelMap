package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public class GuiButtonRowListPlayers extends AbstractSelectionList<GuiButtonRowListPlayers.Row> {
    private final ArrayList<PlayerInfo> players;
    private ArrayList<PlayerInfo> playersFiltered;
    final GuiSelectPlayer parentGui;
    final Row everyoneRow;
    static final Component ALL = Component.translatable("minimap.waypointshare.all");
    static final Component TITLE = Component.translatable("minimap.waypointshare.sharewitheveryone");
    static final Component EXPLANATION = Component.translatable("minimap.waypointshare.sharewitheveryone2");
    static final Component AFFIRM = Component.translatable("gui.yes");
    static final Component DENY = Component.translatable("gui.cancel");

    public GuiButtonRowListPlayers(GuiSelectPlayer par1GuiSelectPlayer) {
        super(VoxelConstants.getMinecraft(), par1GuiSelectPlayer.getWidth(), par1GuiSelectPlayer.getHeight() - 65 + 4 - 89, 89, 25);
        this.parentGui = par1GuiSelectPlayer;
        ClientPacketListener netHandlerPlayClient = VoxelConstants.getPlayer().connection;
        this.players = new ArrayList<>(netHandlerPlayClient.getOnlinePlayers());
        this.sort();
        Button everyoneButton = new Button(this.parentGui.getWidth() / 2 - 75, 0, 150, 20, ALL, null, null) {
            public void onPress() {
            }
        };
        this.everyoneRow = new Row(everyoneButton, -1);
        this.updateFilter("");
    }

    private Component getPlayerName(PlayerInfo ScoreboardEntryIn) {
        return Component.literal(ScoreboardEntryIn.getProfile().getName());
    }

    private Button createButtonFor(int x, int y, PlayerInfo ScoreboardEntry) {
        if (ScoreboardEntry == null) {
            return null;
        } else {
            Component name = this.getPlayerName(ScoreboardEntry);
            return new Button.Builder(name, button -> {
            }).bounds(x, y, 150, 20).build();
        }
    }

    public int getRowWidth() {
        return 400;
    }

    protected int scrollBarX() {
        return super.scrollBarX() + 32;
    }

    protected void sort() {
        this.players.sort((player1, player2) -> {
            String name1 = GuiButtonRowListPlayers.this.getPlayerName(player1).getString();
            String name2 = GuiButtonRowListPlayers.this.getPlayerName(player2).getString();
            return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
        });
    }

    protected void updateFilter(String filterString) {
        this.playersFiltered = new ArrayList<>(this.players);
        Iterator<?> iterator = this.playersFiltered.iterator();

        while (iterator.hasNext()) {
            PlayerInfo ScoreboardEntry = (PlayerInfo) iterator.next();
            String name = this.getPlayerName(ScoreboardEntry).getString();
            if (!name.toLowerCase().contains(filterString)) {
                iterator.remove();
            }
        }

        this.clearEntries();
        this.addEntry(this.everyoneRow);

        for (int i = 0; i < this.playersFiltered.size(); i += 2) {
            PlayerInfo ScoreboardEntry1 = this.playersFiltered.get(i);
            PlayerInfo ScoreboardEntry2 = i < this.playersFiltered.size() - 1 ? this.playersFiltered.get(i + 1) : null;
            Button guibutton1 = this.createButtonFor(this.parentGui.getWidth() / 2 - 155, 0, ScoreboardEntry1);
            Button guibutton2 = this.createButtonFor(this.parentGui.getWidth() / 2 - 155 + 160, 0, ScoreboardEntry2);
            this.addEntry(new Row(guibutton1, i, guibutton2, i + 1));
        }
    }

    public void buttonClicked(int id) {
        if (id == -1) {
            this.parentGui.allClicked = true;
            ConfirmScreen confirmScreen = new ConfirmScreen(this.parentGui, TITLE, EXPLANATION, AFFIRM, DENY);
            this.minecraft.setScreen(confirmScreen);
        } else {
            PlayerInfo ScoreboardEntry = this.playersFiltered.get(id);
            String name = this.getPlayerName(ScoreboardEntry).getString();
            this.parentGui.sendMessageToPlayer(name);
        }

    }

    public void updateWidgetNarration(NarrationElementOutput builder) {
    }

    public class Row extends AbstractSelectionList.Entry<Row> {
        private Button button;
        private Button button1;
        private Button button2;
        private int id;
        private int id1;
        private int id2;

        public Row(Button button, int id) {
            this.button = button;
            this.id = id;
        }

        public Row(Button button1, int id1, Button button2, int id2) {
            this.button1 = button1;
            this.id1 = id1;
            this.button2 = button2;
            this.id2 = id2;
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.drawButton(drawContext, this.button, this.id, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            this.drawButton(drawContext, this.button1, this.id1, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            this.drawButton(drawContext, this.button2, this.id2, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        }

        private void drawButton(GuiGraphics drawContext, Button button, int id, int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            if (button != null) {
                button.setY(y);
                button.render(drawContext, mouseX, mouseY, partialTicks);
                if (id != -1) {
                    this.drawIconForButton(drawContext, button, id);
                }

                if (button.isHovered() && mouseY >= GuiButtonRowListPlayers.this.getY() && mouseY <= GuiButtonRowListPlayers.this.getBottom()) {
                    Component tooltip = Component.translatable("minimap.waypointshare.sharewithname", button.getMessage());
                    GuiSelectPlayer.setTooltip(GuiButtonRowListPlayers.this.parentGui, tooltip);
                }
            }

        }

        private void drawIconForButton(GuiGraphics drawContext, Button button, int id) {
            PlayerInfo networkPlayerInfo = GuiButtonRowListPlayers.this.playersFiltered.get(id);
            GameProfile gameProfile = networkPlayerInfo.getProfile();
            Player entityPlayer = VoxelConstants.getPlayer().level().getPlayerByUUID(gameProfile.getId());
            ResourceLocation skinIdentifier = VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(gameProfile).texture();
            drawContext.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 8.0F, 8.0F, 8, 8, 8, 8, 64, 64);
            // FIXME 1.21.6 drawContext.flush();
            if (entityPlayer != null && entityPlayer.isModelPartShown(PlayerModelPart.HAT)) {
                drawContext.blit(RenderPipelines.GUI_TEXTURED, skinIdentifier, button.getX() + 6, button.getY() + 6, 40.0F, 8.0F, 8, 8, 8, 8, 64, 64);
                // FIXME 1.21.6 drawContext.flush();
            }

        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.button != null && this.button.mouseClicked(mouseX, mouseY, button)) {
                GuiButtonRowListPlayers.this.buttonClicked(this.id);
                return true;
            } else if (this.button1 != null && this.button1.mouseClicked(mouseX, mouseY, button)) {
                GuiButtonRowListPlayers.this.buttonClicked(this.id1);
                return true;
            } else if (this.button2 != null && this.button2.mouseClicked(mouseX, mouseY, button)) {
                GuiButtonRowListPlayers.this.buttonClicked(this.id2);
                return true;
            } else {
                return false;
            }
        }

        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.button != null) {
                this.button.mouseReleased(mouseX, mouseY, button);
                return true;
            } else if (this.button1 != null) {
                this.button1.mouseReleased(mouseX, mouseY, button);
                return true;
            } else if (this.button2 != null) {
                this.button2.mouseReleased(mouseX, mouseY, button);
                return true;
            } else {
                return false;
            }
        }
    }
}
