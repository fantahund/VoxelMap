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
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

import java.util.Map;
import java.util.Optional;

public class VillagerVariantDataFactory extends DefaultEntityVariantDataFactory {
    private static final Identifier INVISIBLE_TEXTURE = Identifier.withDefaultNamespace("invisible");

    private static final Map<ResourceKey<VillagerType>, Identifier> TYPE_TEXTURES = Maps.newHashMap(
            Map.ofEntries(
                    Map.entry(VillagerType.DESERT, Identifier.withDefaultNamespace("textures/entity/villager/type/desert.png")),
                    Map.entry(VillagerType.JUNGLE, Identifier.withDefaultNamespace("textures/entity/villager/type/jungle.png")),
                    Map.entry(VillagerType.PLAINS, Identifier.withDefaultNamespace("textures/entity/villager/type/plains.png")),
                    Map.entry(VillagerType.SAVANNA, Identifier.withDefaultNamespace("textures/entity/villager/type/savanna.png")),
                    Map.entry(VillagerType.SNOW, Identifier.withDefaultNamespace("textures/entity/villager/type/snow.png")),
                    Map.entry(VillagerType.SWAMP, Identifier.withDefaultNamespace("textures/entity/villager/type/swamp.png")),
                    Map.entry(VillagerType.TAIGA, Identifier.withDefaultNamespace("textures/entity/villager/type/taiga.png"))
            )
    );

    private static final Map<ResourceKey<VillagerProfession>, Identifier> PROFESSION_TEXTURES = Maps.newHashMap(
            Map.ofEntries(
                    Map.entry(VillagerProfession.NONE, INVISIBLE_TEXTURE),
                    Map.entry(VillagerProfession.ARMORER, Identifier.withDefaultNamespace("textures/entity/villager/profession/armorer.png")),
                    Map.entry(VillagerProfession.BUTCHER, Identifier.withDefaultNamespace("textures/entity/villager/profession/butcher.png")),
                    Map.entry(VillagerProfession.CARTOGRAPHER, Identifier.withDefaultNamespace("textures/entity/villager/profession/cartographer.png")),
                    Map.entry(VillagerProfession.CLERIC, Identifier.withDefaultNamespace("textures/entity/villager/profession/cleric.png")),
                    Map.entry(VillagerProfession.FARMER, Identifier.withDefaultNamespace("textures/entity/villager/profession/farmer.png")),
                    Map.entry(VillagerProfession.FISHERMAN, Identifier.withDefaultNamespace("textures/entity/villager/profession/fisherman.png")),
                    Map.entry(VillagerProfession.FLETCHER, Identifier.withDefaultNamespace("textures/entity/villager/profession/fletcher.png")),
                    Map.entry(VillagerProfession.LEATHERWORKER, Identifier.withDefaultNamespace("textures/entity/villager/profession/leatherworker.png")),
                    Map.entry(VillagerProfession.LIBRARIAN, Identifier.withDefaultNamespace("textures/entity/villager/profession/librarian.png")),
                    Map.entry(VillagerProfession.MASON, Identifier.withDefaultNamespace("textures/entity/villager/profession/mason.png")),
                    Map.entry(VillagerProfession.NITWIT, Identifier.withDefaultNamespace("textures/entity/villager/profession/nitwit.png")),
                    Map.entry(VillagerProfession.SHEPHERD, Identifier.withDefaultNamespace("textures/entity/villager/profession/shepherd.png")),
                    Map.entry(VillagerProfession.TOOLSMITH, Identifier.withDefaultNamespace("textures/entity/villager/profession/toolsmith.png")),
                    Map.entry(VillagerProfession.WEAPONSMITH, Identifier.withDefaultNamespace("textures/entity/villager/profession/weaponsmith.png"))
            )
    );

    public VillagerVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int identifier, int size, boolean addBorder) {
        VillagerDataHolder villager = (VillagerDataHolder) entity;
        Identifier primaryTexture = ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state);
        Identifier secondaryTexture = null;
        Identifier tertiaryTexture = null;

        Optional<ResourceKey<VillagerType>> typeOptional = villager.getVillagerData().type().unwrapKey();
        if (typeOptional.isPresent()) {
            secondaryTexture = TYPE_TEXTURES.get(typeOptional.get());
        }

        Optional<ResourceKey<VillagerProfession>> professionOptional = villager.getVillagerData().profession().unwrapKey();
        if (professionOptional.isPresent()) {
            tertiaryTexture = PROFESSION_TEXTURES.get(professionOptional.get());
        }

        return new DefaultEntityVariantData(getType(), identifier, size, addBorder, primaryTexture, secondaryTexture, tertiaryTexture == INVISIBLE_TEXTURE ? null : tertiaryTexture, null);
    }

}
