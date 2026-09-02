plugins {
    `java-library`
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api(project(":cecamed-core-db"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
