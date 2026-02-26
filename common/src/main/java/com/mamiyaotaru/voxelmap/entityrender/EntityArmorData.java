package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public class EntityArmorData {
    private final Identifier material;
    private final Identifier texture;
    private final int size;
    private final boolean addOutline;

    public EntityArmorData(Identifier material, Identifier texture, int size, boolean addOutline) {
        this.material = material;
        this.texture = texture;
        this.size = size;
        this.addOutline = addOutline;
    }

    public Identifier getMaterial() {
        return material;
    }

    public Identifier getTexture() {
        return texture;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        EntityArmorData other = (EntityArmorData) obj;
        return Objects.equals(material, other.material) && size == other.size && addOutline == other.addOutline && Objects.equals(texture, other.texture);
    }

    @Override
    public int hashCode() {
        int code = Objects.hashCode(material);
        code = code * 3 + size;
        code = code * 3 + (addOutline ? 1 : 0);
        code = code * 3 + Objects.hashCode(texture);
        return code;
    }
}
