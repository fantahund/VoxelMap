package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.gui.settings.EntityTypeDialog;
import com.mamiyaotaru.voxelmap.gui.settings.SettingsCategory;
import com.mamiyaotaru.voxelmap.gui.settings.SettingsListWidget;
import com.mamiyaotaru.voxelmap.gui.settings.SettingsOption;
import com.mamiyaotaru.voxelmap.gui.settings.VoxelMapSettings;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

public class GuiMinimapOptions extends GuiScreenMinimap {
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;
    private static final int CATEGORY_GAP = 4;

    private final List<SettingsCategory> categories = VoxelMapSettings.create(this::openEntityTypeDialog);
    private final List<Button> categoryButtons = new ArrayList<>();
    private int selectedCategory;
    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int categoryWidth;
    private SettingsListWidget optionList;
    private EntityTypeDialog entityTypeDialog;

    public GuiMinimapOptions(Screen parent) {
        this(parent, "minimap");
    }

    public GuiMinimapOptions(Screen parent, String initialCategory) {
        super();
        this.lastScreen = parent;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id().equals(initialCategory)) {
                selectedCategory = i;
                break;
            }
        }
    }

    @Override
    protected void init() {
        boolean reopenEntityDialog = entityTypeDialog != null;
        entityTypeDialog = null;
        clearWidgets();
        categoryButtons.clear();

        int maximumWidth = 760;
        contentWidth = Math.min(width - 16, maximumWidth);
        contentHeight = Math.max(80, height - HEADER_HEIGHT - FOOTER_HEIGHT);
        contentX = (width - contentWidth) / 2;
        contentY = HEADER_HEIGHT;
        categoryWidth = Math.clamp(contentWidth / 5, 92, 132);

        for (int i = 0; i < categories.size(); i++) {
            int index = i;
            Button button = Button.builder(categories.get(i).title(), ignored -> selectCategory(index))
                    .bounds(contentX, contentY + i * 24, categoryWidth, 20).build();
            categoryButtons.add(addRenderableWidget(button));
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), ignored -> onClose())
                .bounds(width / 2 - 100, height - 27, 200, 20).build());
        rebuildContent();
        if (reopenEntityDialog)
            openEntityTypeDialog();
    }

    private void selectCategory(int index) {
        if (index == selectedCategory)
            return;
        selectedCategory = index;
        rebuildContent();
    }

    public void rebuildContent() {
        if (optionList != null) {
            optionList.commitPendingText();
            removeWidget(optionList);
        }
        int listX = contentX + categoryWidth + CATEGORY_GAP;
        int listWidth = contentWidth - categoryWidth - CATEGORY_GAP;
        optionList = new SettingsListWidget(this, listX, contentY, listWidth, contentHeight, categories.get(selectedCategory));
        addRenderableWidget(optionList);
        updateCategoryButtons();
    }

    public void cycleChoice(SettingsOption<?> option) {
        if (option.choices().isEmpty())
            return;
        int currentIndex = 0;
        for (int i = 0; i < option.choices().size(); i++) {
            if (option.choices().get(i).value().equals(option.value())) {
                currentIndex = i;
                break;
            }
        }
        setChoice(option, (currentIndex + 1) % option.choices().size());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (entityTypeDialog != null) {
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                closeEntityTypeDialog();
                return true;
            }
            return entityTypeDialog.keyPressed(event);
        }
        if (optionList != null && optionList.isEditingKey()) {
            return optionList.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return entityTypeDialog != null ? entityTypeDialog.keyReleased(event) : super.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return entityTypeDialog != null ? entityTypeDialog.charTyped(event) : super.charTyped(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        return entityTypeDialog != null ? entityTypeDialog.preeditUpdated(event) : super.preeditUpdated(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (entityTypeDialog != null)
            entityTypeDialog.mouseMoved(mouseX, mouseY);
        else
            super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (entityTypeDialog != null) {
            entityTypeDialog.mouseClicked(event, doubleClick);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (entityTypeDialog != null) {
            entityTypeDialog.mouseReleased(event);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (entityTypeDialog != null) {
            entityTypeDialog.mouseDragged(event, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (entityTypeDialog != null) {
            entityTypeDialog.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void openEntityTypeDialog() {
        if (entityTypeDialog != null)
            return;
        entityTypeDialog = new EntityTypeDialog(this, this::closeEntityTypeDialog);
        addRenderableWidget(entityTypeDialog);
        setFocused(entityTypeDialog);
    }

    private void closeEntityTypeDialog() {
        if (entityTypeDialog == null)
            return;
        removeWidget(entityTypeDialog);
        entityTypeDialog = null;
        setFocused(optionList);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setChoiceUnchecked(SettingsOption<T> option, int action) {
        option.set(option.choices().get(action).value());
    }

    private static void setChoice(SettingsOption<?> option, int action) {
        setChoiceUnchecked(option, action);
    }

    private void updateCategoryButtons() {
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            boolean selected = i == selectedCategory;
            button.active = !selected;
            button.setMessage(selected
                    ? Component.literal("◆ ").withStyle(ChatFormatting.AQUA).append(categories.get(i).title())
                    : categories.get(i).title());
        }
    }

    @Override
    public void onClose() {
        if (entityTypeDialog != null) {
            closeEntityTypeDialog();
            return;
        }
        if (optionList != null)
            optionList.commitPendingText();
        super.onClose();
    }

    @Override
    public void removed() {
        if (optionList != null)
            optionList.commitPendingText();
        MapSettingsManager.instance.saveAll();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        if (entityTypeDialog == null)
            graphics.centeredText(getFont(), Component.translatable("options.minimap.title"), width / 2, 12, 0xFFFFFFFF);
    }

    @Override
    public void extractMenuBackground(GuiGraphicsExtractor graphics) {
        super.extractMenuBackground(graphics);
        graphics.fill(contentX - 4, contentY - 4, contentX + contentWidth + 4, contentY + contentHeight + 4, 0x66000000);
        graphics.fill(contentX + categoryWidth + 1, contentY, contentX + categoryWidth + 2, contentY + contentHeight, 0x88707070);
    }

}
