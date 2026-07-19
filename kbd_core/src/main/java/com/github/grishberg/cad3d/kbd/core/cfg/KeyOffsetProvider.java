package com.github.grishberg.cad3d.kbd.core.cfg;

import com.github.grishberg.javascad.coords.V3d;

public class KeyOffsetProvider {
   private static final double OFFSET = 2;
   public V3d getOffset(int column) {
      switch (column) {
         case 0:
            return new V3d(-OFFSET - 4, -7.8, 3.0);
         case 1:
            return new V3d(-OFFSET, -5.8, 3.0);
         case 2:
            return new V3d(1.5, 2.82, -3.5);
         case 3:
            return new V3d(OFFSET + 4, -2.0, 0);
         case 4:
            return new V3d(OFFSET + 7.5, -15.0, 5.64);
         case 5:
            return new V3d(OFFSET + 12, -20.0, 5.64);
         default:
            return new V3d(0.0, -2.0, 0.0);
      }
   }
}
