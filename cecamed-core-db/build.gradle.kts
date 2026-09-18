plugins {
    `java-library`
}

tasks.test {
    useJUnitPlatform { excludeTags("postgres") }
}

tasks.register<Test>("postgresTest") {
    description = "Validates Flyway migrations and repositories against a dedicated PostgreSQL test database."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("postgres") }
    systemProperty("spring.profiles.active", "postgres-test")
    outputs.upToDateWhen { false }
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.postgresql:postgresql")
    api("org.flywaydb:flyway-core")
    api("org.flywaydb:flyway-database-postgresql")

    // For H2 in-memory testing
    testImplementation("com.h2database:h2")
}
