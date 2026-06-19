package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.TropicalFish;

import java.util.EnumMap;
import java.util.Map;

public class TropicalFishVariantDataFactory extends EntityVariantDataFactory {
    private static final EnumMap<TropicalFish.Pattern, Identifier> PATTERN_TEXTURES = Maps.newEnumMap(
            Map.ofEntries(
                    Map.entry(TropicalFish.Pattern.KOB, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_1.png")),
                    Map.entry(TropicalFish.Pattern.SUNSTREAK, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_2.png")),
                    Map.entry(TropicalFish.Pattern.SNOOPER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_3.png")),
                    Map.entry(TropicalFish.Pattern.DASHER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_4.png")),
                    Map.entry(TropicalFish.Pattern.BRINELY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_5.png")),
                    Map.entry(TropicalFish.Pattern.SPOTTY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_a_pattern_6.png")),
                    Map.entry(TropicalFish.Pattern.FLOPPER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_1.png")),
                    Map.entry(TropicalFish.Pattern.STRIPEY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_2.png")),
                    Map.entry(TropicalFish.Pattern.GLITTER, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_3.png")),
                    Map.entry(TropicalFish.Pattern.BLOCKFISH, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_4.png")),
                    Map.entry(TropicalFish.Pattern.BETTY, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_5.png")),
                    Map.entry(TropicalFish.Pattern.CLAYFISH, Identifier.withDefaultNamespace("textures/entity/fish/tropical_b_pattern_6.png"))
            )
    );

    public TropicalFishVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        Identifier baseTexture = getBaseTexture(renderer, state);
        int baseColor = ((TropicalFish) entity).getBaseColor().getMapColor().col | 0xFF000000;
        Identifier overlay0 = PATTERN_TEXTURES.get(((TropicalFish) entity).getPattern());
        int color0 = ((TropicalFish) entity).getPatternColor().getMapColor().col | 0xFF000000;
        return new EntityVariantData(getType(), id, baseTexture, baseColor, overlay0, color0, size, addBorder);
    }
}
