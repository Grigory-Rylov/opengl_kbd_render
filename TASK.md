# TASK: Замена BSP CSG движка на Half-edge Mesh

## Проблема

Текущий движок CSG использует BSP-деревья (порт csg.js). Для модели `matrix_right.stl`:
- 122,528 полигонов → 245,332 треугольника
- **13,427 non-manifold edges** (naked=10,980, multi-face=2,447)
- Orca Slicer: 5,394 non-manifold edges

Корень проблемы: BSP не может гарантировать watertight mesh. Сопланарные перекрытия, накопление ошибок floating-point при split, независимая триангуляция соседних полигонов.

**Цель:** Заменить BSP CSG на Half-edge Mesh с гарантией manifold topology. Zero non-manifold edges.

---

## Архитектура Half-edge Mesh

### Структуры данных

```
HalfEdge:
  vertex: Vertex        // начало ребра
  twin: HalfEdge        // противоположная半边 того же ребра
  next: HalfEdge        // следующая半边 в той же грани
  prev: HalfEdge        // предыдущая半边 в той же грани
  face: Face            // грань, которой принадлежит半边
  edge: Edge            // ребро

Vertex:
  position: Vec3        // координата
  outgoing: HalfEdge    // любая半边, начинающаяся в этой вершине
  normal: Vec3          // нормаль (вычисленная)

Face:
  halfedge: HalfEdge    // любая半边 грани
  normal: Vec3          // нормаль грани
  color: Color          // цвет
  inside: boolean       // флаг для boolean ops (inside/outside второго тела)

Edge:
  halfedge: HalfEdge    // одна из двух полу-граней
  twin: Edge            // (опционально) ссылка на twin

Mesh:
  vertices: HashMap<VertexId, Vertex>
  edges: HashMap<EdgeId, Edge>
  faces: HashMap<FaceId, Face>
  halfedges: HashMap<HEdgeId, HalfEdge>
```

### Инварианты (garантируются всегда)

1. `he.twin.twin == he`
2. `he.next.prev == he`
3. `he.twin.next` — следующая半边 в соседней грани
4. Каждое ребро принадлежит ровно 2 граням (manifold)
5. Ориентация: обход `he → he.next → he.next...` идёт CCW по грани

### Robust predicates

Для операций пересечения использовать точные предикаты (Shewchuk):
- `orient3d(a, b, c, p)` — ориентация точки относительно плоскости
- `insphere(a, b, c, d, p)` — лежит ли точка внутри сферы
- Библиотека: `Adaptive.java` из Shewchuk's Adaptive Precision Arithmetic

---

## Требуемые операции (сохранить полный API)

### 1. Булевы операции (ЗАМЕНА BSP)

| Операция | Текущий путь | Новый механизм |
|---|---|---|
| `union(A, B)` | `CSG.union()` → BSP | Boundary traversal + half-edge reconstruction |
| `difference(A, B)` | `CSG.difference()` → BSP | Boundary traversal + half-edge reconstruction |
| `intersection(A, B)` | `CSG.intersect()` → BSP | Boundary traversal + half-edge reconstruction |

**Алгоритм boolean ops (на основе Maniataki-Elad / Manifold):**

1. Найти все пересечения рёбер A с гранями B и наоборот
2. Вставить точки пересечения в half-edge структуру обоих мешей
3. Маркировать грани: inside/outside второго тела
4. Пройти по границам (boundary traversal), собирая грани для результата:
   - **Union**: грани, которые outside хотя бы одного тела
   - **Difference**: грани A outside B
   - **Intersection**: грани, которые inside обоих тел
5. Перестроить half-edge структуру результата
6. Проверить инварианты manifold

### 2. Трансформации (ПЕРЕНОС КАК ЕСТЬ)

| Операция | Что делает | Зависимости |
|---|---|---|
| `Translate(V3d)` | Смещение всех вершин | Matrix 4×4 |
| `Rotate(Angles3d)` | Поворот по XYZ | Matrix 4×4 |
| `Scale(V3d)` | Масштабирование по осям | Matrix 4×4 |
| `Mirror(Direction)` | Зеркальное отражение | Matrix 4×4 + flip winding |

