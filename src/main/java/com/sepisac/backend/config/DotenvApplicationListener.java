package com.sepisac.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DotenvApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String SUPABASE_TRANSACTION_POOLER_SEGMENT = "pooler.supabase.com:6543/";
    private static final String SUPABASE_SESSION_POOLER_SEGMENT = "pooler.supabase.com:5432/";
    private static final Set<String> FLYWAY_OPTIONAL_KEYS = Set.of(
            "FLYWAY_DB_URL",
            "FLYWAY_DB_USER",
            "FLYWAY_DB_PASSWORD"
    );

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Map<String, String> entries = new LinkedHashMap<>();
        dotenv.entries().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));

        applyProperties(event.getEnvironment(), entries, true);
    }

    static void applyProperties(
            ConfigurableEnvironment environment,
            Map<String, String> dotenvEntries,
            boolean syncSystemProperties) {
        Map<String, Object> envMap = new HashMap<>();

        if (dotenvEntries != null) {
            dotenvEntries.forEach((key, value) -> {
                if (FLYWAY_OPTIONAL_KEYS.contains(key) && (value == null || value.isBlank())) {
                    return;
                }
                envMap.put(key, value);
                if (syncSystemProperties && System.getProperty(key) == null && value != null) {
                    System.setProperty(key, value);
                }
            });
        }

        String explicitFlywayUrl = firstNonBlank(
                asString(envMap.get("FLYWAY_DB_URL")),
                environment.getProperty("FLYWAY_DB_URL")
        );
        String dbUrl = firstNonBlank(
                asString(envMap.get("DB_URL")),
                environment.getProperty("DB_URL")
        );
        String resolvedFlywayUrl = resolveFlywayDbUrl(explicitFlywayUrl, dbUrl);

        if (resolvedFlywayUrl != null) {
            envMap.put("FLYWAY_DB_URL", resolvedFlywayUrl);
            if (syncSystemProperties) {
                String currentSystemFlywayUrl = System.getProperty("FLYWAY_DB_URL");
                if (currentSystemFlywayUrl == null || currentSystemFlywayUrl.isBlank()) {
                    System.setProperty("FLYWAY_DB_URL", resolvedFlywayUrl);
                }
            }
        }

        for (String suffix : List.of("USER", "PASSWORD")) {
            String flywayKey = "FLYWAY_DB_" + suffix;
            String dbKey = "DB_" + suffix;
            String existingEnvFlywayValue = environment.getProperty(flywayKey);
            if (!envMap.containsKey(flywayKey) && existingEnvFlywayValue != null && existingEnvFlywayValue.isBlank()) {
                String fallbackDbValue = firstNonBlank(
                        asString(envMap.get(dbKey)),
                        environment.getProperty(dbKey)
                );
                if (fallbackDbValue != null) {
                    envMap.put(flywayKey, fallbackDbValue);
                }
            }
        }

        if (!envMap.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envMap));
        }
    }

    static String resolveFlywayDbUrl(String explicitFlywayDbUrl, String dbUrl) {
        if (explicitFlywayDbUrl != null && !explicitFlywayDbUrl.isBlank()) {
            return explicitFlywayDbUrl.trim();
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            return null;
        }
        String trimmedDbUrl = dbUrl.trim();
        if (trimmedDbUrl.contains(SUPABASE_TRANSACTION_POOLER_SEGMENT)) {
            return trimmedDbUrl.replace(SUPABASE_TRANSACTION_POOLER_SEGMENT, SUPABASE_SESSION_POOLER_SEGMENT);
        }
        return trimmedDbUrl;
    }

    private static String firstNonBlank(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary.trim();
        }
        return null;
    }

    private static String asString(Object value) {
        return value instanceof String str ? str : null;
    }
}
