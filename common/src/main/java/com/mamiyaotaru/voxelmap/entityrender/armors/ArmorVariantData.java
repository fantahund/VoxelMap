package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.entityrender.VariantDataHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ArmorVariantData extends VariantDataHolder {
    private final Item type;
    private final String id;
    private final int size;
    private final boolean addBorder;

    public ArmorVariantData(Item type, String id, Identifier texture0, int color0, int size, boolean addBorder) {
        this(type, id, texture0, color0, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier texture0, int color0, Identifier texture1, int color1, int size, boolean addBorder) {
        this(type, id, texture0, color0, texture1, color1, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier texture0, int color0, Identifier texture1, int color1, Identifier texture2, int color2, int size, boolean addBorder) {
        this(type, id, texture0, color0, texture1, color1, texture2, color2, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier texture0, int color0, Identifier texture1, int color1, Identifier texture2, int color2, Identifier texture3, int color3, int size, boolean addBorder) {
        super(buildName(type, id, size, addBorder), texture0, color0, texture1, color1, texture2, color2, texture3, color3);
        this.type = type;
        this.id = id;
        this.size = size;
        this.addBorder = addBorder;
    }

    private static String buildName(Item type, String id, int size, boolean addBorder) {
        return type.getDescriptionId() + "id:[" + id + "], size: " + size + ", addBorder:" + addBorder;
    }

    public Item getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public boolean hasOutline() {
        return addBorder;
    }
}
