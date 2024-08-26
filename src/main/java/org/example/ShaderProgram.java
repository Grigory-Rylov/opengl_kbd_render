package org.example;

import com.jogamp.opengl.GL2;
public class ShaderProgram {
   private int programId;

   public ShaderProgram(GL2 gl, String vertexShaderSource, String fragmentShaderSource) {
      int vertexShaderId = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
      gl.glShaderSource(vertexShaderId, 1, new String[]{vertexShaderSource}, null);
      gl.glCompileShader(vertexShaderId);

      int fragmentShaderId = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
      gl.glShaderSource(fragmentShaderId, 1, new String[]{fragmentShaderSource}, null);
      gl.glCompileShader(fragmentShaderId);

      programId = gl.glCreateProgram();
      gl.glAttachShader(programId, vertexShaderId);
      gl.glAttachShader(programId, fragmentShaderId);
      gl.glLinkProgram(programId);

      gl.glDeleteShader(vertexShaderId);
      gl.glDeleteShader(fragmentShaderId);
   }

   public void use(GL2 gl) {
      gl.glUseProgram(programId);
   }
}
