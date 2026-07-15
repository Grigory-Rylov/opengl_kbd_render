package eu.printingin3d.javascad.models

import eu.printingin3d.javascad.context.IScadGenerationContext
import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.coords.Boundaries3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.enums.Plane
import eu.printingin3d.javascad.enums.Side
import eu.printingin3d.javascad.exceptions.IllegalValueException
import eu.printingin3d.javascad.tranform.TransformationFactory
import eu.printingin3d.javascad.tranzitions.Difference
import eu.printingin3d.javascad.tranzitions.Union
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.utils.AssertValue
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.utils.RoundProperties

import eu.printingin3d.javascad.vrl.FacetGenerationContext
import java.util.Arrays
import java.util.stream.Collectors

/**
 *
 * Immutable implementation of IModel interface and adds convenient methods to make it easier
 * to move or rotate
 * the 3D models. Every primitive 3D object and 3D transition extend this class.
 *
 * None of the methods changing this object rather creates a new object with the changes and
 * gives that
 * changed object back. This make cloning and other similar techniques unnecessary.
 *
 * @author ivivan <ivivan></ivivan>@printingin3d.eu>
 */
abstract class Abstract3dModel : IModel {

    /**
     * For testing purposes only.
     *
     * @return the tag of the model
     */
    protected var tag: Int = 0
        private set
    var move: V3d = V3d.ZERO
        private set
    private var rotate: Angles3d = Angles3d.ZERO

    /**
     * For testing purposes only.
     *
     * @return the debug flag of the model
     */
    protected var isDebug: Boolean = false
        private set

    /**
     * For testing purposes only.
     *
     * @return the background flag of the model
     */
    protected var isBackground: Boolean = false
        private set
    private val roundingPlane: MutableMap<Plane, RoundProperties> = HashMap<Plane, RoundProperties>()
    private val annotations: MutableSet<String> = HashSet<String>()
    var color: Color = Color.GRAY
        protected set

    /**
     * Moves this object by the given coordinates. This object won't be changed, but a new object
     * will be created.
     *
     * @param delta the coordinates used by the move
     * @return the new object created
     */
    fun move(delta: V3d): Abstract3dModel {
        val result = cloneModel()
        result.move = this.move.add(delta)
        return result
    }

    fun move(x: Number, y: Number, z: Number): Abstract3dModel {
        val result = cloneModel()
        result.move = this.move.add(V3d(x.toDouble(), y.toDouble(), z.toDouble()))
        return result
    }

    fun moveY(y: Number): Abstract3dModel {
        val result = cloneModel()
        result.move = this.move.add(V3d(0.0, y.toDouble(), 0.0))
        return result
    }

    fun moveX(x: Number): Abstract3dModel {
        val result = cloneModel()
        result.move = this.move.add(V3d(x.toDouble(), 0.0, 0.0))
        return result
    }

    fun moveZ(z: Number): Abstract3dModel {
        val result = cloneModel()
        result.move = this.move.add(V3d(0.0, 0.0, z.toDouble()))
        return result
    }

    fun resetZ(): Abstract3dModel {
        val result = cloneModel()
        result.move = V3d(this.move.x, this.move.y, 0.0)
        return result
    }

    /**
     *
     * Add moves to this model, which converts this to an [Union], representing more
     * than one model.
     *
     * @param delta the collection of coordinates used by the move operation
     * @return a new object which holds the moved objects
     */
    fun moves(delta: MutableCollection<V3d>): Abstract3dModel? {
        if (!delta.isEmpty()) {
            var result: Abstract3dModel = Empty3dModel()
            for (c in delta) {
                result = result.addModel(this.move(c))
            }
            return result
        }
        return this
    }

