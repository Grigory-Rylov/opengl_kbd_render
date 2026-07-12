cmake_minimum_required(VERSION 3.16)
project(csg_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(CGAL REQUIRED)
include(${CGAL_USE_FILE})

# Find Java SDK for JNI
find_package(Java REQUIRED COMPONENTS Development)
find_package(JNI REQUIRED)
include(UseJava)

# Include OpenSCAD sources for CGAL utilities
set(OPENSCAD_DIR "${CMAKE_SOURCE_DIR}/../openscad")

add_library(csg_native SHARED
    src/CsgEngine.cpp
    src/StlExport.cpp

    # OpenSCAD CGAL utilities (exact CSG core)
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-polyhedron.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-applyops.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-applyops-minkowski.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-kernel.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-triangulate.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-tess.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-orient.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-closed.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-convex.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-project.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/cgalutils-mesh.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/CGALCache.cc
    ${OPENSCAD_DIR}/src/geometry/cgal/CGALNefGeometry.cc

    # Core geometry types
    ${OPENSCAD_DIR}/src/geometry/Geometry.cc
    ${OPENSCAD_DIR}/src/geometry/GeometryCache.cc
    ${OPENSCAD_DIR}/src/geometry/PolySet.cc
    ${OPENSCAD_DIR}/src/geometry/PolySetBuilder.cc
    ${OPENSCAD_DIR}/src/geometry/PolySetUtils.cc
    ${OPENSCAD_DIR}/src/geometry/linalg.cc

    # Manifold backend
    ${OPENSCAD_DIR}/src/geometry/manifold/ManifoldGeometry.cc
    ${OPENSCAD_DIR}/src/geometry/manifold/manifoldutils.cc
    ${OPENSCAD_DIR}/src/geometry/manifold/manifold-applyops.cc
    ${OPENSCAD_DIR}/src/geometry/manifold/manifold-applyops-minkowski.cc

    # Utils
    ${OPENSCAD_DIR}/src/utils/calc.cc
)

target_include_directories(csg_native PRIVATE
    ${OPENSCAD_DIR}/src
    ${JNI_INCLUDE_DIRS}
    ${JNI_INCLUDE_PALETTE_DIRS}
)

target_compile_definitions(csg_native PRIVATE
    ENABLE_CGAL
    ENABLE_MANIFOLD
)

target_link_libraries(csg_native
    CGAL::CGAL
    ${JAVA_JVM_LIBRARY}
)

# Link manifold library from submodule
target_include_directories(csg_native PRIVATE
    ${OPENSCAD_DIR}/submodules/manifold/src
)

install(TARGETS csg_native DESTINATION lib)
