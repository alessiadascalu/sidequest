package dev.sidequest.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void parsesARenderInternalUrlWithoutPort() {
        // Formatul "Internal Database URL" din Render: fără port
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.parse(
                "postgresql://sidequest:s3cr3t@dpg-abc123-a/sidequest");
        assertEquals("dpg-abc123-a", p.get("DB_HOST"));
        assertEquals("5432", p.get("DB_PORT"));
        assertEquals("sidequest", p.get("DB_NAME"));
        assertEquals("sidequest", p.get("DB_USER"));
        assertEquals("s3cr3t", p.get("DB_PASSWORD"));
        assertNull(p.get("DB_QUERY"));
    }

    @Test
    void parsesAnExternalUrlWithPortQueryAndEncodedPassword() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.parse(
                "postgres://u:p%40ss%3Aw0rd@dpg-abc.frankfurt-postgres.render.com:6543/db_x?sslmode=require");
        assertEquals("dpg-abc.frankfurt-postgres.render.com", p.get("DB_HOST"));
        assertEquals("6543", p.get("DB_PORT"));
        assertEquals("db_x", p.get("DB_NAME"));
        assertEquals("p@ss:w0rd", p.get("DB_PASSWORD"));
        assertEquals("?sslmode=require", p.get("DB_QUERY"));
    }

    @Test
    void rejectsNonPostgresOrIncompleteUrls() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseUrlEnvironmentPostProcessor.parse("mysql://u:p@host/db"));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseUrlEnvironmentPostProcessor.parse("postgresql://u:p@host/"));
    }

    @Test
    void fillsDbVariablesFromDatabaseUrl() {
        MockEnvironment env = new MockEnvironment().withProperty("DATABASE_URL", "postgresql://a:b@h:5433/d");
        processor.postProcessEnvironment(env, null);
        assertEquals("h", env.getProperty("DB_HOST"));
        assertEquals("5433", env.getProperty("DB_PORT"));
        assertEquals("d", env.getProperty("DB_NAME"));
    }

    @Test
    void explicitDbVariablesWinOverDatabaseUrl() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://a:b@from-url/d")
                .withProperty("DB_HOST", "explicit-host");
        processor.postProcessEnvironment(env, null);
        assertEquals("explicit-host", env.getProperty("DB_HOST"));
        assertFalse(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE_NAME));
    }

    @Test
    void doesNothingWithoutDatabaseUrl() {
        MockEnvironment env = new MockEnvironment();
        processor.postProcessEnvironment(env, null);
        assertNull(env.getProperty("DB_HOST"));
    }
}
