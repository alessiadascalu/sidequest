package dev.sidequest.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Render (și Heroku etc.) dau conexiunea ca {@code DATABASE_URL=postgresql://user:parola@host:port/db},
 * format pe care JDBC nu îl acceptă (fără prefixul "jdbc:", cu credențialele în URL).
 *
 * <p>Dacă {@code DATABASE_URL} există și {@code DB_HOST} nu, îl desfacem în DB_HOST, DB_PORT, DB_NAME,
 * DB_USER, DB_PASSWORD (+ DB_QUERY, ex. "?sslmode=require"), pe care application-prod.properties le
 * folosește ca să construiască spring.datasource.*. Sursa e adăugată cu prioritate minimă, deci orice
 * variabilă setată explicit câștigă.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "databaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || environment.containsProperty("DB_HOST")) {
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, parse(databaseUrl)));
    }

    static Map<String, Object> parse(String databaseUrl) {
        URI uri = URI.create(databaseUrl.strip());
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException(
                    "DATABASE_URL trebuie să înceapă cu postgres:// sau postgresql://, nu cu " + scheme + "://");
        }
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() <= 1) {
            throw new IllegalArgumentException("DATABASE_URL trebuie să conțină host-ul și numele bazei de date");
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("DB_HOST", uri.getHost());
        props.put("DB_PORT", String.valueOf(uri.getPort() == -1 ? 5432 : uri.getPort()));
        props.put("DB_NAME", uri.getPath().substring(1));
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            props.put("DB_USER", decode(colon < 0 ? userInfo : userInfo.substring(0, colon)));
            if (colon >= 0) {
                props.put("DB_PASSWORD", decode(userInfo.substring(colon + 1)));
            }
        }
        if (uri.getRawQuery() != null) {
            props.put("DB_QUERY", "?" + uri.getRawQuery());
        }
        return props;
    }

    // Parolele generate pot conține caractere codate în URL (ex. %40 pentru @).
    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