    /**
     *
     * Add moves to this model, which converts this to an [Union], representing more
     * than one model.
     *
     * The moved objects is annotated with the given list of annotations respectively. There
     * has to be an equal number of moves and annotations given, otherwise an exception is thrown
     * .
     *
     * If any item in the annotations list is null that copy of the object won't be annotated
     * .
     *
     * It is very convenient with the use of Coords3d.createVariances. For example:
     * `<pre>
     * object.moves(new Coords3d(1,1,0).createVariances(), "+1+1", "+1-1", "-1+1", "-1-1")
    </pre>` *
     *
     * @param delta the collection of coordinates used by the move operation
     * @param annotations the list of annotations to be used
     * @return a new object which holds the moved objects
     * @throws IllegalValueException in case the number of moves and annotations are not equal
     */
    fun moves(delta: MutableList<V3d>, vararg annotations: String): Abstract3dModel {
        return moves(delta, Arrays.asList<String>(*annotations))
    }

    /**
     *
     * Add moves to this model, which converts this to an [Union], representing more
     * than one model.
     *
     * The moved objects is annotated with the given list of annotations respectively. There
     * has to be an equal number of moves and annotations given, otherwise an exception is thrown
     * .
     *
     * If any item in the annotations list is null that copy of the object won't be annotated
     * .
     *
     * @param delta the collection of coordinates used by the move operation
     * @param annotations the list of annotations to be used
     * @return a new object which holds the moved objects
     * @throws IllegalValueException in case the number of moves and annotations are not equal
     */
    fun moves(delta: MutableList<V3d>, annotations: MutableList<String>): Abstract3dModel {
        AssertValue.isTrue(
            delta.size == annotations.size,
            ("There should be the same number of moves and annotations given, " + "but " + delta.size + " moves and " + annotations.size + " annotations have been given.")
        )

        if (!delta.isEmpty()) {
            var result: Abstract3dModel = Empty3dModel()
            var i = 0
            for (c in delta) {
                result = result.addModel(this.move(c).annotate(annotations.get(i++)))
            }
            return result
        }
        return this
    }

    /**
     * Add moves to this model, which converts this to an [Union], representing more than
     * one model.
     *
     * @param delta the collection of coordinates used by the move operation
     * @return a new object which holds the moved objects
     */
    fun moves(vararg delta: V3d): Abstract3dModel {
        return moves(Arrays.asList<V3d?>(*delta))
    }

    /**
     * Creates a new object by rotating this object with the given angle.
     *
     * @param delta the angle it will be rotated
     * @return the new object created
     */
    fun rotate(delta: Angles3d): Abstract3dModel {
        val result = cloneModel()
        result.rotate = this.rotate.rotate(delta)
        result.move = this.move.rotate(delta)
        return result
    }

    fun rotate(x: Double, y: Double, z: Double): Abstract3dModel {
        val result = cloneModel()
        val delta = Angles3d(x, y, z)
        result.rotate = this.rotate.rotate(delta)
        result.move = this.move.rotate(delta)
        return result
    }

    /**
     * Add rotates to this model, which converts this to an [Union], representing more than
     * one model.
     * This object won't be changed.
     *
     * @param delta the collection of angles used by the rotate operation
     * @return a new object which holds the moved objects
     */
    fun rotates(delta: MutableCollection<Angles3d>): Abstract3dModel {
        if (!delta.isEmpty()) {
            val newModels: MutableList<Abstract3dModel> = ArrayList<Abstract3dModel>()
            for (c in delta) {
                newModels.add(this.rotate(c))
            }
            return Union(newModels)
        }
        return this
    }

    /**
     * Add rotates to this model, which converts this to an [Union], representing more than
     * one model.
     * This object won't be changed.
     *
     * @param delta the collection of angles used by the rotate operation
     * @return a new object which holds the moved objects
     */
    fun rotates(vararg delta: Angles3d): Abstract3dModel {
        return rotates(Arrays.asList<Angles3d?>(*delta))
    }

    /**
     * Creates a new object with the debug flag set, which means it is rendered in a different color
     * in preview mode (it does not affect the CGAL rendering or STL export). It is quite
     * useful debugging Difference.
     *
     * @return the new object created
     */
    fun debug(): Abstract3dModel {
        val result = cloneModel()
        result.isDebug = true
        return result
    }

