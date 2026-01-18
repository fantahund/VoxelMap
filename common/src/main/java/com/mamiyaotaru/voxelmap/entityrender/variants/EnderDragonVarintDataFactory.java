package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EnderDragonVarintDataFactory extends DefaultEntityVariantDataFactory {
    private static final Identifier DRAGON_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    public EnderDragonVarintDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings({ "rawtypes" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder) {
        return new DefaultEntityVariantData(getType(), DRAGON_TEXTURE, null, size, addBorder);
    }
}
