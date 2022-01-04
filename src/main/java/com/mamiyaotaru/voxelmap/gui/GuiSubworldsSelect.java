package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.interfaces.IWaypointManager;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Text title;
    private Text select;
    private boolean multiworld = false;
    private TextFieldWidget newNameField;
    private boolean newWorld = false;
    private float yaw;
    private final Perspective thirdPersonViewOrig;
    private String[] worlds;
    private final Screen parent;
    ClientPlayerEntity thePlayer;
    ClientPlayerEntity camera;
    private final IVoxelMap master;
    private final IWaypointManager waypointManager;

    public GuiSubworldsSelect(Screen parent, IVoxelMap master) {
        this.client = MinecraftClient.getInstance();
        this.parent = parent;
        this.thePlayer = this.getMinecraft().player;
        this.camera = new ClientPlayerEntity(this.getMinecraft(), this.getMinecraft().world, this.getMinecraft().getNetworkHandler(), this.thePlayer.getStatHandler(), new ClientRecipeBook(), false, false);
        this.camera.input = new KeyboardInput(this.getMinecraft().options);
        this.camera.refreshPositionAndAngles(this.thePlayer.getX(), this.thePlayer.getY() - this.thePlayer.getHeightOffset(), this.thePlayer.getZ(), this.thePlayer.getYaw(), 0.0F);
        this.yaw = this.thePlayer.getYaw();
        this.thirdPersonViewOrig = this.getMinecraft().options.getPerspective();
        this.master = master;
        this.waypointManager = master.getWaypointManager();
    }

    public void init() {
        ArrayList<String> knownSubworldNames = new ArrayList(this.waypointManager.getKnownSubworldNames());
        if (!this.multiworld && !this.waypointManager.isMultiworld() && !this.getMinecraft().isConnectedToRealms()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, new TranslatableText("worldmap.multiworld.isthismultiworld"), new TranslatableText("worldmap.multiworld.explanation"), new TranslatableText("gui.yes"), new TranslatableText("gui.no"));
            this.getMinecraft().setScreen(confirmScreen);
        } else {
            this.getMinecraft().options.setPerspective(Perspective.FIRST_PERSON);
            this.getMinecraft().setCameraEntity(this.camera);
        }

        this.title = new TranslatableText("worldmap.multiworld.title");
        this.select = new TranslatableText("worldmap.multiworld.select");
        this.clearChildren();
        int centerX = this.width / 2;
        int buttonsPerRow = this.width / 150;
        if (buttonsPerRow == 0) {
            buttonsPerRow = 1;
        }

        int buttonWidth = this.width / buttonsPerRow - 5;
        int xSpacing = (this.width - buttonsPerRow * buttonWidth) / 2;
        ButtonWidget cancelBtn = new ButtonWidget(centerX - 100, this.height - 30, 200, 20, new TranslatableText("gui.cancel"), button -> this.getMinecraft().setScreen(null));
        this.addDrawableChild(cancelBtn);
        final Collator collator = I18nUtils.getLocaleAwareCollator();
        knownSubworldNames.sort((name1, name2) -> -collator.compare(name1, name2));
        int numKnownSubworlds = knownSubworldNames.size();
        int completeRows = (int) Math.floor((float) (numKnownSubworlds + 1) / (float) buttonsPerRow);
        int lastRowShiftBy = (int) (Math.ceil((float) (numKnownSubworlds + 1) / (float) buttonsPerRow) * (double) buttonsPerRow - (double) (numKnownSubworlds + 1));
        this.worlds = new String[numKnownSubworlds];
        ButtonWidget[] selectButtons = new ButtonWidget[numKnownSubworlds + 1];
        ButtonWidget[] editButtons = new ButtonWidget[numKnownSubworlds + 1];

        for (int t = 0; t < numKnownSubworlds; ++t) {
            int shiftBy = 1;
            if (t / buttonsPerRow >= completeRows) {
                shiftBy = lastRowShiftBy + 1;
            }

            this.worlds[t] = knownSubworldNames.get(t);
            int tt = t;
            selectButtons[t] = new ButtonWidget((buttonsPerRow - shiftBy - t % buttonsPerRow) * buttonWidth + xSpacing, this.height - 60 - t / buttonsPerRow * 21, buttonWidth - 32, 20, new LiteralText(this.worlds[t]), button -> this.worldSelected(this.worlds[tt]));
            editButtons[t] = new ButtonWidget((buttonsPerRow - shiftBy - t % buttonsPerRow) * buttonWidth + xSpacing + buttonWidth - 32, this.height - 60 - t / buttonsPerRow * 21, 30, 20, new LiteralText("⚒"), button -> this.editWorld(this.worlds[tt]));
            this.addDrawableChild(selectButtons[t]);
            this.addDrawableChild(editButtons[t]);
        }

        int numButtons = selectButtons.length - 1;
        if (!this.newWorld) {
            selectButtons[numButtons] = new ButtonWidget((buttonsPerRow - 1 - lastRowShiftBy - numButtons % buttonsPerRow) * buttonWidth + xSpacing, this.height - 60 - numButtons / buttonsPerRow * 21, buttonWidth - 2, 20, new LiteralText("< " + I18nUtils.getString("worldmap.multiworld.newname") + " >"), button -> {
                this.newWorld = true;
                this.newNameField.setTextFieldFocused(true);
            });
            this.addDrawableChild(selectButtons[numButtons]);
        }

        this.newNameField = new TextFieldWidget(this.getFontRenderer(), (buttonsPerRow - 1 - lastRowShiftBy - numButtons % buttonsPerRow) * buttonWidth + xSpacing + 1, this.height - 60 - numButtons / buttonsPerRow * 21 + 1, buttonWidth - 4, 18, null);
    }

    public void accept(boolean par1) {
        if (!par1) {
            this.getMinecraft().setScreen(this.parent);
        } else {
            this.multiworld = true;
            this.getMinecraft().setScreen(this);
        }

    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (this.newWorld) {
            this.newNameField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public boolean keyPressed(int keysm, int scancode, int b) {
        if (this.newNameField.isFocused()) {
            this.newNameField.keyPressed(keysm, scancode, b);
            if ((keysm == 257 || keysm == 335) && this.newNameField.isFocused()) {
                String newName = this.newNameField.getText();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.keyPressed(keysm, scancode, b);
    }

    public boolean charTyped(char typedChar, int keyCode) {
        if (this.newNameField.isFocused()) {
            this.newNameField.charTyped(typedChar, keyCode);
            if (keyCode == 28) {
                String newName = this.newNameField.getText();
                if (newName != null && !newName.isEmpty()) {
                    this.worldSelected(newName);
                }
            }
        }

        return super.charTyped(typedChar, keyCode);
    }

    public void tick() {
        this.newNameField.tick();
        super.tick();
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int titleStringWidth = this.getFontRenderer().getWidth(this.title);
        titleStringWidth = Math.max(titleStringWidth, this.getFontRenderer().getWidth(this.select));
        fill(matrixStack, this.width / 2 - titleStringWidth / 2 - 5, 0, this.width / 2 + titleStringWidth / 2 + 5, 27, -1073741824);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.title, this.width / 2, 5, 16777215);
        drawCenteredText(matrixStack, this.getFontRenderer(), this.select, this.width / 2, 15, 16711680);
        this.camera.prevPitch = 0.0F;
        this.camera.setPitch(0.0F);
        this.camera.prevYaw = this.yaw;
        this.camera.setYaw(this.yaw);
        float var4 = 0.475F;
        this.camera.lastRenderY = this.camera.prevY = this.thePlayer.getY();
        this.camera.lastRenderX = this.camera.prevX = this.thePlayer.getX() - (double) var4 * Math.sin((double) this.yaw / 180.0 * Math.PI);
        this.camera.lastRenderZ = this.camera.prevZ = this.thePlayer.getZ() + (double) var4 * Math.cos((double) this.yaw / 180.0 * Math.PI);
        this.camera.setPos(this.camera.prevX, this.camera.prevY, this.camera.prevZ);
        float var5 = 1.0F;
        this.yaw = (float) ((double) this.yaw + (double) var5 * (1.0 + 0.7F * Math.cos((double) (this.yaw + 45.0F) / 45.0 * Math.PI)));
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        if (this.newWorld) {
            this.newNameField.render(matrixStack, mouseX, mouseY, partialTicks);
        }

    }

    @Override
    public void removed() {
        super.removed();
        this.getMinecraft().options.setPerspective(this.thirdPersonViewOrig);
        this.getMinecraft().setCameraEntity(this.thePlayer);
    }

    private void worldSelected(String selectedSubworldName) {
        this.waypointManager.setSubworldName(selectedSubworldName, false);
        this.getMinecraft().setScreen(this.parent);
    }

    private void editWorld(String subworldNameToEdit) {
        this.getMinecraft().setScreen(new GuiSubworldEdit(this, this.master, subworldNameToEdit));
    }
}
