// DSL script — F5 to run
// Available: bindings.cube/shell/cylinder/sphere, v3(), union/minus, color()

hull(
	cube(50.0),
	sphere(10.0).move(70.0, 0.0, 0.0)
).withColor(Color.RED)


sphere(10.0).move(0.0, 80.0, 0.0)