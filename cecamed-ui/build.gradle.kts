plugins {
    application
    id("org.springframework.boot")
}

application {
    mainClass.set("com.cecamed.ui.CecamedApplication")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform { excludeTags("postgres", "local-env", "javafx") }
}

tasks.register<Test>("uiTest") {
    description = "Loads FXML views and shows the login window using JavaFX (requires a display)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("javafx") }
    outputs.upToDateWhen { false }
}

tasks.named<JavaExec>("bootRun") {
    workingDir = rootProject.projectDir
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.register<Test>("envCheck") {
    description = "Verifies application startup and PostgreSQL connectivity using the root .env file."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    workingDir = rootProject.projectDir
    useJUnitPlatform { includeTags("local-env") }
    val applyMigrations = providers.gradleProperty("applyMigrations").orElse("false").get().toBoolean()
    systemProperty("cecamed.env-check.migrate", applyMigrations)
    systemProperty("cecamed.env-check.read-only", !applyMigrations)
    outputs.upToDateWhen { false }
}

tasks.register<Test>("postgresTest") {
    description = "Starts the application context against a dedicated PostgreSQL test database."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("postgres") }
    outputs.upToDateWhen { false }
}

val javafxVersion = "21.0.4"
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch")
val platform = when {
    osName.contains("linux") -> "linux"
    osName.contains("mac") || osName.contains("darwin") -> if (osArch == "aarch64") "mac-aarch64" else "mac"
    osName.contains("windows") -> "win"
    else -> "linux"
}

dependencies {
    implementation(project(":cecamed-services"))
    implementation(project(":cecamed-calendar-integration"))
    implementation(project(":cecamed-core-db"))

    implementation("org.springframework.boot:spring-boot-starter")

    // JavaFX 21
    implementation("org.openjfx:javafx-base:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$platform")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:$platform")

    implementation("org.openjfx:javafx-base:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion")
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-fxml:$javafxVersion")

    // AtlantaFX
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")

    // ControlsFX
    implementation("org.controlsfx:controlsfx:11.2.1")

    // Ikonli
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-material2-pack:12.3.1")

}
