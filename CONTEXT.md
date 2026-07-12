# CONTEXT.md — Project Context Summary

## Goal
- Полностью удалить JSCAD и перенести пайплайн генерации геометрии на собственный Kotlin-движок (CSG + DSL поверх `PolySet3`).

## Constraints & Preferences
- Строго Kotlin (отказ от C++, JNI, CGAL native, JSCAD Java legacy).
- Отказ от внешних CLI-зависимостей (OpenSCAD binary).
- STL формат: бинарный и ASCII, совместимый с OpenSCAD.
- Ветка разработки: `openscad_fork`.
- Нельзя создавать классы в пакете `eu.printingin3d.javascad.*` — конфликт на уровне classpath.

## Progress

### Done
- BSP-движок (`BspEngine.kt`), примитивы (`cube`, `sphere`, `cylinder`), геометрия (`Vec3`, `PolySet3`, `Matrix4`).
- `JscadAdapter`: конвертация JSCAD N-gons -> `PolySet3` с шарингом вершин.
- Переписан `StlExporter.kt`: `fixPolygons` -> `JscadAdapter` -> `StlValidator.validateAndRepair`.
- **Создан полный пакет `com.github.grishberg.openscad.*`** в `cad3d`:
  - `coords/V3d.kt` — `V3d`, `Angles3d`, `Dims3d`, member-функции `getX/Y/Z`, `distance`, `unit`, `subtract`, `isZero`, `projectionX/Y/Z`.
  - `basic/Basic.kt` — `Radius`, `Angle` (`@JvmInline value class`).
  - `utils/Utils.kt` — `Color` (30+ констант), `Const`, `DoubleUtils`, `AssertValue`.
  - `enums/Side.kt` — полный enum `Side` (TOP_IN, TOP_OUT, BOTTOM_IN_CENTER и т.д.).
  - `vrl/CsgClasses.kt` — `Facet`, `Triangle3d` (3-arg + 4-arg normal-aware), `Polygon` (@JvmOverloads, toJson), `CSG`, `FacetGenerationContext`, `ColorFacetGenerationContext`.
  - `models/IModel.kt` — интерфейс `IModel`.
  - `models/Abstract3dModel.kt` — immutable wrapper над `CsgModel`: `move/rotate(x,y,z)/scale/mirror/addModel/subtractModel/align(side1,side2,other)/withColor`.
  - `models/Primitives.kt` — `Cube`, `Cylinder` (4 конструктора), `Sphere`, `StlModel`, `Hull`, `Minkowski`.
  - `models/EdgeType.kt` — enum `EdgeType`.
  - `models/surfaces/Surfaces.kt` — `S12x3`, `SmoothSurface`, `VoronoiSurface`, `BicubicSurfaceSpline`, `BicubicInterpolator` (with `generateSurface`).
  - `tranzitions/Union.kt` — `Union` (vararg/Collection/List конструкторы), `Difference`.
- **Массовое замещение импортов выполнено**: 0 оставшихся импортов `eu.printingin3d.javascad.*` в `cad3d`.
- **Kotlin + Java компиляция cad3d: 0 ошибок** — BUILD SUCCESSFUL.
- Исправлена проблема с classpath: `kbd_core/V3d.kt` дублирует `cad3d/V3d.kt` и стоит раньше на classpath — синхронизированы оба.

### In Progress
- Удаление `implementation(project(":javascad"))` из `cad3d/build.gradle.kts`.
- Миграция модулей `kbd_core`, `common`, `viewer`.

### Blocked
- Fan-триангуляция N-gons в `JscadAdapter` создает ~1500 open edges на общих рёбрах.
- `MeshRepair.kt` не справляется с 108K open edges без потери геометрии.

## Key Decisions
- Использовать пакет `com.github.grishberg.openscad.*` вместо `eu.printingin3d.javascad.*` — избегает classpath конфликтов.
- `Abstract3dModel` оборачивает `CsgModel` из `com.github.grishberg.csg.model` — immutable, цепочка операций создает новые экземпляры.
- `Radius` как `@JvmInline value class` с методом `.value` для извлечения `Double`.
- `Side` enum дублируется в `enums/` и `utils/` — использовать `enums/Side.kt`.
- `StlValidator.validateAndRepair` как финальный этап репарации на float32.
- `V3d.getX/Y/Z()` без `@JvmName` — Java-код зависит от стандартных getter-имён.
- Синхронизация `V3d.kt` между `cad3d` и `kbd_core` критична из-за classpath order.

## Next Steps
1. Удалить `implementation(project(":javascad"))` из `cad3d/build.gradle.kts` → проверить компиляцию.
2. Если ошибки — фиксить оставшиеся JSCAD-зависимости в cad3d.
3. Миграция `kbd_core`, `common`, `viewer` — замена импортов JSCAD → openscad.
4. Интеграция `BspEngine.csgOperation()` в пайплайн вместо JSCAD `CSG.union/diff/intersect`.
5. Реализация согласованной триангуляции N-gons для устранения open edges.
6. Реализация CSG operations в `Abstract3dModel` (union/difference/intersect через BspEngine).

## Critical Context
- `fixPolygons` (JSCAD) дает ~1 open edge, новый адаптер + fan-триангуляция дает ~1500.
- Viewer рендерит через JOGL fixed-function (`glBegin/glEnd`), требует конвертации CSG -> `VertexHolder`.
- BSP рекурсия стабильна для текущих моделей.
- `MeshRepair.kt` создан, но spatial hashing + union-find нестабилен.
- `kbd_core/V3d.kt` и `cad3d/V3d.kt` — дубликаты, kbd_core стоит раньше на classpath cad3d.
- `Union` принимает `vararg IModel`, `Collection<IModel>`, `List<Abstract3dModel>`.
- `Cylinder` конструкторы: `(height, r)`, `(height, r1, r2)`, `(height, r, segments)`, `(height, r1, r2, segments)`.
- `Sphere` конструкторы: `(r, segments)`, `(r)`.

## Relevant Files
- `cad3d/src/main/java/com/github/grishberg/csg/bsp/BspEngine.kt`: ядро BSP CSG на чистом Kotlin.
- `cad3d/src/main/java/com/github/grishberg/csg/adapter/JscadAdapter.kt`: адаптер JSCAD -> PolySet3.
- `cad3d/src/main/java/com/github/grishberg/javascad/StlExporter.kt`: обновленный пайплайн экспорта STL.
- `cad3d/src/main/java/com/github/grishberg/csg/optimizator/MeshRepair.kt`: WIP репаратор.
- `cad3d/src/main/java/com/github/grishberg/openscad/coords/V3d.kt`: V3d (синхронизирован с kbd_core).
- `kbd_core/src/main/java/com/github/grishberg/openscad/coords/V3d.kt`: V3d (приоритетный на classpath).
- `cad3d/src/main/java/com/github/grishberg/openscad/vrl/CsgClasses.kt`: Facet, Triangle3d, Polygon, CSG.
- `cad3d/src/main/java/com/github/grishberg/openscad/models/Abstract3dModel.kt`: immutable wrapper.
- `cad3d/src/main/java/com/github/grishberg/openscad/models/Primitives.kt`: Cube, Cylinder, Sphere, Hull.
- `cad3d/src/main/java/com/github/grishberg/openscad/enums/Side.kt`: полный Side enum.
- `cad3d/src/main/java/com/github/grishberg/openscad/models/surfaces/Surfaces.kt`: surface stubs.
- `cad3d/build.gradle.kts`: временно содержит `:javascad`, нужно удалить.