Трансформации применяются к `Vertex.position` через `TransformationMatrix`. Flip winding при mirror (обратить порядок半边).

### 3. Примитивы (ГЕНЕРАЦИЯ МESHA)

| Модель | Геометрия | Генерация mesh |
|---|---|---|
| `Cube(Dims3d)` | Параллелепипед | 6 граней × 4 вершины = 24 halfedges |
| `Sphere(Radius)` | Сфера | Сферическая параметризация, numSlices × numStacks quads |
| `Cylinder(length, r1, r2)` | Цилиндр/конус | Крышки + боковая поверхность |
| `Prism(length, r1, r2, sides)` | N-угольная призма | Частный случай Cylinder |
| `Polyhedron(List<Triangle3d>)` | Набор треугольников | Прямое создание half-edge из треугольников |
| `Import(File)` | Импорт STL | Parse STL → triangles → half-edge mesh |

### 4. Продвинутые операции

| Модель | Геометрия | Реализация |
|---|---|---|
| **`Hull(models...)`** | Выпуклая оболочка | **ОСТАВИТЬ QuickHull3D** (уже использует HalfEdge внутри) → конвертировать результат в нашу Half-edge структуру |
| **`Minkowski(models...)`** | Сумма Минковского | Декартово произведение вершин → Hull |
| **`LinearExtrude(2dModel, height, twist, scale)`** | Экструзия 2D → 3D | Послойная генерация: нижняя крышка + боковые грани + верхняя крышка |
| **`Support(dims, thickness)`** | Опорная сетка | `Cube` minus zigzag slices → `Difference` |

### 5. Поверхности

| Модель | Тип | Реализация |
|---|---|---|
| `SurfaceBuilder(grid, thickness)` | Гладкая поверхность | Сетка → нормали → offset по нормали → боковые стенки |
| `SmoothSurface(strategy, thickness, edges)` | Параметрическая поверхность | strategy.buildSurface() → offset → боковые стенки |
| `BicubicSurfaceSpline` | Бикубическая spline | Catmull-Rom на 4×4 патчах |
| `BSplineSurface` | B-Spline | Uniform knot vector |
| `BezierSurface` | Безье | Lagrange basis functions |

Все поверхности генерируют closed mesh: верхний слой + нижний слой + боковые грани.

### 6. Композиция и fluent API (СОХРАНИТЬ)

```kotlin
// Текущий API — сохранить полностью
cube.move(10, 20, 30)
    .rotate(0, 45, 0)
    .addModel(cylinder)
    .subtractModel(sphere)
    .align(Side.X.MIN_IN, otherModel)
    .round(Plane.XY, 2.0)
    .withColor(Color.RED)
```

Все методы `Abstract3dModel` (move, rotate, align, round, annotate, etc.) — сохранить как есть.

### 7. Не реализовано (оставить как есть)

- `Ring` — NotImplementedException
- `Hull` transition — NotImplementedException
- `NURBSSurface` — пустой цикл

---

## План реализации

### Фаза 1: Half-edge структуры данных

**Новые файлы:**

```
javascad/src/main/java/eu/printingin3d/javascad/halfedge/
├── HalfEdge.java          // Полуребро
├── Vertex.java            // Вершина
├── Face.java              // Грань
├── Edge.java              // Ребро
├── Mesh.java              // Mesh — контейнер + операции
├── MeshBuilder.java       // DSL для построения mesh
├── MeshOps.java           // Статические операции (split, merge, sew)
└── RobustPredicate.java   // Orient3d, Insphere (Shewchuk)
```

**MeshBuilder API:**
```java
Mesh mesh = new MeshBuilder()
    .addFace(v0, v1, v2, v3)     // добавить N-gon
    .sew(mesh2)                  // сшить два меша по общему ребру
    .build();
```

**MeshOps:**
- `splitEdge(he, newVertex)` — разделить ребро, вставив вершину
- `splitFace(face, v1, v2)` — разделить грань диагональю
- `collapseEdge(edge)` — свернуть ребро (удалить)
- `flipEdge(edge)` — перевернуть диагональ треугольника
- `mergeVertices(v1, v2, threshold)` — слить близкие вершины
- `computeNormals()` — рассчитать нормали вершин и граней
- `verifyManifold()` — проверить инварианты

