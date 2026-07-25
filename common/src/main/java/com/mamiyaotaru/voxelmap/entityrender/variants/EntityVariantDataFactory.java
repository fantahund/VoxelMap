package com.mamiyaotaru.voxelmap.entityrender.variants;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityVariantDataFactory {
    private final EntityType<?> type;
    private final Identifier tex1;
    private final int col1;
    private final Identifier tex2;
    private final int col2;
    private final Identifier tex3;
    private final int col3;

    public EntityVariantDataFactory(EntityType<?> type) {
        this(type, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier tex1, int col1) {
        this(type, tex1, col1, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier tex1, int col1, Identifier tex2, int col2) {
        this(type, tex1, col1, tex2, col2, null, -1);
    }

    public EntityVariantDataFactory(EntityType<?> type, Identifier tex1, int col1, Identifier tex2, int col2, Identifier tex3, int col3) {
        this.type = type;
        this.tex1 = tex1;
        this.col1 = col1;
        this.tex2 = tex2;
        this.col2 = col2;
        this.tex3 = tex3;
        this.col3 = col3;
    }

    public EntityType<?> type() {
        return type;
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

    @SuppressWarnings("rawtypes")
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(renderer, state);
        return new EntityVariantData(entity.getType(), id, tex0, 0xFFFFFFFF, tex1, col1, tex2, col2, tex3, col3, size, addBorder);
    }

    @SuppressWarnings("rawtypes")
    public static EntityVariantData createSimple(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier tex0 = loadBaseTexture(renderer, state);
        return new EntityVariantData(entity.getType(), id, tex0, 0xFFFFFFFF, size, addBorder);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Identifier loadBaseTexture(EntityRenderer renderer, EntityRenderState state) {
        return ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
    }
}
