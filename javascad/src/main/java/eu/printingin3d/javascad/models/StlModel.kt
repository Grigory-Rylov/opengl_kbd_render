package eu.printingin3d.javascad.models

import eu.printingin3d.javascad.context.IScadGenerationContext
import eu.printingin3d.javascad.coords.Boundaries3d
import eu.printingin3d.javascad.coords.Boundary
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon

/**
 * 3D модель, созданная из STL файла (списка полигонов)
 */
class StlModel(private val polygons: List<Polygon>) : Abstract3dModel() {

    init {
        require(polygons.isNotEmpty()) { "Список полигонов не может быть пустым" }
        // Native rendering path colors the whole mesh with model.color, so propagate
        // the polygons' color (set by StlImporter) to the model, otherwise it defaults
        // to gray regardless of the color passed when loading the STL.
        polygons.first().color?.let { this.color = it }
    }

    // Кэшируем границы для производительности
    private val cachedBoundaries by lazy { calculateBoundaries() }

    override fun innerCloneModel(): Abstract3dModel {
        return StlModel(polygons).apply {
            this.color = this@StlModel.color
        }
    }

    override val modelBoundaries: Boundaries3d
        get() = cachedBoundaries

    override fun toInnerNativeMesh(context: FacetGenerationContext): Long {
        return Manifold3dEngine.polygonsToManifold(Manifold3dEngine.bindings(), polygons)
    }

    /**
     * Вычисляет границы модели на основе всех вершин всех полигонов
     */
    private fun calculateBoundaries(): Boundaries3d {
        var minX = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE

        // Проходим по всем полигонам и всем вершинам
        for (polygon in polygons) {
            for (vertex in polygon.vertices) {
                val x = vertex.x
                val y = vertex.y
                val z = vertex.z

                // Обновляем минимальные и максимальные значения
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                if (z < minZ) minZ = z
                if (z > maxZ) maxZ = z
            }
        }

        // Проверяем на случай, если все вершины имеют одинаковые координаты
        if (minX == Double.MAX_VALUE) {
            return Boundaries3d.EMPTY
        }

        return Boundaries3d(
            Boundary(minX, maxX), Boundary(minY, maxY), Boundary(minZ, maxZ)
        )
    }

    /**
     * Возвращает количество полигонов в модели
     */
    fun getPolygonCount(): Int = polygons.size

    /**
     * Возвращает копию списка полигонов
     */
    fun getPolygons(): List<Polygon> = ArrayList(polygons)

    override fun toString(): String {
        return "StlModel(polygons=${polygons.size}, boundaries=$modelBoundaries)"
    }

    override val childrenModels: MutableList<Abstract3dModel> = mutableListOf()
    override fun innerSubModel(context: IScadGenerationContext?): Abstract3dModel? = null
}
