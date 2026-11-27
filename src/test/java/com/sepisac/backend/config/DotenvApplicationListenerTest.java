package com.sepisac.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DotenvApplicationListener Unit Tests")
class DotenvApplicationListenerTest {

    private static ConfigurableEnvironment isolatedEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        return environment;
    }

    @Nested
    @DisplayName("resolveFlywayDbUrl")
    class ResolveFlywayDbUrlTests {

        @Test
        @DisplayName("Should return explicit FLYWAY_DB_URL unchanged when provided")
        void shouldReturnExplicitFlywayDbUrlWhenPresent() {
            String explicit = "jdbc:postgresql://custom-host:5432/postgres";
            String dbUrl = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0";

            String result = DotenvApplicationListener.resolveFlywayDbUrl(explicit, dbUrl);

            assertThat(result).isEqualTo(explicit);
        }

        @Test
        @DisplayName("Should derive port 5432 session pooler URL when explicit URL is null or blank and DB_URL uses Supabase 6543")
        void shouldDeriveSessionPoolerUrlFromSupabaseTransactionPooler() {
            String dbUrl = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0";

            assertThat(DotenvApplicationListener.resolveFlywayDbUrl(null, dbUrl))
                    .isEqualTo("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0");
            assertThat(DotenvApplicationListener.resolveFlywayDbUrl("", dbUrl))
                    .isEqualTo("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0");
            assertThat(DotenvApplicationListener.resolveFlywayDbUrl("   ", dbUrl))
                    .isEqualTo("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0");
        }

        @Test
        @DisplayName("Should return DB_URL unchanged when it already uses port 5432 or standard PostgreSQL host")
        void shouldReturnStandardPostgresUrlUnchanged() {
            String supabase5432 = "jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0";
            String localPostgres = "jdbc:postgresql://localhost:5432/sepisac";

            assertThat(DotenvApplicationListener.resolveFlywayDbUrl(null, supabase5432)).isEqualTo(supabase5432);
            assertThat(DotenvApplicationListener.resolveFlywayDbUrl("", localPostgres)).isEqualTo(localPostgres);
        }

        @Test
        @DisplayName("Should return null when both explicit URL and DB_URL are null or blank")
        void shouldReturnNullWhenBothAreNullOrBlank() {
            assertThat(DotenvApplicationListener.resolveFlywayDbUrl(null, null)).isNull();
            assertThat(DotenvApplicationListener.resolveFlywayDbUrl("  ", "  ")).isNull();
        }
    }

    @Nested
    @DisplayName("applyProperties")
    class ApplyPropertiesTests {

        @Test
        @DisplayName("Should ignore blank FLYWAY_* dotenv entries and derive FLYWAY_DB_URL from DB_URL")
        void shouldIgnoreBlankFlywayEntriesAndDeriveUrl() {
            ConfigurableEnvironment environment = isolatedEnvironment();
            Map<String, String> dotenvEntries = new HashMap<>();
            dotenvEntries.put("DB_URL", "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0");
            dotenvEntries.put("DB_USER", "postgres.tenant");
            dotenvEntries.put("DB_PASSWORD", "secret");
            dotenvEntries.put("FLYWAY_DB_URL", "   ");
            dotenvEntries.put("FLYWAY_DB_USER", "");
            dotenvEntries.put("FLYWAY_DB_PASSWORD", "");

            DotenvApplicationListener.applyProperties(environment, dotenvEntries, false);

            assertThat(environment.getProperty("FLYWAY_DB_URL"))
                    .isEqualTo("jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0");
            assertThat(environment.getProperty("FLYWAY_DB_USER")).isNull();
            assertThat(environment.getProperty("FLYWAY_DB_PASSWORD")).isNull();
            assertThat(environment.resolvePlaceholders("${FLYWAY_DB_USER:${DB_USER}}")).isEqualTo("postgres.tenant");
            assertThat(environment.resolvePlaceholders("${FLYWAY_DB_PASSWORD:${DB_PASSWORD}}")).isEqualTo("secret");
        }

        @Test
        @DisplayName("Should override blank FLYWAY_* environment properties with DB_* fallbacks")
        void shouldOverrideBlankFlywayEnvironmentProperties() {
            ConfigurableEnvironment environment = isolatedEnvironment();
            Map<String, Object> existingEnv = new HashMap<>();
            existingEnv.put("DB_URL", "jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:6543/postgres?prepareThreshold=0");
            existingEnv.put("DB_USER", "postgres.tenant");
            existingEnv.put("DB_PASSWORD", "secret");
            existingEnv.put("FLYWAY_DB_URL", "");
            existingEnv.put("FLYWAY_DB_USER", "");
            existingEnv.put("FLYWAY_DB_PASSWORD", "");
            environment.getPropertySources().addLast(new MapPropertySource("mockSystemEnv", existingEnv));

            DotenvApplicationListener.applyProperties(environment, Map.<String, String>of(), false);

            assertThat(environment.getProperty("FLYWAY_DB_URL"))
                    .isEqualTo("jdbc:postgresql://aws-0-sa-east-1.pooler.supabase.com:5432/postgres?prepareThreshold=0");
            assertThat(environment.getProperty("FLYWAY_DB_USER")).isEqualTo("postgres.tenant");
            assertThat(environment.getProperty("FLYWAY_DB_PASSWORD")).isEqualTo("secret");
        }
    }
}
