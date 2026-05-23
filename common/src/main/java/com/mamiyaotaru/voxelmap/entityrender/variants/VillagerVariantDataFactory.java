package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

import java.util.Map;
import java.util.Optional;

public class VillagerVariantDataFactory extends EntityVariantDataFactory {
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

    @SuppressWarnings("rawtypes")
    @Override
    public EntityVariantData create(Entity entity, EntityRenderer renderer, EntityRenderState state, String id, int size, boolean addBorder) {
        VillagerDataHolder villager = (VillagerDataHolder) entity;
        Identifier baseTexture = getBaseTexture(renderer, state);
        Identifier overlay0 = null;
        Identifier overlay1 = null;

        Optional<ResourceKey<VillagerType>> typeOptional = villager.getVillagerData().type().unwrapKey();
        if (typeOptional.isPresent()) {
            overlay0 = TYPE_TEXTURES.get(typeOptional.get());
        }

        if (!((LivingEntity) entity).isBaby()) {
            Optional<ResourceKey<VillagerProfession>> professionOptional = villager.getVillagerData().profession().unwrapKey();
            if (professionOptional.isPresent()) {
                overlay1 = PROFESSION_TEXTURES.get(professionOptional.get());
            }
        }

        return new EntityVariantData(getType(), id, baseTexture, 0xFFFFFFFF, overlay0, 0xFFFFFFFF, overlay1 == INVISIBLE_TEXTURE ? null : overlay1, 0xFFFFFFFF, size, addBorder);
    }
}
