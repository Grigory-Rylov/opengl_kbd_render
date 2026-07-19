package com.github.grishberg.javascad.coords;

import static com.github.grishberg.javascad.vrl.Const.EPSILON;

import com.github.grishberg.javascad.basic.Angle;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Immutable representation of a 3D coordinate with useful helper methods.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class V3d extends Basic3dFunc<V3d> {

    /**
     * Represents the (1,0,0) coordinate.
     */
    public static final V3d X = xOnly(1.0);
    /**
     * Represents the (0,1,0) coordinate.
     */
    public static final V3d Y = yOnly(1.0);
    /**
     * Represents the (0,0,1) coordinate.
     */
    public static final V3d Z = zOnly(1.0);

    /**
     * Represents the (0,0,0) coordinate.
     */
    public static final V3d ZERO = new V3d(0.0, 0.0, 0.0);

    /**
     * Can be used to move the object that tiny bit that will solve the different annoying
     * manifold issues
     * and CSG rendering problems.
     */
    public static final V3d TINY = new V3d(0.001, 0.001, 0.001);
    /**
     * Can be used to move the object that tiny bit that will solve the different annoying
     * manifold issues
     * and CSG rendering problems.
     */
    public static final V3d MINUS_TINY = TINY.inverse();

    /**
     * Creates a coordinate where the Y and Z fields are zero.
     *
     * @param x the X coordinate
     * @return the newly create coordinate which points to (x,0,0)
     */
    public static V3d xOnly(double x) {
        return new V3d(x, 0.0, 0.0);
    }

    /**
     * Creates a coordinate where the X and Z fields are zero.
     *
     * @param y the Y coordinate
     * @return the newly create coordinate which points to (0,y,0)
     */
    public static V3d yOnly(double y) {
        return new V3d(0.0, y, 0.0);
    }

    /**
     * Creates a coordinate where the X and Y fields are zero.
     *
     * @param z the Z coordinate
     * @return the newly create coordinate which points to (0,0,z)
     */
    public static V3d zOnly(double z) {
        return new V3d(0.0, 0.0, z);
    }

    /**
     * Creates a coordinate based on the given byte array. This is the inverse operation of
     * the {@link Abstract3d#toByteArray()} method.
     *
     * @param bb the byte buffer to be read
     * @return the new coordinate
     */
    public static V3d fromByteBuffer(ByteBuffer bb) {
        return new V3d(bb.getFloat(), bb.getFloat(), bb.getFloat());
    }

    /**
     * Calculates the midpoint of the ab segment.
     *
     * @param a first point
     * @param b second point
     * @return the coordinate of the midpoint of the ab segment
     */
    public static V3d midPoint(V3d a, V3d b) {
        return new V3d((a.x + b.x) / 2.0, (a.y + b.y) / 2.0, (a.z + b.z) / 2.0);
    }

    /**
     * Instantiating a new coordinate with the given coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public V3d(double x, double y, double z) {
        super(x, y, z);
    }

    final static double multiplier = 1.0 / EPSILON;

    private static double roundToEps(double value) {

        // Округляем каждую координату
        return Math.round(value * multiplier) * EPSILON;
    }

    /**
     * Rotating this coordinate by the given angle, but this object will be unchanged and
     * this method will create a new object with the new coordinates.
     *
     * @param angles the angles used by the rotation
     * @return a new coordinate instance which points to the new location
     */
    public V3d rotate(Angles3d angles) {
        V3d result = this;
        if (!angles.isXZero()) {
            Angle xAngle = angles.getXAngle();
            result = new V3d(
                result.x,
                xAngle.cos() * result.y - xAngle.sin() * result.z,
                xAngle.sin() * result.y + xAngle.cos() * result.z
            );
        }
        if (!angles.isYZero()) {
            Angle yAngle = angles.getYAngle();
            result = new V3d(
                yAngle.cos() * result.x + yAngle.sin() * result.z,
                result.y,
                -yAngle.sin() * result.x + yAngle.cos() * result.z
            );
        }
        if (!angles.isZZero()) {
            Angle zAngle = angles.getZAngle();
            result = new V3d(
                zAngle.cos() * result.x - zAngle.sin() * result.y,
                zAngle.sin() * result.x + zAngle.cos() * result.y,
                result.z
            );
        }
        return result;
    }

    /**
     * Returns the cross product of this vector and the specified vector.
     *
     * @param a the vector
     * @return the cross product of this vector and the specified vector.
     */
    public V3d cross(V3d a) {
        return new V3d(
            this.y * a.z - this.z * a.y,
            this.z * a.x - this.x * a.z,
            this.x * a.y - this.y * a.x
        );
    }

    /**
     * Returns a normalized copy of this vector with {@code length}.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a normalized copy of this vector with {@code length}
     */
    public V3d unit() {
        return this.mul(1.0 / this.magnitude());
    }

    /**
     * Linearly interpolates between this and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a vector
     * @param t interpolation value
     * @return copy of this vector if {@code t = 0}; copy of {@code a} if {@code t = 1};
     * the point midway between this and the specified vector if {@code t = 0.5}
     */
    public V3d lerp(V3d a, double t) {
        return this.add(a.add(this.inverse()).mul(t));
    }

    @Override
    protected V3d create(double x, double y, double z) {
        return new V3d(x, y, z);
    }

    public V3d subtract(V3d v2) {
        return new V3d(x - v2.x, y - v2.y, z - v2.z);
    }

    // Метод для вычисления скалярного произведения
    public double dot(V3d b) {
        return x * b.x + y * b.y + z * b.z;
    }

    public V3d projectionZ(double newZ) {
        return new V3d(x, y, newZ);
    }

    // Метод для вычисления скалярного произведения
    public V3d scale(double scale) {
        return new V3d(x * scale, y * scale, z * scale);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "V3d(%.4f, %.4f, %.4f)", x, y, z);
    }

    /**
     * Вычисляет hashCode для double с учетом точности EPSILON.
     * Близкие значения (разница <= EPSILON) будут иметь одинаковый хэш.
     *
     * @param value Значение double.
     * @return Хэш-код, совместимый с equals, использующим EPSILON.
     */
    public static int hashCodeWithEpsilon(double value) {

        // Обработка специальных случаев
        if (Double.isNaN(value)) {
            // Используем стандартный хэш для NaN
            return Double.hashCode(value);
        }
        if (Double.isInfinite(value)) {
            // Используем стандартный хэш для бесконечностей
            return Double.hashCode(value);
        }

        // "Округляем" значение до ближайшего кратного EPSILON
        // Это делает близкие значения идентичными для хэширования
        long rounded = Math.round(value / EPSILON);

        // Используем часть битов long для получения int хэш-кода
        // XOR с правым сдвигом - стандартный способ сжатия long в int
        return (int) (rounded ^ (rounded >>> 32));
    }

    public String toJson() {
        return "{\"x\":" + x +
            ",\"y\":" + y +
            ",\"z\":" + z +
            "}";
    }

    /**
     * Creates a V3d instance from JSON string.
     * Expected format: {"x":1.0,"y":2.0,"z":3.0}
     *
     * @param json the JSON string
     * @return the V3d instance
     */
    public static V3d fromJson(String json) {
        // Simple JSON parsing for the expected format
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format for V3d: " + json);
        }

        String content = json.substring(1, json.length() - 1);
        String[] parts = content.split(",");

        double x = 0.0, y = 0.0, z = 0.0;

        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length != 2) {
                continue;
            }

            String key = keyValue[0].trim().replace("\"", "");
            double value = Double.parseDouble(keyValue[1].trim());

            switch (key) {
                case "x":
                    x = value;
                    break;
                case "y":
                    y = value;
                    break;
                case "z":
                    z = value;
                    break;
            }
        }

        return new V3d(x, y, z);
    }

    public V3d roundedToEpsilon() {
        // Вычисляем множитель для округления
        final double multiplier = 1.0 / EPSILON;

        // Округляем каждую координату
        double roundedX = Math.round(this.x * multiplier) * EPSILON;
        double roundedY = Math.round(this.y * multiplier) * EPSILON;
        double roundedZ = Math.round(this.z * multiplier) * EPSILON;

        // Создаем и возвращаем новый экземпляр
        // Используем конструктор напрямую, чтобы избежать лишних проверок в create()
        return new V3d(roundedX, roundedY, roundedZ);
    }
}
