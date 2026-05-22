package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
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
        super();
        textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
        textureAtlas.setFilter(true, false);
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
    public void renderMapMobs(Matrix4fStack matrixStack, Contact.DisplayState displayState, int x, int y, int scScale, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, textureAtlas.getIdentifier());
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

                int color = minimapContext.playerY - contact.y < 0 ? ARGB.colorFromFloat(contact.brightness, 1, 1, 1) : ARGB.colorFromFloat(1, contact.brightness, contact.brightness, contact.brightness);
                switch (contact.category) {
                    case HOSTILE -> color = ARGB.multiply(color, 0xFFFF8080);
                    case NEUTRAL -> color = ARGB.multiply(color, 0xFF80FF80);
                }

                Sprite contactIcon = textureAtlas.getAtlasSprite("contact");
                RenderUtils.drawSpriteRect(matrixStack, contactIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);

                if (radarOptions.showFacing.get()) {
                    Sprite facingIcon = textureAtlas.getAtlasSprite("facing");
                    RenderUtils.drawSpriteRect(matrixStack, facingIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);
                }
            } catch (Exception e) {
                VoxelConstants.getLogger().error("Error rendering mob icon! {} contact type {}", e.getLocalizedMessage(), BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
            } finally {
                matrixStack.popMatrix();
            }
        }
        RenderUtils.endBatch();

        matrixStack.popMatrix();
    }
}
