package org.example;

import com.jogamp.opengl.GL4;

public class ShaderUtils {

   public static int createShaderProgram(GL4 gl, String vertexShaderSource, String fragmentShaderSource) {
      int vertexShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
      gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSource }, null);
      gl.glCompileShader(vertexShader);
      checkCompileErrors(gl, vertexShader, "VERTEX");

      int fragmentShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);
      gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSource }, null);
      gl.glCompileShader(fragmentShader);
      checkCompileErrors(gl, fragmentShader, "FRAGMENT");

      int shaderProgram = gl.glCreateProgram();
      gl.glAttachShader(shaderProgram, vertexShader);
      gl.glAttachShader(shaderProgram, fragmentShader);
      gl.glLinkProgram(shaderProgram);
      checkLinkErrors(gl, shaderProgram);

      gl.glDeleteShader(vertexShader);
      gl.glDeleteShader(fragmentShader);

      return shaderProgram;
   }

   public static int createVertexArray(GL4 gl) {
      int[] vao = new int[1];
      gl.glGenVertexArrays(1, vao, 0);
      gl.glBindVertexArray(vao[0]);
      checkGLError(gl, "glGenVertexArrays");
      return vao[0];
   }

   public static int createBuffer(GL4 gl) {
      int[] vbo = new int[1];
      gl.glGenBuffers(1, vbo, 0);
      gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo[0]);
      checkGLError(gl, "glGenBuffers");
      return vbo[0];
   }

   private static void checkCompileErrors(GL4 gl, int shader, String type) {
      int[] success = new int[1];
      gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, success, 0);
      if (success[0] == GL4.GL_FALSE) {
         byte[] infoLog = new byte[512];
         gl.glGetShaderInfoLog(shader, 512, null, 0, infoLog, 0);
         System.err.println("Ошибка компиляции шейдера (" + type + "): " + new String(infoLog));
      }
   }

   private static void checkLinkErrors(GL4 gl, int program) {
      int[] success = new int[1];
      gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, success, 0);
      if (success[0] == GL4.GL_FALSE) {
         byte[] infoLog = new byte[512];
         gl.glGetProgramInfoLog(program, 512, null, 0, infoLog, 0);
         System.err.println("Ошибка связывания программы: " + new String(infoLog));
      }
   }

   private static void checkGLError(GL4 gl, String operation) {
      int error = gl.glGetError();
      if (error != GL4.GL_NO_ERROR) {
         System.err.println("Ошибка OpenGL после " + operation + ": " + error);
      }
   }
}