    /**
     * Creates a new object with the background flag set, which means it will render in a
     * transparent
     * light gray color in preview mode and is skipped in CGAL rendering or STL export.
     * This is mainly used as a helping object for reference during the sketch.
     *
     * @return the new object created
     */
    fun background(): Abstract3dModel {
        val result = cloneModel()
        result.isBackground = true
        return result
    }

    protected abstract val childrenModels: MutableList<Abstract3dModel>

    protected fun findAnnotatedModel(annotation: String?): MutableList<Abstract3dModel> {
        if (annotation == null) {
            return mutableListOf(this)
        }

        var result: MutableList<Abstract3dModel> =
            if (annotations.contains(annotation)) mutableListOf(this) else mutableListOf()
        val children = this.childrenModels
        if (!children.isEmpty()) {
            result = ArrayList(result)
            result.addAll(children.stream().flatMap { m: Abstract3dModel -> m.findAnnotatedModel(annotation).stream() }
                .map { a: Abstract3dModel -> a.move(move) }.map { a: Abstract3dModel -> a.rotate(rotate) }
                .collect(Collectors.toList()))
        }
        return result
    }

    /**
     *
     * Annotate this object with the given annotation.
     *
     * An object can be annotated by several annotation at the same time.
     * A new annotation never overrides an old one.
     *
     * If the given parameter is null this method does nothing.
     *
     * @param annotation the annotation will be used - can be null
     * @return the new object created
     */
    fun annotate(annotation: String): Abstract3dModel {
        if (annotation == null) {
            return this
        }
        val result = cloneModel()
        result.annotations.add(annotation)
        return result
    }

    /**
     * Creates a clone of this model, so after the cloning any change on it won't affect.
     * this model
     *
     * @return a copy of this model
     */
    protected fun cloneModel(): Abstract3dModel {
        val model = innerCloneModel()

        model.tag = tag
        model.move = move
        model.rotate = rotate
        model.isDebug = this.isDebug
        model.isBackground = this.isBackground
        model.roundingPlane.putAll(roundingPlane)
        model.annotations.addAll(annotations)
        model.color = color

        return model
    }

    protected abstract fun innerCloneModel(): Abstract3dModel

    protected abstract val modelBoundaries: Boundaries3d

    val boundaries: Boundaries3d
        /**
         * Calculate the including cuboid for the current model. Rotation is not yet supported.
         *
         * @return the calculated boundaries
         */
        get() {
            var boundaries = this.modelBoundaries.rotate(rotate).move(move)
            for (rp in roundingPlane.values) {
                boundaries = boundaries.add(rp.getRoundingSize())
            }
            return boundaries
        }

    val halfSize: V3d
        get() {
            val boundaries3d = this.boundaries
            return V3d(
                boundaries3d.getX().getSize() / 2, boundaries3d.getY().getSize() / 2, boundaries3d.getZ().getSize() / 2
            )
        }

    val isRotated: Boolean
        /**
         * Returns true if the result will contain rotating transformation.
         *
         * @return true if the result will contain rotating transformation
         */
        get() = !rotate.isZero()

    val isMoved: Boolean
        /**
         * Returns true if this object is moved false otherwise.
         *
         * @return true if this object is moved false otherwise
         */
        get() = !move.isZero()

    /**
     *
     * Moves this model to the position relative to the given model. The position is
     * controlled by
     * the place - see [Side] - and inside parameters.
     *
     * @param place where to move this model
     * @param model the model used as a reference point
     * @param inside controls which side of the aligned model will be aligned
     * @return the new object created
     */
    @Deprecated("Use {@link #align(Side, Abstract3dModel)} instead.")
    fun align(place: Side, model: Abstract3dModel, inside: Boolean): Abstract3dModel {
        return move(place.calculateCoords(this.boundaries, model.boundaries, inside))
    }

    /**
     *
     * Moves this model to the position relative to the given model. The position is
     * controlled by
     * the place - see [Side] - parameter.
     *
     * @param place where to move this model
     * @param model the model used as a reference point
     * @return the new object created
     */
    fun align(place: Side, model: Abstract3dModel): Abstract3dModel {
        return move(place.calculateCoords(this.boundaries, model.boundaries))
    }

