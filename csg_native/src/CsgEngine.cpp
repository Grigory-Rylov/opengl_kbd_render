/**
 * Native CSG engine wrapping OpenSCAD's CGAL Nef polyhedra.
 * Provides exact boolean operations that produce manifold-correct results.
 */
#include <jni.h>
#include <vector>
#include <memory>
#include <string>
#include <map>

#include <CGAL/Exact_predicates_inexact_constructions_kernel.h>
#include <CGAL/Nef_polyhedron_3.h>
#include <CGAL/Polyhedron_3.h>
#include <CGAL/IO/Polyhedron_iostream.h>

typedef CGAL::Exact_predicates_inexact_constructions_kernel CGAL_Kernel3;
typedef CGAL::Nef_polyhedron_3<CGAL_Kernel3> CGAL_Nef_polyhedron3;
typedef CGAL::Polyhedron_3<CGAL_Kernel3> CGAL_Polyhedron;

// Global handle to JVM (set in JNI_OnLoad)
static JavaVM* jvm = nullptr;

/**
 * Build a CGAL Nef polyhedron from vertex/face arrays.
 */
std::shared_ptr<CGAL_Nef_polyhedron3> buildNefFromArrays(
    const std::vector<double>& verts,
    const std::vector<int>& faces)
{
    CGAL_Polyhedron poly;

    // Build vertex array
    std::vector<CGAL_Kernel3::Point_3> points;
    points.reserve(verts.size() / 3);
    for (size_t i = 0; i < verts.size(); i += 3) {
        points.emplace_back(verts[i], verts[i + 1], verts[i + 2]);
    }

    // Incremental builder
    CGAL::Polyhedron_incremental_builder_3<typename CGAL_Polyhedron::HalfedgeDS> builder(
        poly.halfedge_data_structure(), true);

    builder.begin_surface(points.size(), faces.size() / 3);
    for (const auto& p : points) {
        builder.add_vertex(p);
    }

    // Each face is a triangle
    for (size_t i = 0; i < faces.size(); i += 3) {
        std::vector<typename CGAL_Polyhedron::Vertex_index> vi(3);
        vi[0] = faces[i];
        vi[1] = faces[i + 1];
        vi[2] = faces[i + 2];

        if (builder.test_facet(vi.begin(), vi.end())) {
            builder.add_facet(vi.begin(), vi.end());
        }
    }
    builder.end_surface();

    // Convert to Nef for exact CSG
    CGAL_Nef_polyhedron3 nef(poly);
    return std::make_shared<CGAL_Nef_polyhedron3>(nef);
}

/**
 * Boolean operation on two Nef polyhedra.
 * opType: 0=union, 1=intersection, 2=difference
 */
std::shared_ptr<CGAL_Nef_polyhedron3> booleanOp(
    const std::shared_ptr<CGAL_Nef_polyhedron3>& a,
    const std::shared_ptr<CGAL_Nef_polyhedron3>& b,
    int opType)
{
    try {
        switch (opType) {
            case 0: return std::make_shared<CGAL_Nef_polyhedron3>(*a + *b);
            case 1: return std::make_shared<CGAL_Nef_polyhedron3>(*a * *b);
            case 2: return std::make_shared<CGAL_Nef_polyhedron3>(*a - *b);
            default: return nullptr;
        }
    } catch (const CGAL::Failure_exception& e) {
        fprintf(stderr, "CGAL boolean error: %s\n", e.what());
        return nullptr;
    }
}

/**
 * Convert Nef polyhedron to triangular mesh (PolySet).
 * Returns interleaved [x0,y0,z0, x1,y1,z1, ...] and triangle indices [a0,b0,c0, a1,b1,c1, ...].
 */
