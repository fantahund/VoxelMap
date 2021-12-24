package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

public class PersistentMapSettingsManager implements ISubSettingsManager {
   protected int mapX;
   protected int mapZ;
   protected float zoom = 4.0F;
   private float minZoomPower = -1.0F;
   private float maxZoomPower = 4.0F;
   protected float minZoom = 0.5F;
   protected float maxZoom = 16.0F;
   protected int cacheSize = 500;
   protected boolean outputImages = false;
   public boolean showWaypoints = true;
   public boolean showWaypointNames = true;
   protected final int MINMINZOOMPOWER = -3;
   protected final int MAXMAXZOOMPOWER = 5;
   protected final int MAXCACHESIZE = 5000;

   @Override
   public void loadSettings(File settingsFile) {
      try {
         BufferedReader in = new BufferedReader(new FileReader(settingsFile));

         String sCurrentLine;
         while((sCurrentLine = in.readLine()) != null) {
            String[] curLine = sCurrentLine.split(":");
            if (curLine[0].equals("Worldmap Zoom")) {
               this.zoom = Float.parseFloat(curLine[1]);
            } else if (curLine[0].equals("Worldmap Minimum Zoom")) {
               this.minZoom = Float.parseFloat(curLine[1]);
            } else if (curLine[0].equals("Worldmap Maximum Zoom")) {
               this.maxZoom = Float.parseFloat(curLine[1]);
            } else if (curLine[0].equals("Worldmap Cache Size")) {
               this.cacheSize = Integer.parseInt(curLine[1]);
            } else if (curLine[0].equals("Show Worldmap Waypoints")) {
               this.showWaypoints = Boolean.parseBoolean(curLine[1]);
            } else if (curLine[0].equals("Show Worldmap Waypoint Names")) {
               this.showWaypointNames = Boolean.parseBoolean(curLine[1]);
            } else if (curLine[0].equals("Output Images")) {
               this.outputImages = Boolean.parseBoolean(curLine[1]);
            }
         }

         in.close();
      } catch (Exception var5) {
      }

      for(int power = -3; power <= 5; ++power) {
         if (Math.pow(2.0, (double)power) == (double)this.minZoom) {
            this.minZoomPower = (float)power;
         }

         if (Math.pow(2.0, (double)power) == (double)this.maxZoom) {
            this.maxZoomPower = (float)power;
         }
      }

      this.bindCacheSize();
      this.bindZoom();
   }

   @Override
   public void saveAll(PrintWriter out) {
      out.println("Worldmap Zoom:" + Float.toString(this.zoom));
      out.println("Worldmap Minimum Zoom:" + Float.toString(this.minZoom));
      out.println("Worldmap Maximum Zoom:" + Float.toString(this.maxZoom));
      out.println("Worldmap Cache Size:" + Integer.toString(this.cacheSize));
      out.println("Show Worldmap Waypoints:" + Boolean.toString(this.showWaypoints));
      out.println("Show Worldmap Waypoint Names:" + Boolean.toString(this.showWaypointNames));
   }

   @Override
   public String getKeyText(EnumOptionsMinimap par1EnumOptions) {
      String s = I18nUtils.getString(par1EnumOptions.getName()) + ": ";
      if (par1EnumOptions.isFloat()) {
         float f = this.getOptionFloatValue(par1EnumOptions);
         if (par1EnumOptions == EnumOptionsMinimap.MINZOOM) {
            return s + (float)Math.pow(2.0, (double)f) + "x";
         }

         if (par1EnumOptions == EnumOptionsMinimap.MAXZOOM) {
            return s + (float)Math.pow(2.0, (double)f) + "x";
         }

         if (par1EnumOptions == EnumOptionsMinimap.CACHESIZE) {
            return s + (int)f;
         }
      }

      if (par1EnumOptions.isBoolean()) {
         boolean flag = this.getOptionBooleanValue(par1EnumOptions);
         return flag ? s + I18nUtils.getString("options.on") : s + I18nUtils.getString("options.off");
      } else {
         return s;
      }
   }

   @Override
   public float getOptionFloatValue(EnumOptionsMinimap par1EnumOptions) {
      if (par1EnumOptions == EnumOptionsMinimap.MINZOOM) {
         return this.minZoomPower;
      } else if (par1EnumOptions == EnumOptionsMinimap.MAXZOOM) {
         return this.maxZoomPower;
      } else {
         return par1EnumOptions == EnumOptionsMinimap.CACHESIZE ? (float)this.cacheSize : 0.0F;
      }
   }

   public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
      switch(par1EnumOptions) {
      case SHOWWAYPOINTS:
         return this.showWaypoints;
      case SHOWWAYPOINTNAMES:
         return this.showWaypointNames;
      default:
         throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean)");
      }
   }

   @Override
   public void setOptionFloatValue(EnumOptionsMinimap par1EnumOptions, float par2) {
      if (par1EnumOptions == EnumOptionsMinimap.MINZOOM) {
         this.minZoomPower = (float)((int)(par2 * 8.0F) + -3);
         this.minZoom = (float)Math.pow(2.0, (double)this.minZoomPower);
         if (this.maxZoom < this.minZoom) {
            this.maxZoom = this.minZoom;
            this.maxZoomPower = this.minZoomPower;
         }
      } else if (par1EnumOptions == EnumOptionsMinimap.MAXZOOM) {
         this.maxZoomPower = (float)((int)(par2 * 8.0F) + -3);
         this.maxZoom = (float)Math.pow(2.0, (double)this.maxZoomPower);
         if (this.minZoom > this.maxZoom) {
            this.minZoom = this.maxZoom;
            this.minZoomPower = this.maxZoomPower;
         }
      } else if (par1EnumOptions == EnumOptionsMinimap.CACHESIZE) {
         this.cacheSize = (int)(par2 * 5000.0F);
         this.cacheSize = Math.max(this.cacheSize, 30);

         for(int minCacheSize = (int)((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F);
            this.cacheSize < minCacheSize;
            minCacheSize = (int)((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F)
         ) {
            ++this.minZoomPower;
            this.minZoom = (float)Math.pow(2.0, (double)this.minZoomPower);
         }

         if (this.maxZoom < this.minZoom) {
            this.maxZoom = this.minZoom;
            this.maxZoomPower = this.minZoomPower;
         }
      }

      this.bindZoom();
      this.bindCacheSize();
   }

   public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
      switch(par1EnumOptions) {
      case SHOWWAYPOINTS:
         this.showWaypoints = !this.showWaypoints;
         break;
      case SHOWWAYPOINTNAMES:
         this.showWaypointNames = !this.showWaypointNames;
         break;
      default:
         throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName());
      }

   }

   private void bindCacheSize() {
      int minCacheSize = (int)((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F);
      this.cacheSize = Math.max(this.cacheSize, minCacheSize);
   }

   private void bindZoom() {
      this.zoom = Math.max(this.zoom, this.minZoom);
      this.zoom = Math.min(this.zoom, this.maxZoom);
   }
}
