# TASK.md — Migration JSCAD → openscad

## Status: IN PROGRESS (99 errors remain)

## What's Done
- ✅ Создан пакет `com.github.grishberg.openscad.*` со всеми базовыми типами
- ✅ Mass import replacement completed (JSCAD → openscad)
- ✅ Side enum has all JSCAD values
- ✅ Abstract3dModel has rotate(x,y,z), align(s1,s2,other), withColor, etc.
- ✅ V3d has projectionZ/Y/X as member functions
- ✅ CSG has getPolygons
- ✅ Polygon has getVertices, getNormal, getColor
- ✅ Cylinder/Sphere accept Double parameters

## Remaining Issues (99 errors)
1. Integer literal errors — need .0 suffix:
   - TrackballCase.kt, RP2040Pink.kt, Walls.kt, KeyPlace.kt, KeyboardBuilder.kt, Amoeba.kt, BottomPoints.kt
2. Radius → Double type mismatch in Cylinder/Sphere constructors (~50 occurrences)
3. JscadAdapter uses mixed JSCAD/openscad types
4. VertexHolderUtils uses JSCAD types
5. Union constructor type mismatch with nullable lists
6. ThumbKeyPlace rotate(x,y,z) function needed

## Fix Order
1. Fix integer literals (add .0)
2. Fix Radius → Double (add .value to Radius parameters)
3. Fix JscadAdapter, VertexHolderUtils types
4. Remove JSCAD dependency

## Files That Need Fixes
- TrackballCase.kt (lines 34, 35, 51, 52, 56, 58, 59, 63, 64, 68)
- RP2040Pink.kt (lines 29, 44)
- Walls.kt (line 377)
- KeyPlace.kt (lines 26-30)
- KeyboardBuilder.kt (line 890)
- Amoeba.kt (line 32)
- BottomPoints.kt (lines 173, 179, 213, 219)
- ControllerHolderBuilder.kt (lines 76, 84, 86, 96)
- ThumbKeyPlace.kt (line 144)
- KeyMatrix.kt (line 64)
- JscadAdapter.kt (lines 19, 44, 82)
- VertexHolderUtils.kt (lines 30, 32, 49, 50)
- StlExporter.kt (lines 23, 109)
- PolygonValidatorMultithreading.kt (lines 50, 77, 82)
- CommonPolygonFinder.kt (line 15)
