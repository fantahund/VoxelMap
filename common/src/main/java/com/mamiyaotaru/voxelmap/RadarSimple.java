package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapRenderTypes;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import org.joml.Matrix4fStack;

public class RadarSimple extends AbstractRadar {
    private final TextureAtlas textureAtlas;
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath("voxelmap", "atlas/radarsimple/marker");

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
            NativeImage contact = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath("voxelmap", "images/radar/contact.png")).image();
            textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath("voxelmap", "images/radar/contact_facing.png")).image();
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
    protected void updateContact(Contact contact) {
        super.updateContact(contact);
        contact.rotationFactor = contact.entity.getYHeadRot();
    }

    @Override
    public void renderMapMobs(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Contact.DisplayState displayState, int x, int y, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        RenderType iconRenderType = VoxelMapRenderTypes.GUI_TEXTURED_LEQUAL_DEPTH_TEST.apply(resourceTextureAtlasMarker);
        VertexConsumer iconBuffer = bufferSource.getBuffer(iconRenderType);

        for (Contact contact : contacts) {
            contact.updateLocation();

            if (contact.displayState != displayState) {
                continue;
            }

            matrixStack.pushMatrix();
            applyContactTransform(matrixStack, contact, x, y);

            int color = minimapContext.playerY - contact.y < 0 ? ARGB.colorFromFloat(contact.brightness, 1, 1, 1) : ARGB.colorFromFloat(1, contact.brightness, contact.brightness, contact.brightness);
            switch (contact.category) {
                case HOSTILE -> color = ARGB.multiply(color, 0xFFFF8080);
                case NEUTRAL -> color = ARGB.multiply(color, 0xFF80FF80);
            }

            Sprite contactIcon = textureAtlas.getAtlasSprite("contact");
            RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, contactIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);

            if (radarOptions.showFacing) {
                Sprite facingIcon = textureAtlas.getAtlasSprite("facing");
                RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, facingIcon, x - 4.0F, y - 4.0F, 0.0F, 8.0F, 8.0F, color);
            }

            matrixStack.popMatrix();
        }
        bufferSource.endBatch(iconRenderType);

        matrixStack.popMatrix();
    }
}
