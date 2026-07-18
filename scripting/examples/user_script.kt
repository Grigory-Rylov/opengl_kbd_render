// DSL script — F5 to run
// Available: bindings.cube/shell/cylinder/sphere, v3(), union/minus, color()

bindings.hull(
	bindings.cube(50.0),
	bindings.sphere(10.0).move(70.0, 0.0, 0.0)
).withColor(Color.RED)