package dev.sidequest.group;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * Coduri de invitație de 6 caractere, ex. "K7QX2M".
 *
 * <p>Alfabetul exclude caracterele ușor de confundat când codul e dictat sau copiat de mână:
 * 0/O, 1/I/L. Rămân 31 de simboluri, deci 31^6 ≈ 887 milioane de coduri posibile.
 * SecureRandom, ca un cod să nu poată fi ghicit din codurile văzute anterior.
 */
@Component
public class InviteCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 6;

    private final RandomGenerator random;

    public InviteCodeGenerator() {
        this(new SecureRandom());
    }

    InviteCodeGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** Ce scrie utilizatorul (" k7qx-2m ") → forma salvată ("K7QX2M"). */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
