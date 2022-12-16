package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.render.model.BakedQuad;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class BlockModel {
    ArrayList<BlockFace> faces;
    BlockVertex[] longestSide;
    float failedToLoadX;
    float failedToLoadY;

    public BlockModel(List<BakedQuad> quads, float failedToLoadX, float failedToLoadY) {
        BlockFace face;
        this.failedToLoadX = failedToLoadX;
        this.failedToLoadY = failedToLoadY;
        this.faces = new ArrayList<>();
        for (BakedQuad quad2 : quads) {
            face = new BlockFace(quad2.getVertexData());
            if (!face.isClockwise || face.isVertical)
                continue;
            this.faces.add(face);
        }
        Collections.sort(this.faces);
        this.longestSide = new BlockVertex[2];
        float greatestLength = 0.0f;
        for (BlockFace face2 : this.faces) {
            float uDiff = face2.longestSide[0].u - face2.longestSide[1].u;
            float vDiff = face2.longestSide[0].v - face2.longestSide[1].v;
            float segmentLength = (float) Math.sqrt(uDiff * uDiff + vDiff * vDiff);
            if (!(segmentLength > greatestLength))
                continue;
            greatestLength = segmentLength;
            this.longestSide = face2.longestSide;
        }
    }

    public int numberOfFaces() {
        return this.faces.size();
    }

    public ArrayList<BlockFace> getFaces() {
        return this.faces;
    }

    public BufferedImage getImage(BufferedImage terrainImage) {
        float terrainImageAspectRatio = (float) terrainImage.getWidth() / (float) terrainImage.getHeight();
        float longestSideUV = Math.max(Math.abs(this.longestSide[0].u - this.longestSide[1].u), Math.abs(this.longestSide[0].v - this.longestSide[1].v) / terrainImageAspectRatio);
        float modelImageWidthUV = longestSideUV / Math.max(Math.abs(this.longestSide[0].x - this.longestSide[1].x), Math.abs(this.longestSide[0].z - this.longestSide[1].z));
        int modelImageWidth = Math.round(modelImageWidthUV * (float) terrainImage.getWidth());
        BufferedImage modelImage = new BufferedImage(modelImageWidth, modelImageWidth, 6);
        Graphics2D g2 = modelImage.createGraphics();
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, modelImage.getWidth(), modelImage.getHeight());
        g2.dispose();

        for (BlockFace var32 : this.faces) {
            float minU = var32.getMinU();
            float maxU = var32.getMaxU();
            float minV = var32.getMinV();
            float maxV = var32.getMaxV();
            float minX = var32.getMinX();
            float maxX = var32.getMaxX();
            float minZ = var32.getMinZ();
            float maxZ = var32.getMaxZ();
            if (this.similarEnough(minU, minV, this.failedToLoadX, this.failedToLoadY)) {
                return null;
            }

            int faceImageX = Math.round(minX * (float) modelImage.getWidth());
            int faceImageY = Math.round(minZ * (float) modelImage.getHeight());
            int faceImageWidth = Math.round(maxX * (float) modelImage.getWidth()) - faceImageX;
            int faceImageHeight = Math.round(maxZ * (float) modelImage.getHeight()) - faceImageY;
            if (faceImageWidth == 0) {
                if (faceImageX > modelImageWidth - 1) {
                    faceImageX = modelImageWidth - 1;
                }

                faceImageWidth = 1;
            }

            if (faceImageHeight == 0) {
                if (faceImageY > modelImageWidth - 1) {
                    faceImageY = modelImageWidth - 1;
                }

                faceImageHeight = 1;
            }

            int faceImageU = Math.round(minU * (float) terrainImage.getWidth());
            int faceImageV = Math.round(minV * (float) terrainImage.getHeight());
            int faceImageUVWidth = Math.round(maxU * (float) terrainImage.getWidth()) - faceImageU;
            int faceImageUVHeight = Math.round(maxV * (float) terrainImage.getHeight()) - faceImageV;
            if (faceImageUVWidth == 0) {
                faceImageUVWidth = 1;
            }

            if (faceImageUVHeight == 0) {
                faceImageUVHeight = 1;
            }

            BufferedImage faceImage = terrainImage.getSubimage(faceImageU, faceImageV, faceImageUVWidth, faceImageUVHeight);
            if (faceImageWidth != faceImageUVWidth || faceImageHeight != faceImageUVHeight) {
                if (faceImageWidth == faceImageUVHeight && faceImageHeight == faceImageUVWidth) {
                    BufferedImage tmp = new BufferedImage(faceImageWidth, faceImageHeight, 6);
                    AffineTransform transform = new AffineTransform();
                    transform.translate(faceImage.getHeight() / 2f, faceImage.getWidth() / 2f);
                    transform.rotate(Math.PI / 2);
                    transform.translate(-faceImage.getWidth() / 2f, -faceImage.getHeight() / 2f);
                    AffineTransformOp op = new AffineTransformOp(transform, 1);
                    faceImage = op.filter(faceImage, tmp);
                } else {
                    BufferedImage tmp = new BufferedImage(faceImageWidth, faceImageHeight, 6);
                    g2 = tmp.createGraphics();
                    g2.drawImage(faceImage, 0, 0, faceImageWidth, faceImageHeight, null);
                    g2.dispose();
                    faceImage = tmp;
                }
            }

            g2 = modelImage.createGraphics();
            g2.drawImage(faceImage, faceImageX, faceImageY, null);
            g2.dispose();
        }

        return modelImage;
    }

    private boolean similarEnough(float a, float b, float one, float two) {
        boolean similar = (double) Math.abs(a - one) < 1.0E-4;
        return similar && (double) Math.abs(b - two) < 1.0E-4;
    }

    public static class BlockFace implements Comparable<BlockFace> {
        BlockVertex[] vertices;
        boolean isHorizontal;
        boolean isVertical;
        boolean isClockwise;
        float yLevel;
        BlockVertex[] longestSide;

        BlockFace(int[] values) {
            int arraySize = values.length;
            int intsPerVertex = arraySize / 4;
            this.vertices = new BlockVertex[4];

            for (int t = 0; t < 4; ++t) {
                float x = Float.intBitsToFloat(values[t * intsPerVertex]);
                float y = Float.intBitsToFloat(values[t * intsPerVertex + 1]);
                float z = Float.intBitsToFloat(values[t * intsPerVertex + 2]);
                float u = Float.intBitsToFloat(values[t * intsPerVertex + 4]);
                float v = Float.intBitsToFloat(values[t * intsPerVertex + 5]);
                this.vertices[t] = new BlockVertex(x, y, z, u, v);
            }

            this.isHorizontal = this.checkIfHorizontal();
            this.isVertical = this.checkIfVertical();
            this.isClockwise = this.checkIfClockwise();
            this.yLevel = this.calculateY();
            this.longestSide = this.getLongestSide();
        }

        private boolean checkIfHorizontal() {
            boolean isHorizontal;
            float initialY = this.vertices[0].y;

            isHorizontal = IntStream.range(1, this.vertices.length).noneMatch(t -> this.vertices[t].y != initialY);

            return isHorizontal;
        }

        private boolean checkIfVertical() {
            boolean allSameX = true;
            boolean allSameZ = true;
            float initialX = this.vertices[0].x;
            float initialZ = this.vertices[0].z;

            for (int t = 1; t < this.vertices.length; ++t) {
                if (this.vertices[t].x != initialX) {
                    allSameX = false;
                }

                if (this.vertices[t].z != initialZ) {
                    allSameZ = false;
                }
            }

            return allSameX || allSameZ;
        }

        private boolean checkIfClockwise() {
            float sum = 0.0F;

            for (int t = 0; t < this.vertices.length; ++t) {
                sum += (this.vertices[t == this.vertices.length - 1 ? 0 : t + 1].x - this.vertices[t].x) * (this.vertices[t == this.vertices.length - 1 ? 0 : t + 1].z + this.vertices[t].z);
            }

            return sum > 0.0F;
        }

        private float calculateY() {
            float sum = 0.0F;

            for (BlockVertex vertex : this.vertices) {
                sum += vertex.y;
            }

            return sum / (float) this.vertices.length;
        }

        private BlockVertex[] getLongestSide() {
            float greatestLength = -1.0F;
            BlockVertex[] longestSide = new BlockVertex[0];

            for (int t = 0; t < this.vertices.length; ++t) {
                float uDiff = this.vertices[t].u - this.vertices[t == this.vertices.length - 1 ? 0 : t + 1].u;
                float vDiff = this.vertices[t].v - this.vertices[t == this.vertices.length - 1 ? 0 : t + 1].v;
                float segmentLength = (float) Math.sqrt(uDiff * uDiff + vDiff * vDiff);
                if (segmentLength > greatestLength) {
                    greatestLength = segmentLength;
                    longestSide = new BlockVertex[]{this.vertices[t], this.vertices[t == this.vertices.length - 1 ? 0 : t + 1]};
                }
            }

            return longestSide;
        }

        public float getMinX() {
            float minX = 1.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.x < minX) {
                    minX = vertex.x;
                }
            }

            return minX;
        }

        public float getMaxX() {
            float maxX = 0.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.x > maxX) {
                    maxX = vertex.x;
                }
            }

            return maxX;
        }

        public float getMinZ() {
            float minZ = 1.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.z < minZ) {
                    minZ = vertex.z;
                }
            }

            return minZ;
        }

        public float getMaxZ() {
            float maxZ = 0.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.z > maxZ) {
                    maxZ = vertex.z;
                }
            }

            return maxZ;
        }

        public float getMinU() {
            float minU = 1.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.u < minU) {
                    minU = vertex.u;
                }
            }

            return minU;
        }

        public float getMaxU() {
            float maxU = 0.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.u > maxU) {
                    maxU = vertex.u;
                }
            }

            return maxU;
        }

        public float getMinV() {
            float minV = 1.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.v < minV) {
                    minV = vertex.v;
                }
            }

            return minV;
        }

        public float getMaxV() {
            float maxV = 0.0F;

            for (BlockVertex vertex : this.vertices) {
                if (vertex.v > maxV) {
                    maxV = vertex.v;
                }
            }

            return maxV;
        }

        public int compareTo(BlockFace compareTo) {
            if (this.yLevel > compareTo.yLevel) {
                return 1;
            } else {
                return this.yLevel < compareTo.yLevel ? -1 : 0;
            }
        }
    }

    private static class BlockVertex {
        float x;
        float y;
        float z;
        float u;
        float v;

        BlockVertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }
    }
}
