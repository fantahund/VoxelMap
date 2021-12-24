package com.mamiyaotaru.voxelmap.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.block.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.util.registry.Registry;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.PistonExtensionBlock;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;

public class BlockRepository {
   public static Block air;
   public static Block voidAir;
   public static Block caveAir;
   public static int airID = 0;
   public static int voidAirID = 0;
   public static int caveAirID = 0;
   public static PistonExtensionBlock pistonTechBlock;
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
   public static FluidState dry = Fluids.EMPTY.getDefaultState();
   public static HashSet biomeBlocks;
   public static Block[] biomeBlocksArray = new Block[]{
      grassBlock,
      oakLeaves,
      spruceLeaves,
      birchLeaves,
      jungleLeaves,
      acaciaLeaves,
      darkOakLeaves,
      grass,
      fern,
      tallGrass,
      largeFern,
      reeds,
      vine,
      lilypad,
      tallFlower,
      water
   };
   public static HashSet shapedBlocks;
   public static Block[] shapedBlocksArray = new Block[]{ladder, vine};
   private static ConcurrentHashMap stateToInt = new ConcurrentHashMap(1024);
   private static ReferenceArrayList blockStates = new ReferenceArrayList(16384);
   private static int count = 1;
   private static ReadWriteLock incrementLock = new ReentrantReadWriteLock();

   public static void getBlocks() {
      air = Blocks.AIR;
      airID = getStateId(air.getDefaultState());
      voidAir = Blocks.VOID_AIR;
      voidAirID = getStateId(voidAir.getDefaultState());
      caveAir = Blocks.CAVE_AIR;
      caveAirID = getStateId(caveAir.getDefaultState());
      pistonTechBlock = (PistonExtensionBlock)Blocks.MOVING_PISTON;
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
      grass = Blocks.GRASS;
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
      biomeBlocksArray = new Block[]{
         grassBlock,
         oakLeaves,
         spruceLeaves,
         birchLeaves,
         jungleLeaves,
         acaciaLeaves,
         darkOakLeaves,
         grass,
         fern,
         tallGrass,
         largeFern,
         reeds,
         vine,
         lilypad,
         tallFlower,
         water
      };
      biomeBlocks = new HashSet(Arrays.asList(biomeBlocksArray));
      shapedBlocksArray = new Block[]{ladder, vine};
      shapedBlocks = new HashSet(Arrays.asList(shapedBlocksArray));

      for(Block block : Registry.BLOCK) {
         if (block instanceof DoorBlock || block instanceof AbstractSignBlock) {
            shapedBlocks.add(block);
         }
      }

   }

   public static int getStateId(BlockState blockState) {
      Integer id = (Integer)stateToInt.get(blockState);
      if (id == null) {
         synchronized(incrementLock) {
            id = (Integer)stateToInt.get(blockState);
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
      return (BlockState)blockStates.get(id);
   }

   static {
      BlockState airBlockState = Blocks.AIR.getDefaultState();
      stateToInt.put(airBlockState, 0);
      blockStates.add(airBlockState);
   }
}
