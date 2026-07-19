package com.github.grishberg.javascad.utils;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.vrl.Polygon;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Утилита для сохранения и загрузки списка полигонов в бинарный формат.
 * Формат файла:
 * - 4 байта: int32 - количество полигонов (N)
 * - Повтор N раз:
 *   - 4 байта: int32 - количество вершин в полигоне (V)
 *   - V * 3 * 8 байт: double - координаты X, Y, Z для каждой вершины
 *   - 3 * 8 байт: double - компоненты нормали X, Y, Z
 *   - 8 байт: double - dist
 *   - 4 байта: int32 - длина строки цвета (L)
 *   - L байт: UTF-8 строка - представление цвета
 */
public class PolygonBinaryIO {


    /**
     * Сохраняет список полигонов в бинарный файл.
     *
     * @param polygons     Список полигонов для сохранения.
     * @param filePath     Путь к файлу для записи.
     * @throws IOException Если возникает ошибка ввода-вывода.
     */
    public static void savePolygonsToBinary(List<Polygon> polygons, String filePath) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            // Записываем количество полигонов
            dos.writeInt(polygons.size());
            System.out.println("Saving " + polygons.size() + " polygons...");

            for (int i = 0; i < polygons.size(); i++) {
                Polygon polygon = polygons.get(i);
                List<V3d> vertices = polygon.getVertices();

                // Записываем количество вершин
                dos.writeInt(vertices.size());

                // Записываем вершины (X, Y, Z)
                for (V3d vertex : vertices) {
                    dos.writeDouble(vertex.x);
                    dos.writeDouble(vertex.y);
                    dos.writeDouble(vertex.z);
                }

                // Записываем нормаль (X, Y, Z)
                V3d normal = polygon.getNormal();
                dos.writeDouble(normal.x);
                dos.writeDouble(normal.y);
                dos.writeDouble(normal.z);

                // Записываем dist
                dos.writeDouble(polygon.getDist());

                // Записываем цвет как 32-битное целое (RRGGBBAA -> 0xAARRGGBB в Java)
                // getRGB() возвращает 0xAARRGGBB, что соответствует формату DataOutputStream.writeInt
                dos.writeInt(polygon.getColor().getRGB());

                if (i % 1000 == 0) {
                    System.out.println("Saved " + i + " polygons...");
                }
            }
            System.out.println("Finished saving " + polygons.size() + " polygons.");
        }
    }
}
