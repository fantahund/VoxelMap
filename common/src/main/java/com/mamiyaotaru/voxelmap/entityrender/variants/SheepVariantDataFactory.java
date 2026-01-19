package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;

public class SheepVariantDataFactory extends DefaultEntityVariantDataFactory {
    public SheepVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder) {
        Sheep sheep = (Sheep) entity;

        // it's a simple trick to create different variant data for each sheep.
        String location = sheep.isSheared() ? "sheared" : Integer.toString(sheep.getColor().getTextureDiffuseColor());
        Identifier variantId = Identifier.fromNamespaceAndPath("voxelmap", "entityvariant/sheep/" + location);

        return new DefaultEntityVariantData(getType(), variantId, ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state), size, addBorder);
    }
}
