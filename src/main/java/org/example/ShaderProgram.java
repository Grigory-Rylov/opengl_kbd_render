package org.example;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

public class ShaderProgram {
   private int programId;

   public ShaderProgram(GL2 gl, String vertexShaderSource, String fragmentShaderSource) {
      int vertexShaderId = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
      gl.glShaderSource(vertexShaderId, 1, new String[]{vertexShaderSource}, null);
      gl.glCompileShader(vertexShaderId);
      IntBuffer compiled = IntBuffer.allocate(1);
      gl.glGetShaderiv(vertexShaderId, GL2.GL_COMPILE_STATUS, compiled);
      if (compiled.get(0) == GL4.GL_FALSE) {
         IntBuffer logLength = IntBuffer.allocate(1);
         gl.glGetShaderiv(vertexShaderId, GL2.GL_INFO_LOG_LENGTH, logLength);

         ByteBuffer log = ByteBuffer.allocate(logLength.get(0));
         gl.glGetShaderInfoLog(vertexShaderId, logLength.get(0), null, log);
         String logMessage = StandardCharsets.UTF_8.decode(log).toString();

         System.err.println("Vertex Shader Compilation Error: " + logMessage);
         gl.glDeleteShader(vertexShaderId);
      }


      int fragmentShaderId = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
      gl.glShaderSource(fragmentShaderId, 1, new String[]{fragmentShaderSource}, null);
      gl.glCompileShader(fragmentShaderId);
      gl.glGetShaderiv(fragmentShaderId, GL2.GL_COMPILE_STATUS, compiled);
      if (compiled.get(0) == GL4.GL_FALSE) {
         IntBuffer logLength = IntBuffer.allocate(1);
         gl.glGetShaderiv(fragmentShaderId, GL4.GL_INFO_LOG_LENGTH, logLength);

         ByteBuffer log = ByteBuffer.allocate(logLength.get(0));
         gl.glGetShaderInfoLog(fragmentShaderId, logLength.get(0), null, log);
         String logMessage = StandardCharsets.UTF_8.decode(log).toString();

         System.err.println("Fragment Shader Compilation Error: " + logMessage);
         gl.glDeleteShader(fragmentShaderId);
         gl.glDeleteShader(fragmentShaderId);
      }

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
