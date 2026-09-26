package dev.sidequest.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Înlocuiește Clock-ul aplicației cu un {@link MutableClock}. Clasele de test care îl importă cu
 * aceeași configurație împart un singur context Spring (și aceeași bază H2 în memorie).
 */
@TestConfiguration
public class TestClockConfig {

    public static final Instant START = Instant.parse("2026-06-10T09:00:00Z");

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(START);
    }
}
