package com.github.grishberg.cad3d.util;

import eu.printingin3d.javascad.vrl.VertexHolder;
import java.util.List;

public interface SceneBuilder {

   void requestBuffers();

   void setListener(ReadyListener listener);

   interface ReadyListener{
      void  onReady(List<VertexHolder> buffers);
   }
}
