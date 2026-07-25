package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapDataConfig;
import com.mamiyaotaru.voxelmap.persistent.VoxelMapMigration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiServerAliases extends GuiScreenMinimap {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 96;
    private static final int LIST_BOTTOM_MARGIN = 40;
    private static final int FIELD_WIDTH = 282;
    private static final int DELETE_WIDTH = 20;

    private final class AliasRow {
        final EditBox field;
        final Button delete;

        AliasRow(String value) {
            this.field = new EditBox(GuiServerAliases.this.getFont(), 0, 0, FIELD_WIDTH, 20, Component.empty());
            this.field.setMaxLength(256);
            this.field.setValue(value);
            this.field.setResponder(text -> onRowEdited(this));
            this.delete = new Button.Builder(Component.literal("✕"), button -> deleteRow(this)).bounds(0, 0, DELETE_WIDTH, 20).build();
        }
    }

    private final String contextServerName;
    private final List<AliasRow> rows = new ArrayList<>();
    private EditBox canonicalField;
    private int scrollRow;
    private int visibleRows = 1;
    private int fieldX;
    private List<String> originalAliases = List.of();

    public GuiServerAliases(Screen parent, String contextServerName) {
        this.lastScreen = parent;
        this.contextServerName = contextServerName == null ? "" : contextServerName;
    }

    @Override
    public void init() {
        this.clearWidgets();
        this.rows.clear();
        int centerX = this.getWidth() / 2;
        this.fieldX = centerX - 155;
        this.visibleRows = Math.max(1, (this.getHeight() - LIST_BOTTOM_MARGIN - LIST_TOP) / ROW_HEIGHT);

        VoxelMapDataConfig config = VoxelMapDataConfig.getInstance();
        String canonical = this.contextServerName.isEmpty() ? "" : config.resolveCanonical(this.contextServerName);

        this.canonicalField = new EditBox(this.getFont(), this.fieldX, 52, 310, 20, Component.empty());
        this.canonicalField.setMaxLength(256);
        this.canonicalField.setValue(canonical);
        this.addRenderableWidget(this.canonicalField);

        this.originalAliases = config.getAliasesFor(canonical);
        for (String alias : this.originalAliases) {
            addRow(alias);
        }

        addRow("");

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> this.save())
                .bounds(centerX - 155, this.getHeight() - 27, 150, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX + 5, this.getHeight() - 27, 150, 20).build());

        layoutRows();
    }

    private void addRow(String value) {
        AliasRow row = new AliasRow(value);
        this.rows.add(row);
        this.addRenderableWidget(row.field);
        this.addRenderableWidget(row.delete);
    }

    private void onRowEdited(AliasRow row) {
        if (row == this.rows.getLast() && !row.field.getValue().isEmpty()) {
            addRow("");
            layoutRows();
        }
    }

    private void deleteRow(AliasRow row) {
        this.removeWidget(row.field);
        this.removeWidget(row.delete);
        this.rows.remove(row);

        if (this.rows.isEmpty() || !this.rows.getLast().field.getValue().isEmpty()) {
            addRow("");
        }

        layoutRows();
    }

    private void layoutRows() {
        int maxScroll = Math.max(0, this.rows.size() - this.visibleRows);
        this.scrollRow = Math.clamp(this.scrollRow, 0, maxScroll);

        for (int i = 0; i < this.rows.size(); i++) {
            AliasRow row = this.rows.get(i);
            int visIndex = i - this.scrollRow;
            boolean visible = visIndex >= 0 && visIndex < this.visibleRows;

            row.field.visible = visible;
            row.field.active = visible;
            row.delete.visible = visible;
            row.delete.active = visible;

            if (visible) {
                int y = LIST_TOP + visIndex * ROW_HEIGHT;
                row.field.setX(this.fieldX);
                row.field.setY(y);
                row.delete.setX(this.fieldX + FIELD_WIDTH + 4);
                row.delete.setY(y);
            } else {
                row.field.setFocused(false);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        if (this.rows.size() > this.visibleRows && mouseY >= LIST_TOP && mouseY <= this.getHeight() - LIST_BOTTOM_MARGIN) {
            this.scrollRow -= (int) Math.signum(amount);
            layoutRows();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    private List<String> collectAliases(String canonical) {
        List<String> result = new ArrayList<>();
        for (AliasRow row : this.rows) {
            String line = row.field.getValue().trim();
            if (line.isEmpty() || line.equalsIgnoreCase(canonical)) {
                continue;
            }

            boolean duplicate = result.stream().anyMatch(existing -> existing.equalsIgnoreCase(line));
            if (!duplicate) {
                result.add(line);
            }
        }

        return result;
    }

    private void save() {
        String canonical = this.canonicalField.getValue().trim();
        if (canonical.isEmpty()) {
            super.onClose();
            return;
        }

        List<String> aliases = collectAliases(canonical);
        VoxelMapDataConfig config = VoxelMapDataConfig.getInstance();
        config.setAliasesFor(canonical, aliases);

        List<String> mergeCandidates = new ArrayList<>();
        for (String alias : aliases) {
            boolean isNew = this.originalAliases.stream().noneMatch(existing -> existing.equalsIgnoreCase(alias));
            if (isNew && VoxelMapMigration.forServerMerge(alias, canonical).hasData()) {
                mergeCandidates.add(alias);
            }
        }

        processMerges(canonical, mergeCandidates, 0);
    }

    private void processMerges(String canonical, List<String> candidates, int index) {
        if (index >= candidates.size()) {
            super.onClose();
            return;
        }

        String alias = candidates.get(index);
        VoxelMapMigration job = VoxelMapMigration.forServerMerge(alias, canonical);
        Component title = Component.translatable("voxelmap.migration.merge.title");
        Component description = Component.translatable("voxelmap.migration.merge.description", alias, canonical);
        VoxelMapMigrationFlow.start(this, title, description, job, true, new VoxelMapMigrationFlow.Handler() {
            @Override
            public void onKeepLegacy() {
            }

            @Override
            public void onMigrated() {
            }

            @Override
            public void resolved() {
                processMerges(canonical, candidates, index + 1);
            }
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.getWidth() / 2;
        graphics.centeredText(this.getFont(), Component.translatable("voxelmap.alias.title"), centerX, 16, 0xFFFFFFFF);

        String currentLabel = this.contextServerName.isEmpty()
                ? Component.translatable("voxelmap.alias.noServer").getString()
                : Component.translatable("voxelmap.alias.current", this.contextServerName.toLowerCase(Locale.ROOT)).getString();
        graphics.text(this.getFont(), currentLabel, this.fieldX, 30, 0xFF808080);

        graphics.text(this.getFont(), Component.translatable("voxelmap.alias.canonical"), this.fieldX, 42, 0xFFA0A0A0);
        graphics.text(this.getFont(), Component.translatable("voxelmap.alias.aliases"), this.fieldX, 86, 0xFFA0A0A0);

        if (this.rows.size() > this.visibleRows) {
            String more = "↓ " + (this.rows.size() - this.visibleRows) + " …";
            graphics.text(this.getFont(), more, this.fieldX + FIELD_WIDTH - 30, this.getHeight() - LIST_BOTTOM_MARGIN + 2, 0xFF808080);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
