package com.github.grishberg.cad3d.keyboard.cfg;

public class KeyZAngleProvider {
   public double getZAngle(int column) {
      switch (column) {
         case 0: return  4.0;
         case 1: return 2.0;
         case 3: return  -7.0;
         case 4: return -13.0;
         default: return  0.0;
      }
   }
}
