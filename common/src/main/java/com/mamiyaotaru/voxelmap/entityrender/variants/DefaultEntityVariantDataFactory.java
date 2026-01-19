package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.EntityVariantDataFactory;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class DefaultEntityVariantDataFactory implements EntityVariantDataFactory {
    private final EntityType<?> type;
    private final Identifier secondaryTexture;

    public DefaultEntityVariantDataFactory(EntityType<?> type) {
        this(type, null);
    }

    public DefaultEntityVariantDataFactory(EntityType<?> type, Identifier secondaryTexture) {
        this.type = type;
        this.secondaryTexture = secondaryTexture;
    }

    @Override
    public EntityType<?> getType() {
        return type;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, String identifier, int size, boolean addBorder) {
        return new DefaultEntityVariantData(type, ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state), secondaryTexture, identifier, size, addBorder);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static EntityVariantData createSimpleVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, String identifier, int size, boolean addBorder) {
        return new DefaultEntityVariantData(entity.getType(), ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state), null, identifier, size, addBorder);
    }

}
