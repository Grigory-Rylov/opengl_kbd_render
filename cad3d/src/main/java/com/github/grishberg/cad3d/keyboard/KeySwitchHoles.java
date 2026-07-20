package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.Utils.cube;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.kbd.core.cfg.KeyPlaceConfig;
import com.github.grishberg.javascad.models.Model;
import java.util.ArrayList;
import java.util.List;

public class KeySwitchHoles {
   private final KeyPlaceConfig cfg;
   private final KeyPlace keyPlace;

   public KeySwitchHoles(KeyPlaceConfig cfg, KeyPlace keyPlace) {
      this.cfg = cfg;
      this.keyPlace = keyPlace;
   }

   public Model build() {
      final List<Model> models = new ArrayList<>();

      for (int column = 0; column < cfg.getColumnsCount(); column++) {
         for (int row = 0; row < cfg.getRowsCount(); row++) {
            models.add(keyPlace.place(column, row, cube(cfg.getKeyswitchWidth())));
         }
      }

      return union(models);
   }
}
