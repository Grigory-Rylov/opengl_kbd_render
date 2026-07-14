plugins {
    id("java")
    kotlin("jvm")
}

tasks.register<JavaExec>("runTestModel") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("eu.printingin3d.javascad.manifold.TestModel")
}

group = "eu.printingin3d.javascad"
version = "1.0"

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.madhephaestus:manifold3d:3.2.13")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17) // Устанавливаем единую версию Java для всех задач
}
