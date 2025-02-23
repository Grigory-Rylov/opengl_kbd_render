package eu.printingin3d.javascad.utils;

import eu.printingin3d.javascad.vrl.VertexHolder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StlExporter {

    public static void exportToSTL(
        VertexHolder vertexHolder,
        String fileName
    ) throws IOException {
        float[] vertices = vertexHolder.getVertex();
        float[] normals = vertexHolder.getNormals();
        if (vertices.length != normals.length * 3) {
            throw new IllegalArgumentException(
                "Количество вершин не соответствует количеству нормалей");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("solid exported");

            for (int i = 0; i < vertices.length; i += 9) { // 9 координат на треугольник
                // Вычисляем среднюю нормаль для треугольника
                float nx = (normals[i] + normals[i + 3] + normals[i + 6]) / 3.0f;
                float ny = (normals[i + 1] + normals[i + 4] + normals[i + 7]) / 3.0f;
                float nz = (normals[i + 2] + normals[i + 5] + normals[i + 8]) / 3.0f;

                writer.printf("  facet normal %e %e %e\n", nx, ny, nz);
                writer.println("    outer loop");
                writer.printf(
                    "      vertex %e %e %e\n",
                    vertices[i],
                    vertices[i + 1],
                    vertices[i + 2]
                );
                writer.printf(
                    "      vertex %e %e %e\n",
                    vertices[i + 3],
                    vertices[i + 4],
                    vertices[i + 5]
                );
                writer.printf(
                    "      vertex %e %e %e\n",
                    vertices[i + 6],
                    vertices[i + 7],
                    vertices[i + 8]
                );
                writer.println("    endloop");
                writer.println("  endfacet");
            }

            writer.println("endsolid exported");
        }
    }
    /**
     * Сохраняет строку в текстовый файл
     * @param content  - содержимое для сохранения
     * @param filePath - путь к файлу для записи
     * @throws IOException при ошибках записи
     */
    public static void saveStringToFile(VertexHolder vertexHolder, String filePath) throws IOException {
        String stl = exportToSTL(vertexHolder);
        Path path = new File(filePath).toPath();
        Files.writeString(
            path,
            stl,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    public static String exportToSTL(VertexHolder vertexHolder) {
        float[] vertices = vertexHolder.getVertex();
        float[] normals = vertexHolder.getNormals();

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        writer.println("solid exported");

        int vertexIndex = 0;

        for (int i = 0; i < normals.length; i += 9) { // 9 координат на треугольник
            // Вычисляем среднюю нормаль для треугольника
            float nx = (normals[i] + normals[i + 3] + normals[i + 6]) / 3.0f;
            float ny = (normals[i + 1] + normals[i + 4] + normals[i + 7]) / 3.0f;
            float nz = (normals[i + 2] + normals[i + 5] + normals[i + 8]) / 3.0f;

            writer.printf("  facet normal %e %e %e\n", nx, ny, nz);
            writer.println("    outer loop");
            writer.printf(
                "      vertex %e %e %e\n",
                vertices[vertexIndex],
                vertices[vertexIndex + 1],
                vertices[vertexIndex + 2]
            );
            writer.printf(
                "      vertex %e %e %e\n",
                vertices[vertexIndex + 7],
                vertices[vertexIndex + 8],
                vertices[vertexIndex + 9]
            );
            writer.printf(
                "      vertex %e %e %e\n",
                vertices[vertexIndex + 14],
                vertices[vertexIndex + 15],
                vertices[vertexIndex + 16]
            );
            writer.println("    endloop");
            writer.println("  endfacet");
            vertexIndex += 7 * 3;
        }

        writer.println("endsolid exported");
        writer.flush();

        return stringWriter.toString();
    }
}
