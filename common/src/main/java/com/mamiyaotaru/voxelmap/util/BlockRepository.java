package com.mamiyaotaru.voxelmap.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockRepository {
    public static Block air = Blocks.AIR;
    public static Block voidAir;
    public static Block caveAir;
    public static int airID;
    public static int voidAirID;
    public static int caveAirID;
    public static MovingPistonBlock pistonTechBlock;
    public static Block water;
    public static Block lava;
    public static Block ice;
    public static Block grassBlock;
    public static Block oakLeaves;
    public static Block spruceLeaves;
    public static Block birchLeaves;
    public static Block jungleLeaves;
    public static Block acaciaLeaves;
    public static Block darkOakLeaves;
    public static Block mangroveLeaves;
    public static Block grass;
    public static Block fern;
    public static Block tallGrass;
    public static Block largeFern;
    public static Block reeds;
    public static Block vine;
    public static Block lilypad;
    public static Block tallFlower;
    public static Block cobweb;
    public static Block stickyPiston;
    public static Block piston;
    public static Block redstone;
    public static Block ladder;
    public static Block barrier;
    public static Block chorusPlant;
    public static Block chorusFlower;
    public static Block leafLitter;
    public static HashSet<Block> biomeBlocks;
    public static Block[] biomeBlocksArray;
    public static HashSet<Block> shapedBlocks;
    public static Block[] shapedBlocksArray;
    private static final ConcurrentHashMap<BlockState, Integer> stateToInt = new ConcurrentHashMap<>(1024);
    private static final ReferenceArrayList<BlockState> blockStates = new ReferenceArrayList<>(16384);
    private static int count = 1;
    private static final ReadWriteLock incrementLock = new ReentrantReadWriteLock();

    public static void getBlocks() {
        air = Blocks.AIR;
        airID = getStateId(air.defaultBlockState());
        voidAir = Blocks.VOID_AIR;
        voidAirID = getStateId(voidAir.defaultBlockState());
        caveAir = Blocks.CAVE_AIR;
        caveAirID = getStateId(caveAir.defaultBlockState());
        pistonTechBlock = (MovingPistonBlock) Blocks.MOVING_PISTON;
        water = Blocks.WATER;
        lava = Blocks.LAVA;
        ice = Blocks.ICE;
        grassBlock = Blocks.GRASS_BLOCK;
        oakLeaves = Blocks.OAK_LEAVES;
        spruceLeaves = Blocks.SPRUCE_LEAVES;
        birchLeaves = Blocks.BIRCH_LEAVES;
        jungleLeaves = Blocks.JUNGLE_LEAVES;
        acaciaLeaves = Blocks.ACACIA_LEAVES;
        darkOakLeaves = Blocks.DARK_OAK_LEAVES;
        mangroveLeaves = Blocks.MANGROVE_LEAVES;
        grass = Blocks.SHORT_GRASS;
        fern = Blocks.FERN;
        tallGrass = Blocks.TALL_GRASS;
        largeFern = Blocks.LARGE_FERN;
        reeds = Blocks.SUGAR_CANE;
        vine = Blocks.VINE;
        lilypad = Blocks.LILY_PAD;
        cobweb = Blocks.COBWEB;
        stickyPiston = Blocks.STICKY_PISTON;
        piston = Blocks.PISTON;
        redstone = Blocks.REDSTONE_WIRE;
        ladder = Blocks.LADDER;
        barrier = Blocks.BARRIER;
        chorusPlant = Blocks.CHORUS_PLANT;
        chorusFlower = Blocks.CHORUS_FLOWER;
        leafLitter = Blocks.LEAF_LITTER;
        biomeBlocksArray = new Block[]{grassBlock, oakLeaves, spruceLeaves, birchLeaves, jungleLeaves, acaciaLeaves, darkOakLeaves, mangroveLeaves, grass, fern, tallGrass, largeFern, reeds, vine, lilypad, tallFlower, water, leafLitter};
        biomeBlocks = new HashSet<>(Arrays.asList(biomeBlocksArray));
        shapedBlocksArray = new Block[]{ladder, vine};
        shapedBlocks = new HashSet<>(Arrays.asList(shapedBlocksArray));

        for (Block block : BuiltInRegistries.BLOCK) {
            switch (block) {
                case DoorBlock doorBlock -> shapedBlocks.add(block);
                case SignBlock signBlock -> shapedBlocks.add(block);
                default -> {}
            }

        }

    }

    public static int getStateId(BlockState blockState) {
        Integer id = stateToInt.get(blockState);
        if (id == null) {
            synchronized (incrementLock) {
                id = stateToInt.get(blockState);
                if (id == null) {
                    id = count;
                    blockStates.add(blockState);
                    stateToInt.put(blockState, id);
                    ++count;
                }
            }
        }

        return id;
    }

    public static BlockState getStateById(int id) {
        return blockStates.get(id);
    }

    static {
        BlockState airBlockState = Blocks.AIR.defaultBlockState();
        stateToInt.put(airBlockState, 0);
        blockStates.add(airBlockState);
    }
}
