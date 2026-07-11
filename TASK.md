# TASK.md — STL Validation & Repair + CSG BSP Render для opengl_kbd_render

## 0. Где склонирован OpenSCAD

```
/home/orangepi/data/projects/cpp/openscad/
```

Клон полный (`git clone https://github.com/openscad/openscad.git`).

**Что нужно изучить:**
- CSG BSP tree rendering — как OpenSCAD строит и рендерит CSG-деревья (класс `CGAL_Nef_polyhedron`, `CSGTreeEvaluator`, `Renderer`)
- Вывод в STL — как OpenSCAD конвертирует CSG в STL (через CGAL's `Nef_polyhedron_3` → `Polyhedron_3` → triangulation → binary STL)
- Отличия от текущего подхода (javascad — Java-based CSG, OpenSCAD — C++ with CGAL)
- OpenSCAD клавиатура работает корректно, значит CSG BSP pipeline в OpenSCAD надёжный — нужно адаптировать его логику для Kotlin

**Ключевые директории:**
- `src/core/` — CSG tree, evaluators
- `src/geometry/` — CGAL-обёртки, полиэдры
- `src/io/` — импорт/экспорт (STL, OFF и т.д.)

---

## 1. Где склонирован OrcaSlicer

```
/tmp/opencode2/OrcaSlicer/src/libslic3r/
```

Клон выполнен с `--depth 1` (только последний коммит).

---

## 2. Интересные места в OrcaSlicer для улучшения текущего проекта

### 2.1 Валидатор STL — Non-manifold edge detection

**Файлы:**
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/MeshSplitImpl.hpp` (строки 224–270) — `create_face_neighbors_index()`
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/TriangleMesh.cpp` (строки 1525–1563) — `its_num_open_edges()`, `VertexFaceIndex`

**Алгоритм:**
1. Строится карта вершин → граней (`VertexFaceIndex`) с префиксными суммами для O(1) доступа к списку граней на каждую вершину
2. Для каждого ребра каждой грани ищется соседняя грань, делящая это ребро **с противоположной ориентацией** (directed half-edge matching)
3. Если соседа нет (`n < 0`), ребро считается "открытым" = non-manifold edge

**Как адаптировать:** Создать `StlValidator.kt`, который принимает список треугольников и возвращает количество open edges, degenerate triangles, disconnected components. Использовать группировку вершин по EPSILON для корректного сравнения близких точек.

---

### 2.2 Репарер STL — Admesh pipeline (import-time repair)

**Файлы:**
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/TriangleMesh.cpp` (строки 79–178) — `trianglemesh_repair_on_import()`

**Pipeline из 5 шагов:**

| Шаг | Функция admesh | Назначение |
|-----|----------------|------------|
| 1 | `stl_check_facets_exact` | Классификация граней по количеству правильно соединенных ребер (3-edge, 2-edge, 1-edge, 0-edge) |
| 2 | `stl_check_facets_nearby` (до 2 итераций) | **Edge snapping**: объединение близких вершин в пределах допуску. Допуск начинается от длины кратчайшего ребра и увеличивается на bounding_diameter/10000 каждую итерацию |
| 3 | `stl_remove_unconnected_facets` | Удаление полностью отсоединенных граней (без правильно соединенных ребер) |
| 4 | ~~`stl_fill_holes`~~ **ОТКЛЮЧЕНО** (`#if 0`) — алгоритм делает больше вреда, чем пользы на сложных дырах. Вместо этого слайсинг закрывает разрывы в 2D слайсах |
| 5 | `stl_fix_normal_directions` + `stl_fix_normal_values` | Flood-fill для согласованных нормалей + пересчет значений из позиций вершин |

**Как адаптировать:** Создать `StlRepairer.kt`, который выполняет:
1. Edge snapping с настраиваемым допуском (по умолчанию 0.001)
2. Удаление вырожденных треугольников (площадь ~0)
3. Удаление отсоединенных граней
4. Исправление направлений нормалей через flood-fill

---

### 2.3 CGAL-based repair (aggressive, user-initiated)

**Файлы:**
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/MeshBoolean.cpp` (строки 478–562) — `MeshBoolean::cgal::repair()`

**Pipeline из 10 шагов:**
1. Конвертация в polygon soup
2. `PMP::repair_polygon_soup` — агрессивная очистка: удаление вырожденных треугольников, объединение близких вершин, исправление self-intersections
3. `PMP::polygon_soup_to_polygon_mesh` — построение proper mesh из очищенного soup
4. `PMP::remove_degenerate_faces` — удаление оставшихся zero-area граней
5. `PMP::remove_isolated_vertices` — удаление неиспользуемых вершин
6. **`PMP::duplicate_non_manifold_vertices`** — дублирование не-manifold вершин, чтобы каждый manifold patch получил свою копию (эффективно "разрезает" через non-manifold вершины)
7. `PMP::corefine_and_compute_union(mesh, mesh)` — boolean union с самим собой: удаляет внутренние грани и разрешает overlapping geometry
8. **`PMP::extract_boundary_cycles()` + `PMP::triangulate_and_refine_hole()`** — реальное заполнение дыр (отсутствует в admesh pipeline)
9. Валидация через `CGAL::is_closed(mesh)`
10. `PMP::orient_to_bound_a_volume` — финальная ориентация нормалей наружу

**Как адаптировать:** Для Java/Kotlin проекта CGAL недоступен напрямую, но можно реализовать упрощенные аналоги:
- Non-manifold vertex splitting через дублирование вершин в конфликтующих конфигурациях
- Hole filling через triangulation boundary cycles (аналогично `PMP::triangulate_and_refine_hole`)

---

### 2.4 STL Reader/Parser

**Файлы:**
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/TriangleMesh.cpp` (строки 181–218) — `ReadSTLFile()`, `from_stl()`
- `/tmp/opencode2/OrcaSlicer/src/libslic3r/Format/STL.hpp` и `.cpp`

**Ключевой момент:** STL читается через библиотеку admesh (`stl_open`), затем конвертируется в `indexed_triangle_set` с shared vertices через `stl_generate_shared_vertices`. После этого вызывается `trianglemesh_repair_on_import()` по умолчанию.

---

### 2.5 GUI Mesh Error Reporting

**Файлы:**
- `/tmp/opencode2/OrcaSlicer/src/slic3r/GUI/GUI_ObjectList.cpp` (строки 588–629) — `get_mesh_errors_info()`

**Что показывает пользователю:**
- Количество non-manifold edges: `"Error: %d non-manifold edge(s)."`
- Статистику репары: сколько было исправлено vs осталось ошибок
- Warning icon на объекте в UI если mesh не manifold

---

## 3. Текущее состояние (opengl_kbd_render)

### Задача 1: Создать StlValidator.kt ✅ СДЕЛАНО

**Файл:** `cad3d/src/main/java/com/github/grishberg/javascad/StlValidator.kt`

**Что реализовано:**
- Алгоритм из OrcaSlicer's `create_face_neighbors_index()` для поиска non-manifold edges
- Группировка вершин по EPSILON (1e-6) через `V3dKey` с хешированием
- VertexFaceIndex с префиксными суммами для быстрого доступа к граням на каждую вершину  
- Directed half-edge matching: поиск соседей с противоположной ориентацией ребра
- Подсчет open edges, degenerate triangles, disconnected components через BFS

**API:**
```kotlin
val validator = StlValidator()
val result = validator.validate(polygons)  // polygons: List<Polygon> из javascad
println(result.openEdges)   // количество non-manifold edges  
println(result.isManifold)  // true если openEdges == 0 && degenerateTriangles == 0
```

---

### Задача 2: Создать StlRepairer.kt ✅ СДЕЛАНО (базовый pipeline)

**Файл:** `cad3d/src/main/java/com/github/grishberg/javascad/StlRepairer.kt`

**Что реализовано (аналог admesh pipeline):**
1. **Edge snapping** — объединение близких вершин в пределах допуску с настраиваемым количеством итераций
2. **Remove degenerate triangles** — удаление треугольников с нулевой площадью  
3. **Remove disconnected facets** — удаление граней без правильно соединенных ребер (0-edge facets)
4. **Fix normal directions** — flood-fill для согласованных нормалей + пересчет из позиций вершин

**API:**
```kotlin
val repairer = StlRepairer()
val (repairedPolygons, result) = repairer.repair(polygons, RepairOptions(
    snapTolerance = 0.001,           // допуск для snapping  
    maxSnapIterations = 2,            // максимум итераций (как в OrcaSlicer)
    removeDisconnectedFacets = true,   // удалять отсоединенные грани?
    fixNormals = true                 // исправлять нормали?
))
```

---

### Задача 3: Интеграция с существующим конвейером экспорта STL

**Файл:** `cad3d/src/main/java/com/github/grishberg/javascad/StlExporter.kt` (строки 22–72)

**Текущий конвейер:**
```
CSG.polygons → PolygonValidatorMultithreading.fixPolygons() 
            → Triangulator.triangulate() → Facet[] → writeBinaryStl()
```

**Нужно добавить валидацию и репару после триангуляции, перед записью STL:**
```kotlin
val facetsFromPolygons: MutableList<Facet> = ArrayList()
for (p in fixPolygons) {
    val triangles = Triangulator.triangulate(p.getVertices(), p.getNormal())
    for (t in triangles) { ... }  // создание Facets
}

// === НОВАЯ ВАЛИДАЦИЯ ===  
val validatorResult = StlValidator().validate(facetsFromPolygons.mapToPolygons())
if (!validatorResult.isManifold && autoRepair) {
    val (repaired, repairStats) = StlRepairer().repair(polygonsBeforeTriangulation)
    // Перетриангулировать repaired полигоны  
}

// === ЗАПИСЬ STL ===
writeBinaryStl(facetsFromPolygons, channel)
```

---

### Задача 4: Тестирование на matrix_right.stl

**Что нужно сделать:**
1. Сгенерировать `matrix_right.stl` через существующий конвейер (`KeyboardBuilder.exportStl()`)  
2. Прочитать полученный STL через `StlImporter.loadBinarySTL()`
3. Запустить валидатор: проверить количество non-manifold edges **без** репары — ожидается много ошибок (CSG операции часто создают не-manifold геометрию)
4. Запустить репарер и снова валидировать — проверить, что openEdges стало 0

---

## 4. Что еще можно улучшить на основе OrcaSlicer

### 4.1 Hole Filling (отсутствует в текущем проекте)

В OrcaSlicer hole filling отключен в admesh pipeline (`#if 0`), но реализован через CGAL:
- `PMP::extract_boundary_cycles()` — находит все boundary cycles (open edges forming loops)  
- `PMP::triangulate_and_refine_hole()` — триангулирует каждую дыру

**Для Java:** Можно реализовать упрощенный hole filling:
1. Найти open edges через валидатор
2. Сгруппировать их в циклы (boundary cycles)  
3. Для каждого цикла выполнить constrained Delaunay triangulation или Ear Clipping

### 4.2 Non-manifold Vertex Splitting

В OrcaSlicer используется `PMP::duplicate_non_manifold_vertices()`: если вершина разделяется более чем двумя гранями в не-manifold конфигурации, она дублируется так, что каждый manifold patch получает свою копию.

**Для Java:** Реализовать через поиск конфликтующих вершин и их дублирование с пересозданием треугольников.

### 4.3 Boolean Union Self-Repair

В OrcaSlicer используется `PMP::corefine_and_compute_union(mesh, mesh)` — boolean union меша с самим собой:
- Удаляет внутренние грани (internal faces)  
- Разрешает self-intersections и overlapping geometry
- Оставляет только outer shell

**Для Java:** Можно использовать javascad's CSG operations для аналогичного эффекта: `model.subtractModel(model).addModel(model)` или реализовать через plane sweep.

### 4.4 Улучшение PolygonValidatorMultithreading

Текущий валидатор (`PolygonValidatorMultithreading.kt`) работает на уровне полигонов (до триангуляции) и исправляет коллинеарные точки, близкие вершины, naked edges. Можно интегрировать с новым `StlRepairer` для более агрессивной репары после триангуляции:

```kotlin
// До триангуляции  
val fixedPolygons = PolygonValidatorMultithreading().fixPolygons(polygons)

// После триангуляции (НОВОЕ)
val triangles = triangulate(fixedPolygons)
if (!StlValidator().validate(triangles).isManifold) {
    val (repaired, _) = StlRepairer().repair(triangles.toPolygons())  
}
```

---

## 5. Структура файлов после реализации

```
cad3d/src/main/java/com/github/grishberg/javascad/
├── StlExporter.kt              # Существующий экспорт STL (нужно модифицировать)
├── StlImporter.kt              # Существующий импорт STL  
├── StlValidator.kt             # НОВЫЙ — валидатор non-manifold edges (OrcaSlicer algorithm)
├── StlRepairer.kt              # НОВЫЙ — репарер STL mesh (admesh pipeline from OrcaSlicer)
└── optimizator/
    ├── PolygonValidatorMultithreading.kt  # Существующий валидатор полигонов (до триангуляции)
    └── ... другие утилиты оптимизации
```

---

## 6. Ключевые отличия от OrcaSlicer

| Аспект | OrcaSlicer | Наш проект |
|--------|------------|------------|
| Язык | C++ с CGAL + admesh | Kotlin/Java без внешних библиотек для mesh repair |  
| STL Reader | admesh library (`stl_open`) | Собственный `StlImporter` (binary STL parsing) |
| Non-manifold detection | Face-neighbor index через BFS | Аналогичный алгоритм в `StlValidator.kt` |
| Edge snapping | admesh's `stl_check_facets_nearby` с автоматическим допуском | Собственная реализация с настраиваемым допуском |  
| Hole filling | CGAL's `PMP::triangulate_and_refine_hole()` (aggressive) | Не реализовано (можно добавить позже через Ear Clipping) |
| Boolean self-repair | CGAL's `corefine_and_compute_union(mesh, mesh)` | Не реализовано (можно использовать javascad CSG operations) |  
| Normal fixing | admesh flood-fill + volume check | Собственная реализация через BFS по соседям граней |

---

## 7. Следующие шаги

1. ✅ Создать `StlValidator.kt` — валидатор non-manifold edges
2. ✅ Создать `StlRepairer.kt` — базовый репарер (edge snapping, degenerate removal, disconnected facets, normal fixing)  
3. ✅ Интегрировать валидацию/репару в `StlExporter.saveStl()` с опцией auto-repair
4. ✅ Написать тест для matrix_right.stl и проверки non-manifold edges до/после репары
5. ✅ Реализовать hole filling (boundary cycle extraction + ear-clipping triangulation)  
6. ✅ Добавить GUI отображение прогресса экспорта в viewer (проценты в StlExportDialog)
7. ✅ Создать `StlRepairerTest` — тест ремонта куба с дыркой
8. ✅ Исправить `StlValidator`: edge-map counting (как OrcaSlicer) + findSharedEdge (оба направления)
9. ⬜ **Изучить OpenSCAD CSG BSP** — понять как OpenSCAD строит CSG-деревья и рендерит в STL, перенести подход в Kotlin
10. ⬜ Переписать CSG pipeline (BSP tree) — текущий javascad генерирует не-manifold меши, нужно сделать правильно как в OpenSCAD
11. ⬜ Оптимизировать StlRepairer.fillHoles — на больших мешах (257k треугольников) ремонт может быть медленным, нужно профилировать
12. ⬜ Добавить GUI отображение ошибок mesh в viewer (как в OrcaSlicer's `GUI_ObjectList.cpp`)

### Примечание по производительности

На файле `matrix_right_test.stl` (257635 треугольников):
- Валидация (`StlValidator.validate()`): 5 секунд ✅
- Репарер (`StlRepairer.repair()`) с fillHoles: **таймаут 5 минут** ❌ — нужно профилировать и оптимизировать (вероятно edge snapping O(n²), BFS в flood-fill, или ear-clipping на больших циклах)

### OpenSCAD как референс

OpenSCAD использует CGAL для построения CSG, что гарантирует manifold-результат:
- `CGAL::Nef_polyhedron_3` — boolean операции всегда дают корректную 2-manifold геометрию
- `CGAL::Polyhedron_3` → triangulation → STL — конвертация в треугольники сохраняет manifold
- Для Kotlin нужно либо портировать логику CGAL, либо реализовать собственный BSP tree с корректным разрезанием и триангуляцией
