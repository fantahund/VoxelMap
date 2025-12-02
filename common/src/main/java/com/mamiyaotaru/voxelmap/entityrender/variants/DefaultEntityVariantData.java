package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class DefaultEntityVariantData implements EntityVariantData {
    private final EntityType<?> type;
    private final Identifier primaryTexture;
    private final Identifier secondaryTexture;
    private final int size;
    private final boolean addBorder;

    public DefaultEntityVariantData(EntityType<?> type, Identifier primaryTexture, Identifier secondaryTexture, int size, boolean addBorder) {
        this.type = type;
        this.primaryTexture = primaryTexture;
        this.secondaryTexture = secondaryTexture;
        this.size = size;
        this.addBorder = addBorder;
    }

    @Override
    public EntityType<?> getType() {
        return type;
    }

    @Override
    public Identifier getPrimaryTexture() {
        return primaryTexture;
    }

    @Override
    public Identifier getSecondaryTexture() {
        return secondaryTexture;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        DefaultEntityVariantData other = (DefaultEntityVariantData) obj;
        return type == other.type && size == other.size && addBorder == other.addBorder && Objects.equals(primaryTexture, other.primaryTexture) && Objects.equals(secondaryTexture, other.secondaryTexture);
    }

    @Override
    public int hashCode() {
        int code = type.hashCode();
        code = code * 3 + size;
        code = code * 3 + (addBorder ? 1 : 0);
        code = code * 3 + Objects.hashCode(primaryTexture);
        code = code * 3 + Objects.hashCode(secondaryTexture);
        return code;
    }
}
