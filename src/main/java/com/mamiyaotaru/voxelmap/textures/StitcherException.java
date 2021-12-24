package com.mamiyaotaru.voxelmap.textures;

public class StitcherException extends RuntimeException {
   private static final long serialVersionUID = -7466680150055757059L;

   public StitcherException(Stitcher.Holder holder, String message) {
      super(message);
   }
}
