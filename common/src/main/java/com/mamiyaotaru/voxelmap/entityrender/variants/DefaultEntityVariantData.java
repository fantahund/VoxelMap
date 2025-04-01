package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class DefaultEntityVariantData implements EntityVariantData {
    private final EntityType<?> type;
    private final ResourceLocation primaryTexture;
    private final ResourceLocation secondaryTexture;
    private final int size;
    private final boolean addBorder;

    public DefaultEntityVariantData(EntityType<?> type, ResourceLocation primaryTexture, ResourceLocation secondaryTexture, int size, boolean addBorder) {
        this.type = type;
        this.primaryTexture = primaryTexture;
        this.secondaryTexture = secondaryTexture;
        this.size = size;
        this.addBorder = addBorder;
    }

    public EntityType<?> getType() {
        return type;
    }

    public ResourceLocation getPrimaryTexture() {
        return primaryTexture;
    }

    public ResourceLocation getSecondaryTexture() {
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
