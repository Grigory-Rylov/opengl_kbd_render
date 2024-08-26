package org.example.sample;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.*;

public class CubeRenderer implements GLEventListener {

   private float[] vertices = { -1, -1, -1, 1, -1, -1, 1, 1, -1, -1, 1, -1, -1, -1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1, -1 };
   private float[] normals = { 0, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, -1, 0, 0, 1, 0, 0 };

   @Override
   public void display(GLAutoDrawable drawable) {
      final GL2 gl = drawable.getGL().getGL2();

      gl.glBegin(GL.GL_TRIANGLES);
      for (int i = 0; i < vertices.length; i += 3) {
         gl.glNormal3f(normals[i / 3 * 3], normals[i / 3 * 3 + 1], normals[i / 3 * 3 + 2]);
         gl.glVertex3f(vertices[i], vertices[i + 1], vertices[i + 2]);
      }
      gl.glEnd();
   }

   @Override
   public void init(GLAutoDrawable drawable) {
   }

   @Override
   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
   }

   @Override
   public void dispose(GLAutoDrawable drawable) {
   }

   public static void main(String[] args) {
      GLProfile glProfile = GLProfile.get(GLProfile.GL2);
      GLCanvas canvas = new GLCanvas();
      canvas.addGLEventListener(new CubeRenderer());

      JFrame frame = new JFrame("3D Cube Renderer");
      frame.getContentPane().add(canvas);
      frame.setSize(800, 600);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
   }
}
