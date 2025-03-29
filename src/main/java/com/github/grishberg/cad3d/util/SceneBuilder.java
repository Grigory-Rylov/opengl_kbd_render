package com.github.grishberg.cad3d.util;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.vrl.VertexHolder;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface SceneBuilder {

   void requestBuffers();

   void setListener(ReadyListener listener);

   void setConfig(@NotNull KeyboardConfig cfg);

   interface ReadyListener{
      void  onReady(List<VertexHolder> buffers, boolean isAll);
   }
}
