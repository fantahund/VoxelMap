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
    private final Identifier tertiaryTexture;
    private final Identifier quaternaryTexture;

    public DefaultEntityVariantDataFactory(EntityType<?> type) {
        this(type, null, null, null);
    }

    public DefaultEntityVariantDataFactory(EntityType<?> type, Identifier secondaryTexture, Identifier tertiaryTexture, Identifier quaternaryTexture) {
        this.type = type;
        this.secondaryTexture = secondaryTexture;
        this.tertiaryTexture = tertiaryTexture;
        this.quaternaryTexture = quaternaryTexture;
    }

    @Override
    public EntityType<?> getType() {
        return type;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int identifier, int size, boolean addBorder) {
        Identifier primaryTexture = ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
        return new DefaultEntityVariantData(type, identifier, size, addBorder, primaryTexture, secondaryTexture, tertiaryTexture, quaternaryTexture);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static EntityVariantData createSimpleVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int identifier, int size, boolean addBorder) {
        Identifier primaryTexture = ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
        return new DefaultEntityVariantData(entity.getType(), identifier, size, addBorder, primaryTexture, null, null, null);
    }

}
