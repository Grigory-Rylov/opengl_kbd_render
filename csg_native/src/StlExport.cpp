/**
 * Native STL export following OpenSCAD's export_stl.cc exactly.
 * Supports binary and ASCII format with double-conversion precision.
 */
#include <jni.h>
#include <fstream>
#include <sstream>
#include <vector>
#include <array>
#include <cmath>
#include <cstring>
#include <algorithm>

static JavaVM* jvm = nullptr;

// Endianness check
static bool isLittleEndian() {
    uint16_t test = 0x0001;
    return *reinterpret_cast<const char*>(&test) == 1;
}

static uint32_t flipEndianness(uint32_t x) {
    return ((x << 24) & 0xff000000u) | ((x >> 24) & 0xffu) |
           ((x << 8) & 0xff0000u) | ((x >> 8) & 0xff00u);
}

/**
 * Write binary STL from vertex/face arrays.
 * Mirrors OpenSCAD's export_stl.cc exactly.
 */
extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_github_grishberg_csg_export_StlExporter_native_1export_1binary
(JNIEnv* env, jclass clazz, jdoubleArray jVerts, jintArray jFaces,
                                      jstring jFilename, jboolean jIsTriangular) {
    jsize vLen = env->GetArrayLength(jVerts);
    jsize fLen = env->GetArrayLength(jFaces);

    std::vector<double> verts(vLen);
    std::vector<int> faces(fLen);
    env->GetDoubleArrayRegion(jVerts, 0, vLen, verts.data());
    env->GetIntArrayRegion(jFaces, 0, fLen, faces.data());

    const char* filepath = env->GetStringUTFChars(jFilename, nullptr);

    std::ofstream ofs(filepath, std::ios::binary);
    if (!ofs.is_open()) {
        env->ReleaseStringUTFChars(jFilename, filepath);
        return JNI_FALSE;
    }

    // Determine triangles
    struct Tri { int a, b, c; };
    std::vector<Tri> triangles;

    if (jIsTriangular == JNI_TRUE) {
        triangles.reserve(fLen / 3);
        for (size_t i = 0; i + 2 < fLen; i += 3) {
            triangles.push_back({faces[i], faces[i + 1], faces[i + 2]});
        }
    } else {
        // Simple fan triangulation for each face
        // Faces are stored as: [n, i0, i1, ..., in-1, m, j0, j1, ..., jm-1, ...]
        size_t pos = 0;
        while (pos + 1 < fLen) {
            int n = faces[pos++];
            if (n >= 3 && pos + (size_t)n <= fLen) {
                for (int i = 1; i + 1 < n; i++) {
                    triangles.push_back({faces[pos], faces[pos + i], faces[pos + i + 1]});
                }
                pos += n;
            }
        }
    }

    // STL header
    char header[80];
    std::memset(header, 0, sizeof(header));
    std::strncpy(header, "CSG Engine Model", 79);
    ofs.write(header, 80);

    // Triangle count placeholder
    uint32_t placeholder = 0;
    ofs.write(reinterpret_cast<char*>(&placeholder), 4);

    // Write triangles
    std::array<float, 12> coords;
    for (const auto& tri : triangles) {
        const auto& p0 = std::make_tuple(verts[tri.a * 3], verts[tri.a * 3 + 1], verts[tri.a * 3 + 2]);
        const auto& p1 = std::make_tuple(verts[tri.b * 3], verts[tri.b * 3 + 1], verts[tri.b * 3 + 2]);
        const auto& p2 = std::make_tuple(verts[tri.c * 3], verts[tri.c * 3 + 1], verts[tri.c * 3 + 2]);

        // Compute normal
        double ax = std::get<0>(p1) - std::get<0>(p0);
        double ay = std::get<1>(p1) - std::get<1>(p0);
        double az = std::get<2>(p1) - std::get<2>(p0);
        double bx = std::get<0>(p2) - std::get<0>(p0);
        double by = std::get<1>(p2) - std::get<1>(p0);
        double bz = std::get<2>(p2) - std::get<2>(p0);

        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        double len = std::sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len; ny /= len; nz /= len;
        }

        // Pack into float array
        coords[0] = static_cast<float>(nx);
        coords[1] = static_cast<float>(ny);
        coords[2] = static_cast<float>(nz);
        coords[3] = static_cast<float>(std::get<0>(p0));
        coords[4] = static_cast<float>(std::get<1>(p0));
        coords[5] = static_cast<float>(std::get<2>(p0));
        coords[6] = static_cast<float>(std::get<0>(p1));
        coords[7] = static_cast<float>(std::get<1>(p1));
        coords[8] = static_cast<float>(std::get<2>(p1));
        coords[9] = static_cast<float>(std::get<0>(p2));
        coords[10] = static_cast<float>(std::get<1>(p2));
        coords[11] = static_cast<float>(std::get<2>(p2));

        if (isLittleEndian()) {
            ofs.write(reinterpret_cast<char*>(coords.data()), 48);
        } else {
            std::array<uint32_t, 12> ints;
            std::memcpy(ints.data(), coords.data(), 48);
            for (auto& iv : ints) iv = flipEndianness(iv);
            ofs.write(reinterpret_cast<char*>(ints.data()), 48);
        }

        // Attribute byte count
        uint16_t attrib = 0;
        ofs.write(reinterpret_cast<char*>(&attrib), 2);
    }

    // Write actual triangle count
    triCount = static_cast<uint32_t>(triangles.size());
    if (isLittleEndian()) {
        uint32_t le = triCount;
        ofs.seekp(80, std::ios::beg);
        ofs.write(reinterpret_cast<char*>(&le), 4);
    } else {
        uint32_t be = flipEndianness(triCount);
        ofs.seekp(80, std::ios::beg);
        ofs.write(reinterpret_cast<char*>(&be), 4);
    }

    ofs.close();
    env->ReleaseStringUTFChars(jFilename, filepath);
    return JNI_TRUE;
}