    fun align(place1: Side, place2: Side, model: Abstract3dModel): Abstract3dModel {
        val result = this.move(place1.calculateCoords(this.boundaries, model.boundaries))
        return result.move(place2.calculateCoords(result.boundaries, model.boundaries))
    }

    fun align(place1: Side, place2: Side, place3: Side, model: Abstract3dModel): Abstract3dModel {
        var result = this.move(place1.calculateCoords(this.boundaries, model.boundaries))
        result = result.move(place2.calculateCoords(result.boundaries, model.boundaries))
        return result.move(place3.calculateCoords(result.boundaries, model.boundaries))
    }

    /**
     *
     * Moves this model to the position relative to the annotated part of the given model.
     * The position is controlled by the place - see [Side] - parameter.
     *
     * @param innerAnnotation the annotated part of this object which will be used for the
     * positioning.
     * Can be null which case it does filter anything and the call is equivalent to
     * `align(place, model)`
     * @param place where to move this model
     * @param model the model used as a reference point
     * @return the new object created
     * @throws IllegalValueException if there are more than one pieces of this model is annotated
     * with innerAnnotation
     */
    fun align(innerAnnotation: String, place: Side, model: Abstract3dModel): Abstract3dModel {
        val annotatedModel =
            findAnnotatedModel(innerAnnotation).stream().reduce { a: Abstract3dModel, b: Abstract3dModel ->
                throw IllegalValueException(
                    "Multiple elements has been annotated with " + innerAnnotation
                )
            }.get()
        return move(place.calculateCoords(annotatedModel.boundaries, model.boundaries))
    }

    /**
     *
     * Moves this model to the position relative to the annotated part of the given model.
     * The position is controlled by the place - see [Side] - parameter.
     *
     * In case more than one pieces of the target object is annotated with externalAnnotation
     * this model
     * will be aligned to all of them and the returned object will be the union of those new
     * objects.
     *
     * @param innerAnnotation the annotated part of this object which will be used for the
     * positioning.
     * Can be null which case it does not filter on the source side.
     * @param place where to move this model
     * @param model the model used as a reference point
     * @param externalAnnotation the annotation used to filter the target object.
     * Can be null which case it does not filter on the target side.
     * @return the new object created
     * @throws IllegalValueException if there are more than one pieces of this model is annotated
     * with innerAnnotation
     * or if there are no pieces of the target model is annotated
     * with externalAnnotation
     */
    fun align(
        innerAnnotation: String, place: Side, model: Abstract3dModel, externalAnnotation: String
    ): Abstract3dModel {
        val externalAnnotatedModels = model.findAnnotatedModel(externalAnnotation)
        AssertValue.isNotEmpty<Abstract3dModel>(
            externalAnnotatedModels, "No part of the model has been annotated with $externalAnnotation"
        )
        var result: Abstract3dModel? = null
        for (m in externalAnnotatedModels) {
            val tmp = align(innerAnnotation, place, m)
            result = result?.addModel(tmp) ?: tmp
        }
        return result ?: this.addModel(model)
    }

    /**
     *
     * Moves this model to the position relative to the given coordinate. The position is
     * controlled by
     * the place - see [Side].
     *
     * @param place where to move this model
     * @param coords the coordinates used as a reference point
     * @return the new object created
     */
    fun align(place: Side, coords: V3d): Abstract3dModel {
        return move(place.calculateCoords(this.boundaries, coords))
    }

    /**
     *
     * Rounding this object with the given radius. It is possible to round only on one plane,
     * or rounding all around the object.
     *
     * Please pay attention that the rounding increases the objects size with the given radius
     * .>
     *
     * @param plane the rounding will happen on this plane - or all around if it set to ALL.
     * @param radius the radius of the rounding
     * @return the newly created object
     * @throws IllegalValueException if the given radius is negative
     */
    @Throws(IllegalValueException::class)
    fun round(plane: Plane, radius: Double): Abstract3dModel {
        AssertValue.isNotNegative(radius, "Radius of the rounding should not be negative!")

        val result = cloneModel()
        result.roundingPlane.put(plane, RoundProperties(plane, radius))
        return result
    }

