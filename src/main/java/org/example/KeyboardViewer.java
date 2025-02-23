package org.example;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

public class KeyboardViewer implements KeyListener, MouseListener {
   private static final boolean DEBUG = true;
   protected GLWindow window;
   protected Animator animator;
  // protected Vec2i windowSize = new Vec2i(640,480);
   private float scaleFactor = 1.0f;

   public static void main(String[] args) {
      new KeyboardViewer().setup("test");
   }

   public void setup(String title) {

      //        Display display = NewtFactory.createDisplay(null);
      //        Screen screen = NewtFactory.createScreen(display, 0);
      GLProfile glProfile = GLProfile.get(GLProfile.GL4);
      GLCapabilities glCapabilities = new GLCapabilities(glProfile);
      KeyboardRenderer renderer = new KeyboardRenderer();

      //        window = GLWindow.create(screen, glCapabilities);
      window = GLWindow.create(glCapabilities);

      if (DEBUG) {
         window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
      }

      window.setUndecorated(false);
      window.setAlwaysOnTop(false);
      window.setFullscreen(false);
      window.setPointerVisible(true);
      window.confinePointer(false);
      window.setTitle(title);
     // window.setSize(windowSize.x(), windowSize.y());

      window.setVisible(true);


      window.addGLEventListener(renderer);
      window.addKeyListener(this);
      window.addMouseListener(this);

      animator = new Animator();
      animator.add(window);
      animator.start();

      window.addWindowListener(new WindowAdapter() {
         @Override
         public void windowDestroyed(WindowEvent e) {
            new Thread(new Runnable() {
               public void run() {

                  //stop the animator thread when user close the window
                  animator.stop();
                  // This is actually redundant since the JVM will terminate when all threads are closed.
                  // It's useful just in case you create a thread and you forget to stop it.
                  System.exit(1);
               }
            }).start();
         }
      });
   }

   @Override
   public void keyPressed(KeyEvent keyEvent) {

   }

   @Override
   public void keyReleased(KeyEvent keyEvent) {

   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseEntered(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseExited(MouseEvent mouseEvent) {

   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {

   }

   @Override
   public void mouseWheelMoved(MouseEvent mouseEvent) {

   }
}
