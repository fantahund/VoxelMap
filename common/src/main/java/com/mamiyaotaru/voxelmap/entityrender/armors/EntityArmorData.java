package com.mamiyaotaru.voxelmap.entityrender.armors;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Objects;

public class EntityArmorData {
    private final Item item;
    private final Identifier texture;
    private final int size;
    private final boolean addOutline;

    public EntityArmorData(Item item, Identifier texture, int size, boolean addOutline) {
        this.item = item;
        this.texture = texture;
        this.size = size;
        this.addOutline = addOutline;
    }

    public Item getItem() {
        return item;
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
        return Objects.equals(item, other.item) && size == other.size && addOutline == other.addOutline && Objects.equals(texture, other.texture);
    }

    @Override
    public int hashCode() {
        int code = Objects.hashCode(item);
        code = code * 3 + size;
        code = code * 3 + (addOutline ? 1 : 0);
        code = code * 3 + Objects.hashCode(texture);
        return code;
    }
}
