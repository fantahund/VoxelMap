package com.mamiyaotaru.voxelmap.entityrender.armors;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Objects;

public class EntityArmorData {
    private final Item item;
    private final Identifier texture;
    private final int identifier;
    private final int size;
    private final boolean addOutline;

    public EntityArmorData(Item item, Identifier texture, int identifier, int size, boolean addOutline) {
        this.item = item;
        this.texture = texture;
        this.identifier = identifier;
        this.size = size;
        this.addOutline = addOutline;
    }

    public Item getItem() {
        return item;
    }

    public Identifier getTexture() {
        return texture;
    }

    public int getIdentifier() {
        return identifier;
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
        return Objects.equals(item, other.item) && identifier == other.identifier && size == other.size && addOutline == other.addOutline && Objects.equals(texture, other.texture);
    }

    @Override
    public int hashCode() {
        int code = Objects.hashCode(item);
        code = code * 31 + identifier;
        code = code * 31 + size;
        code = code * 31 + (addOutline ? 1 : 0);
        code = code * 31 + Objects.hashCode(texture);
        return code;
    }
}
