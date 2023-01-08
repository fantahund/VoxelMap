package com.mamiyaotaru.voxelmap.textures;

import java.io.Serial;

public class StitcherException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 2593319073559447986L;

    public StitcherException(String message) { super (message); }
}