void nefToTriMesh(
    const CGAL_Nef_polyhedron3& nef,
    std::vector<double>& outVerts,
    std::vector<int>& outIndices)
{
    CGAL_Polyhedron poly;
    nef.convert_to_polyhedron(poly);

    // Collect unique vertices
    std::map<CGAL_Kernel3::Point_3, int> vertexMap;
    outVerts.clear();
    outVerts.reserve(poly.number_of_vertices() * 3);

    auto getVertexIndex = [&](const CGAL_Kernel3::Point_3& p) -> int {
        auto it = vertexMap.find(p);
        if (it != vertexMap.end()) return it->second;
        int idx = vertexMap.size();
        vertexMap[p] = idx;
        outVerts.push_back(CGAL::to_double(p.x()));
        outVerts.push_back(CGAL::to_double(p.y()));
        outVerts.push_back(CGAL::to_double(p.z()));
        return idx;
    };

    // Index existing vertices
    for (auto vit = poly.vertices_begin(); vit != poly.vertices_end(); ++vit) {
        getVertexIndex(vit->point());
    }

    // Collect face indices (triangulate non-triangular faces)
    outIndices.clear();
    for (auto fit = poly.facets_begin(); fit != poly.facets_end(); ++fit) {
        std::vector<int> faceIndices;
        for (auto hit = fit->facet_begin(); hit != fit->facet_end(); ++hit) {
            faceIndices.push_back(getVertexIndex(hit->vertex().point()));
        }

        // Triangulate fan
        if (faceIndices.size() >= 3) {
            for (size_t i = 1; i + 1 < faceIndices.size(); i++) {
                outIndices.push_back(faceIndices[0]);
                outIndices.push_back(faceIndices[i]);
                outIndices.push_back(faceIndices[i + 1]);
            }
        }
    }
}

// ---- JNI Bindings ----

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    jvm = vm;
    return JNI_VERSION_17;
}

/**
 * Create a Nef polyhedron handle from vertex/face arrays.
 * Returns a long (pointer cast to jlong).
 */
JNIEXPORT jlong JNICALL
Java_com_github_grishberg_csg_native_CsgEngine_native_1build_1from_1mesh
(JNIEnv* env, jclass clazz, jdoubleArray jVerts, jintArray jFaces) {
    // Convert Java arrays to C++ vectors
    jsize vLen = env->GetArrayLength(jVerts);
    jsize fLen = env->GetArrayLength(jFaces);

    std::vector<double> verts(vLen);
    std::vector<int> faces(fLen);

    env->GetDoubleArrayRegion(jVerts, 0, vLen, verts.data());
    env->GetIntArrayRegion(jFaces, 0, fLen, faces.data());

    auto nef = buildNefFromArrays(verts, faces);
    if (!nef) return 0;

    // Store in a global map keyed by pointer address
    return reinterpret_cast<jlong>(nef.get());
}

/**
 * Boolean operation on two Nef handles.
 * Returns a new handle (jlong).
 */
JNIEXPORT jlong JNICALL
Java_com_github_grishberg_csg_native_CsgEngine_native_1boolean_1op
(JNIEnv* env, jclass clazz, jlong handleA, jlong handleB, jint opType) {
    auto* a = reinterpret_cast<CGAL_Nef_polyhedron3*>(handleA);
    auto* b = reinterpret_cast<CGAL_Nef_polyhedron3*>(handleB);

    auto result = booleanOp(
        std::shared_ptr<CGAL_Nef_polyhedron3>(a),
        std::shared_ptr<CGAL_Nef_polyhedron3>(b),
        opType);

    if (!result) return 0;
    return reinterpret_cast<jlong>(result.get());
}

/**
 * Convert Nef polyhedron to triangular mesh arrays.
 * Sets outVerts and outIndices Java arrays.
 */
JNIEXPORT void JNICALL
Java_com_github_grishberg_csg_native_CsgEngine_native_1to_1mesh
(JNIEnv* env, jclass clazz, jlong handle, jdoubleArray outVerts, jintArray outIndices) {
    auto* nef = reinterpret_cast<CGAL_Nef_polyhedron3*>(handle);

    std::vector<double> verts;
    std::vector<int> indices;
    nefToTriMesh(*nef, verts, indices);

    env->SetDoubleArrayRegion(outVerts, 0, verts.size(), verts.data());
    env->SetIntArrayRegion(outIndices, 0, indices.size(), indices.data());
}

