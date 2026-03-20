package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Objects;

public class DefaultEntityVariantData implements EntityVariantData {
    private final EntityType<?> type;
    private final int identifier;
    private final int size;
    private final boolean addBorder;
    private final Identifier primaryTexture;
    private final Identifier secondaryTexture;
    private final Identifier tertiaryTexture;
    private final Identifier quaternaryTexture;

    public DefaultEntityVariantData(EntityType<?> type, int identifier, int size, boolean addBorder, Identifier primaryTexture, Identifier secondaryTexture, Identifier tertiaryTexture, Identifier quaternaryTexture) {
        this.type = type;
        this.identifier = identifier;
        this.size = size;
        this.addBorder = addBorder;
        this.primaryTexture = primaryTexture;
        this.secondaryTexture = secondaryTexture;
        this.tertiaryTexture = tertiaryTexture;
        this.quaternaryTexture = quaternaryTexture;
    }

    @Override
    public EntityType<?> getType() {
        return type;
    }

    @Override
    public int getIdentifier() {
        return identifier;
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
    public Identifier getTertiaryTexture() {
        return tertiaryTexture;
    }

    @Override
    public Identifier getQuaternaryTexture() {
        return quaternaryTexture;
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
        return type == other.type
                && identifier == other.identifier
                && size == other.size
                && addBorder == other.addBorder
                && Objects.equals(primaryTexture, other.primaryTexture)
                && Objects.equals(secondaryTexture, other.secondaryTexture)
                && Objects.equals(tertiaryTexture, other.tertiaryTexture)
                && Objects.equals(quaternaryTexture, other.quaternaryTexture);
    }

    @Override
    public int hashCode() {
        int code = type.hashCode();
        code = code * 31 + identifier;
        code = code * 31 + size;
        code = code * 31 + (addBorder ? 1 : 0);
        code = code * 31 + Objects.hashCode(primaryTexture);
        code = code * 31 + Objects.hashCode(secondaryTexture);
        code = code * 31 + Objects.hashCode(tertiaryTexture);
        code = code * 31 + Objects.hashCode(quaternaryTexture);
        return code;
    }
}