    protected abstract fun toInnerNativeMesh(context: FacetGenerationContext): Long

    override fun toNativeMesh(aContext: FacetGenerationContext): Long {
        val context = aContext.applyTag(tag)
        var mesh = toInnerNativeMesh(context)

        if (!rotate.isZero()) {
            val r = Manifold3dEngine.rotate(mesh, rotate.x, rotate.y, rotate.z)
            Manifold3dEngine.delete(mesh)
            mesh = r
        }

        if (!move.isZero()) {
            val t = Manifold3dEngine.translate(mesh, move.x, move.y, move.z)
            Manifold3dEngine.delete(mesh)
            mesh = t
        }

        return mesh
    }

    fun toNativeMesh(): Long {
        return toNativeMesh(FacetGenerationContext.DEFAULT)
    }

    /**
     * Tag the model with the given value. This value can be used to color objects
     * and include / exclude them from the export.
     *
     * @param tag the value to be used
     * @return this object to make it possible to chain more method call
     */
    fun withTag(tag: Int): Abstract3dModel {
        val result = cloneModel()
        result.tag = tag
        return result
    }

    /**
     * Convenient method to create a Union.
     *
     * @param model the model to be added to this object
     * @return a new model which contains the union of this object and the given object
     */
    open fun addModel(model: Abstract3dModel?): Abstract3dModel {
        if (model == null) {
            return this
        }
        return Union(this, model).withColor(color)
    }

    fun addModels(models: List<Abstract3dModel>): Abstract3dModel {
        val result: Abstract3dModel = Union(this, models.get(0))
        for (i in 1..<models.size) {
            result.addModel(models.get(i))
        }

        return result
    }

    /**
     * Convenient method to create a Union. Adding the given model to the side of this model.
     * Calling
     * `model1.addModelTo(side, model2)` is always equivalent to
     * `model1.addModel(model2.align(side, model1, false))`.
     *
     * @param side where to move this model
     * @param model the model to be added to this object
     * @return a new model which contains the union of this object and the given object
     * @see Abstract3dModel.addModel
     */
    fun addModelTo(side: Side, model: Abstract3dModel): Abstract3dModel {
        return addModel(model.align(side, this))
    }

    /**
     * Convenient method to create a Difference.
     *
     * @param model the model to be subtracted to this object
     * @return a new model which contains the difference of this object and the given object
     */
    open fun subtractModel(model: Abstract3dModel?): Abstract3dModel {
        if (model == null) {
            return this
        }
        return Difference(this, model)
    }

    fun withColor(color: Color): Abstract3dModel {
        this.color = color
        return this
    }

    protected abstract fun innerSubModel(context: IScadGenerationContext?): Abstract3dModel?

    /**
     *
     * Copies parts of the model to a new model based on the given context. It is very useful
     * if we want
     * to use a tagged part of the model as a separate model. Lots of things can be done to the
     * new model:
     * we can render it or we can use it to align to it.
     *
     * If the given context is the
     * [ ScadGenerationContextFactory.DEFAULT][eu.printingin3d.javascad.context.ScadGenerationContextFactory.DEFAULT] then this method call is logically
     * equivalent to a [.cloneModel] method call.
     *
     * @param context the context to be used as a filter during the copy process.
     * @return a copy of the selected parts of this model
     */
    fun subModel(context: IScadGenerationContext): Abstract3dModel? {
        val currentContext = context.applyTag(tag)

        val model = innerSubModel(currentContext)
        if (model != null) {
            model.tag = tag
            model.isDebug = this.isDebug
            model.isBackground = this.isBackground

            model.move = move
            model.rotate = rotate

            model.roundingPlane.putAll(roundingPlane)
        }

        return model
    }
}
