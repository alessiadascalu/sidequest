package dev.sidequest.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorsConfigTest {

    @Test
    void parsesACommaSeparatedListTrimmingSpacesAndTrailingSlashes() {
        assertEquals(List.of("https://sidequest.vercel.app", "https://sidequest-*.vercel.app"),
                CorsConfig.parse(" https://sidequest.vercel.app/ ,https://sidequest-*.vercel.app,, "));
    }

    @Test
    void aSingleOriginWorksToo() {
        assertEquals(List.of("http://localhost:5173"), CorsConfig.parse("http://localhost:5173"));
    }
}
