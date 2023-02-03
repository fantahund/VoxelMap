package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Iterator;

public class GuiButtonRowListPlayers extends EntryListWidget<GuiButtonRowListPlayers.Row> {
    private final ArrayList<PlayerListEntry> players;
    private ArrayList<?> playersFiltered;
    final GuiSelectPlayer parentGui;
    final Row everyoneRow;
    static final Text ALL = Text.translatable("minimap.waypointshare.all");
    static final Text TITLE = Text.translatable("minimap.waypointshare.sharewitheveryone");
    static final Text EXPLANATION = Text.translatable("minimap.waypointshare.sharewitheveryone2");
    static final Text AFFIRM = Text.translatable("gui.yes");
    static final Text DENY = Text.translatable("gui.cancel");

    public GuiButtonRowListPlayers(GuiSelectPlayer par1GuiSelectPlayer) {
        super(VoxelConstants.getMinecraft(), par1GuiSelectPlayer.getWidth(), par1GuiSelectPlayer.getHeight(), 89, par1GuiSelectPlayer.getHeight() - 65 + 4, 25);
        this.parentGui = par1GuiSelectPlayer;
        ClientPlayNetworkHandler netHandlerPlayClient = VoxelConstants.getPlayer().networkHandler;
        this.players = new ArrayList<>(netHandlerPlayClient.getPlayerList());
        this.sort();
        ButtonWidget everyoneButton = new ButtonWidget(this.parentGui.getWidth() / 2 - 75, 0, 150, 20, ALL, null, null) {
            public void onPress() {
            }
        };
        this.everyoneRow = new Row(everyoneButton, -1);
        this.updateFilter("");
    }

    private Text getPlayerName(PlayerListEntry ScoreboardEntryIn) {
        return ScoreboardEntryIn.getDisplayName() != null ? ScoreboardEntryIn.getDisplayName() : Text.literal(ScoreboardEntryIn.getProfile().getName());
    }

    private ButtonWidget createButtonFor(int x, int y, PlayerListEntry ScoreboardEntry) {
        if (ScoreboardEntry == null) {
            return null;
        } else {
            Text name = this.getPlayerName(ScoreboardEntry);
            return new ButtonWidget.Builder(name, button -> {
            }).dimensions(x, y, 150, 20).build();
        }
    }

    public int getRowWidth() {
        return 400;
    }

    protected int getScrollbarPositionX() {
        return super.getScrollbarPositionX() + 32;
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
            PlayerListEntry ScoreboardEntry = (PlayerListEntry) iterator.next();
            String name = this.getPlayerName(ScoreboardEntry).getString();
            if (!name.toLowerCase().contains(filterString)) {
                iterator.remove();
            }
        }

        this.clearEntries();
        this.addEntry(this.everyoneRow);

        for (int i = 0; i < this.playersFiltered.size(); i += 2) {
            PlayerListEntry ScoreboardEntry1 = (PlayerListEntry) this.playersFiltered.get(i);
            PlayerListEntry ScoreboardEntry2 = i < this.playersFiltered.size() - 1 ? (PlayerListEntry) this.playersFiltered.get(i + 1) : null;
            ButtonWidget guibutton1 = this.createButtonFor(this.parentGui.getWidth() / 2 - 155, 0, ScoreboardEntry1);
            ButtonWidget guibutton2 = this.createButtonFor(this.parentGui.getWidth() / 2 - 155 + 160, 0, ScoreboardEntry2);
            this.addEntry(new Row(guibutton1, i, guibutton2, i + 1));
        }

    }

    public void buttonClicked(int id) {
        if (id == -1) {
            this.parentGui.allClicked = true;
            ConfirmScreen confirmScreen = new ConfirmScreen(this.parentGui, this.TITLE, this.EXPLANATION, this.AFFIRM, this.DENY);
            this.client.setScreen(confirmScreen);
        } else {
            PlayerListEntry ScoreboardEntry = (PlayerListEntry) this.playersFiltered.get(id);
            String name = this.getPlayerName(ScoreboardEntry).getString();
            this.parentGui.sendMessageToPlayer(name);
        }

    }

    public void appendNarrations(NarrationMessageBuilder builder) {
    }

    public class Row extends EntryListWidget.Entry<Row> {
        private ButtonWidget button;
        private ButtonWidget button1;
        private ButtonWidget button2;
        private int id;
        private int id1;
        private int id2;

        public Row(ButtonWidget button, int id) {
            this.button = button;
            this.id = id;
        }

        public Row(ButtonWidget button1, int id1, ButtonWidget button2, int id2) {
            this.button1 = button1;
            this.id1 = id1;
            this.button2 = button2;
            this.id2 = id2;
        }

        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.drawButton(matrices, this.button, this.id, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            this.drawButton(matrices, this.button1, this.id1, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            this.drawButton(matrices, this.button2, this.id2, index, x, y, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
        }

        private void drawButton(MatrixStack matrixStack, ButtonWidget button, int id, int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
            if (button != null) {
                button.setY(y);
                button.render(matrixStack, mouseX, mouseY, partialTicks);
                if (id != -1) {
                    this.drawIconForButton(matrixStack, button, id);
                }

                if (button.isHovered() && mouseY >= GuiButtonRowListPlayers.this.top && mouseY <= GuiButtonRowListPlayers.this.bottom) {
                    Text tooltip = Text.translatable("minimap.waypointshare.sharewithname", button.getMessage());
                    GuiSelectPlayer.setTooltip(GuiButtonRowListPlayers.this.parentGui, tooltip);
                }
            }

        }

        private void drawIconForButton(MatrixStack matrixStack, ButtonWidget button, int id) {
            PlayerListEntry networkPlayerInfo = (PlayerListEntry) GuiButtonRowListPlayers.this.playersFiltered.get(id);
            GameProfile gameProfile = networkPlayerInfo.getProfile();
            PlayerEntity entityPlayer = VoxelConstants.getPlayer().world.getPlayerByUuid(gameProfile.getId());
            RenderSystem.setShader(GameRenderer::getPositionTexProgram);
            RenderSystem.setShaderTexture(0, networkPlayerInfo.getSkinTexture());
            Screen.drawTexture(matrixStack, button.getX() + 6, button.getY() + 6, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
            if (entityPlayer != null && entityPlayer.isPartVisible(PlayerModelPart.HAT)) {
                Screen.drawTexture(matrixStack, button.getX() + 6, button.getY() + 6, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
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
