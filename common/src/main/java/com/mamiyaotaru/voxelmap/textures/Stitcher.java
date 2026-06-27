package com.mamiyaotaru.voxelmap.textures;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;

public class Stitcher {
    private final Set<Holder> setStitchHolders = Sets.newHashSetWithExpectedSize(256);
    private final List<Slot> stitchSlots = Lists.newArrayListWithCapacity(256);
    private int currentWidth;
    private int currentHeight;
    private int currentWidthToPowerOfTwo;
    private int currentHeightToPowerOfTwo;
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
            int iconWidth = holder.getWidth();
            int iconHeight = holder.getHeight();
            boolean allSameSize = true;

            for (int stitcherHolderIndex = 1; stitcherHolderIndex < stitcherHoldersArrayLength && allSameSize; ++stitcherHolderIndex) {
                holder = stitchHoldersArray[stitcherHolderIndex];
                allSameSize = holder.getWidth() == iconWidth && holder.getHeight() == iconHeight;
            }

            if (allSameSize) {
                int nextPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(stitcherHoldersArrayLength);
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

        this.currentWidthToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentWidth);
        this.currentHeightToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentHeight);
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

        this.currentWidthToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentWidth);
        this.currentHeightToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentHeight);
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
        ExpansionCandidate expandRight = this.createExpandRightCandidate(holder);
        ExpansionCandidate expandDown = this.createExpandDownCandidate(holder);
        ExpansionCandidate selectedCandidate = this.selectExpansionCandidate(expandRight, expandDown);

        if (selectedCandidate == null) {
            return false;
        }

        if (!selectedCandidate.primarySlot.addSlot(holder)) {
            return false;
        }

        this.currentWidth = selectedCandidate.width;
        this.currentHeight = selectedCandidate.height;
        this.stitchSlots.add(selectedCandidate.primarySlot);
        if (selectedCandidate.extraSlot != null) {
            this.stitchSlots.add(selectedCandidate.extraSlot);
        }
        return true;
    }

    private ExpansionCandidate createExpandRightCandidate(Holder holder) {
        int newWidth = this.currentWidth + holder.getWidth();
        int newHeight = Math.max(this.currentHeight, holder.getHeight());
        if (!this.canFit(newWidth, newHeight)) {
            return null;
        }

        Slot primarySlot = new Slot(this.currentWidth, 0, holder.getWidth(), newHeight);
        Slot extraSlot = newHeight > this.currentHeight && this.currentWidth > 0 ? new Slot(0, this.currentHeight, this.currentWidth, newHeight - this.currentHeight) : null;
        return new ExpansionCandidate(newWidth, newHeight, primarySlot, extraSlot);
    }

    private ExpansionCandidate createExpandDownCandidate(Holder holder) {
        int newWidth = Math.max(this.currentWidth, holder.getWidth());
        int newHeight = this.currentHeight + holder.getHeight();
        if (!this.canFit(newWidth, newHeight)) {
            return null;
        }

        Slot primarySlot = new Slot(0, this.currentHeight, newWidth, holder.getHeight());
        Slot extraSlot = newWidth > this.currentWidth && this.currentHeight > 0 ? new Slot(this.currentWidth, 0, newWidth - this.currentWidth, this.currentHeight) : null;
        return new ExpansionCandidate(newWidth, newHeight, primarySlot, extraSlot);
    }

    private ExpansionCandidate selectExpansionCandidate(ExpansionCandidate expandRight, ExpansionCandidate expandDown) {
        if (expandRight == null) {
            return expandDown;
        }
        if (expandDown == null) {
            return expandRight;
        }

        int rightWidthToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(expandRight.width);
        int rightHeightToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(expandRight.height);
        int downWidthToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(expandDown.width);
        int downHeightToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(expandDown.height);
        long rightArea = (long) rightWidthToPowerOfTwo * rightHeightToPowerOfTwo;
        long downArea = (long) downWidthToPowerOfTwo * downHeightToPowerOfTwo;
        if (rightArea != downArea) {
            return rightArea < downArea ? expandRight : expandDown;
        }

        int currentWidthToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentWidth);
        int currentHeightToPowerOfTwo = Mth.smallestEncompassingPowerOfTwo(this.currentHeight);
        boolean rightWidthWouldChange = currentWidthToPowerOfTwo != rightWidthToPowerOfTwo;
        boolean downHeightWouldChange = currentHeightToPowerOfTwo != downHeightToPowerOfTwo;
        if (rightWidthWouldChange != downHeightWouldChange) {
            return !rightWidthWouldChange ? expandRight : expandDown;
        }

        return rightWidthToPowerOfTwo <= downHeightToPowerOfTwo ? expandRight : expandDown;
    }

    private boolean canFit(int width, int height) {
        return Mth.smallestEncompassingPowerOfTwo(width) <= this.maxWidth && Mth.smallestEncompassingPowerOfTwo(height) <= this.maxHeight;
    }

    private record ExpansionCandidate(int width, int height, Slot primarySlot, Slot extraSlot) {
    }

    public static class Holder implements Comparable<Holder> {
        private final Sprite icon;
        private final int width;
        private final int height;
        private float scaleFactor = 1.0F;

        public Holder(Sprite icon) {
            this.icon = icon;
            this.width = icon.getAtlasUploadWidth();
            this.height = icon.getAtlasUploadHeight();
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

        @Override
        public int compareTo(Holder o) {
            int var2;
            if (this.getHeight() == o.getHeight()) {
                if (this.getWidth() == o.getWidth()) {
                    return 0;
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
            if (holder.getWidth() >= this.failsAt) {
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

                        this.failsAt = holder.getWidth();
                        return false;
                    }
                } else {
                    this.failsAt = holder.getWidth();
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
