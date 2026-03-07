package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.google.common.collect.Maps;
import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class TropicalFishVariantDataFactory extends DefaultEntityVariantDataFactory {
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int identifier, int size, boolean addBorder) {
        TropicalFish fish = (TropicalFish) entity;
        Identifier primaryTexture = ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
        Identifier secondaryTexture = PATTERN_TEXTURES.get(fish.getPattern());

        return new DefaultEntityVariantData(getType(), identifier, size, addBorder, primaryTexture, secondaryTexture, null, null);
    }

}
