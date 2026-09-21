package dev.sidequest.xp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelServiceTest {

    private final LevelService levels = new LevelService();

    @ParameterizedTest(name = "{0} XP -> nivel {1}")
    @CsvSource({
            "0,    1",
            "99,   1",
            "100,  2",
            "299,  2",
            "300,  3",
            "599,  3",
            "600,  4",
            "999,  4",
            "1000, 5",
            "2100, 7",
            "2800, 8",
            "50000, 32"
    })
    void levelBoundaries(int xp, int expectedLevel) {
        assertEquals(expectedLevel, levels.forXp(xp).level());
    }

    @Test
    void progressWithinALevel() {
        LevelInfo info = levels.forXp(150); // nivel 2 începe la 100, are lățime 200
        assertEquals(2, info.level());
        assertEquals(50, info.xpIntoLevel());
        assertEquals(200, info.xpForNextLevel());
        assertEquals(0.25, info.progress(), 1e-9);
    }

    @Test
    void freshUserStartsAtLevelOneWithEmptyBar() {
        LevelInfo info = levels.forXp(0);
        assertEquals(1, info.level());
        assertEquals("Cartof de Canapea", info.title());
        assertEquals(0, info.xpIntoLevel());
        assertEquals(100, info.xpForNextLevel());
        assertEquals(0.0, info.progress());
    }

    @Test
    void justBelowTheNextLevelTheBarIsAlmostFullButNeverOne() {
        LevelInfo info = levels.forXp(99);
        assertEquals(1, info.level());
        assertTrue(info.progress() < 1.0);
        assertTrue(info.progress() > 0.98);
    }

    @Test
    void everyLevelHasATitleAndTheLastOneRepeatsForeverAfterwards() {
        assertEquals("Ucenic Curios", levels.titleFor(2));
        assertEquals("Legendă Locală", levels.titleFor(7));
        assertEquals("Boss Final", levels.titleFor(8));
        assertEquals("Boss Final", levels.titleFor(9));
        assertEquals("Boss Final", levels.forXp(50_000).title());
    }

    @Test
    void levelStartsAreStrictlyIncreasing() {
        for (int level = 1; level < 50; level++) {
            assertTrue(levels.xpRequiredForLevel(level + 1) > levels.xpRequiredForLevel(level));
        }
        assertEquals(0, levels.xpRequiredForLevel(1));
    }

    @Test
    void negativeXpIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> levels.forXp(-1));
    }
}
