package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.VariantDataHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class EntityVariantData extends VariantDataHolder {
    private final EntityType<?> type;
    private final String id;
    private final boolean addBorder;

    public EntityVariantData(EntityType<?> type, String id, Identifier tex0, int col0, boolean addBorder) {
        this(type, id, tex0, col0, null, -1, addBorder);
    }

    public EntityVariantData(EntityType<?> type, String id, Identifier tex0, int col0, Identifier tex1, int col1, boolean addBorder) {
        this(type, id, tex0, col0, tex1, col1, null, -1, addBorder);
    }

    public EntityVariantData(EntityType<?> type, String id, Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, boolean addBorder) {
        this(type, id, tex0, col0, tex1, col1, tex2, col2, null, -1, addBorder);
    }

    public EntityVariantData(EntityType<?> type, String id, Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3, boolean addBorder) {
        super(buildName(type, id, addBorder), tex0, col0, tex1, col1, tex2, col2, tex3, col3);
        this.type = type;
        this.id = id;
        this.addBorder = addBorder;
    }

    private static String buildName(EntityType<?> type, String id, boolean addBorder) {
        return type.getDescriptionId() + "id:\"" + id + "\", addBorder:" + addBorder;
    }

    public EntityType<?> type() {
        return type;
    }

    public String id() {
        return id;
    }

    public boolean outline() {
        return addBorder;
    }
}
