package com.mamiyaotaru.voxelmap.entityrender;


import java.util.Objects;
import net.minecraft.resources.Identifier;

public class VariantDataHolder {
    private final String name;
    private final Identifier tex0;
    private final int col0;
    private final Identifier tex1;
    private final int col1;
    private final Identifier tex2;
    private final int col2;
    private final Identifier tex3;
    private final int col3;

    public VariantDataHolder(String name, Identifier tex0, int col0, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3) {
        this.name = name;
        this.tex0 = tex0;
        this.col0 = col0;
        this.tex1 = tex1;
        this.col1 = col1;
        this.tex2 = tex2;
        this.col2 = col2;
        this.tex3 = tex3;
        this.col3 = col3;
    }

    public String name() {
        return name;
    }

    public Identifier tex0() {
        return tex0;
    }

    public int col0() {
        return col0;
    }

    public Identifier tex1() {
        return tex1;
    }

    public int col1() {
        return col1;
    }

    public Identifier tex2() {
        return tex2;
    }

    public int col2() {
        return col2;
    }

    public Identifier tex3() {
        return tex3;
    }

    public int col3() {
        return col3;
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
                && Objects.equals(tex0, other.tex0)
                && col0 == other.col0
                && Objects.equals(tex1, other.tex1)
                && col1 == other.col1
                && Objects.equals(tex2, other.tex2)
                && col2 == other.col2
                && Objects.equals(tex3, other.tex3)
                && col3 == other.col3;
    }

    @Override
    public int hashCode() {
        int code = Objects.hashCode(name);
        code = code * 31 + Objects.hashCode(tex0);
        code = code * 31 + col0;
        code = code * 31 + Objects.hashCode(tex1);
        code = code * 31 + col1;
        code = code * 31 + Objects.hashCode(tex2);
        code = code * 31 + col2;
        code = code * 31 + Objects.hashCode(tex3);
        code = code * 31 + col3;
        return code;
    }
}
