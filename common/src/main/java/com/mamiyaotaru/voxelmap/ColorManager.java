package com.mamiyaotaru.voxelmap;

import com.google.common.collect.UnmodifiableIterator;
import com.mamiyaotaru.voxelmap.mixins.BiomeAccessor;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.BlockModel;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColorManager {
    private boolean resourcePacksChanged;
    private ClientLevel world;
    private BufferedImage terrainBuff;
    private BufferedImage colorPicker;
    private int sizeOfBiomeArray;
    private int[] blockColors = new int[16384];
    private int[] blockColorsWithDefaultTint = new int[16384];
    private final HashSet<Integer> biomeTintsAvailable = new HashSet<>();
    private boolean optifineInstalled;
    private final HashMap<Integer, int[][]> blockTintTables = new HashMap<>();
    private final HashSet<Integer> biomeTextureAvailable = new HashSet<>();
    private final HashMap<String, Integer> blockBiomeSpecificColors = new HashMap<>();
    private float failedToLoadX;
    private float failedToLoadY;
    private String renderPassThreeBlendMode;
    private final RandomSource random = RandomSource.create();
    private boolean loaded;
    private final MutableBlockPos dummyBlockPos = new MutableBlockPos(BlockPos.ZERO.getX(), BlockPos.ZERO.getY(), BlockPos.ZERO.getZ());
    private final Vector3f fullbright = new Vector3f(1.0F, 1.0F, 1.0F);
    private final ColorResolver spruceColorResolver = (blockState, biomex, blockPos) -> FoliageColor.getEvergreenColor();
    private final ColorResolver birchColorResolver = (blockState, biomex, blockPos) -> FoliageColor.getBirchColor();
    private final ColorResolver grassColorResolver = (blockState, biomex, blockPos) -> biomex.getGrassColor(blockPos.getX(), blockPos.getZ());
    private final ColorResolver foliageColorResolver = (blockState, biomex, blockPos) -> biomex.getFoliageColor();
    private final ColorResolver waterColorResolver = (blockState, biomex, blockPos) -> biomex.getWaterColor();
    private final ColorResolver redstoneColorResolver = (blockState, biomex, blockPos) -> RedStoneWireBlock.getColorForPower(blockState.getValue(RedStoneWireBlock.POWER));

    public ColorManager() {
        this.optifineInstalled = false;
        Field ofProfiler = null;

        try {
            ofProfiler = Options.class.getDeclaredField("ofProfiler");
        } catch (SecurityException | NoSuchFieldException ignored) {
        } finally {
            if (ofProfiler != null) {
                this.optifineInstalled = true;
            }

        }

        // TODO 1.19.3
        /*for (Biome biome : BuiltinRegistries.BIOME) {
            int biomeID = BuiltinRegistries.BIOME.getRawId(biome);
            if (biomeID > this.sizeOfBiomeArray) {
                this.sizeOfBiomeArray = biomeID;
            }
        }*/

        ++this.sizeOfBiomeArray;
    }

    public int getAirColor() {
        return this.blockColors[BlockRepository.airID];
    }

    public BufferedImage getColorPicker() {
        return this.colorPicker;
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourcePacksChanged = true;
    }

    public boolean checkForChanges() {
        boolean biomesChanged = false;

        if (VoxelConstants.getClientWorld() != this.world) {
            this.world = VoxelConstants.getClientWorld();
            int largestBiomeID = 0;

            for (Biome biome : this.world.registryAccess().registryOrThrow(Registries.BIOME)) {
                int biomeID = this.world.registryAccess().registryOrThrow(Registries.BIOME).getId(biome);
                if (biomeID > largestBiomeID) {
                    largestBiomeID = biomeID;
                }
            }

            if (this.sizeOfBiomeArray != largestBiomeID + 1) {
                this.sizeOfBiomeArray = largestBiomeID + 1;
                biomesChanged = true;
            }
        }

        boolean changed = this.resourcePacksChanged || biomesChanged;
        this.resourcePacksChanged = false;
        if (changed) {
            this.loadColors();
        }

        return changed;
    }

    private void loadColors() {
        //TODO 1.20.2 //VoxelConstants.getPlayer().getSkinTexture();
        VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(VoxelConstants.getPlayer().getGameProfile());
        BlockRepository.getBlocks();
        this.loadColorPicker();
        this.loadTexturePackTerrainImage();
        TextureAtlasSprite missing = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("missingno"));
        this.failedToLoadX = missing.getU0();
        this.failedToLoadY = missing.getV0();
        this.loaded = false;

        try {
            Arrays.fill(this.blockColors, -16842497);
            Arrays.fill(this.blockColorsWithDefaultTint, -16842497);
            this.loadSpecialColors();
            this.biomeTintsAvailable.clear();
            this.biomeTextureAvailable.clear();
            this.blockBiomeSpecificColors.clear();
            this.blockTintTables.clear();
            if (this.optifineInstalled) {
                try {
                    this.processCTM();
                } catch (Exception var4) {
                    VoxelConstants.getLogger().error("error loading CTM " + var4.getLocalizedMessage(), var4);
                }

                try {
                    this.processColorProperties();
                } catch (Exception var3) {
                    VoxelConstants.getLogger().error("error loading custom color properties " + var3.getLocalizedMessage(), var3);
                }
            }

            VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        } catch (Exception var5) {
            VoxelConstants.getLogger().error("error loading pack", var5);
        }

        this.loaded = true;
    }

    public final BufferedImage getBlockImage(BlockState blockState, ItemStack stack, Level world, float iconScale, float captureDepth) {
        try {
            BakedModel model = VoxelConstants.getMinecraft().getItemRenderer().getModel(stack, world, null, 0);
            this.drawModel(Direction.EAST, blockState, model, stack, iconScale, captureDepth);
            BufferedImage blockImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
            ImageIO.write(blockImage, "png", new File(VoxelConstants.getMinecraft().gameDirectory, blockState.getBlock().getName().getString() + "-" + Block.getId(blockState) + ".png"));
            return blockImage;
        } catch (Exception var8) {
            VoxelConstants.getLogger().error("error getting block armor image for " + blockState.toString() + ": " + var8.getLocalizedMessage(), var8);
            return null;
        }
    }

    private void drawModel(Direction facing, BlockState blockState, BakedModel model, ItemStack stack, float scale, float captureDepth) {
        float size = 8.0F * scale;
        ItemTransforms transforms = model.getTransforms();
        ItemTransform headTransforms = transforms.head;
        Vector3f translations = headTransforms.translation;
        float transX = -translations.x() * size + 0.5F * size;
        float transY = translations.y() * size + 0.5F * size;
        float transZ = -translations.z() * size + 0.5F * size;
        Vector3f rotations = headTransforms.rotation;
        float rotX = rotations.x();
        float rotY = rotations.y();
        float rotZ = rotations.z();
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.Utils.fboTextureId);
        int width = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TRANSFORM_BIT);
        int height = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TEXTURE_HEIGHT);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, 0);
        OpenGL.glViewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(0.0f, 0.0f, -3000.0f + (captureDepth * scale));
        RenderSystem.applyModelViewMatrix();
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glDisable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGL.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        OpenGL.glClearDepth(1.0);
        OpenGL.glClear(OpenGL.GL11_GL_COLOR_BUFFER_BIT | OpenGL.GL11_GL_DEPTH_BUFFER_BIT);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        matrixStack.pushMatrix();
        matrixStack.translate((width / 2f) - size / 2.0F + transX, (height / 2f) - size / 2.0F + transY, 0.0F + transZ);
        matrixStack.scale(size, size, size);
        VoxelConstants.getMinecraft().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
        OpenGL.Utils.img2(InventoryMenu.BLOCK_ATLAS);
        matrixStack.rotate(Axis.YP.rotationDegrees(180.0F));
        matrixStack.rotate(Axis.YP.rotationDegrees(rotY));
        matrixStack.rotate(Axis.XP.rotationDegrees(rotX));
        matrixStack.rotate(Axis.ZP.rotationDegrees(rotZ));
        if (facing == Direction.UP) {
            matrixStack.rotate(Axis.XP.rotationDegrees(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        Vector4f fullbright2 = new Vector4f(this.fullbright.x, fullbright.y, fullbright.z, 0);
        fullbright2.mul(matrixStack);
        Vector3f fullbright3 = new Vector3f(fullbright2.x, fullbright2.y, fullbright2.z);
        RenderSystem.setShaderLights(fullbright3, fullbright3);
        PoseStack newMatrixStack = new PoseStack();
        MultiBufferSource.BufferSource immediate = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
        VoxelConstants.getMinecraft().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, newMatrixStack, immediate, 15728880, OverlayTexture.NO_OVERLAY, model);
        immediate.endBatch();
        matrixStack.popMatrix();
        matrixStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glDepthMask(false);
        OpenGL.Utils.unbindFramebuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getWidth(), VoxelConstants.getMinecraft().getWindow().getHeight());
    }

    private void loadColorPicker() {
        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/colorpicker.png")).get().open();
            Image picker = ImageIO.read(is);
            is.close();
            this.colorPicker = new BufferedImage(picker.getWidth(null), picker.getHeight(null), 2);
            Graphics gfx = this.colorPicker.createGraphics();
            gfx.drawImage(picker, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Error loading color picker: " + var4.getLocalizedMessage());
        }

    }

    public void setSkyColor(int skyColor) {
        this.blockColors[BlockRepository.airID] = skyColor;
        this.blockColors[BlockRepository.voidAirID] = skyColor;
        this.blockColors[BlockRepository.caveAirID] = skyColor;
    }

    private void loadTexturePackTerrainImage() {
        try {
            TextureManager textureManager = VoxelConstants.getMinecraft().getTextureManager();
            textureManager.bindForSetup(InventoryMenu.BLOCK_ATLAS);
            BufferedImage terrainStitched = ImageUtils.createBufferedImageFromCurrentGLImage();
            this.terrainBuff = new BufferedImage(terrainStitched.getWidth(null), terrainStitched.getHeight(null), 6);
            Graphics gfx = this.terrainBuff.createGraphics();
            gfx.drawImage(terrainStitched, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Error processing new resource pack: " + var4.getLocalizedMessage(), var4);
        }

    }

    private void loadSpecialColors() {
        int blockStateID;
        for (Iterator<BlockState> blockStateIterator = BlockRepository.pistonTechBlock.getStateDefinition().getPossibleStates().iterator(); blockStateIterator.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = blockStateIterator.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

        for (Iterator<BlockState> var6 = BlockRepository.barrier.getStateDefinition().getPossibleStates().iterator(); var6.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = var6.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

    }

    public final int getBlockColorWithDefaultTint(MutableBlockPos blockPos, int blockStateID) {
        if (this.loaded) {
            int col = 452984832;

            try {
                col = this.blockColorsWithDefaultTint[blockStateID];
            } catch (ArrayIndexOutOfBoundsException ignored) {}

            return col != -16842497 ? col : this.getBlockColor(blockPos, blockStateID);
        } else {
            return 0;
        }
    }

    public final int getBlockColor(MutableBlockPos blockPos, int blockStateID, Biome biomeID) {
        if (this.loaded) {
            if (this.optifineInstalled && this.biomeTextureAvailable.contains(blockStateID)) {
                Integer col = this.blockBiomeSpecificColors.get(blockStateID + " " + biomeID);
                if (col != null) {
                    return col;
                }
            }

            return this.getBlockColor(blockPos, blockStateID);
        } else {
            return 0;
        }
    }

    private int getBlockColor(int blockStateID) {
        return this.getBlockColor(this.dummyBlockPos, blockStateID);
    }

    private int getBlockColor(MutableBlockPos blockPos, int blockStateID) {
        int col = 452984832;

        try {
            col = this.blockColors[blockStateID];
        } catch (ArrayIndexOutOfBoundsException var5) {
            this.resizeColorArrays(blockStateID);
        }

        if (col == -16842497 || col == 452984832) {
            BlockState blockState = BlockRepository.getStateById(blockStateID);
            col = this.blockColors[blockStateID] = this.getColor(blockPos, blockState);
        }

        return col;
    }

    private synchronized void resizeColorArrays(int queriedID) {
        if (queriedID >= this.blockColors.length) {
            int[] newBlockColors = new int[this.blockColors.length * 2];
            int[] newBlockColorsWithDefaultTint = new int[this.blockColors.length * 2];
            System.arraycopy(this.blockColors, 0, newBlockColors, 0, this.blockColors.length);
            System.arraycopy(this.blockColorsWithDefaultTint, 0, newBlockColorsWithDefaultTint, 0, this.blockColorsWithDefaultTint.length);
            Arrays.fill(newBlockColors, this.blockColors.length, newBlockColors.length, -16842497);
            Arrays.fill(newBlockColorsWithDefaultTint, this.blockColorsWithDefaultTint.length, newBlockColorsWithDefaultTint.length, -16842497);
            this.blockColors = newBlockColors;
            this.blockColorsWithDefaultTint = newBlockColorsWithDefaultTint;
        }

    }

    private int getColor(MutableBlockPos blockPos, BlockState state) {
        try {
            int color = this.getColorForBlockPosBlockStateAndFacing(blockPos, state, Direction.UP);
            if (color == 452984832) {
                BlockRenderDispatcher blockRendererDispatcher = VoxelConstants.getMinecraft().getBlockRenderer();
                color = this.getColorForTerrainSprite(state, blockRendererDispatcher);
            }

            Block block = state.getBlock();
            if (block == BlockRepository.cobweb) {
                color |= -16777216;
            }

            if (block == BlockRepository.redstone) {
                color = ColorUtils.colorMultiplier(color, VoxelConstants.getMinecraft().getBlockColors().getColor(state, null, null, 0) | 0xFF000000);
            }

            if (BlockRepository.biomeBlocks.contains(block)) {
                this.applyDefaultBuiltInShading(state, color);
            } else {
                this.checkForBiomeTinting(blockPos, state, color);
            }

            if (BlockRepository.shapedBlocks.contains(block)) {
                color = this.applyShape(block, color);
            }

            if ((color >> 24 & 0xFF) < 27) {
                color |= 452984832;
            }

            return color;
        } catch (Exception var5) {
            VoxelConstants.getLogger().error("failed getting color: " + state.getBlock().getName().getString(), var5);
            return 452984832;
        }
    }

    private int getColorForBlockPosBlockStateAndFacing(BlockPos blockPos, BlockState blockState, Direction facing) {
        int color = 452984832;

        try {
            RenderShape blockRenderType = blockState.getRenderShape();
            BlockRenderDispatcher blockRendererDispatcher = VoxelConstants.getMinecraft().getBlockRenderer();
            if (blockRenderType == RenderShape.MODEL) {
                BakedModel iBakedModel = blockRendererDispatcher.getBlockModel(blockState);
                List<BakedQuad> quads = new ArrayList<>();
                quads.addAll(iBakedModel.getQuads(blockState, facing, this.random));
                quads.addAll(iBakedModel.getQuads(blockState, null, this.random));
                BlockModel model = new BlockModel(quads, this.failedToLoadX, this.failedToLoadY);
                if (model.numberOfFaces() > 0) {
                    BufferedImage modelImage = model.getImage(this.terrainBuff);
                    if (modelImage != null) {
                        color = this.getColorForCoordinatesAndImage(new float[]{0.0F, 1.0F, 0.0F, 1.0F}, modelImage);
                    } else {
                        VoxelConstants.getLogger().warn("image was null");
                    }
                }
            }
        } catch (Exception var11) {
            VoxelConstants.getLogger().error(var11.getMessage(), var11);
        }

        return color;
    }

    private int getColorForTerrainSprite(BlockState blockState, BlockRenderDispatcher blockRendererDispatcher) {
        BlockModelShaper blockModelShapes = blockRendererDispatcher.getBlockModelShaper();
        TextureAtlasSprite icon = blockModelShapes.getParticleIcon(blockState);
        if (icon == blockModelShapes.getModelManager().getMissingModel().getParticleIcon()) {
            Block block = blockState.getBlock();
            Block material = blockState.getBlock();
            if (block instanceof LiquidBlock) {
                if (material == Blocks.WATER) {
                    icon = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("minecraft:blocks/water_flow"));
                } else if (material == Blocks.LAVA) {
                    icon = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("minecraft:blocks/lava_flow"));
                }
            } else if (material == Blocks.WATER) {
                icon = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("minecraft:blocks/water_still"));
            } else if (material == Blocks.LAVA) {
                icon = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("minecraft:blocks/lava_still"));
            }
        }

        return this.getColorForIcon(icon);
    }

    private int getColorForIcon(TextureAtlasSprite icon) {
        int color = 452984832;
        if (icon != null) {
            float left = icon.getU0();
            float right = icon.getU1();
            float top = icon.getV0();
            float bottom = icon.getV1();
            color = this.getColorForCoordinatesAndImage(new float[]{left, right, top, bottom}, this.terrainBuff);
        }

        return color;
    }

    private int getColorForCoordinatesAndImage(float[] uv, BufferedImage imageBuff) {
        int color = 452984832;
        if (uv[0] != this.failedToLoadX || uv[2] != this.failedToLoadY) {
            int left = (int) (uv[0] * imageBuff.getWidth());
            int right = (int) Math.ceil(uv[1] * imageBuff.getWidth());
            int top = (int) (uv[2] * imageBuff.getHeight());
            int bottom = (int) Math.ceil(uv[3] * imageBuff.getHeight());

            try {
                BufferedImage blockTexture = imageBuff.getSubimage(left, top, right - left, bottom - top);
                Image singlePixel = blockTexture.getScaledInstance(1, 1, 4);
                BufferedImage singlePixelBuff = new BufferedImage(1, 1, imageBuff.getType());
                Graphics gfx = singlePixelBuff.createGraphics();
                gfx.drawImage(singlePixel, 0, 0, null);
                gfx.dispose();
                color = singlePixelBuff.getRGB(0, 0);
            } catch (RasterFormatException var12) {
                VoxelConstants.getLogger().warn("error getting color");
                VoxelConstants.getLogger().warn(IntStream.of(left, right, top, bottom).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            }
        }

        return color;
    }

    private void applyDefaultBuiltInShading(BlockState blockState, int color) {
        Block block = blockState.getBlock();
        int blockStateID = BlockRepository.getStateId(blockState);
        if (block != BlockRepository.largeFern && block != BlockRepository.tallGrass && block != BlockRepository.reeds) {
            if (block == BlockRepository.water) {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, 0xFF3F76E4);
            } else {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, null, null, 0) | 0xFF000000);
            }
        } else {
            this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, GrassColor.get(0.7, 0.8) | 0xFF000000);
        }

    }

    private void checkForBiomeTinting(MutableBlockPos blockPos, BlockState blockState, int color) {
        try {
            Block block = blockState.getBlock();
            String blockName = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
            if (BlockRepository.biomeBlocks.contains(block) || !blockName.startsWith("minecraft:")) {
                int tint;
                MutableBlockPos tempBlockPos = new MutableBlockPos(0, 0, 0);
                if (blockPos == this.dummyBlockPos) {
                    tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, null); // Biome 4?
                } else {
                    ClientLevel clientWorld = VoxelConstants.getClientWorld();

                    ChunkAccess chunk = clientWorld.getChunk(blockPos);
                    if (chunk != null && !((LevelChunk) chunk).isEmpty() && clientWorld.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                        tint = VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, clientWorld, blockPos, 1) | 0xFF000000;
                    } else {
                        tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, null); // Biome 4?
                    }
                }

                if (tint != 16777215 && tint != -1) {
                    int blockStateID = BlockRepository.getStateId(blockState);
                    this.biomeTintsAvailable.add(blockStateID);
                    this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, tint);
                } else {
                    this.blockColorsWithDefaultTint[BlockRepository.getStateId(blockState)] = 452984832;
                }
            }
        } catch (Exception ignored) {}

    }

    private int tintFromFakePlacedBlock(BlockState blockState, MutableBlockPos loopBlockPos, Biome biomeID) {
        return -1;
    }

    public int getBiomeTint(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        ChunkAccess chunk = world.getChunk(blockPos);
        boolean live = chunk != null && !((LevelChunk) chunk).isEmpty() && VoxelConstants.getPlayer().level().hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        live = live && VoxelConstants.getPlayer().level().hasChunkAt(blockPos);
        int tint = -2;
        if (this.optifineInstalled || !live && this.biomeTintsAvailable.contains(blockStateID)) {
            try {
                int[][] tints = this.blockTintTables.get(blockStateID);
                if (tints != null) {
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    for (int t = blockPos.getX() - 1; t <= blockPos.getX() + 1; ++t) {
                        for (int s = blockPos.getZ() - 1; s <= blockPos.getZ() + 1; ++s) {
                            Biome biome;
                            if (live) {
                                biome = world.getBiome(loopBlockPos.withXYZ(t, blockPos.getY(), s)).value();
                            } else {
                                int dataX = t - startX;
                                int dataZ = s - startZ;
                                dataX = Math.max(dataX, 0);
                                dataX = Math.min(dataX, mapData.getWidth() - 1);
                                dataZ = Math.max(dataZ, 0);
                                dataZ = Math.min(dataZ, mapData.getHeight() - 1);
                                biome = mapData.getBiome(dataX, dataZ);
                            }

                            if (biome == null) {
                                biome = world.registryAccess().registryOrThrow(Registries.BIOME).get(Biomes.PLAINS);
                            }

                            int biomeID = world.registryAccess().registryOrThrow(Registries.BIOME).getId(biome);
                            int biomeTint = tints[biomeID][loopBlockPos.y / 8];
                            r += (biomeTint & 0xFF0000) >> 16;
                            g += (biomeTint & 0xFF00) >> 8;
                            b += biomeTint & 0xFF;
                        }
                    }

                    tint = 0xFF000000 | (r / 9 & 0xFF) << 16 | (g / 9 & 0xFF) << 8 | b / 9 & 0xFF;
                }
            } catch (Exception ignored) {}
        }

        if (tint == -2) {
            tint = this.getBuiltInBiomeTint(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ, live);
        }

        return tint;
    }

    private int getBuiltInBiomeTint(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ, boolean live) {
        int tint = -1;
        Block block = blockState.getBlock();
        if (BlockRepository.biomeBlocks.contains(block) || this.biomeTintsAvailable.contains(blockStateID)) {
            if (live) {
                try {
                    DebugRenderState.blockX = blockPos.x;
                    DebugRenderState.blockY = blockPos.y;
                    DebugRenderState.blockZ = blockPos.z;
                    tint = VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, world, blockPos, 0) | 0xFF000000;
                } catch (Exception ignored) {}
            }

            if (tint == -1) {
                tint = this.getBuiltInBiomeTintFromUnloadedChunk(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ) | 0xFF000000;
            }
        }

        return tint;
    }

    private int getBuiltInBiomeTintFromUnloadedChunk(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        int tint = -1;
        Block block = blockState.getBlock();
        ColorResolver colorResolver = null;
        if (block == BlockRepository.water) {
            colorResolver = this.waterColorResolver;
        } else if (block == BlockRepository.spruceLeaves) {
            colorResolver = this.spruceColorResolver;
        } else if (block == BlockRepository.birchLeaves) {
            colorResolver = this.birchColorResolver;
        } else if (block != BlockRepository.oakLeaves && block != BlockRepository.jungleLeaves && block != BlockRepository.acaciaLeaves && block != BlockRepository.darkOakLeaves && block != BlockRepository.mangroveLeaves && block != BlockRepository.vine) {
            if (block == BlockRepository.redstone) {
                colorResolver = this.redstoneColorResolver;
            } else if (BlockRepository.biomeBlocks.contains(block)) {
                colorResolver = this.grassColorResolver;
            }
        } else {
            colorResolver = this.foliageColorResolver;
        }

        if (colorResolver != null) {
            int r = 0;
            int g = 0;
            int b = 0;

            for (int t = blockPos.getX() - 1; t <= blockPos.getX() + 1; ++t) {
                for (int s = blockPos.getZ() - 1; s <= blockPos.getZ() + 1; ++s) {
                    int dataX = t - startX;
                    int dataZ = s - startZ;
                    dataX = Math.max(dataX, 0);
                    dataX = Math.min(dataX, 255);
                    dataZ = Math.max(dataZ, 0);
                    dataZ = Math.min(dataZ, 255);
                    Biome biome = mapData.getBiome(dataX, dataZ);
                    if (biome == null) {
                        MessageUtils.printDebug("Null biome ID! " + " at " + t + "," + s);
                        MessageUtils.printDebug("block: " + mapData.getBlockstate(dataX, dataZ) + ", height: " + mapData.getHeight(dataX, dataZ));
                        MessageUtils.printDebug("Mapdata: " + mapData);
                    }

                    int biomeTint = biome == null ? 0 : colorResolver.getColorAtPos(blockState, biome, loopBlockPos.withXYZ(t, blockPos.getY(), s));
                    r += (biomeTint & 0xFF0000) >> 16;
                    g += (biomeTint & 0xFF00) >> 8;
                    b += biomeTint & 0xFF;
                }
            }

            tint = (r / 9 & 0xFF) << 16 | (g / 9 & 0xFF) << 8 | b / 9 & 0xFF;
        } else if (this.biomeTintsAvailable.contains(blockStateID)) {
            tint = this.getCustomBlockBiomeTintFromUnloadedChunk(mapData, world, blockState, blockPos, loopBlockPos, startX, startZ);
        }

        return tint;
    }

    private int getCustomBlockBiomeTintFromUnloadedChunk(AbstractMapData mapData, Level world, BlockState blockState, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        int tint;

        try {
            int dataX = blockPos.getX() - startX;
            int dataZ = blockPos.getZ() - startZ;
            dataX = Math.max(dataX, 0);
            dataX = Math.min(dataX, mapData.getWidth() - 1);
            dataZ = Math.max(dataZ, 0);
            dataZ = Math.min(dataZ, mapData.getHeight() - 1);
            Biome biome = mapData.getBiome(dataX, dataZ);
            tint = this.tintFromFakePlacedBlock(blockState, loopBlockPos, biome);
        } catch (Exception var12) {
            tint = -1;
        }

        return tint;
    }

    private int applyShape(Block block, int color) {
        int alpha = color >> 24 & 0xFF;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        if (block instanceof SignBlock) {
            alpha = 31;
        } else if (block instanceof DoorBlock) {
            alpha = 47;
        } else if (block == BlockRepository.ladder || block == BlockRepository.vine) {
            alpha = 15;
        }

        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    private void processCTM() {
        this.renderPassThreeBlendMode = "alpha";
        Properties properties = new Properties();
        ResourceLocation propertiesFile = ResourceLocation.fromNamespaceAndPath("minecraft", "optifine/renderpass.properties");

        try {
            InputStream input = VoxelConstants.getMinecraft().getResourceManager().getResource(propertiesFile).get().open();
            if (input != null) {
                properties.load(input);
                input.close();
                this.renderPassThreeBlendMode = properties.getProperty("blend.3", "alpha");
            }
        } catch (IOException var9) {
            this.renderPassThreeBlendMode = "alpha";
        }

        String namespace = "minecraft";

        for (ResourceLocation s : this.findResources(namespace, "/optifine/ctm", ".properties", true, false, true)) {
            try {
                this.loadCTM(s);
            } catch (IllegalArgumentException ignored) {}
        }

        for (int t = 0; t < this.blockColors.length; ++t) {
            if (this.blockColors[t] != 452984832 && this.blockColors[t] != -16842497) {
                if ((this.blockColors[t] >> 24 & 0xFF) < 27) {
                    this.blockColors[t] |= 452984832;
                }

                this.checkForBiomeTinting(this.dummyBlockPos, BlockRepository.getStateById(t), this.blockColors[t]);
            }
        }

    }

    private void loadCTM(ResourceLocation propertiesFile) {
        if (propertiesFile != null) {
            BlockRenderDispatcher blockRendererDispatcher = VoxelConstants.getMinecraft().getBlockRenderer();
            BlockModelShaper blockModelShapes = blockRendererDispatcher.getBlockModelShaper();
            Properties properties = new Properties();

            try {
                InputStream input = VoxelConstants.getMinecraft().getResourceManager().getResource(propertiesFile).get().open();
                if (input != null) {
                    properties.load(input);
                    input.close();
                }
            } catch (IOException var39) {
                return;
            }

            String filePath = propertiesFile.getPath();
            String method = properties.getProperty("method", "").trim().toLowerCase();
            String faces = properties.getProperty("faces", "").trim().toLowerCase();
            String matchBlocks = properties.getProperty("matchBlocks", "").trim().toLowerCase();
            String matchTiles = properties.getProperty("matchTiles", "").trim().toLowerCase();
            String metadata = properties.getProperty("metadata", "").trim().toLowerCase();
            String tiles = properties.getProperty("tiles", "").trim();
            String biomes = properties.getProperty("biomes", "").trim().toLowerCase();
            String renderPass = properties.getProperty("renderPass", "").trim().toLowerCase();
            metadata = metadata.replaceAll("\\s+", ",");
            Set<BlockState> blockStates = new HashSet<>(this.parseBlocksList(matchBlocks, metadata));
            String directory = filePath.substring(0, filePath.lastIndexOf("/") + 1);
            String[] tilesParsed = this.parseStringList(tiles);
            String tilePath = directory + "0";
            if (tilesParsed.length > 0) {
                tilePath = tilesParsed[0].trim();
            }

            if (tilePath.startsWith("~")) {
                tilePath = tilePath.replace("~", "optifine");
            } else if (!tilePath.contains("/")) {
                tilePath = directory + tilePath;
            }

            if (!tilePath.toLowerCase().endsWith(".png")) {
                tilePath = tilePath + ".png";
            }

            String[] biomesArray = biomes.split(" ");
            if (blockStates.isEmpty()) {
                Block block;
                Pattern pattern = Pattern.compile(".*/block_(.+).properties");
                Matcher matcher = pattern.matcher(filePath);
                if (matcher.find()) {
                    block = this.getBlockFromName(matcher.group(1));
                    if (block != null) {
                        Set<BlockState> matching = this.parseBlockMetadata(block, metadata);
                        if (matching.isEmpty()) {
                            matching.addAll(block.getStateDefinition().getPossibleStates());
                        }

                        blockStates.addAll(matching);
                    }
                } else {
                    if (matchTiles.isEmpty()) {
                        matchTiles = filePath.substring(filePath.lastIndexOf('/') + 1, filePath.lastIndexOf(".properties"));
                    }

                    if (!matchTiles.contains(":")) {
                        matchTiles = "minecraft:blocks/" + matchTiles;
                    }

                    ResourceLocation matchID = ResourceLocation.parse(matchTiles);
                    TextureAtlasSprite compareIcon = VoxelConstants.getMinecraft().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(matchID);
                    if (compareIcon.atlasLocation() != MissingTextureAtlasSprite.getLocation()) {
                        ArrayList<BlockState> tmpList = new ArrayList<>();

                        for (Block testBlock : BuiltInRegistries.BLOCK) {

                            for (BlockState blockState : testBlock.getStateDefinition().getPossibleStates()) {
                                try {
                                    BakedModel bakedModel = blockModelShapes.getBlockModel(blockState);
                                    List<BakedQuad> quads = new ArrayList<>();
                                    quads.addAll(bakedModel.getQuads(blockState, Direction.UP, this.random));
                                    quads.addAll(bakedModel.getQuads(blockState, null, this.random));
                                    BlockModel model = new BlockModel(quads, this.failedToLoadX, this.failedToLoadY);
                                    if (model.numberOfFaces() > 0) {
                                        ArrayList<BlockModel.BlockFace> blockFaces = model.getFaces();

                                        for (int i = 0; i < blockFaces.size(); ++i) {
                                            BlockModel.BlockFace face = model.getFaces().get(i);
                                            float minU = face.getMinU();
                                            float maxU = face.getMaxU();
                                            float minV = face.getMinV();
                                            float maxV = face.getMaxV();
                                            if (this.similarEnough(minU, maxU, minV, maxV, compareIcon.getU0(), compareIcon.getU1(), compareIcon.getV0(), compareIcon.getV1())) {
                                                tmpList.add(blockState);
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }

                        blockStates.addAll(tmpList);
                    }
                }
            }

            if (!blockStates.isEmpty()) {
                if (!method.equals("horizontal") && !method.startsWith("overlay") && (method.equals("sandstone") || method.equals("top") || faces.contains("top") || faces.contains("all") || faces.isEmpty())) {
                    try {
                        ResourceLocation pngResource = ResourceLocation.fromNamespaceAndPath(propertiesFile.getNamespace(), tilePath);
                        InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(pngResource).get().open();
                        Image top = ImageIO.read(is);
                        is.close();
                        top = top.getScaledInstance(1, 1, 4);
                        BufferedImage topBuff = new BufferedImage(top.getWidth(null), top.getHeight(null), 6);
                        Graphics gfx = topBuff.createGraphics();
                        gfx.drawImage(top, 0, 0, null);
                        gfx.dispose();
                        int topRGB = topBuff.getRGB(0, 0);
                        if ((topRGB >> 24 & 0xFF) == 0) {
                            return;
                        }

                        for (BlockState blockState : blockStates) {
                            topRGB = topBuff.getRGB(0, 0);
                            if (blockState.getBlock() == BlockRepository.cobweb) {
                                topRGB |= -16777216;
                            }

                            if (renderPass.equals("3")) {
                                topRGB = this.processRenderPassThree(topRGB);
                                int blockStateID = BlockRepository.getStateId(blockState);
                                int baseRGB = this.blockColors[blockStateID];
                                if (baseRGB != 452984832 && baseRGB != -16842497) {
                                    topRGB = ColorUtils.colorMultiplier(baseRGB, topRGB);
                                }
                            }

                            if (BlockRepository.shapedBlocks.contains(blockState.getBlock())) {
                                topRGB = this.applyShape(blockState.getBlock(), topRGB);
                            }

                            int blockStateID = BlockRepository.getStateId(blockState);
                            if (!biomes.isEmpty()) {
                                this.biomeTextureAvailable.add(blockStateID);

                                for (String s : biomesArray) {
                                    int biomeInt = this.parseBiomeName(s);
                                    if (biomeInt != -1) {
                                        this.blockBiomeSpecificColors.put(blockStateID + " " + biomeInt, topRGB);
                                    }
                                }
                            } else {
                                this.blockColors[blockStateID] = topRGB;
                            }
                        }
                    } catch (IOException var40) {
                        VoxelConstants.getLogger().error("error getting CTM block from " + propertiesFile.getPath() + ": " + filePath + " " + BuiltInRegistries.BLOCK.getKey(blockStates.iterator().next().getBlock()) + " " + tilePath, var40);
                    }
                }

            }
        }
    }

    private boolean similarEnough(float a, float b, float c, float d, float one, float two, float three, float four) {
        boolean similar = Math.abs(a - one) < 1.0E-4;
        similar = similar && Math.abs(b - two) < 1.0E-4;
        similar = similar && Math.abs(c - three) < 1.0E-4;
        return similar && Math.abs(d - four) < 1.0E-4;
    }

    private int processRenderPassThree(int rgb) {
        if (this.renderPassThreeBlendMode.equals("color") || this.renderPassThreeBlendMode.equals("overlay")) {
            int red = rgb >> 16 & 0xFF;
            int green = rgb >> 8 & 0xFF;
            int blue = rgb & 0xFF;
            float colorAverage = (red + blue + green) / 3.0F;
            float lighteningFactor = (colorAverage - 127.5F) * 2.0F;
            red += (int) (red * (lighteningFactor / 255.0F));
            blue += (int) (red * (lighteningFactor / 255.0F));
            green += (int) (red * (lighteningFactor / 255.0F));
            int newAlpha = (int) Math.abs(lighteningFactor);
            rgb = newAlpha << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }

        return rgb;
    }

    private String[] parseStringList(String list) {
        ArrayList<String> tmpList = new ArrayList<>();

        for (String token : list.split("\\s+")) {
            token = token.trim();

            try {
                if (token.matches("^\\d+$")) {
                    tmpList.add(String.valueOf(Integer.parseInt(token)));
                } else if (token.matches("^\\d+-\\d+$")) {
                    String[] t = token.split("-");
                    int min = Integer.parseInt(t[0]);
                    int max = Integer.parseInt(t[1]);

                    for (int i = min; i <= max; ++i) {
                        tmpList.add(String.valueOf(i));
                    }
                } else if (!token.isEmpty()) {
                    tmpList.add(token);
                }
            } catch (NumberFormatException ignored) {}
        }

        return tmpList.toArray(String[]::new);
    }

    private Set<BlockState> parseBlocksList(String blocks, String metadataLine) {
        Set<BlockState> blockStates = new HashSet<>();

        for (String blockString : blocks.split("\\s+")) {
            StringBuilder metadata = new StringBuilder(metadataLine);
            blockString = blockString.trim();
            String[] blockComponents = blockString.split(":");
            int tokensUsed = 0;
            Block block;
            block = this.getBlockFromName(blockComponents[0]);
            if (block != null) {
                tokensUsed = 1;
            } else if (blockComponents.length > 1) {
                block = this.getBlockFromName(blockComponents[0] + ":" + blockComponents[1]);
                if (block != null) {
                    tokensUsed = 2;
                }
            }

            if (block != null) {
                if (blockComponents.length > tokensUsed) {
                    metadata = new StringBuilder(blockComponents[tokensUsed]);

                    for (int t = tokensUsed + 1; t < blockComponents.length; ++t) {
                        metadata.append(":").append(blockComponents[t]);
                    }
                }

                blockStates.addAll(this.parseBlockMetadata(block, metadata.toString()));
            }
        }

        return blockStates;
    }

    private Set<BlockState> parseBlockMetadata(Block block, String metadataList) {
        Set<BlockState> blockStates = new HashSet<>();
        if (metadataList.isEmpty()) {
            blockStates.addAll(block.getStateDefinition().getPossibleStates());
        } else {
            Set<String> valuePairs = Arrays.stream(metadataList.split(":")).map(String::trim).filter(metadata -> metadata.contains("=")).collect(Collectors.toSet());

            if (!valuePairs.isEmpty()) {

                for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
                    boolean matches = true;

                    for (String pair : valuePairs) {
                        String[] propertyAndValues = pair.split("\\s*=\\s*", 5);
                        if (propertyAndValues.length == 2) {
                            Property<?> property = block.getStateDefinition().getProperty(propertyAndValues[0]);
                            if (property != null) {
                                boolean valueIncluded = false;
                                String[] values = propertyAndValues[1].split(",");

                                for (String value : values) {
                                    if (property.getValueClass() == Integer.class && value.matches("^\\d+-\\d+$")) {
                                        String[] range = value.split("-");
                                        int min = Integer.parseInt(range[0]);
                                        int max = Integer.parseInt(range[1]);
                                        int intValue = (Integer) blockState.getValue(property);
                                        if (intValue >= min && intValue <= max) {
                                            valueIncluded = true;
                                        }
                                    } else if (!blockState.getValue(property).equals(property.getValue(value))) {
                                        valueIncluded = true;
                                    }
                                }

                                matches = matches && valueIncluded;
                            }
                        }
                    }

                    if (matches) {
                        blockStates.add(blockState);
                    }
                }
            }
        }

        return blockStates;
    }

    private int parseBiomeName(String name) {
        Biome biome = this.world.registryAccess().registryOrThrow(Registries.BIOME).get(ResourceLocation.parse(name));
        return biome != null ? this.world.registryAccess().registryOrThrow(Registries.BIOME).getId(biome) : -1;
    }

    private List<ResourceLocation> findResources(String namespace, String startingPath, String suffixMaybeNull, boolean recursive, boolean directories, boolean sortByFilename) {
        if (startingPath == null) {
            startingPath = "";
        }

        if (!startingPath.isEmpty() && startingPath.charAt(0) == '/') {
            startingPath = startingPath.substring(1);
        }

        String suffix = suffixMaybeNull == null ? "" : suffixMaybeNull;
        ArrayList<ResourceLocation> resources;

        Map<ResourceLocation, Resource> resourceMap = VoxelConstants.getMinecraft().getResourceManager().listResources(startingPath, asset -> asset.getPath().endsWith(suffix));
        resources = resourceMap.keySet().stream().filter(candidate -> candidate.getNamespace().equals(namespace)).collect(Collectors.toCollection(ArrayList::new));

        if (sortByFilename) {
            resources.sort((o1, o2) -> {
                String f1 = o1.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                String f2 = o2.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                int result = f1.compareTo(f2);
                return result != 0 ? result : o1.getPath().compareTo(o2.getPath());
            });
        } else {
            resources.sort(Comparator.comparing(ResourceLocation::getPath));
        }

        return resources;
    }

    private void processColorProperties() {
        Properties properties = new Properties();

        try {
            InputStream input = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse("optifine/color.properties")).get().open();
            if (input != null) {
                properties.load(input);
                input.close();
            }
        } catch (IOException exception) {
            VoxelConstants.getLogger().error(exception);
        }

        BlockState blockState = BlockRepository.lilypad.defaultBlockState();
        int blockStateID = BlockRepository.getStateId(blockState);
        int lilyRGB = this.getBlockColor(blockStateID);
        int lilypadMultiplier = 2129968;
        String lilypadMultiplierString = properties.getProperty("lilypad");
        if (lilypadMultiplierString != null) {
            lilypadMultiplier = Integer.parseInt(lilypadMultiplierString, 16);
        }

        for (UnmodifiableIterator<BlockState> defaultFormat = BlockRepository.lilypad.getStateDefinition().getPossibleStates().iterator(); defaultFormat.hasNext(); this.blockColorsWithDefaultTint[blockStateID] = this.blockColors[blockStateID]) {
            BlockState padBlockState = defaultFormat.next();
            blockStateID = BlockRepository.getStateId(padBlockState);
            this.blockColors[blockStateID] = ColorUtils.colorMultiplier(lilyRGB, lilypadMultiplier | 0xFF000000);
        }

        String defaultFormat = properties.getProperty("palette.format");
        boolean globalGrid = defaultFormat != null && defaultFormat.equalsIgnoreCase("grid");
        Enumeration<?> e = properties.propertyNames();

        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith("palette.block")) {
                String filename = key.substring("palette.block.".length());
                filename = filename.replace("~", "optifine");
                this.processColorPropertyHelper(ResourceLocation.parse(filename), properties.getProperty(key), globalGrid);
            }
        }

        for (ResourceLocation resource : this.findResources("minecraft", "/optifine/colormap/blocks", ".properties", true, false, true)) {
            Properties colorProperties = new Properties();

            try {
                InputStream input = VoxelConstants.getMinecraft().getResourceManager().getResource(resource).get().open();
                if (input != null) {
                    colorProperties.load(input);
                    input.close();
                }
            } catch (IOException var21) {
                break;
            }

            String names = colorProperties.getProperty("blocks");
            if (names == null) {
                String name = resource.getPath();
                name = name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf(".properties"));
                names = name;
            }

            String source = colorProperties.getProperty("source");
            ResourceLocation resourcePNG;
            if (source != null) {
                resourcePNG = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), source);

                VoxelConstants.getMinecraft().getResourceManager().getResource(resourcePNG);
            } else {
                resourcePNG = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), resource.getPath().replace(".properties", ".png"));
            }

            String format = colorProperties.getProperty("format");
            boolean grid;
            if (format != null) {
                grid = format.equalsIgnoreCase("grid");
            } else {
                grid = globalGrid;
            }

            String yOffsetString = colorProperties.getProperty("yOffset");
            int yOffset = 0;
            if (yOffsetString != null) {
                yOffset = Integer.parseInt(yOffsetString);
            }

            this.processColorProperty(resourcePNG, names, grid, yOffset);
        }

        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/water.png"), "water", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/watercolorx.png"), "water", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/swampgrass.png"), "grass_block grass fern tall_grass large_fern", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/swampgrasscolor.png"), "grass_block grass fern tall_grass large_fern", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/swampfoliage.png"), "oak_leaves vine", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/swampfoliagecolor.png"), "oak_leaves vine", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/pine.png"), "spruce_leaves", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/pinecolor.png"), "spruce_leaves", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/birch.png"), "birch_leaves", globalGrid);
        this.processColorPropertyHelper(ResourceLocation.parse("optifine/colormap/birchcolor.png"), "birch_leaves", globalGrid);
    }

    private void processColorPropertyHelper(ResourceLocation resource, String list, boolean grid) {
        ResourceLocation resourceProperties = ResourceLocation.fromNamespaceAndPath(resource.getNamespace(), resource.getPath().replace(".png", ".properties"));
        Properties colorProperties = new Properties();
        int yOffset = 0;

        try {
            InputStream input = VoxelConstants.getMinecraft().getResourceManager().getResource(resourceProperties).get().open();
            if (input != null) {
                colorProperties.load(input);
                input.close();
            }

            String format = colorProperties.getProperty("format");
            if (format != null) {
                grid = format.equalsIgnoreCase("grid");
            }

            String yOffsetString = colorProperties.getProperty("yOffset");
            if (yOffsetString != null) {
                yOffset = Integer.parseInt(yOffsetString);
            }
        } catch (IOException ignored) {
        }

        this.processColorProperty(resource, list, grid, yOffset);
    }

    private void processColorProperty(ResourceLocation resource, String list, boolean grid, int yOffset) {
        int[][] tints = new int[this.sizeOfBiomeArray][32];

        for (int[] row : tints) {
            Arrays.fill(row, -1);
        }

        boolean swamp = resource.getPath().contains("/swamp");
        Image tintColors;

        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(resource).get().open();
            tintColors = ImageIO.read(is);
            is.close();
        } catch (IOException var21) {
            return;
        }

        BufferedImage tintColorsBuff = new BufferedImage(tintColors.getWidth(null), tintColors.getHeight(null), 1);
        Graphics gfx = tintColorsBuff.createGraphics();
        gfx.drawImage(tintColors, 0, 0, null);
        gfx.dispose();
        int numBiomesToCheck = grid ? Math.min(tintColorsBuff.getWidth(), this.sizeOfBiomeArray) : this.sizeOfBiomeArray;

        for (int t = 0; t < numBiomesToCheck; ++t) {
            Biome biome = this.world.registryAccess().registryOrThrow(Registries.BIOME).byId(t);
            if (biome != null) {
                int tintMult;
                int heightMultiplier = tintColorsBuff.getHeight() / 32;

                for (int s = 0; s < 32; ++s) {
                    if (grid) {
                        tintMult = tintColorsBuff.getRGB(t, Math.max(0, s * heightMultiplier - yOffset)) & 16777215;
                    } else {
                        double var1 = Mth.clamp(biome.getBaseTemperature(), 0.0F, 1.0F);
                        double var2 = Mth.clamp(((BiomeAccessor) (Object) biome).getClimateSettings().downfall(), 0.0F, 1.0F);

                        var2 *= var1;
                        var1 = 1.0 - var1;
                        var2 = 1.0 - var2;
                        tintMult = tintColorsBuff.getRGB((int) ((tintColorsBuff.getWidth() - 1) * var1), (int) ((tintColorsBuff.getHeight() - 1) * var2)) & 16777215;
                    }

                    if (tintMult != 0 && !swamp) {
                        tints[t][s] = tintMult;
                    }
                }
            }
        }

        Set<BlockState> blockStates = new HashSet<>(this.parseBlocksList(list, ""));

        for (BlockState blockState : blockStates) {
            int blockStateID = BlockRepository.getStateId(blockState);
            int[][] previousTints = this.blockTintTables.get(blockStateID);
            if (swamp && previousTints == null) {
                ResourceLocation defaultResource;
                if (resource.getPath().contains("grass")) {
                    defaultResource = ResourceLocation.parse("textures/colormap/grass.png");
                } else {
                    defaultResource = ResourceLocation.parse("textures/colormap/foliage.png");
                }

                String stateString = blockState.toString().toLowerCase();
                stateString = stateString.replaceAll("^block", "");
                stateString = stateString.replace("{", "");
                stateString = stateString.replace("}", "");
                stateString = stateString.replace("[", ":");
                stateString = stateString.replace("]", "");
                stateString = stateString.replace(",", ":");
                this.processColorProperty(defaultResource, stateString, false, 0);
                previousTints = this.blockTintTables.get(blockStateID);
            }

            if (previousTints != null) {
                for (int t = 0; t < this.sizeOfBiomeArray; ++t) {
                    for (int s = 0; s < 32; ++s) {
                        if (tints[t][s] == -1) {
                            tints[t][s] = previousTints[t][s];
                        }
                    }
                }
            }

            this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(this.getBlockColor(blockStateID), tints[4][8] | 0xFF000000);
            this.blockTintTables.put(blockStateID, tints);
            this.biomeTintsAvailable.add(blockStateID);
        }

    }

    private Block getBlockFromName(String name) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(name);
            return BuiltInRegistries.BLOCK.containsKey(resourceLocation) ? BuiltInRegistries.BLOCK.get(resourceLocation) : null;
        } catch (ResourceLocationException | NumberFormatException var3) {
            return null;
        }
    }

    @FunctionalInterface
    private interface ColorResolver {
        int getColorAtPos(BlockState state, Biome biome, BlockPos pos);
    }
}