package com.mamiyaotaru.voxelmap.entityrender.variants;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EnderDragonVariantDataFactory extends EntityVariantDataFactory {
    private static final Identifier DRAGON_TEXTURE = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    public EnderDragonVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        return new EntityVariantData(getType(), id, DRAGON_TEXTURE, 0xFFFFFFFF, size, addBorder);
    }
}
