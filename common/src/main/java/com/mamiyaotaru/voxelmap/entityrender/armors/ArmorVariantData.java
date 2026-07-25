package com.mamiyaotaru.voxelmap.entityrender.armors;

import com.mamiyaotaru.voxelmap.entityrender.VariantDataHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ArmorVariantData extends VariantDataHolder {
    private final Item type;
    private final String id;
    private final int size;
    private final boolean addBorder;

    public ArmorVariantData(Item type, String id, Identifier tex0, int col0, int size, boolean addBorder) {
        this(type, id, tex0, col0, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier tex0, int col0, Identifier tex1, int col1, int size, boolean addBorder) {
        this(type, id, tex0, col0, tex1, col1, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, int size, boolean addBorder) {
        this(type, id, tex0, col0, tex1, col1, tex2, col2, null, -1, size, addBorder);
    }

    public ArmorVariantData(Item type, String id, Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3, int size, boolean addBorder) {
        super(buildName(type, id, size, addBorder), tex0, col0, tex1, col1, tex2, col2, tex3, col3);
        this.type = type;
        this.id = id;
        this.size = size;
        this.addBorder = addBorder;
    }

    private static String buildName(Item type, String id, int size, boolean addBorder) {
        return type.getDescriptionId() + "id: \"" + id + "\", size: " + size + ", addBorder:" + addBorder;
    }

    public Item type() {
        return type;
    }

    public String id() {
        return id;
    }

    public int size() {
        return size;
    }

    public boolean outline() {
        return addBorder;
    }
}
