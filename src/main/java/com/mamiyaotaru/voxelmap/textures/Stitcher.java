package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import net.minecraft.util.math.MathHelper;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Stitcher {
    private final Set<Holder> setStitchHolders = Sets.newHashSetWithExpectedSize(256);
    private final List<Slot> stitchSlots = Lists.newArrayListWithCapacity(256);
    private int currentWidth = 0;
    private int currentHeight = 0;
    private int currentWidthToPowerOfTwo = 0;
    private int currentHeightToPowerOfTwo = 0;
    private final int maxWidth;
    private final int maxHeight;
    private final int maxTileDimension;

    public Stitcher(int maxWidth, int maxHeight, int maxTileDimension) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxTileDimension = maxTileDimension;
    }

    public int getCurrentImageWidth() {
        return this.currentWidthToPowerOfTwo;
    }

    public int getCurrentImageHeight() {
        return this.currentHeightToPowerOfTwo;
    }

    public int getCurrentWidth() {
        return this.currentWidth;
    }

    public int getCurrentHeight() {
        return this.currentHeight;
    }

    public void addSprite(Sprite icon) {
        Holder holder = new Holder(icon);
        if (this.maxTileDimension > 0) {
            holder.setNewDimension(this.maxTileDimension);
        }

        this.setStitchHolders.add(holder);
    }

    public void doStitch() {
        Holder[] stitchHoldersArray = this.setStitchHolders.toArray(new Holder[0]);
        Arrays.sort(stitchHoldersArray);
        int stitcherHoldersArrayLength = stitchHoldersArray.length;
        if (stitcherHoldersArrayLength > 0) {
            Holder holder = stitchHoldersArray[0];
            int iconWidth = holder.width;
            int iconHeight = holder.height;
            boolean allSameSize = true;

            for (int stitcherHolderIndex = 1; stitcherHolderIndex < stitcherHoldersArrayLength && allSameSize; ++stitcherHolderIndex) {
                holder = stitchHoldersArray[stitcherHolderIndex];
                allSameSize = holder.width == iconWidth && holder.height == iconHeight;
            }

            if (allSameSize) {
                int nextPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(stitcherHoldersArrayLength);
                int power = Integer.numberOfTrailingZeros(nextPowerOfTwo);
                int width = (int) Math.pow(2.0, Math.ceil(power / 2.0)) * iconWidth;
                int height = (int) Math.pow(2.0, Math.floor(power / 2.0)) * iconHeight;
                this.currentWidth = width;
                this.currentHeight = height;
                this.currentWidthToPowerOfTwo = width;
                this.currentHeightToPowerOfTwo = height;
                Slot slot = new Slot(0, 0, this.currentWidth, this.currentHeight);
                this.stitchSlots.add(slot);
            }
        }

        for (Holder holder : stitchHoldersArray) {
            if (!this.allocateSlot(holder)) {
                String errorString = String.format("Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", holder.getAtlasSprite().getIconName(), holder.getAtlasSprite().getIconWidth(), holder.getAtlasSprite().getIconHeight());
                throw new StitcherException(errorString);
            }
        }

        this.currentWidthToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth);
        this.currentHeightToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight);
        this.setStitchHolders.clear();
    }

    public void doStitchNew() {
        Holder[] stitchHoldersArray = this.setStitchHolders.toArray(new Holder[0]);
        Arrays.sort(stitchHoldersArray);

        for (Holder holder : stitchHoldersArray) {
            if (!this.allocateSlot(holder)) {
                String errorString = String.format("Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", holder.getAtlasSprite().getIconName(), holder.getAtlasSprite().getIconWidth(), holder.getAtlasSprite().getIconHeight());
                throw new StitcherException(errorString);
            }
        }

        this.currentWidthToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth);
        this.currentHeightToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight);
        this.setStitchHolders.clear();
    }

    public List<Sprite> getStitchSlots() {
        ArrayList<Slot> listOfStitchSlots = Lists.newArrayList();

        for (Slot slot : this.stitchSlots) {
            slot.getAllStitchSlots(listOfStitchSlots);
        }

        ArrayList<Sprite> spritesList = Lists.newArrayList();

        for (Slot stitcherSlot : listOfStitchSlots) {
            Holder stitcherHolder = stitcherSlot.getStitchHolder();
            Sprite icon = stitcherHolder.getAtlasSprite();
            icon.initSprite(this.currentWidthToPowerOfTwo, this.currentHeightToPowerOfTwo, stitcherSlot.getOriginX(), stitcherSlot.getOriginY());
            spritesList.add(icon);
        }

        return spritesList;
    }

    private boolean allocateSlot(Holder holder) {
        return this.stitchSlots.stream().anyMatch(stitchSlot -> stitchSlot.addSlot(holder)) || this.expandAndAllocateSlot(holder);
    }

    private boolean expandAndAllocateSlot(Holder holder) {
        int expandBy = holder.getWidth();
        int currentWidthToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth);
        int currentHeightToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight);
        int possibleNewWidthToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentWidth + expandBy);
        int possibleNewHeightToPowerOfTwo = MathHelper.smallestEncompassingPowerOfTwo(this.currentHeight + expandBy);
        boolean isRoomToExpandRight = possibleNewWidthToPowerOfTwo <= this.maxWidth;
        boolean isRoomToExpandDown = possibleNewHeightToPowerOfTwo <= this.maxHeight;
        if (!isRoomToExpandRight && !isRoomToExpandDown) {
            return false;
        } else {
            boolean widthWouldChange = currentWidthToPowerOfTwo != possibleNewWidthToPowerOfTwo;
            boolean heightWouldChange = currentHeightToPowerOfTwo != possibleNewHeightToPowerOfTwo;
            boolean shouldExpandRight;
            if (widthWouldChange ^ heightWouldChange) {
                shouldExpandRight = !widthWouldChange;
            } else {
                shouldExpandRight = isRoomToExpandRight && currentWidthToPowerOfTwo <= currentHeightToPowerOfTwo;
            }

            if (MathHelper.smallestEncompassingPowerOfTwo((shouldExpandRight ? this.currentWidth : this.currentHeight) + expandBy) > (shouldExpandRight ? this.maxWidth : this.maxHeight)) {
                return false;
            } else {
                Slot slot;
                if (shouldExpandRight) {
                    if (this.currentHeight == 0) {
                        this.currentHeight = holder.getHeight();
                    }

                    slot = new Slot(this.currentWidth, 0, holder.getWidth(), this.currentHeight);
                    this.currentWidth += holder.getWidth();
                } else {
                    slot = new Slot(0, this.currentHeight, this.currentWidth, holder.getHeight());
                    this.currentHeight += holder.getHeight();
                }

                if (!slot.addSlot(holder)) {
                    String errorString = String.format("Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", holder.getAtlasSprite().getIconName(), holder.getAtlasSprite().getIconWidth(), holder.getAtlasSprite().getIconHeight());
                    VoxelConstants.getLogger().warn(errorString);
                }

                this.stitchSlots.add(slot);
                return true;
            }
        }
    }

    public static class Holder implements Comparable<Holder> {
        private final Sprite icon;
        private final int width;
        private final int height;
        private float scaleFactor = 1.0F;

        public Holder(Sprite icon) {
            this.icon = icon;
            this.width = icon.getIconWidth();
            this.height = icon.getIconHeight();
        }

        public Sprite getAtlasSprite() {
            return this.icon;
        }

        public int getWidth() {
            return (int) (this.width * this.scaleFactor);
        }

        public int getHeight() {
            return (int) (this.height * this.scaleFactor);
        }

        public void setNewDimension(int newDimension) {
            if (this.width > newDimension && this.height > newDimension) {
                this.scaleFactor = (float) newDimension / Math.min(this.width, this.height);
            }

        }

        public int compareTo(Holder o) {
            int var2;
            if (this.getHeight() == o.getHeight()) {
                if (this.getWidth() == o.getWidth()) {
                    if (this.icon.getIconName() == null) {
                        return o.icon.getIconName() == null ? 0 : -1;
                    }

                    Collator collator = I18nUtils.getLocaleAwareCollator();
                    return collator.compare(this.icon.getIconName(), o.icon.getIconName());
                }

                var2 = this.getWidth() < o.getWidth() ? 1 : -1;
            } else {
                var2 = this.getHeight() < o.getHeight() ? 1 : -1;
            }

            return var2;
        }
    }

    public class Slot {
        private final int originX;
        private final int originY;
        private final int width;
        private final int height;
        private int failsAt = Stitcher.this.maxWidth;
        private List<Slot> subSlots;
        private Holder holder;

        public Slot(int originX, int originY, int width, int height) {
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
        }

        public Holder getStitchHolder() {
            return this.holder;
        }

        public int getOriginX() {
            return this.originX;
        }

        public int getOriginY() {
            return this.originY;
        }

        public boolean addSlot(Holder holder) {
            if (holder.width >= this.failsAt) {
                return false;
            } else if (this.holder != null) {
                this.failsAt = 0;
                return false;
            } else {
                int holderWidth = holder.getWidth();
                int holderHeight = holder.getHeight();
                if (holderWidth <= this.width && holderHeight <= this.height) {
                    if (holderWidth == this.width && holderHeight == this.height) {
                        this.holder = holder;
                        return true;
                    } else {
                        if (this.subSlots == null) {
                            this.subSlots = Lists.newArrayListWithCapacity(1);
                            this.subSlots.add(Stitcher.this.new Slot(this.originX, this.originY, holderWidth, holderHeight));
                            int excessWidth = this.width - holderWidth;
                            int excessHeight = this.height - holderHeight;
                            if (excessHeight > 0 && excessWidth > 0) {
                                int var6 = Math.max(this.height, excessWidth);
                                int var7 = Math.max(this.width, excessHeight);
                                if (var6 > var7) {
                                    this.subSlots.add(Stitcher.this.new Slot(this.originX, this.originY + holderHeight, holderWidth, excessHeight));
                                    this.subSlots.add(Stitcher.this.new Slot(this.originX + holderWidth, this.originY, excessWidth, this.height));
                                } else {
                                    this.subSlots.add(Stitcher.this.new Slot(this.originX + holderWidth, this.originY, excessWidth, holderHeight));
                                    this.subSlots.add(Stitcher.this.new Slot(this.originX, this.originY + holderHeight, this.width, excessHeight));
                                }
                            } else if (excessWidth == 0) {
                                this.subSlots.add(Stitcher.this.new Slot(this.originX, this.originY + holderHeight, holderWidth, excessHeight));
                            } else if (excessHeight == 0) {
                                this.subSlots.add(Stitcher.this.new Slot(this.originX + holderWidth, this.originY, excessWidth, holderHeight));
                            }
                        }

                        for (Slot slot : this.subSlots) {
                            if (slot.addSlot(holder)) {
                                return true;
                            }
                        }

                        this.failsAt = holder.width;
                        return false;
                    }
                } else {
                    this.failsAt = holder.width;
                    return false;
                }
            }
        }

        public void getAllStitchSlots(List<Slot> listOfStitchSlots) {
            if (this.holder != null) {
                listOfStitchSlots.add(this);
            } else if (this.subSlots != null) {
                for (Slot slot : this.subSlots) {
                    slot.getAllStitchSlots(listOfStitchSlots);
                }
            }

        }
    }
}
