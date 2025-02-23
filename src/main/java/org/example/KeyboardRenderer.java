package org.example;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import java.nio.FloatBuffer;

public class KeyboardRenderer implements GLEventListener {

   private int shaderProgram;
   private int vao;

   @Override
   public void init(GLAutoDrawable drawable) {
      GL4 gl = drawable.getGL().getGL4();


      // Вершинный шейдер
      String vertexShaderSource = "#version 330 core\n" +
          "layout(location = 0) in vec3 position;\n" +
          "layout(location = 1) in vec3 color;\n" +
          "out vec3 fragColor;\n" +
          "void main() {\n" +
          "    gl_Position = vec4(position, 1.0);\n" +
          "    fragColor = color;\n" +
          "}";

      String fragmentShaderSource = "#version 330 core\n" +
          "in vec3 fragColor;\n" +
          "out vec4 finalColor;\n" +
          "void main() {\n" +
          "    finalColor = vec4(fragColor, 1.0);\n" +
          "}";

      // Компиляция и привязка шейдеров
      shaderProgram = ShaderUtils.createShaderProgram(gl, vertexShaderSource, fragmentShaderSource);
      // Проверка ошибок компиляции шейдеров
      if (shaderProgram == 0) {
         System.err.println("Ошибка компиляции шейдеров");
         return;
      }

      // Создание и привязка VAO
      vao = ShaderUtils.createVertexArray(gl);

      // Создание и привязка VBO для вершин и цветов
      float[] vertices = {
          -0.5f, -0.5f, 0.0f, 1.0f, 0.0f, 0.0f, // Красный
          0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f, // Зеленый
          0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 1.0f // Синий
      };

      FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(vertices);
      int vbo = ShaderUtils.createBuffer(gl); // Создание VBO
      gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
      gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertices.length * Float.BYTES, vertexBuffer, GL4.GL_STATIC_DRAW);

      // Установка указателей атрибутов вершин и цветов
      gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 6 * Float.BYTES, 0);
      gl.glEnableVertexAttribArray(0);

      gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 6  * Float.BYTES, 3 * Float.BYTES);
      gl.glEnableVertexAttribArray(1);

      // Проверка ошибок OpenGL
      int error = gl.glGetError();
      if (error != GL4.GL_NO_ERROR) {
         System.err.println("Ошибка OpenGL: " + error);
      }
   }


   @Override
   public void display(GLAutoDrawable drawable) {
      GL4 gl = drawable.getGL().getGL4();

      gl.glClear(GL4.GL_COLOR_BUFFER_BIT);
      gl.glUseProgram(shaderProgram);
      gl.glBindVertexArray(vao);
      gl.glDrawArrays(GL4.GL_TRIANGLES, 0, 3);
      // Проверка ошибок OpenGL
      int error = gl.glGetError();
      if (error != GL4.GL_NO_ERROR) {
         System.err.println("Ошибка OpenGL: " + error);
      }
   }

   public void dispose(GLAutoDrawable drawable) {
      GL4 gl = drawable.getGL().getGL4();

      // Освобождение ресурсов
      gl.glDeleteProgram(shaderProgram);
      gl.glDeleteVertexArrays(1, new int[] { vao }, 0);
   }

   public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      // Обработка изменения размеров окна
      GL4 gl = drawable.getGL().getGL4();
      gl.glViewport(0, 0, width, height);
   }

}
