package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.Utils.cube;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.models.Abstract3dModel;
import java.util.ArrayList;
import java.util.List;

public class KeySwitchHoles {
   private final KeyboardConfig cfg;
   private final KeyPlace keyPlace;

   public KeySwitchHoles(KeyboardConfig cfg, KeyPlace keyPlace) {
      this.cfg = cfg;
      this.keyPlace = keyPlace;
   }

   public Abstract3dModel build() {
      final List<Abstract3dModel> models = new ArrayList<>();

      for (int column = 0; column < cfg.getColumnsCount(); column++) {
         for (int row = 0; row < cfg.getRowsCount(); row++) {
            models.add(keyPlace.place(column, row, cube(cfg.getKeyswitchWidth())));
         }
      }

      return union(models);
   }
}
