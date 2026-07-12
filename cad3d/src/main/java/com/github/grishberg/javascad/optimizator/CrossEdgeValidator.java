package com.github.grishberg.javascad.optimizator;

import static com.github.grishberg.openscad.utils.Const.EPSILON;

import com.github.grishberg.openscad.coords.V3d;

public class CrossEdgeValidator {

    public static boolean isPointBetween(V3d point, V3d start, V3d end) {

        V3d ab = new V3d(end.x - start.x, end.y - start.y, end.z - start.z);
        V3d ap = new V3d(point.x - start.x, point.y - start.y, point.z - start.z);

        // Векторное произведение AP x AB. Если оно нулевое, точка лежит на линии.
        V3d cross = crossProduct(ap, ab);
        if (Math.abs(cross.x) > EPSILON || Math.abs(cross.y) > EPSILON || Math.abs(cross.z) > EPSILON) {
            return false;
        }

        // Проверяем, находится ли точка *строго* между start и end
        // Скалярное произведение AP * AB > 0 и AP * AB < AB * AB
        double ap_dot_ab = dotProduct(ap, ab);
        double ab_dot_ab = dotProduct(ab, ab);

        return ap_dot_ab > EPSILON && ap_dot_ab < (ab_dot_ab - EPSILON);
    }

    // Вспомогательные методы для векторной алгебры
    private static V3d crossProduct(V3d a, V3d b) {
        return new V3d(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x
        );
    }

    private static double dotProduct(V3d a, V3d b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }
}