### Фаза 2: Примитивы на Half-edge

Переписать генерацию примитивов через `MeshBuilder`:

```java
// Cube
Mesh cube = Mesh.primitive().cube(width, height, depth);

// Sphere
Mesh sphere = Mesh.primitive().sphere(radius, slices, stacks);

// Cylinder
Mesh cylinder = Mesh.primitive().cylinder(height, rBottom, rTop, segments);

// Polyhedron
Mesh polyhedron = Mesh.fromTriangles(triangles);
```

**Файлы:**
```
javascad/src/main/java/eu/printingin3d/javascad/primitives/
├── PrimitiveMeshFactory.java   // Статические методы создания примитивов
└── MeshPrimitive.java          // Enum: CUBE, SPHERE, CYLINDER, PRISM
```

### Фаза 3: Булевы операции на Half-edge

**Самая сложная фаза.** Реализовать algorithm из Maniataki-Elad paper:

```
javascad/src/main/java/eu/printingin3d/javascad/booleanops/
├── BooleanOp.java           // Enum: UNION, DIFFERENCE, INTERSECTION
├── MeshBoolean.java         // Основное API
│   └── static Mesh operate(Mesh a, Mesh b, BooleanOp op)
├── IntersectionFinder.java  // Поиск пересечений рёбер и граней
├── InsideTester.java        // Тест inside/outside (ray casting)
├── BoundaryCollector.java   // Boundary traversal для сбора граней результата
└── SeamResolver.java        // Разрешение неоднозначностей на швах
```

**Алгоритм `MeshBoolean.operate(a, b, op)`:**

```
1. Для каждого ребра A: найти пересечения с гранями B → вставить вершины
2. Для каждого ребра B: найти пересечения с гранями A → вставить вершины
3. Для каждой грани A: определить inside/outside B
4. Для каждой грани B: определить inside/outside A
5. Boundary traversal: собрать грани по правилам boolean операции
6. Построить результирующий mesh из собранных граней
7. Merge близкие вершины (threshold = 1e-8)
8. Compute normals
9. Verify manifold
```

**Оптимизация поиска пересечений:** Spatial hash grid (cell size = boundingBox.diag / 100).

### Фаза 4: Интеграция с существующим API

Сохранить `IModel` интерфейс и `Abstract3dModel`, но изменить внутреннее представление:

```kotlin
// Было:
interface IModel {
    fun toCSG(context: FacetGenerationContext): CSG
}

// Станет:
interface IModel {
    fun toMesh(context: FacetGenerationContext): Mesh
}
```

`Abstract3dModel`:
```kotlin
abstract class Abstract3dModel {
    protected abstract fun toInnerMesh(context: FacetGenerationContext): Mesh
    
    fun addModel(other: Abstract3dModel): Abstract3dModel =
        BooleanUnion(this, other)
    
    fun subtractModel(other: Abstract3dModel): Abstract3dModel =
        BooleanDiff(this, other)
    
    override fun toMesh(context: FacetGenerationContext): Mesh {
        var mesh = toInnerMesh(context)
        if (rotate != null) mesh = mesh.transformed(rotationMatrix)
        if (move != null) mesh = mesh.transformed(translationMatrix)
        return mesh
    }
}
```

**Конвертация Mesh → Facet (для экспорта STL):**
```java
// Mesh.toFacets() — обход всех граней, триангуляция N-gon → Fan triangulation
List<Facet> Mesh.toFacets() {
    List<Facet> facets = new ArrayList<>();
    for (Face f : faces) {
        List<Vec3> verts = f.getVertices(); // CCW order
        for (int i = 1; i < verts.size() - 1; i++) {
            facets.add(new Facet(
                new Triangle3d(verts[0], verts[i], verts[i+1]),
                f.normal, f.color
            ));
        }
    }
    return facets;
}
```

### Фаза 5: Hull и Minkowski