/**
 * Export Nef polyhedron directly to binary STL file.
 * Mirrors OpenSCAD's export_stl.cc.
 */
JNIEXPORT jboolean JNICALL
Java_com_github_grishberg_csg_native_CsgEngine_native_1export_1stl
(JNIEnv* env, jclass clazz, jlong handle, jstring jFilename) {
    auto* nef = reinterpret_cast<CGAL_Nef_polyhedron3*>(handle);

    const char* filepath = env->GetStringUTFChars(jFilename, nullptr);
    std::ofstream ofs(filepath, std::ios::binary);
    env->ReleaseStringUTFChars(jFilename, filepath);

    if (!ofs.is_open()) return JNI_FALSE;

    // Convert to polyhedron and triangulate
    CGAL_Polyhedron poly;
    nef->convert_to_polyhedron(poly);

    // Build vertex map and triangulated indices
    std::map<CGAL_Kernel3::Point_3, int> vertexMap;
    std::vector<float> vertsFloat;
    std::vector<int> indices;

    for (auto vit = poly.vertices_begin(); vit != poly.vertices_end(); ++vit) {
        auto p = vit->point();
        vertexMap[p] = vertexMap.size();
        vertsFloat.push_back(static_cast<float>(CGAL::to_double(p.x())));
        vertsFloat.push_back(static_cast<float>(CGAL::to_double(p.y())));
        vertsFloat.push_back(static_cast<float>(CGAL::to_double(p.z())));
    }

    for (auto fit = poly.facets_begin(); fit != poly.facets_end(); ++fit) {
        std::vector<int> faceIndices;
        for (auto hit = fit->facet_begin(); hit != fit->facet_end(); ++hit) {
            faceIndices.push_back(vertexMap[hit->vertex().point()]);
        }
        for (size_t i = 1; i + 1 < faceIndices.size(); i++) {
            indices.push_back(faceIndices[0]);
            indices.push_back(faceIndices[i]);
            indices.push_back(faceIndices[i + 1]);
        }
    }

    // Write STL header
    char header[80] = "CGAL-CSG Model";
    ofs.write(header, 80);

    uint32_t triCount = static_cast<uint32_t>(indices.size() / 3);
    ofs.write(reinterpret_cast<char*>(&triCount), 4);

    // Write each triangle
    for (size_t i = 0; i < indices.size(); i += 3) {
        const auto& v0 = CGAL_Kernel3::Vector_3(
            vertsFloat[indices[i] * 3], vertsFloat[indices[i] * 3 + 1], vertsFloat[indices[i] * 3 + 2]);
        const auto& v1 = CGAL_Kernel3::Vector_3(
            vertsFloat[indices[i + 1] * 3], vertsFloat[indices[i + 1] * 3 + 1], vertsFloat[indices[i + 1] * 3 + 2]);
        const auto& v2 = CGAL_Kernel3::Vector_3(
            vertsFloat[indices[i + 2] * 3], vertsFloat[indices[i + 2] * 3 + 1], vertsFloat[indices[i + 2] * 3 + 2]);

        auto normal = CGAL::cross_product(v1 - v0, v2 - v0);
        double len = std::sqrt(normal.squared_length());
        if (len > 0) {
            normal = normal / len;
        }

        // Write normal
        float n[3] = {
            static_cast<float>(CGAL::to_double(normal.x())),
            static_cast<float>(CGAL::to_double(normal.y())),
            static_cast<float>(CGAL::to_double(normal.z()))
        };
        ofs.write(reinterpret_cast<char*>(n), 12);

        // Write 3 vertices
        ofs.write(reinterpret_cast<char*>(&vertsFloat[indices[i] * 3]), 12);
        ofs.write(reinterpret_cast<char*>(&vertsFloat[indices[i + 1] * 3]), 12);
        ofs.write(reinterpret_cast<char*>(&vertsFloat[indices[i + 2] * 3]), 12);

        // Attribute byte count
        uint16_t attrib = 0;
        ofs.write(reinterpret_cast<char*>(&attrib), 2);
    }

    ofs.close();
    return JNI_TRUE;
}

} // extern "C"
