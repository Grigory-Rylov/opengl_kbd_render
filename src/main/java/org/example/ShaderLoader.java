package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderLoader {
   public static String loadShader(String fileName) {
      StringBuilder shaderSource = new StringBuilder();
      try {
         InputStream is = ShaderLoader.class.getResourceAsStream(fileName);
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));
         String line;
         while ((line = reader.readLine()) != null) {
            shaderSource.append(line).append("n");
         }
         reader.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
      return shaderSource.toString();
   }
}
