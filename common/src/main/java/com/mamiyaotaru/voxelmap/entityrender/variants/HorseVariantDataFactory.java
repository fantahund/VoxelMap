package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;

import java.util.Map;

public class HorseVariantDataFactory extends EntityVariantDataFactory {
    private static final Identifier INVISIBLE_TEXTURE = Identifier.withDefaultNamespace("invisible");
    private static final Map<Markings, Identifier> LOCATION_BY_MARKINGS = Maps.newEnumMap(
            Map.ofEntries(
                    Map.entry(Markings.NONE, INVISIBLE_TEXTURE),
                    Map.entry(Markings.WHITE, Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_white.png")),
                    Map.entry(Markings.WHITE_FIELD, Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitefield.png")),
                    Map.entry(Markings.WHITE_DOTS, Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_whitedots.png")),
                    Map.entry(Markings.BLACK_DOTS, Identifier.withDefaultNamespace("textures/entity/horse/horse_markings_blackdots.png"))
            )
    );

    public HorseVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(renderer, state);
        Identifier overlay0 = LOCATION_BY_MARKINGS.get(((Horse) entity).getMarkings());
        return new EntityVariantData(getType(), id, baseTexture, 0xFFFFFFFF, overlay0 == INVISIBLE_TEXTURE ? null : overlay0, 0xFFFFFFFF, size, addBorder);
    }
}
