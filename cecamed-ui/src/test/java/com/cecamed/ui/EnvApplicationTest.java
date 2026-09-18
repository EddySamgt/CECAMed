package com.cecamed.ui;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("local-env")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.flyway.enabled=${cecamed.env-check.migrate:false}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.hikari.read-only=${cecamed.env-check.read-only:true}"
})
class EnvApplicationTest {
    @Autowired ConfigurableEnvironment environment;
    @Autowired DataSource dataSource;

    @Test
    void loadsDotEnvAndConnectsToPostgres() throws Exception {
        assertThat(environment.getPropertySources().stream()
                .anyMatch(source -> source.getName().contains(".env"))).isTrue();
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT 1")) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void preservesSpanishTextInPostgresConnection() throws Exception {
        String spanish = "áéíóú ÁÉÍÓÚ ñÑ üÜ ¿Cómo está? ¡Atención!";
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT CAST(? AS text), current_setting('server_encoding'), current_setting('client_encoding')")) {
            statement.setString(1, spanish);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo(spanish);
                assertThat(result.getString(2)).isEqualTo("UTF8");
                assertThat(result.getString(3)).isEqualTo("UTF8");
            }
        }
    }
}
