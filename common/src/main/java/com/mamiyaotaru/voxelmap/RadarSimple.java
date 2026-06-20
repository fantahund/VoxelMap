package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.render.DeferredRenderPass;
import com.mamiyaotaru.voxelmap.render.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fStack;

public class RadarSimple extends AbstractRadar {
    private final TextureAtlas textureAtlas;
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "atlas/radarsimple/marker");

    public RadarSimple() {
        textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
        textureAtlas.sampler = VoxelMapPipelines.LINEAR_CLAMP_SAMPLER;
        loadTexturePackIcons();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        try {
            textureAtlas.reset();
            NativeImage contact = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/contact.png")).image();
            textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath(VoxelConstants.MOD_ID, "images/radar/contact_facing.png")).image();
            textureAtlas.registerIconForBufferedImage("facing", facing);
            textureAtlas.stitch();
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Failed getting mobs " + var4.getLocalizedMessage(), var4);
        }
    }

    @Override
    protected void initContact(Contact contact) {
    }

    @Override
    public void renderMapMobs(Matrix4fStack matrixStack, DeferredRenderPass pass, int x, int y, Contact.DisplayState displayState, int scScale, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        pass.setPipeline(VoxelMapPipelines.GUI_TEXTURED_DEPTH_TEST);
        pass.bindTexture("Sampler0", textureAtlas);
        pass.beginBatch();
        for (Contact contact : contacts) {
            if (contact.displayState != displayState) {
                continue;
            }

            try {
                matrixStack.pushMatrix();

                float facing = contact.entity.getYHeadRot();
                if (mapOptions.rotates.get()) {
                    facing -= minimapContext.direction;
                } else if (mapOptions.oldNorth.get()) {
                    facing += 90.0F;
                }
                float distance = (float) (contact.distance / minimapContext.zoomScaleAdjusted);
                matrixStack.translate(x, y, 0.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
                matrixStack.translate(0.0F, -distance, 0.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + facing));
                matrixStack.translate(-x, -y, 0.0F);

                int color;
                if (minimapContext.playerY - contact.y < 0) {
                    color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
                } else {
                    float brightness = contact.brightness * 0.7F + 0.3F;
                    color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
                }
                switch (contact.category) {
                    case HOSTILE -> color = ARGB.multiply(color, 0xFFFF8080);
                    case NEUTRAL -> color = ARGB.multiply(color, 0xFF80FF80);
                }

                Sprite contactIcon = textureAtlas.getAtlasSprite("contact");
                pass.drawSpriteRect(matrixStack, contactIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);

                if (radarOptions.showFacing.get()) {
                    Sprite facingIcon = textureAtlas.getAtlasSprite("facing");
                    pass.drawSpriteRect(matrixStack, facingIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);
                }
            } catch (Exception e) {
                VoxelConstants.getLogger().error("Error rendering mob icon! {} contact type {}", e.getLocalizedMessage(), BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
            } finally {
                matrixStack.popMatrix();
            }
        }
        pass.endBatch();

        matrixStack.popMatrix();
    }
}
