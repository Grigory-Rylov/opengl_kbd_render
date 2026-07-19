// Пример DSL-скрипта для 3D-моделирования
// Компилируется и выполняется на лету через ScriptEvaluator

val base = bindings.cube(100.0, 60.0, 15.0)

val hole = bindings.cylinder(20.0, 3.0).move(0.0, 0.0, 0.0)

val leg1 = bindings.cylinder(8.0, 4.0).moveZ(-8.0)
val leg2 = leg1.moveX(85.0).moveY(46.0)
val leg3 = leg1.moveX(-85.0).moveY(46.0)
val leg4 = leg1.moveX(85.0).moveY(-46.0)
val leg5 = leg1.moveX(-85.0).moveY(-46.0)

base
    .subtractModel(hole)
    .addModel(leg1)
    .addModel(leg2)
    .addModel(leg3)
    .addModel(leg4)
    .addModel(leg5)
