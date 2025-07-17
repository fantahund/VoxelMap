package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Markings;

public class HorseVariantDataFactory extends DefaultEntityVariantDataFactory {
    private static final ResourceLocation INVISIBLE_TEXTURE = ResourceLocation.withDefaultNamespace("invisible");
    private static final Map<Markings, ResourceLocation> LOCATION_BY_MARKINGS = Maps.newEnumMap(
            Map.of(
                    Markings.NONE,
                    INVISIBLE_TEXTURE,
                    Markings.WHITE,
                    ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_white.png"),
                    Markings.WHITE_FIELD,
                    ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png"),
                    Markings.WHITE_DOTS,
                    ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png"),
                    Markings.BLACK_DOTS,
                    ResourceLocation.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png")));

    public HorseVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder) {
        Horse horse = (Horse) entity;
        Markings markings = horse.getMarkings();
        ResourceLocation secondaryTexture = LOCATION_BY_MARKINGS.get(markings);
        return new DefaultEntityVariantData(getType(), ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state), secondaryTexture == INVISIBLE_TEXTURE ? null : secondaryTexture, size, addBorder);
    }

}
