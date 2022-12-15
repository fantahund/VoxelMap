package com.mamiyaotaru.voxelmap;

import com.google.common.collect.UnmodifiableIterator;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IColorManager;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockModel;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.Material;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
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

public class ColorManager implements IColorManager {
    private IVoxelMap master;
    MinecraftClient game;
    private boolean resourcePacksChanged = false;
    private ClientWorld world = null;
    private BufferedImage terrainBuff = null;
    private BufferedImage colorPicker;
    private int sizeOfBiomeArray = 0;
    private int[] blockColors = new int[16384];
    private int[] blockColorsWithDefaultTint = new int[16384];
    private HashSet biomeTintsAvailable = new HashSet();
    private boolean optifineInstalled = false;
    private HashMap blockTintTables = new HashMap();
    private HashSet biomeTextureAvailable = new HashSet();
    private HashMap blockBiomeSpecificColors = new HashMap();
    private float failedToLoadX = 0.0F;
    private float failedToLoadY = 0.0F;
    private String renderPassThreeBlendMode;
    private final Random random = Random.create();
    private boolean loaded = false;
    private final MutableBlockPos dummyBlockPos = new MutableBlockPos(BlockPos.ORIGIN.getX(), BlockPos.ORIGIN.getY(), BlockPos.ORIGIN.getZ());
    private final Vec3f fullbright = new Vec3f(1.0F, 1.0F, 1.0F);
    private final ColorResolver spruceColorResolver = (blockState, biomex, blockPos) -> FoliageColors.getSpruceColor();
    private final ColorResolver birchColorResolver = (blockState, biomex, blockPos) -> FoliageColors.getBirchColor();
    private final ColorResolver grassColorResolver = (blockState, biomex, blockPos) -> biomex.getGrassColorAt(blockPos.getX(), blockPos.getZ());
    private final ColorResolver foliageColorResolver = (blockState, biomex, blockPos) -> biomex.getFoliageColor();
    private final ColorResolver waterColorResolver = (blockState, biomex, blockPos) -> biomex.getWaterColor();
    private final ColorResolver redstoneColorResolver = (blockState, biomex, blockPos) -> RedstoneWireBlock.getWireColor(blockState.get(RedstoneWireBlock.POWER));

    public ColorManager(IVoxelMap master) {
        this.master = master;
        this.game = MinecraftClient.getInstance();
        this.optifineInstalled = false;
        Field ofProfiler = null;

        try {
            ofProfiler = GameOptions.class.getDeclaredField("ofProfiler");
        } catch (SecurityException | NoSuchFieldException ignored) {
        } finally {
            if (ofProfiler != null) {
                this.optifineInstalled = true;
            }

        }

        for (Biome biome : BuiltinRegistries.BIOME) {
            int biomeID = BuiltinRegistries.BIOME.getRawId(biome);
            if (biomeID > this.sizeOfBiomeArray) {
                this.sizeOfBiomeArray = biomeID;
            }
        }

        ++this.sizeOfBiomeArray;
    }

    @Override
    public int getAirColor() {
        return this.blockColors[BlockRepository.airID];
    }

    @Override
    public BufferedImage getColorPicker() {
        return this.colorPicker;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourcePacksChanged = true;
    }

    @Override
    public boolean checkForChanges() {
        boolean biomesChanged = false;
        if (this.game.world != null && this.game.world != this.world) {
            this.world = this.game.world;
            int largestBiomeID = 0;

            for (Biome biome : this.world.getRegistryManager().get(Registry.BIOME_KEY)) {
                int biomeID = this.world.getRegistryManager().get(Registry.BIOME_KEY).getRawId(biome);
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
        this.game.player.getSkinTexture();
        BlockRepository.getBlocks();
        BiomeRepository.getBiomes();
        this.loadColorPicker();
        this.loadTexturePackTerrainImage();
        Sprite missing = this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(new Identifier("missingno"));
        this.failedToLoadX = missing.getMinU();
        this.failedToLoadY = missing.getMinV();
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
                    System.err.println("error loading CTM " + var4.getLocalizedMessage());
                    var4.printStackTrace();
                }

                try {
                    this.processColorProperties();
                } catch (Exception var3) {
                    System.err.println("error loading custom color properties " + var3.getLocalizedMessage());
                    var3.printStackTrace();
                }
            }

            this.master.getMap().forceFullRender(true);
        } catch (Exception var5) {
            System.err.println("error loading pack");
            var5.printStackTrace();
        }

