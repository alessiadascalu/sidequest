package dev.sidequest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.Random;
import java.util.random.RandomGenerator;

@Configuration
public class AppConfig {

    // UTC e doar sursa de Instant; "ce zi e azi" se decide mereu per utilizator, cu ZoneId-ul lui.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // Nu RandomGenerator.getDefault(): acesta caută "L32X64MixRandom" prin ServiceLoader în modulul
    // jdk.random, care lipsește din imaginea de runtime pe Render. java.util.Random e în java.base.
    @Bean
    public RandomGenerator questRandom() {
        return new Random();
    }
}
