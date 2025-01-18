package com.mamiyaotaru.voxelmap.util;

import java.util.Arrays;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

public enum EnumMobs {
    GENERICHOSTILE(null, "Monster", false, 8.0F, "textures/entity/zombie/zombie.png", "", true, false),
    GENERICNEUTRAL(null, "Mob", false, 8.0F, "textures/entity/pig/pig.png", "", false, true),
    GENERICTAME(null, "Unknown_Tame", false, 8.0F, "textures/entity/wolf/wolf.png", "", false, true),
    AXOLOTL(Axolotl.class, "Axolotl", true, 0.0F, "textures/entity/axolotl/axolotl_blue.png", "", false, true),
    ARMADILLO(Armadillo.class, "Armadillo", true, 0.0F, "textures/entity/armadillo/armadillo.png", "", false, true),
    ALLAY(Allay.class, "Allay", true, 4.0F, "textures/entity/allay/allay.png", "", false, true),
    ARMOR_STAND(ArmorStand.class, "Armor_Stand", true, 0.0F, "textures/entity/armorstand/wood.png", "", true, true),
    BAT(Bat.class, "Bat", true, 4.0F, "textures/entity/bat.png", "", false, true),
    BEE(Bee.class, "Bee", true, 0.0F, "textures/entity/bee/bee.png", "", true, true),
    BLAZE(Blaze.class, "Blaze", true, 0.0F, "textures/entity/blaze.png", "", true, false),
    BOGGED(Bogged.class, "Bogged", true, 0.0F, "textures/entity/skeleton/bogged.png", "textures/entity/skeleton/bogged_overlay.png", true, false),
    BREEZE(Breeze.class, "Breeze", true, 0.0F, "textures/entity/breeze.png", "", true, false),
    CAT(Cat.class, "Cat", true, 0.0F, "textures/entity/cat/siamese.png", "", false, true),
    CAMEL(Camel.class, "Camel", true, 0.0F, "textures/entity/camel/camel.png", "", false, true),
    CAVESPIDER(CaveSpider.class, "Cave_Spider", true, 0.0F, "textures/entity/spider/cave_spider.png", "", true, false),
    CHICKEN(Chicken.class, "Chicken", true, 6.0F, "textures/entity/chicken.png", "", false, true),
    COD(Cod.class, "Cod", true, 8.0F, "textures/entity/fish/cod.png", "", false, true),
    COW(Cow.class, "Cow", true, 0.0F, "textures/entity/cow/cow.png", "", false, true),
    CREAKING(Creaking.class, "Creaking", true, 0.0F, "textures/entity/creaking/creaking.png", "textures/entity/creaking/creaking_eyes.png", true, false),
    CREEPER(Creeper.class, "Creeper", true, 0.0F, "textures/entity/creeper/creeper.png", "", true, false),
    DOLPHIN(Dolphin.class, "Dolphin", true, 0.0F, "textures/entity/dolphin.png", "", false, true),
    DROWNED(Drowned.class, "Drowned", true, 0.0F, "textures/entity/zombie/drowned.png", "textures/entity/zombie/drowned_outer_layer.png", true, false),
    ENDERDRAGON(EnderDragon.class, "Ender_Dragon", true, 16.0F, "textures/entity/enderdragon/dragon.png", "", true, false),
    ENDERMAN(EnderMan.class, "Enderman", true, 0.0F, "textures/entity/enderman/enderman.png", "textures/entity/enderman/enderman_eyes.png", true, false),
    ENDERMITE(Endermite.class, "Endermite", true, 0.0F, "textures/entity/endermite.png", "", true, false),
    EVOKER(Evoker.class, "Evoker", true, 0.0F, "textures/entity/illager/evoker.png", "", true, false),
    FOX(Fox.class, "Fox", true, 0.0F, "textures/entity/fox/fox.png", "", false, true),
    FROG(Frog.class, "Frog", true, 0.0F, "textures/entity/frog/cold_frog.png", "", false, true),
    GHAST(Ghast.class, "Ghast", true, 16.0F, "textures/entity/ghast/ghast.png", "", true, false),
    GHASTATTACKING(null, "Ghast", false, 16.0F, "textures/entity/ghast/ghast_shooting.png", "", true, false),
    GLOWSQUID(GlowSquid.class, "Glow_Squid", true, 0.0F, "textures/entity/squid/glow_squid.png", "", false, true),
    GOAT(Goat.class, "Goat", true, 0.0F, "textures/entity/goat/goat.png", "", false, true),
    GUARDIAN(Guardian.class, "Guardian", true, 6.0F, "textures/entity/guardian.png", "", true, false),
    GUARDIANELDER(ElderGuardian.class, "Elder_Guardian", true, 12.0F, "textures/entity/guardian_elder.png", "", true, false),
    HOGLIN(Hoglin.class, "Hoglin", true, 0.0F, "textures/entity/hoglin/hoglin.png", "", true, false),
    HORSE(Horse.class, "Horse", true, 8.0F, "textures/entity/horse/horse_creamy.png", "textures/entity/horse/horse_markings_white.png", false, true),
    HUSK(Husk.class, "Husk", true, 0.0F, "textures/entity/zombie/husk.png", "", true, false),
    ILLUSIONER(Illusioner.class, "Illusioner", true, 0.0F, "textures/entity/illager/illusioner.png", "", true, false),
    IRONGOLEM(IronGolem.class, "Iron_Golem", true, 8.0F, "textures/entity/iron_golem/iron_golem.png", "", false, true),
    LLAMA(Llama.class, "Llama", true, 8.0F, "textures/entity/llama/brown.png", "", false, true),
    LLAMATRADER(TraderLlama.class, "Trader_Llama", true, 8.0F, "textures/entity/llama/brown.png", "", false, true),
    MAGMA(MagmaCube.class, "Magma_Cube", true, 8.0F, "textures/entity/slime/magmacube.png", "", true, false),
    MOOSHROOM(MushroomCow.class, "Mooshroom", true, 40.0F, "textures/entity/cow/red_mooshroom.png", "", false, true),
    OCELOT(Ocelot.class, "Ocelot", true, 0.0F, "textures/entity/cat/ocelot.png", "", false, true),
    PANDA(Panda.class, "Panda", true, 0.0F, "textures/entity/panda/panda.png", "", true, true),
    PARROT(Parrot.class, "Parrot", true, 8.0F, "textures/entity/parrot/parrot_red_blue.png", "", false, true),
    PHANTOM(Phantom.class, "Phantom", true, 10.0F, "textures/entity/phantom.png", "textures/entity/phantom_eyes.png", true, false),
    PIG(Pig.class, "Pig", true, 0.0F, "textures/entity/pig/pig.png", "", false, true),
    PIGLIN(Piglin.class, "Piglin", true, 0.0F, "textures/entity/piglin/piglin.png", "", true, false),
    PIGLINBRUTE(PiglinBrute.class, "Piglin_Brute", true, 0.0F, "textures/entity/piglin/piglin_brute.png", "", true, false),
    PIGLINZOMBIE(ZombifiedPiglin.class, "Zombified_Piglin", true, 0.0F, "textures/entity/piglin/zombified_piglin.png", "", true, true),
    PILLAGER(Pillager.class, "Pillager", true, 0.0F, "textures/entity/illager/pillager.png", "", true, false),
    PLAYER(RemotePlayer.class, "Player", false, 8.0F, "textures/entity/steve.png", "", false, false),
    POLARBEAR(PolarBear.class, "Polar_Bear", true, 0.0F, "textures/entity/bear/polarbear.png", "", true, true),
    PUFFERFISH(Pufferfish.class, "Pufferfish", true, 3.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    PUFFERFISHHALF(null, "Pufferfish_Half", false, 5.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    PUFFERFISHFULL(null, "Pufferfish_Full", false, 8.0F, "textures/entity/fish/pufferfish.png", "", false, true),
    RABBIT(Rabbit.class, "Rabbit", true, 0.0F, "textures/entity/rabbit/salt.png", "", false, true),
    RAVAGER(Ravager.class, "Ravager", true, 0.0F, "textures/entity/illager/ravager.png", "", true, false),
    SALMON(Salmon.class, "Salmon", true, 13.0F, "textures/entity/fish/salmon.png", "", false, true),
    SHEEP(Sheep.class, "Sheep", true, 0.0F, "textures/entity/sheep/sheep.png", "", false, true),
    SHULKER(Shulker.class, "Shulker", true, 0.0F, "textures/entity/shulker/shulker_purple.png", "", true, false),
    SILVERFISH(Silverfish.class, "Silverfish", true, 0.0F, "textures/entity/silverfish.png", "", true, false),
    SKELETON(Skeleton.class, "Skeleton", true, 0.0F, "textures/entity/skeleton/skeleton.png", "", true, false),
    SKELETONWITHER(WitherSkeleton.class, "Wither_Skeleton", true, 0.0F, "textures/entity/skeleton/wither_skeleton.png", "", true, false),
    SLIME(Slime.class, "Slime", true, 8.0F, "textures/entity/slime/slime.png", "", true, false),
    SNIFFER(Sniffer.class, "Sniffer", true, 0.0F, "textures/entity/sniffer/sniffer.png", "", false, true),
    SNOWGOLEM(SnowGolem.class, "Snow_Golem", true, 0.0F, "textures/entity/snow_golem.png", "", false, true),
    SPIDER(Spider.class, "Spider", true, 0.0F, "textures/entity/spider/spider.png", "", true, false),
    SQUID(Squid.class, "Squid", true, 0.0F, "textures/entity/squid/squid.png", "", false, true),
    STRAY(Stray.class, "Stray", true, 0.0F, "textures/entity/skeleton/stray.png", "textures/entity/skeleton/stray_overlay.png", true, false),
    STRIDER(Strider.class, "Strider", true, 0.0F, "textures/entity/strider/strider.png", "", false, true),
    TADPOLE(Tadpole.class, "Tadpole", true, 0.0F, "textures/entity/tadpole/tadpole.png", "", false, true),
    TROPICALFISHA(TropicalFish.class, "Tropical_Fish", true, 5.0F, "textures/entity/fish/tropical_a.png", "textures/entity/fish/tropical_a_pattern_1.png", false, true),
    TROPICALFISHB(null, "Tropical_Fish", false, 6.0F, "textures/entity/fish/tropical_b.png", "textures/entity/fish/tropical_b_pattern_4.png", false, true),
    TURTLE(Turtle.class, "Turtle", true, 0.0F, "textures/entity/turtle/big_sea_turtle.png", "", false, true),
    VEX(Vex.class, "Vex", true, 0.0F, "textures/entity/illager/vex.png", "", true, false),
    VEXCHARGING(null, "Vex", false, 0.0F, "textures/entity/illager/vex_charging.png", "", true, false),
    VILLAGER(Villager.class, "Villager", true, 0.0F, "textures/entity/villager/villager.png", "textures/entity/villager/profession/farmer.png", false, true),
    VINDICATOR(Vindicator.class, "Vindicator", true, 0.0F, "textures/entity/illager/vindicator.png", "", true, false),
    WANDERINGTRADER(WanderingTrader.class, "Wandering_Trader", true, 0.0F, "textures/entity/wandering_trader.png", "", false, true),
    WITCH(Witch.class, "Witch", true, 0.0F, "textures/entity/witch.png", "", true, false),
    WITHER(WitherBoss.class, "Wither", true, 24.0F, "textures/entity/wither/wither.png", "", true, false),
    WITHERINVULNERABLE(null, "Wither", false, 24.0F, "textures/entity/wither/wither_invulnerable.png", "", true, false),
    WARDEN(Warden.class, "Warden", true, 0.0F, "textures/entity/warden/warden.png", "", true, false),
    WOLF(Wolf.class, "Wolf", true, 0.0F, "textures/entity/wolf/wolf.png", "", true, true),
    ZOGLIN(Zoglin.class, "Zoglin", true, 0.0F, "textures/entity/hoglin/zoglin.png", "", true, false),
    ZOMBIE(Zombie.class, "Zombie", true, 0.0F, "textures/entity/zombie/zombie.png", "", true, false),
    ZOMBIEVILLAGER(ZombieVillager.class, "Zombie_villager", true, 0.0F, "textures/entity/zombie_villager/zombie_villager.png", "textures/entity/zombie_villager/profession/farmer.png", true, false),
    UNKNOWN(null, "Unknown", false, 8.0F, "/mob/uknown.png", "", true, true);

    public final Class<? extends Entity> clazz;
    public final String id;
    public final boolean isTopLevelUnit;
    public final float expectedWidth;
    public final ResourceLocation resourceLocation;
    public ResourceLocation secondaryResourceLocation;
    public final boolean isHostile;
    public final boolean isNeutral;
    public boolean enabled;

    public static EnumMobs getMobByName(String par0) {
        return Arrays.stream(values()).filter(enumMob -> enumMob.id.equals(par0)).findFirst().orElse(null);
    }

    public static EnumMobs getMobTypeByEntity(Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (clazz.equals(TropicalFish.class)) {
            //return ((TropicalFish) entity).getVariant().getPackedId() == 0 ? TROPICALFISHA : TROPICALFISHB;
            return TROPICALFISHA;
        } else {
            return getMobTypeByClass(clazz);
        }
    }

    private static EnumMobs getMobTypeByClass(Class<? extends Entity> clazz) {
        if (RemotePlayer.class.isAssignableFrom(clazz)) {
            return PLAYER;
        } else if (!clazz.equals(Horse.class) && !clazz.equals(Donkey.class) && !clazz.equals(Mule.class) && !clazz.equals(SkeletonHorse.class) && !clazz.equals(ZombieHorse.class)) {
            return Arrays.stream(values()).filter(enumMob -> clazz.equals(enumMob.clazz)).findFirst().orElse(UNKNOWN);
        } else {
            return HORSE;
        }
    }

    EnumMobs(Class<? extends Entity> clazz, String name, boolean topLevelUnit, float expectedWidth, String path, String secondaryPath, boolean isHostile, boolean isNeutral) {
        this.clazz = clazz;
        this.id = name;
        this.isTopLevelUnit = topLevelUnit;
        this.expectedWidth = expectedWidth;
        this.resourceLocation = ResourceLocation.parse(path.toLowerCase());
        this.secondaryResourceLocation = secondaryPath.isEmpty() ? null : ResourceLocation.parse(secondaryPath.toLowerCase());
        this.isHostile = isHostile;
        this.isNeutral = isNeutral;
        this.enabled = true;
    }

    public int returnEnumOrdinal() {
        return this.ordinal();
    }
}