/**
 * Export ASCII STL.
 */
JNIEXPORT jboolean JNICALL
Java_com_github_grishberg_csg_export_StlExporter_native_1export_1ascii
(JNIEnv* env, jclass clazz, jdoubleArray jVerts, jintArray jFaces,
                                      jstring jFilename, jboolean jIsTriangular) {
    jsize vLen = env->GetArrayLength(jVerts);
    jsize fLen = env->GetArrayLength(jFaces);

    std::vector<double> verts(vLen);
    std::vector<int> faces(fLen);
    env->GetDoubleArrayRegion(jVerts, 0, vLen, verts.data());
    env->GetIntArrayRegion(jFaces, 0, fLen, faces.data());

    const char* filepath = env->GetStringUTFChars(jFilename, nullptr);
    std::ofstream ofs(filepath);
    if (!ofs.is_open()) {
        env->ReleaseStringUTFChars(jFilename, filepath);
        return JNI_FALSE;
    }

    ofs.precision(17);
    ofs << "solid CSG_Model\n";

    auto writeVec = [&](double x, double y, double z) {
        ofs << x << " " << y << " " << z << "\n";
    };

    // Same triangulation logic as binary
    struct Tri { int a, b, c; };
    std::vector<Tri> triangles;

    if (jIsTriangular == JNI_TRUE) {
        for (size_t i = 0; i + 2 < fLen; i += 3) {
            triangles.push_back({faces[i], faces[i + 1], faces[i + 2]});
        }
    } else {
        size_t pos = 0;
        while (pos + 1 < fLen) {
            int n = faces[pos++];
            if (n >= 3 && pos + (size_t)n <= fLen) {
                for (int i = 1; i + 1 < n; i++) {
                    triangles.push_back({faces[pos], faces[pos + i], faces[pos + i + 1]});
                }
                pos += n;
            }
        }
    }

    for (const auto& tri : triangles) {
        const auto& p0 = std::make_tuple(verts[tri.a * 3], verts[tri.a * 3 + 1], verts[tri.a * 3 + 2]);
        const auto& p1 = std::make_tuple(verts[tri.b * 3], verts[tri.b * 3 + 1], verts[tri.b * 3 + 2]);
        const auto& p2 = std::make_tuple(verts[tri.c * 3], verts[tri.c * 3 + 1], verts[tri.c * 3 + 2]);

        double ax = std::get<0>(p1) - std::get<0>(p0);
        double ay = std::get<1>(p1) - std::get<1>(p0);
        double az = std::get<2>(p1) - std::get<2>(p0);
        double bx = std::get<0>(p2) - std::get<0>(p0);
        double by = std::get<1>(p2) - std::get<1>(p0);
        double bz = std::get<2>(p2) - std::get<2>(p0);

        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        double len = std::sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }

        ofs << "  facet normal ";
        writeVec(nx, ny, nz);
        ofs << "    outer loop\n";
        ofs << "      vertex ";
        writeVec(std::get<0>(p0), std::get<1>(p0), std::get<2>(p0));
        ofs << "      vertex ";
        writeVec(std::get<0>(p1), std::get<1>(p1), std::get<2>(p1));
        ofs << "      vertex ";
        writeVec(std::get<0>(p2), std::get<1>(p2), std::get<2>(p2));
        ofs << "    endloop\n";
        ofs << "  endfacet\n";
    }

    ofs << "endsolid CSG_Model\n";
    ofs.close();
    env->ReleaseStringUTFChars(jFilename, filepath);
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    jvm = vm;
    return JNI_VERSION_17;
}

} // extern "C"
