package com.mamiyaotaru.voxelmap.textures;

import java.io.Serial;

public class StitcherException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -7466680150055757059L;

    public StitcherException(Stitcher.Holder holder, String message) {
        super(message);
    }
}
