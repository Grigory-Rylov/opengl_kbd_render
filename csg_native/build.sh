#!/bin/bash
# Build native CSG library (CGAL Nef CSG + STL export)
# Requires: CGAL, JDK 17, CMake 3.16+

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/build"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=== Building CSG Native Library ==="
echo "Project: $PROJECT_DIR"
echo "Build:   $BUILD_DIR"

# Check dependencies
if ! command -v cmake &> /dev/null; then
    echo "ERROR: cmake not found"
    exit 1
fi

if ! pkg-config --exists CGAL; then
    echo "ERROR: CGAL not found. Install with: sudo apt install libgmp3-dev libmpfr-dev libcgal-dev"
    exit 1
fi

if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME not set"
    exit 1
fi

# Create build directory
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Configure
cmake .. \
    -DCMAKE_BUILD_TYPE=Release \
    -DJAVA_HOME="$JAVA_HOME" \
    -DCMAKE_INSTALL_PREFIX="${PROJECT_DIR}/cad3d/native"

# Build
cmake --build . -j$(nproc)

echo "=== Build complete ==="
echo "Library: ${BUILD_DIR}/libcsg_native.so"
echo ""
echo "To use: copy libcsg_native.so to cad3d/native/ and set java.library.path"
