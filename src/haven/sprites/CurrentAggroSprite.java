package haven.sprites;

import haven.Coord2d;
import haven.Gob;
import haven.Resource;
import haven.Sprite;
import haven.render.BaseColor;
import haven.render.RenderTree;

import java.awt.*;

public class CurrentAggroSprite extends Sprite { // ND: From Trollex's decompiled client
   private static final ObstMesh mesh;
   private static BaseColor col = new BaseColor(new Color(215, 0, 0, 255));
   public CurrentAggroSprite(Gob g) {
      super(g, (Resource)null);
   }

   public void added(RenderTree.Slot slot) {
      super.added(slot);
      slot.add(mesh, col);
   }

   static {
      Coord2d[][] shapes = new Coord2d[28][3];

      int i;
      double angle;
      double centerX;
      double centerY;
      for(i = 0; i < 14; ++i) {
         angle = Math.toRadians(25.714285714285715D * (double)i);
         centerX = 9.0D * Math.cos(angle);
         centerY = 9.0D * Math.sin(angle);
         if (i % 2 == 0) {
            shapes[i][0] = new Coord2d(centerX + 2.7D * Math.cos(angle), centerY + 2.7D * Math.sin(angle));
            shapes[i][1] = new Coord2d(centerX - 2.1D * Math.cos(angle + 1.5707963267948966D), centerY - 2.1D * Math.sin(angle + 1.5707963267948966D));
            shapes[i][2] = new Coord2d(centerX + 2.1D * Math.cos(angle + 1.5707963267948966D), centerY + 2.1D * Math.sin(angle + 1.5707963267948966D));
         } else {
            shapes[i][0] = new Coord2d(centerX + 8.6D * Math.cos(angle), centerY + 8.6D * Math.sin(angle));
            shapes[i][1] = new Coord2d(centerX - 2.0D * Math.cos(angle + 1.5707963267948966D), centerY - 2.0D * Math.sin(angle + 1.5707963267948966D));
            shapes[i][2] = new Coord2d(centerX + 2.0D * Math.cos(angle + 1.5707963267948966D), centerY + 2.0D * Math.sin(angle + 1.5707963267948966D));
         }
      }

      for(i = 0; i < 14; ++i) {
         angle = Math.toRadians(25.714285714285715D * (double)i);
         centerX = 8.0D * Math.cos(angle);
         centerY = 8.0D * Math.sin(angle);
         shapes[14 + i][0] = new Coord2d(centerX + 1.2D * Math.cos(angle + 3.141592653589793D), centerY + 1.2D * Math.sin(angle + 3.141592653589793D));
         shapes[14 + i][1] = new Coord2d(centerX - 1.85D * Math.cos(angle + 1.5707963267948966D), centerY - 1.85D * Math.sin(angle + 1.5707963267948966D));
         shapes[14 + i][2] = new Coord2d(centerX + 1.85D * Math.cos(angle + 1.5707963267948966D), centerY + 1.85D * Math.sin(angle + 1.5707963267948966D));
      }

      mesh = Obst.makeMesh(shapes, col.color(), 1.0F);
   }
}
