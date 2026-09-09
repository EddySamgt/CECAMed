plugins {
    application
    id("org.springframework.boot")
}

application {
    mainClass.set("com.cecamed.ui.CecamedApplication")
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

    runtimeOnly("com.h2database:h2")
}