        this.loaded = true;
    }

    @Override
    public final BufferedImage getBlockImage(BlockState blockState, ItemStack stack, World world, float iconScale, float captureDepth) {
        try {
            BakedModel model = this.game.getItemRenderer().getModel(stack, world, (LivingEntity) null, 0);
            this.drawModel(Direction.EAST, blockState, model, stack, iconScale, captureDepth);
            BufferedImage blockImage = ImageUtils.createBufferedImageFromGLID(GLUtils.fboTextureID);
            ImageIO.write(blockImage, "png", new File(MinecraftClient.getInstance().runDirectory, blockState.getBlock().getName().getString() + "-" + Block.getRawIdFromState(blockState) + ".png"));
            return blockImage;
        } catch (Exception var8) {
            System.out.println("error getting block armor image for " + blockState.toString() + ": " + var8.getLocalizedMessage());
            var8.printStackTrace();
            return null;
        }
    }

    private void drawModel(Direction facing, BlockState blockState, BakedModel model, ItemStack stack, float scale, float captureDepth) {
        float size = 8.0F * scale;
        ModelTransformation transforms = model.getTransformation();
        Transformation headTransforms = transforms.head;
        Vec3f translations = headTransforms.translation;
        float transX = -translations.getX() * size + 0.5F * size;
        float transY = translations.getY() * size + 0.5F * size;
        float transZ = -translations.getZ() * size + 0.5F * size;
        Vec3f rotations = headTransforms.rotation;
        float rotX = rotations.getX();
        float rotY = rotations.getY();
        float rotZ = rotations.getZ();
        GLShim.glBindTexture(3553, GLUtils.fboTextureID);
        int width = GLShim.glGetTexLevelParameteri(3553, 0, 4096);
        int height = GLShim.glGetTexLevelParameteri(3553, 0, 4097);
        GLShim.glBindTexture(3553, 0);
        GLShim.glViewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = Matrix4f.projectionMatrix(0.0F, (float) width, 0.0F, (float) height, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f);
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.loadIdentity();
        matrixStack.translate(0.0, 0.0, -3000.0 + (double) (captureDepth * scale));
        RenderSystem.applyModelViewMatrix();
        GLUtils.bindFrameBuffer();
        GLShim.glDepthMask(true);
        GLShim.glEnable(2929);
        GLShim.glEnable(3553);
        GLShim.glEnable(3042);
        GLShim.glDisable(2884);
        GLShim.glBlendFunc(770, 771);
        GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GLShim.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        GLShim.glClearDepth(1.0);
        GLShim.glClear(16640);
        GLShim.glBlendFunc(770, 771);
        matrixStack.push();
        matrixStack.translate((float) (width / 2) - size / 2.0F + transX, (float) (height / 2) - size / 2.0F + transY, 0.0F + transZ);
        matrixStack.scale(size, size, size);
        MinecraftClient.getInstance().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).setFilter(false, false);
        GLUtils.img2(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
        matrixStack.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(rotY));
        matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(rotX));
        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotZ));
        if (facing == Direction.UP) {
            matrixStack.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
        }

        RenderSystem.applyModelViewMatrix();
        Vector4f fullbright2 = new Vector4f(this.fullbright);
        fullbright2.transform(matrixStack.peek().getPositionMatrix());
        Vec3f fullbright3 = new Vec3f(fullbright2);
        RenderSystem.setShaderLights(fullbright3, fullbright3);
        MatrixStack newMatrixStack = new MatrixStack();
        VertexConsumerProvider.Immediate immediate = this.game.getBufferBuilders().getEntityVertexConsumers();
        this.game.getItemRenderer().renderItem(stack, ModelTransformation.Mode.NONE, false, newMatrixStack, immediate, 15728880, OverlayTexture.DEFAULT_UV, model);
        immediate.draw();
        matrixStack.pop();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        GLShim.glEnable(2884);
        GLShim.glDisable(2929);
        GLShim.glDepthMask(false);
        GLUtils.unbindFrameBuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix);
        GLShim.glViewport(0, 0, this.game.getWindow().getFramebufferWidth(), this.game.getWindow().getFramebufferHeight());
    }

    private void loadColorPicker() {
        try {
            InputStream is = this.game.getResourceManager().getResource(new Identifier("voxelmap", "images/colorpicker.png")).get().getInputStream();
            Image picker = ImageIO.read(is);
            is.close();
            this.colorPicker = new BufferedImage(picker.getWidth(null), picker.getHeight(null), 2);
            Graphics gfx = this.colorPicker.createGraphics();
            gfx.drawImage(picker, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            System.err.println("Error loading color picker: " + var4.getLocalizedMessage());
        }

    }

    @Override
    public void setSkyColor(int skyColor) {
        this.blockColors[BlockRepository.airID] = skyColor;
        this.blockColors[BlockRepository.voidAirID] = skyColor;
        this.blockColors[BlockRepository.caveAirID] = skyColor;
    }

    private void loadTexturePackTerrainImage() {
        try {
            TextureManager textureManager = this.game.getTextureManager();
            textureManager.bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            BufferedImage terrainStitched = ImageUtils.createBufferedImageFromCurrentGLImage();
            this.terrainBuff = new BufferedImage(terrainStitched.getWidth(null), terrainStitched.getHeight(null), 6);
            Graphics gfx = this.terrainBuff.createGraphics();
            gfx.drawImage(terrainStitched, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            System.err.println("Error processing new resource pack: " + var4.getLocalizedMessage());
            var4.printStackTrace();
        }

    }

    private void loadSpecialColors() {
        int blockStateID;
        for (Iterator<BlockState> blockStateIterator = BlockRepository.pistonTechBlock.getStateManager().getStates().iterator(); blockStateIterator.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = blockStateIterator.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

        for (Iterator<BlockState> var6 = BlockRepository.barrier.getStateManager().getStates().iterator(); var6.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = var6.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

    }

    @Override
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

    @Override
    public final int getBlockColor(MutableBlockPos blockPos, int blockStateID, int biomeID) {
        if (this.loaded) {
            if (this.optifineInstalled && this.biomeTextureAvailable.contains(blockStateID)) {
                Integer col = (Integer) this.blockBiomeSpecificColors.get(blockStateID + " " + biomeID);
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

    private final int getBlockColor(MutableBlockPos blockPos, int blockStateID) {
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

    private int getColor(MutableBlockPos blockPos, BlockState blockState) {
        try {
            int color = this.getColorForBlockPosBlockStateAndFacing(blockPos, blockState, Direction.UP);
            if (color == 452984832) {
                BlockRenderManager blockRendererDispatcher = this.game.getBlockRenderManager();
                color = this.getColorForTerrainSprite(blockState, blockRendererDispatcher);
            }

            Block block = blockState.getBlock();
            if (block == BlockRepository.cobweb) {
                color |= -16777216;
            }

            if (block == BlockRepository.redstone) {
                color = ColorUtils.colorMultiplier(color, this.game.getBlockColors().getColor(blockState, (BlockRenderView) null, (BlockPos) null, 0) | 0xFF000000);
            }

            if (BlockRepository.biomeBlocks.contains(block)) {
                this.applyDefaultBuiltInShading(blockState, color);
            } else {
                this.checkForBiomeTinting(blockPos, blockState, color);
            }

            if (BlockRepository.shapedBlocks.contains(block)) {
                color = this.applyShape(block, color);
            }

            if ((color >> 24 & 0xFF) < 27) {
                color |= 452984832;
            }

            return color;
        } catch (Exception var5) {
            System.err.println("failed getting color: " + blockState.getBlock().getName().getString());
            var5.printStackTrace();
            return 452984832;
        }
    }

    private int getColorForBlockPosBlockStateAndFacing(BlockPos blockPos, BlockState blockState, Direction facing) {
        int color = 452984832;

        try {
            BlockRenderType blockRenderType = blockState.getRenderType();
            BlockRenderManager blockRendererDispatcher = this.game.getBlockRenderManager();
            if (blockRenderType == BlockRenderType.MODEL) {
                BakedModel iBakedModel = blockRendererDispatcher.getModel(blockState);
                List quads = new ArrayList();
                quads.addAll(iBakedModel.getQuads(blockState, facing, this.random));
                quads.addAll(iBakedModel.getQuads(blockState, (Direction) null, this.random));
                BlockModel model = new BlockModel(quads, this.failedToLoadX, this.failedToLoadY);
                if (model.numberOfFaces() > 0) {
                    BufferedImage modelImage = model.getImage(this.terrainBuff);
                    if (modelImage != null) {
                        color = this.getColorForCoordinatesAndImage(new float[]{0.0F, 1.0F, 0.0F, 1.0F}, modelImage);
                    } else {
                        System.out.println("image was null");
                    }
                }
            }
        } catch (Exception var11) {
            System.out.println(var11.getMessage());
            var11.printStackTrace();
            color = 452984832;
        }

        return color;
    }

    private int getColorForTerrainSprite(BlockState blockState, BlockRenderManager blockRendererDispatcher) {
        int color = 452984832;
        BlockModels blockModelShapes = blockRendererDispatcher.getModels();
        Sprite icon = blockModelShapes.getModelParticleSprite(blockState);
        if (icon == blockModelShapes.getModelManager().getMissingModel().getParticleSprite()) {
            Block block = blockState.getBlock();
            Material material = blockState.getMaterial();
            if (block instanceof FluidBlock) {
                if (material == Material.WATER) {
                    icon = (Sprite) this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(new Identifier("minecraft:blocks/water_flow"));
                } else if (material == Material.LAVA) {
                    icon = (Sprite) this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(new Identifier("minecraft:blocks/lava_flow"));
                }
            } else if (material == Material.WATER) {
                icon = (Sprite) this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(new Identifier("minecraft:blocks/water_still"));
            } else if (material == Material.LAVA) {
                icon = (Sprite) this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(new Identifier("minecraft:blocks/lava_still"));
            }
        }

        return this.getColorForIcon(icon);
    }

    private int getColorForIcon(Sprite icon) {
        int color = 452984832;
        if (icon != null) {
            float left = icon.getMinU();
            float right = icon.getMaxU();
            float top = icon.getMinV();
            float bottom = icon.getMaxV();
            color = this.getColorForCoordinatesAndImage(new float[]{left, right, top, bottom}, this.terrainBuff);
        }

        return color;
    }

    private int getColorForCoordinatesAndImage(float[] uv, BufferedImage imageBuff) {
        int color = 452984832;
        if (uv[0] != this.failedToLoadX || uv[2] != this.failedToLoadY) {
            int left = (int) (uv[0] * (float) imageBuff.getWidth());
            int right = (int) Math.ceil((double) (uv[1] * (float) imageBuff.getWidth()));
            int top = (int) (uv[2] * (float) imageBuff.getHeight());
            int bottom = (int) Math.ceil((double) (uv[3] * (float) imageBuff.getHeight()));

            try {
                BufferedImage blockTexture = imageBuff.getSubimage(left, top, right - left, bottom - top);
                Image singlePixel = blockTexture.getScaledInstance(1, 1, 4);
                BufferedImage singlePixelBuff = new BufferedImage(1, 1, imageBuff.getType());
                Graphics gfx = singlePixelBuff.createGraphics();
                gfx.drawImage(singlePixel, 0, 0, (ImageObserver) null);
                gfx.dispose();
                color = singlePixelBuff.getRGB(0, 0);
            } catch (RasterFormatException var12) {
                System.out.println("error getting color");
                System.out.println(left + " " + right + " " + top + " " + bottom);
                color = 452984832;
            }
        }

        return color;
    }

    private void applyDefaultBuiltInShading(BlockState blockState, int color) {
        Block block = blockState.getBlock();
        int blockStateID = BlockRepository.getStateId(blockState);
        if (block != BlockRepository.largeFern && block != BlockRepository.tallGrass && block != BlockRepository.reeds) {
            if (block == BlockRepository.water) {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, BiomeRepository.FOREST.getWaterColor() | 0xFF000000);
            } else {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, this.game.getBlockColors().getColor(blockState, (BlockRenderView) null, (BlockPos) null, 0) | 0xFF000000);
            }
        } else {
            this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, GrassColors.getColor(0.7, 0.8) | 0xFF000000);
        }

    }

    private void checkForBiomeTinting(MutableBlockPos blockPos, BlockState blockState, int color) {
        try {
            Block block = blockState.getBlock();
            String blockName = Registry.BLOCK.getId(block) + "";
            if (BlockRepository.biomeBlocks.contains(block) || !blockName.startsWith("minecraft:")) {
                int tint = -1;
                MutableBlockPos tempBlockPos = new MutableBlockPos(0, 0, 0);
                if (blockPos == this.dummyBlockPos) {
                    tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, (byte) 4);
                } else {
                    Chunk chunk = this.game.world.getChunk(blockPos);
                    if (chunk != null && !((WorldChunk) chunk).isEmpty() && this.game.world.isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                        tint = this.game.getBlockColors().getColor(blockState, this.game.world, blockPos, 1) | 0xFF000000;
                    } else {
                        tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, (byte) 4);
                    }
                }

                if (tint != 16777215 && tint != -1) {
                    int blockStateID = BlockRepository.getStateId(blockState);
                    this.biomeTintsAvailable.add(blockStateID);
                    this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, tint);
                    //this.createTintTable(blockState, tempBlockPos);
                } else {
                    this.blockColorsWithDefaultTint[BlockRepository.getStateId(blockState)] = 452984832;
                }
            }
        } catch (Exception var9) {
        }

    }

    private int tintFromFakePlacedBlock(BlockState blockState, MutableBlockPos loopBlockPos, byte biomeID) {
        ClientWorld world = this.game.world;
        if (world == null) {
            return -1;
        } else if (blockState.getBlock() == null) {
            return -1;
        } else {
            int tint = -1;
            //TODO Update 1.18 xD
            /*
            try {
                int fakeX = (int) this.game.player.getX() - 32;
                int fakeZ = (int) this.game.player.getZ() - 32;
                Chunk chunk = world.getChunk(loopBlockPos.withXYZ(fakeX, 0, fakeZ));
                BlockState actualBlockState = world.getBlockState(loopBlockPos);
                chunk.setBlockState(loopBlockPos, blockState, false);

                BiomeArray biomeArray = chunk.getBiomeArray();
                Biome[] currentBiomes = (Biome[]) ReflectionUtils.getPrivateFieldValueByType(biomeArray, BiomeArray.class, Biome[].class);
                Biome[] originalBiomes = new Biome[currentBiomes.length];
                System.arraycopy(currentBiomes, 0, originalBiomes, 0, currentBiomes.length);
                Arrays.fill(currentBiomes, world.getRegistryManager().get(Registry.BIOME_KEY).get(biomeID));
                world.resetChunkColor(chunk.getPos());
                tint = this.game.getBlockColors().getColor(blockState, world, loopBlockPos.withXYZ(fakeX + 256, 0, fakeZ), 1) | 0xFF000000;
                tint = this.game.getBlockColors().getColor(blockState, world, loopBlockPos.withXYZ(fakeX, 0, fakeZ), 1) | 0xFF000000;
                System.arraycopy(originalBiomes, 0, currentBiomes, 0, currentBiomes.length);
                chunk.setBlockState(loopBlockPos, actualBlockState, false);
                world.resetChunkColor(chunk.getPos());
            } catch (Exception var13) {
            }
             */

            return tint;
        }
    }
    //TODO Update 1.18 xD
    /*
    private void createTintTable(BlockState blockState, MutableBlockPos loopBlockPos) {
        ClientWorld world = this.game.world;
        if (world != null) {
            Block block = blockState.getBlock();
            if (block != null) {
                try {
                    int[][] tints = new int[this.sizeOfBiomeArray][32];

                    for (int[] row : tints) {
                        Arrays.fill(row, -1);
                    }

                    int fakeX = (int) this.game.player.getX() - 32;
                    int fakeZ = (int) this.game.player.getZ() - 32;
                    Chunk chunk = world.getChunk(loopBlockPos.withXYZ(fakeX, 64, fakeZ));
                    BlockState actualBlockState = world.getBlockState(loopBlockPos);
                    chunk.setBlockState(loopBlockPos, blockState, false);
                    BiomeArray biomeArray = chunk.getBiomeArray();
                    Biome[] currentBiomes = (Biome[]) ReflectionUtils.getPrivateFieldValueByType(biomeArray, BiomeArray.class, Biome[].class);
                    Biome[] originalBiomes = new Biome[currentBiomes.length];
                    System.arraycopy(currentBiomes, 0, originalBiomes, 0, currentBiomes.length);

                    for (int biomeID = 0; biomeID < this.sizeOfBiomeArray; ++biomeID) {
                        Biome biome = (Biome) world.getRegistryManager().get(Registry.BIOME_KEY).get(biomeID);
                        if (biome != null) {
                            int[] row = new int[32];
                            Arrays.fill(currentBiomes, biome);
                            world.resetChunkColor(chunk.getPos());
                            int tint = this.game.getBlockColors().getColor(blockState, world, loopBlockPos.withXYZ(fakeX + 264, 64, fakeZ + 264), 1) | 0xFF000000;
                            tint = this.game.getBlockColors().getColor(blockState, world, loopBlockPos.withXYZ(fakeX, 64, fakeZ), 1) | 0xFF000000;
                            Arrays.fill(row, tint);
                            tints[biomeID] = row;
                        }
                    }

                    System.arraycopy(originalBiomes, 0, currentBiomes, 0, currentBiomes.length);
                    chunk.setBlockState(loopBlockPos, actualBlockState, false);
                    world.resetChunkColor(chunk.getPos());
                    int blockStateID = BlockRepository.getStateId(blockState);
                    this.blockTintTables.put(blockStateID, tints);
                } catch (Exception var17) {
                }

            }
        }
    }
     */

    @Override
    public int getBiomeTint(AbstractMapData mapData, World world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        Chunk chunk = world.getChunk(blockPos);
        boolean live = chunk != null && !((WorldChunk) chunk).isEmpty() && this.game.world.isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        live = live && this.game.world.isChunkLoaded(blockPos);
        int tint = -2;
        if (this.optifineInstalled || !live && this.biomeTintsAvailable.contains(blockStateID)) {
            try {
                int[][] tints = (int[][]) this.blockTintTables.get(blockStateID);
                if (tints != null) {
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    for (int t = blockPos.getX() - 1; t <= blockPos.getX() + 1; ++t) {
                        for (int s = blockPos.getZ() - 1; s <= blockPos.getZ() + 1; ++s) {
                            int biomeID = 0;
                            if (live) {
                                biomeID = world.getRegistryManager().get(Registry.BIOME_KEY).getRawId(world.getBiome(loopBlockPos.withXYZ(t, blockPos.getY(), s)).value());
                            } else {
                                int dataX = t - startX;
                                int dataZ = s - startZ;
                                dataX = Math.max(dataX, 0);
                                dataX = Math.min(dataX, mapData.getWidth() - 1);
                                dataZ = Math.max(dataZ, 0);
                                dataZ = Math.min(dataZ, mapData.getHeight() - 1);
                                biomeID = mapData.getBiomeID(dataX, dataZ);
                            }

                            if (biomeID < 0) {
                                biomeID = 1;
                            }

                            int biomeTint = tints[biomeID][loopBlockPos.y / 8];
                            r += (biomeTint & 0xFF0000) >> 16;
                            g += (biomeTint & 0xFF00) >> 8;
                            b += biomeTint & 0xFF;
                        }
                    }

                    tint = 0xFF000000 | (r / 9 & 0xFF) << 16 | (g / 9 & 0xFF) << 8 | b / 9 & 0xFF;
                }
            } catch (Exception var22) {
                tint = -2;
            }
        }

        if (tint == -2) {
            tint = this.getBuiltInBiomeTint(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ, live);
        }

        return tint;
    }

    private int getBuiltInBiomeTint(AbstractMapData mapData, World world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ, boolean live) {
        int tint = -1;
        Block block = blockState.getBlock();
        if (BlockRepository.biomeBlocks.contains(block) || this.biomeTintsAvailable.contains(blockStateID)) {
            if (live) {
                try {
                    tint = this.game.getBlockColors().getColor(blockState, world, blockPos, 0) | 0xFF000000;
                } catch (Exception var13) {
                }
            }

            if (tint == -1) {
                tint = this.getBuiltInBiomeTintFromUnloadedChunk(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ) | 0xFF000000;
            }
        }

        return tint;
    }

    private int getBuiltInBiomeTintFromUnloadedChunk(AbstractMapData mapData, World world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
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
                    int biomeID = mapData.getBiomeID(dataX, dataZ);
                    Biome biome = (Biome) world.getRegistryManager().get(Registry.BIOME_KEY).get(biomeID);
                    if (biome == null) {
                        MessageUtils.printDebug("Null biome ID! " + biomeID + " at " + t + "," + s);
                        MessageUtils.printDebug("block: " + mapData.getBlockstate(dataX, dataZ) + ", height: " + mapData.getHeight(dataX, dataZ));
                        MessageUtils.printDebug("Mapdata: " + mapData.toString());
                        biome = BiomeRepository.FOREST;
                    }

                    int biomeTint = colorResolver.getColorAtPos(blockState, biome, loopBlockPos.withXYZ(t, blockPos.getY(), s));
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

    private int getCustomBlockBiomeTintFromUnloadedChunk(AbstractMapData mapData, World world, BlockState blockState, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        int tint = -1;

        try {
            int dataX = blockPos.getX() - startX;
            int dataZ = blockPos.getZ() - startZ;
            dataX = Math.max(dataX, 0);
            dataX = Math.min(dataX, mapData.getWidth() - 1);
            dataZ = Math.max(dataZ, 0);
            dataZ = Math.min(dataZ, mapData.getHeight() - 1);
            byte biomeID = (byte) mapData.getBiomeID(dataX, dataZ);
            tint = this.tintFromFakePlacedBlock(blockState, loopBlockPos, biomeID);
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
        if (block instanceof AbstractSignBlock) {
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
        Identifier propertiesFile = new Identifier("minecraft", "optifine/renderpass.properties");

        try {
            InputStream input = this.game.getResourceManager().getResource(propertiesFile).get().getInputStream();
            if (input != null) {
                properties.load(input);
                input.close();
                this.renderPassThreeBlendMode = properties.getProperty("blend.3", "alpha");
            }
        } catch (IOException var9) {
            this.renderPassThreeBlendMode = "alpha";
        }

        String namespace = "minecraft";

        for (Identifier s : this.findResources(namespace, "/optifine/ctm", ".properties", true, false, true)) {
            try {
                this.loadCTM(s);
            } catch (NumberFormatException var7) {
            } catch (IllegalArgumentException var8) {
            }
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

    private void loadCTM(Identifier propertiesFile) {
        if (propertiesFile != null) {
            BlockRenderManager blockRendererDispatcher = this.game.getBlockRenderManager();
            BlockModels blockModelShapes = blockRendererDispatcher.getModels();
            Properties properties = new Properties();

            try {
                InputStream input = this.game.getResourceManager().getResource(propertiesFile).get().getInputStream();
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
            Set<BlockState> blockStates = new HashSet();
            blockStates.addAll(this.parseBlocksList(matchBlocks, metadata));
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
            if (blockStates.size() == 0) {
                Block block = null;
                Pattern pattern = Pattern.compile(".*/block_(.+).properties");
                Matcher matcher = pattern.matcher(filePath);
                if (matcher.find()) {
                    block = this.getBlockFromName(matcher.group(1));
                    if (block != null) {
                        Set<BlockState> matching = this.parseBlockMetadata(block, metadata);
                        if (matching.size() == 0) {
                            matching.addAll(block.getStateManager().getStates());
                        }

                        blockStates.addAll(matching);
                    }
                } else {
                    if (matchTiles.equals("")) {
                        matchTiles = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf(".properties"));
                    }

                    if (!matchTiles.contains(":")) {
                        matchTiles = "minecraft:blocks/" + matchTiles;
                    }

                    Identifier matchID = new Identifier(matchTiles);
                    Sprite compareIcon = (Sprite) this.game.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(matchID);
                    if (compareIcon.getId() != MissingSprite.getMissingSpriteId()) {
                        ArrayList tmpList = new ArrayList();

                        for (Block testBlock : Registry.BLOCK) {
                            UnmodifiableIterator blockStateID = testBlock.getStateManager().getStates().iterator();

                            while (blockStateID.hasNext()) {
                                BlockState blockState = (BlockState) blockStateID.next();

                                try {
                                    BakedModel bakedModel = blockModelShapes.getModel(blockState);
                                    List quads = new ArrayList();
                                    quads.addAll(bakedModel.getQuads(blockState, Direction.UP, this.random));
                                    quads.addAll(bakedModel.getQuads(blockState, (Direction) null, this.random));
                                    BlockModel model = new BlockModel(quads, this.failedToLoadX, this.failedToLoadY);
                                    if (model.numberOfFaces() > 0) {
                                        ArrayList blockFaces = model.getFaces();

                                        for (int i = 0; i < blockFaces.size(); ++i) {
                                            BlockModel.BlockFace face = (BlockModel.BlockFace) model.getFaces().get(i);
                                            float minU = face.getMinU();
                                            float maxU = face.getMaxU();
                                            float minV = face.getMinV();
                                            float maxV = face.getMaxV();
                                            if (this.similarEnough(minU, maxU, minV, maxV, compareIcon.getMinU(), compareIcon.getMaxU(), compareIcon.getMinV(), compareIcon.getMaxV())) {
                                                tmpList.add(blockState);
                                            }
                                        }
                                    }
                                } catch (Exception var41) {
                                }
                            }
                        }

                        blockStates.addAll(tmpList);
                    }
                }
            }

            if (blockStates.size() != 0) {
                if (!method.equals("horizontal") && !method.startsWith("overlay") && (method.equals("sandstone") || method.equals("top") || faces.contains("top") || faces.contains("all") || faces.length() == 0)) {
                    try {
                        Identifier pngResource = new Identifier(propertiesFile.getNamespace(), tilePath);
                        InputStream is = this.game.getResourceManager().getResource(pngResource).get().getInputStream();
                        Image top = ImageIO.read(is);
                        is.close();
                        top = top.getScaledInstance(1, 1, 4);
                        BufferedImage topBuff = new BufferedImage(top.getWidth((ImageObserver) null), top.getHeight((ImageObserver) null), 6);
                        Graphics gfx = topBuff.createGraphics();
                        gfx.drawImage(top, 0, 0, (ImageObserver) null);
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
                            if (!biomes.equals("")) {
                                this.biomeTextureAvailable.add(blockStateID);

                                for (int r = 0; r < biomesArray.length; ++r) {
                                    int biomeInt = this.parseBiomeName(biomesArray[r]);
                                    if (biomeInt != -1) {
                                        this.blockBiomeSpecificColors.put(blockStateID + " " + biomeInt, topRGB);
                                    }
                                }
                            } else {
                                this.blockColors[blockStateID] = topRGB;
                            }
                        }
                    } catch (IOException var40) {
                        System.err.println("error getting CTM block from " + propertiesFile.getPath() + ": " + filePath + " " + Registry.BLOCK.getId(((BlockState) blockStates.iterator().next()).getBlock()).toString() + " " + tilePath);
                        var40.printStackTrace();
                    }
                }

            }
        }
    }

    private boolean similarEnough(float a, float b, float c, float d, float one, float two, float three, float four) {
        boolean similar = (double) Math.abs(a - one) < 1.0E-4;
        similar = similar && (double) Math.abs(b - two) < 1.0E-4;
        similar = similar && (double) Math.abs(c - three) < 1.0E-4;
        return similar && (double) Math.abs(d - four) < 1.0E-4;
    }

    private int processRenderPassThree(int rgb) {
        if (this.renderPassThreeBlendMode.equals("color") || this.renderPassThreeBlendMode.equals("overlay")) {
            int red = rgb >> 16 & 0xFF;
            int green = rgb >> 8 & 0xFF;
            int blue = rgb & 0xFF;
            float colorAverage = (float) (red + blue + green) / 3.0F;
            float lighteningFactor = (colorAverage - 127.5F) * 2.0F;
            red += (int) ((float) red * (lighteningFactor / 255.0F));
            blue += (int) ((float) red * (lighteningFactor / 255.0F));
            green += (int) ((float) red * (lighteningFactor / 255.0F));
            int newAlpha = (int) Math.abs(lighteningFactor);
            rgb = newAlpha << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
        }

        return rgb;
    }

    private String[] parseStringList(String list) {
        ArrayList tmpList = new ArrayList();

        for (String token : list.split("\\s+")) {
            token = token.trim();

            try {
                if (token.matches("^\\d+$")) {
                    tmpList.add(Integer.parseInt(token) + "");
                } else if (token.matches("^\\d+-\\d+$")) {
                    String[] t = token.split("-");
                    int min = Integer.parseInt(t[0]);
                    int max = Integer.parseInt(t[1]);

                    for (int i = min; i <= max; ++i) {
                        tmpList.add(i + "");
                    }
                } else if (token != null && token != "") {
                    tmpList.add(token);
                }
            } catch (NumberFormatException var11) {
            }
        }

        String[] a = new String[tmpList.size()];

        for (int i = 0; i < a.length; ++i) {
            a[i] = (String) tmpList.get(i);
        }

        return a;
    }

    private Set parseBlocksList(String blocks, String metadataLine) {
        Set blockStates = new HashSet();

        for (String blockString : blocks.split("\\s+")) {
            String metadata = metadataLine;
            blockString = blockString.trim();
            String[] blockComponents = blockString.split(":");
            int tokensUsed = 0;
            Block block = null;
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
                    metadata = blockComponents[tokensUsed];

                    for (int t = tokensUsed + 1; t < blockComponents.length; ++t) {
                        metadata = metadata + ":" + blockComponents[t];
                    }
                }

                blockStates.addAll(this.parseBlockMetadata(block, metadata));
            }
        }

        return blockStates;
    }

    private Set parseBlockMetadata(Block block, String metadataList) {
        Set blockStates = new HashSet();
        if (metadataList.equals("")) {
            blockStates.addAll(block.getStateManager().getStates());
        } else {
            Set<String> valuePairs = new HashSet();

            for (String metadata : metadataList.split(":")) {
                metadata.trim();
                if (metadata.contains("=")) {
                    valuePairs.add(metadata);
                }
            }

            if (valuePairs.size() > 0) {
                UnmodifiableIterator var22 = block.getStateManager().getStates().iterator();

                while (var22.hasNext()) {
                    BlockState blockState = (BlockState) var22.next();
                    boolean matches = true;

                    for (String pair : valuePairs) {
                        String[] propertyAndValues = pair.split("\\s*=\\s*", 5);
                        if (propertyAndValues.length == 2) {
                            Property property = block.getStateManager().getProperty(propertyAndValues[0]);
                            if (property != null) {
                                boolean valueIncluded = false;
                                String[] values = propertyAndValues[1].split(",");

                                for (String value : values) {
                                    if (property.getType() == Integer.class && value.matches("^\\d+-\\d+$")) {
                                        String[] range = value.split("-");
                                        int min = Integer.parseInt(range[0]);
                                        int max = Integer.parseInt(range[1]);
                                        int intValue = Integer.class.cast(blockState.get(property));
                                        if (intValue >= min && intValue <= max) {
                                            valueIncluded = true;
                                        }
                                    } else if (!blockState.get(property).equals(property.parse(value))) {
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
        Biome biome = this.world.getRegistryManager().get(Registry.BIOME_KEY).get(new Identifier(name));
        return biome != null ? this.world.getRegistryManager().get(Registry.BIOME_KEY).getRawId(biome) : -1;
    }

    private List<Identifier> findResources(String namespace, String directory, String suffixMaybeNull, boolean recursive, boolean directories, boolean sortByFilename) {
        if (directory == null) {
            directory = "";
        }

        if (directory.startsWith("/")) {
            directory = directory.substring(1);
        }

        String suffix = suffixMaybeNull == null ? "" : suffixMaybeNull;
        ArrayList<Identifier> resources = new ArrayList<>();

        Map<Identifier, Resource> resourceMap = this.game.getResourceManager().findResources(directory, asset -> asset.getPath().endsWith(suffix));
        for (Identifier candidate : resourceMap.keySet()) { //TODO 1.19
            if (candidate.getNamespace().equals(namespace)) {
                resources.add(candidate);
            }
        }

        if (sortByFilename) {
            resources.sort((o1, o2) -> {
                String f1 = o1.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                String f2 = o2.getPath().replaceAll(".*/", "").replaceFirst("\\.properties", "");
                int result = f1.compareTo(f2);
                return result != 0 ? result : o1.getPath().compareTo(o2.getPath());
            });
        } else {
            resources.sort(Comparator.comparing(Identifier::getPath));
        }

        return resources;
    }

    private void processColorProperties() {
        Properties properties = new Properties();

        try {
            InputStream input = this.game.getResourceManager().getResource(new Identifier("optifine/color.properties")).get().getInputStream();
            if (input != null) {
                properties.load(input);
                input.close();
            }
        } catch (IOException ignored) {
        }

        BlockState blockState = BlockRepository.lilypad.getDefaultState();
        int blockStateID = BlockRepository.getStateId(blockState);
        int lilyRGB = this.getBlockColor(blockStateID);
        int lilypadMultiplier = 2129968;
        String lilypadMultiplierString = properties.getProperty("lilypad");
        if (lilypadMultiplierString != null) {
            lilypadMultiplier = Integer.parseInt(lilypadMultiplierString, 16);
        }

        for (UnmodifiableIterator<BlockState> defaultFormat = BlockRepository.lilypad.getStateManager().getStates().iterator(); defaultFormat.hasNext(); this.blockColorsWithDefaultTint[blockStateID] = this.blockColors[blockStateID]) {
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
                this.processColorPropertyHelper(new Identifier(filename), properties.getProperty(key), globalGrid);
            }
        }

        for (Identifier resource : this.findResources("minecraft", "/optifine/colormap/blocks", ".properties", true, false, true)) {
            Properties colorProperties = new Properties();

            try {
                InputStream input = this.game.getResourceManager().getResource(resource).get().getInputStream();
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
                name = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf(".properties"));
                names = name;
            }

            String source = colorProperties.getProperty("source");
            Identifier resourcePNG;
            if (source != null) {
                resourcePNG = new Identifier(resource.getNamespace(), source);

                this.game.getResourceManager().getResource(resourcePNG);
            } else {
                resourcePNG = new Identifier(resource.getNamespace(), resource.getPath().replace(".properties", ".png"));
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

        this.processColorPropertyHelper(new Identifier("optifine/colormap/water.png"), "water", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/watercolorx.png"), "water", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/swampgrass.png"), "grass_block grass fern tall_grass large_fern", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/swampgrasscolor.png"), "grass_block grass fern tall_grass large_fern", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/swampfoliage.png"), "oak_leaves vine", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/swampfoliagecolor.png"), "oak_leaves vine", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/pine.png"), "spruce_leaves", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/pinecolor.png"), "spruce_leaves", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/birch.png"), "birch_leaves", globalGrid);
        this.processColorPropertyHelper(new Identifier("optifine/colormap/birchcolor.png"), "birch_leaves", globalGrid);
    }

    private void processColorPropertyHelper(Identifier resource, String list, boolean grid) {
        Identifier resourceProperties = new Identifier(resource.getNamespace(), resource.getPath().replace(".png", ".properties"));
        Properties colorProperties = new Properties();
        int yOffset = 0;

        try {
            InputStream input = this.game.getResourceManager().getResource(resourceProperties).get().getInputStream();
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
                yOffset = Integer.valueOf(yOffsetString);
            }
        } catch (IOException ignored) {
        }

        this.processColorProperty(resource, list, grid, yOffset);
    }

    private void processColorProperty(Identifier resource, String list, boolean grid, int yOffset) {
        int[][] tints = new int[this.sizeOfBiomeArray][32];

        for (int[] row : tints) {
            Arrays.fill(row, -1);
        }

        boolean swamp = resource.getPath().contains("/swamp");
        Image tintColors;

        try {
            InputStream is = this.game.getResourceManager().getResource(resource).get().getInputStream();
            tintColors = ImageIO.read(is);
            is.close();
        } catch (IOException var21) {
            return;
        }

        BufferedImage tintColorsBuff = new BufferedImage(tintColors.getWidth((ImageObserver) null), tintColors.getHeight((ImageObserver) null), 1);
        Graphics gfx = tintColorsBuff.createGraphics();
        gfx.drawImage(tintColors, 0, 0, null);
        gfx.dispose();
        int numBiomesToCheck = grid ? Math.min(tintColorsBuff.getWidth(), this.sizeOfBiomeArray) : this.sizeOfBiomeArray;

        for (int t = 0; t < numBiomesToCheck; ++t) {
            Biome biome = this.world.getRegistryManager().get(Registry.BIOME_KEY).get(t);
            if (biome != null) {
                int tintMult = 0;
                int heightMultiplier = tintColorsBuff.getHeight() / 32;

                for (int s = 0; s < 32; ++s) {
                    if (grid) {
                        tintMult = tintColorsBuff.getRGB(t, Math.max(0, s * heightMultiplier - yOffset)) & 16777215;
                    } else {
                        double var1 = MathHelper.clamp(biome.getTemperature(), 0.0F, 1.0F);
                        double var2 = MathHelper.clamp(biome.getDownfall(), 0.0F, 1.0F);
                        var2 *= var1;
                        var1 = 1.0 - var1;
                        var2 = 1.0 - var2;
                        tintMult = tintColorsBuff.getRGB((int) ((double) (tintColorsBuff.getWidth() - 1) * var1), (int) ((double) (tintColorsBuff.getHeight() - 1) * var2)) & 16777215;
                    }

                    if (tintMult != 0 && (!swamp || biome == BiomeRepository.SWAMP || biome == BiomeRepository.SWAMP_HILLS)) {
                        tints[t][s] = tintMult;
                    }
                }
            }
        }

        Set<BlockState> blockStates = new HashSet();
        blockStates.addAll(this.parseBlocksList(list, ""));

        for (BlockState blockState : blockStates) {
            int blockStateID = BlockRepository.getStateId(blockState);
            int[][] previousTints = (int[][]) this.blockTintTables.get(blockStateID);
            if (swamp && previousTints == null) {
                Identifier defaultResource;
                if (resource.getPath().contains("grass")) {
                    defaultResource = new Identifier("textures/colormap/grass.png");
                } else {
                    defaultResource = new Identifier("textures/colormap/foliage.png");
                }

                String stateString = blockState.toString().toLowerCase();
                stateString = stateString.replaceAll("^block", "");
                stateString = stateString.replace("{", "");
                stateString = stateString.replace("}", "");
                stateString = stateString.replace("[", ":");
                stateString = stateString.replace("]", "");
                stateString = stateString.replace(",", ":");
                this.processColorProperty(defaultResource, stateString, false, 0);
                previousTints = (int[][]) this.blockTintTables.get(blockStateID);
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
            Identifier resourceLocation = new Identifier(name);
            return Registry.BLOCK.containsId(resourceLocation) ? Registry.BLOCK.get(resourceLocation) : null;
        } catch (InvalidIdentifierException | NumberFormatException var3) {
            return null;
        }
    }

    private interface ColorResolver {
        int getColorAtPos(BlockState var1, Biome var2, BlockPos var3);
    }
}
