package dev.sidequest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Frontend-ul deployat (Vercel) cheamă API-ul de pe alt domeniu (Render), deci browserul cere CORS.
 * Originile permise vin din {@code sidequest.cors.allowed-origins} (variabila FRONTEND_URL).
 *
 * <p>Local, prin proxy-ul Vite, cererile sunt same-origin și CORS nu intervine deloc.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${sidequest.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = parse(allowedOrigins);
    }

    /** "https://a.app/, https://b-*.app" → ["https://a.app", "https://b-*.app"] */
    static List<String> parse(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::strip)
                .map(o -> o.endsWith("/") ? o.substring(0, o.length() - 1) : o)
                .filter(o -> !o.isEmpty())
                .toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // pattern-uri, nu doar origini exacte: permit și preview-urile Vercel (https://app-*.vercel.app)
                .allowedOriginPatterns(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    List<String> allowedOrigins() {
        return allowedOrigins;
    }
}
