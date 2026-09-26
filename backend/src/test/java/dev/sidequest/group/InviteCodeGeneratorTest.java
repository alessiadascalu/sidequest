package dev.sidequest.group;

import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteCodeGeneratorTest {

    @Test
    void codesHaveSixCharactersFromTheUnambiguousAlphabet() {
        InviteCodeGenerator generator = new InviteCodeGenerator();
        for (int i = 0; i < 1_000; i++) {
            String code = generator.next();
            assertEquals(InviteCodeGenerator.LENGTH, code.length());
            assertTrue(code.chars().allMatch(c -> InviteCodeGenerator.ALPHABET.indexOf(c) >= 0), code);
        }
    }

    @Test
    void alphabetHasNoLookalikes() {
        for (char c : "01OIL".toCharArray()) {
            assertEquals(-1, InviteCodeGenerator.ALPHABET.indexOf(c), "caracter ambiguu: " + c);
        }
    }

    @Test
    void generatorUsesTheInjectedRandomSource() {
        RandomGenerator alwaysZero = () -> 0L; // nextInt(bound) -> 0 -> primul caracter
        assertEquals("AAAAAA", new InviteCodeGenerator(alwaysZero).next());
    }

    @Test
    void normalizeIgnoresCaseSpacesAndDashes() {
        assertEquals("K7QX2M", InviteCodeGenerator.normalize(" k7q-x2m "));
        assertEquals("K7QX2M", InviteCodeGenerator.normalize("K7Q X2M"));
        assertEquals("", InviteCodeGenerator.normalize(null));
    }
}
