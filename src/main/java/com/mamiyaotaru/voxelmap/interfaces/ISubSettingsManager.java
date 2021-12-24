package com.mamiyaotaru.voxelmap.interfaces;

import java.io.File;
import java.io.PrintWriter;

public interface ISubSettingsManager extends ISettingsManager {
   void loadSettings(File var1);

   void saveAll(PrintWriter var1);
}
