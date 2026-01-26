package com.mamiyaotaru.voxelmap.entityrender.variants;

import com.mamiyaotaru.voxelmap.entityrender.EntityVariantData;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Optional;

public class VillagerVariantDataFactory extends DefaultEntityVariantDataFactory {

    public VillagerVariantDataFactory(EntityType<?> type) {
        super(type);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public EntityVariantData createVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, String identifier, int size, boolean addBorder) {
        Optional<ResourceKey<VillagerProfession>> optProfession = Optional.empty();
        if (entity instanceof Villager villager) {
            optProfession = villager.getVillagerData().profession().unwrapKey();
        } else if (entity instanceof ZombieVillager zombieVillager) {
            optProfession = zombieVillager.getVillagerData().profession().unwrapKey();
        }

        Identifier secondaryTexture = null;
        if (optProfession.isPresent()) {
            ResourceKey<VillagerProfession> profession = optProfession.get();
            if (profession != VillagerProfession.NONE) {
                Identifier professionId = profession.identifier();
                secondaryTexture = Identifier.fromNamespaceAndPath(professionId.getNamespace(), "textures/entity/villager/profession/" + professionId.getPath() + ".png");
            }
        }

        return new DefaultEntityVariantData(getType(), ((LivingEntityRenderer) renderer).getTextureLocation((LivingEntityRenderState) state), secondaryTexture, identifier, size, addBorder);
    }

}