**Hull** — QuickHull3D уже использует HalfEdge внутри. Конвертировать результат:
```java
Mesh Hull.toInnerMesh(context) {
    List<Vec3> allVertices = collectAllVertices(children);
    QuickHull3D hull = new QuickHull3D(allVertices);
    hull.process();
    // Convert QuickHull3D.Face/Vertex/HalfEdge → our Mesh
    return Mesh.fromQuickHull(hull);
}
```

**Minkowski** — картезианское произведение вершин → Hull.

### Фаза 6: Линейная экструзия и поверхности

**LinearExtrude:**
```java
Mesh LinearExtrude.toInnerMesh(context) {
    Mesh mesh = new MeshBuilder().begin();
    
    // Нижняя крышка (2D contour → face)
    mesh.addFace(contour.points);
    
    // Боковые грани (послойно)
    for (int layer = 0; layer < steps; layer++) {
        Contour lower = getContour(layer);
        Contour upper = getContour(layer + 1);
        // Quad strip между lower и upper
        for (each edge pair) {
            mesh.addEdgeQuad(lowerEdge, upperEdge);
        }
    }
    
    // Верхняя крышка
    mesh.addFace(topContour.points);
    
    return mesh.seal().build(); // seal — закрыть boundary edges
}
```

**SmoothSurfaces** — переписать генерацию: вместо `Polygon.fromPolygons()` → `MeshBuilder.addFace()`.

### Фаза 7: Экспорт и валидация

```java
// StlExporter — упростить
fun Mesh.toStl(fileName: String) {
    val facets = this.toFacets()
    // Удалить вырожденные треугольники
    val clean = facets.filter { !it.isDegenerate() }
    // Deduplication (больше не нужна канонизация — mesh гарантированно manifold)
    val deduped = clean.deduplicate()
    writeBinaryStl(deduped, fileName)
}
```

---

## Что НЕ трогать

- `KeyboardBuilder.kt` — бизнес-логика генерации клавиатуры
- `kbd_core/` — конфигурация расположения клавиш
- `plugin/` — интерфейсы плагинов, настройки
- `viewer/` — GUI, OpenGL рендеринг
- `TransformationFactory` / `TransformationMatrix` — трансформации
- `V3d`, `Angles3d`, `Boundaries3d` — математика
- `Color` — цвета
- `Facet` — для экспорта STL
- `Triangle3d` — треугольник

---

## Критерии приёмки

1. **Zero non-manifold edges** для `matrix_right.stl`
2. Все существующие примитивы работают: Cube, Sphere, Cylinder, Prism, Polyhedron, Import
3. Все булевы операции: union, difference, intersection
4. Hull работает (через QuickHull3D → Mesh конвертацию)
5. Minkowski работает
6. LinearExtrude работает
7. SmoothSurfaces работают
8. Fluent API сохранён: `.move().rotate().addModel().subtractModel().align().round()`
9. Экспорт STL работает
10. Orca Slicer показывает 0 non-manifold edges

---

## Оценка сложности

| Фаза | Оценка | Зависимости |
|---|---|---|
| 1. Half-edge структуры | 2 недели | — |
| 2. Примитивы | 1 неделя | Фаза 1 |
| 3. Булевы операции | 4-6 недель | Фаза 1 |
| 4. Интеграция с API | 2 недели | Фазы 1-3 |
| 5. Hull, Minkowski | 1 неделя | Фазы 1, 4 |
| 6. Extrude, Surfaces | 2 недели | Фазы 1, 4 |
| 7. Экспорт, тесты | 1 неделя | Всё выше |
| **Итого** | **~13-15 недель** | |

---

## Ссылки

- Manifold: https://github.com/elbanha/manifold
- Manifold статья: https://www.elbanha.com/2021/12/manifold-solid-modeling.html
- SIGGRAPH 2022 talk: https://www.youtube.com/watch?v=7JgEOwnQeNs
- Maniataki-Elad paper: "Manifold: A Robust Boolean Operations Library"
- Shewchuk predicates: http://www.cs.cmu.edu/~quake/robust.html
- Half-edge tutorial: https://cs.smith.edu/~thiebaut/CS145/HalfEdgeMeshTutorial.pdf
- OpenCASCADE BooleanOps: https://dev.opencascade.org/doc/overview/html/occt_user_guides__model_algorithms.html
