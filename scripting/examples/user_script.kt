// Available: cube/sphere/cylinder/prism/hull/union, importStl, v3(), move/rotate/withColor
// cylinder(length, radius, fn?) | cylinder(length, bottomR, topR, fn?)
// cylinderD(d, h, fn?) | cylinderR(r, h, fn?)  — fn = number of facets (like OpenSCAD $fn)

// --- 5U+Vertical+Post (converted from OpenSCAD) ---
val nutDiameter = 8.79
val holeDiameter = 7.56 * 0.75776
val w = 20.0
val l = 222.0
val h = 6.0

// Y positions of the screw holes (offset_y + accumulated large/small steps)
val ys = doubleArrayOf(
    6.34, 22.22, 38.10, 50.77, 66.65, 82.53, 95.20, 111.08,
    126.96, 139.63, 155.51, 171.39, 184.06, 199.94, 215.82, 228.49
)

fun place(model: Model): Model {
	val lst = mutableListOf<Model>()
	return union(
		ys.map{y -> model.move(0, y, 0)}
	).move(22.8,0,0)
}

// Base body
val body = cube(w, l, h).move(10.0, 0.0, 0.0).withColor(Color.RED)

// Hex nuts (prism with 6 sides) placed at every screw position, to subtract
val nuts = repeat(ys.size) { i ->
    prism(4.0, nutDiameter / 2.0, 6).move(22.9, ys[i], 2.0).withColor(Color.YELLOW)
}

// Round holes placed at every screw position, to subtract
val holes = repeat(ys.size) { i ->
    cylinder(2.0, holeDiameter / 2.0).move(22.9, ys[i], 0.0).withColor(Color.GREEN)
}

// Imported STL placed above the body
// val post = importStl("5U+Vertical+Post.stl")

place(prism(4.0, nutDiameter / 2.0, 6)).move(0,0,4).withColor(Color.RED)
place(cylinder(2.0, holeDiameter / 2.0)).withColor(Color.YELLOW)

case()
//body.subtractModel(nuts).subtractModel(holes).addModel(post)
