package com.mamiyaotaru.voxelmap.entityrender;

import net.minecraft.resources.Identifier;

import java.util.Objects;

public class VariantDataHolder {
    private final String name;
    private final Identifier texture0;
    private final int color0;
    private final Identifier texture1;
    private final int color1;
    private final Identifier texture2;
    private final int color2;
    private final Identifier texture3;
    private final int color3;

    public VariantDataHolder(String name, Identifier texture0, int color0, Identifier texture1, int color1, Identifier texture2, int color2, Identifier texture3, int color3) {
        this.name = name;
        this.texture0 = texture0;
        this.color0 = color0;
        this.texture1 = texture1;
        this.color1 = color1;
        this.texture2 = texture2;
        this.color2 = color2;
        this.texture3 = texture3;
        this.color3 = color3;
    }

    public String getName() {
        return name;
    }

    public Identifier getTexture0() {
        return texture0;
    }

    public int getColor0() {
        return color0;
    }

    public Identifier getTexture1() {
        return texture1;
    }

    public int getColor1() {
        return color1;
    }

    public Identifier getTexture2() {
        return texture2;
    }

    public int getColor2() {
        return color2;
    }

    public Identifier getTexture3() {
        return texture3;
    }

    public int getColor3() {
        return color3;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        VariantDataHolder other = (VariantDataHolder) obj;
        return name.equals(other.name)
                && Objects.equals(texture0, other.texture0)
                && color0 == other.color0
                && Objects.equals(texture1, other.texture1)
                && color1 == other.color1
                && Objects.equals(texture2, other.texture2)
                && color2 == other.color2
                && Objects.equals(texture3, other.texture3)
                && color3 == other.color3;
    }

    @Override
    public int hashCode() {
        int code = Objects.hashCode(name);
        code = code * 31 + Objects.hashCode(texture0);
        code = code * 31 + color0;
        code = code * 31 + Objects.hashCode(texture1);
        code = code * 31 + color1;
        code = code * 31 + Objects.hashCode(texture2);
        code = code * 31 + color2;
        code = code * 31 + Objects.hashCode(texture3);
        code = code * 31 + color3;
        return code;
    }
}
