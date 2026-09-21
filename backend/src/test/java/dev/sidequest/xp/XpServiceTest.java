package dev.sidequest.xp;

import dev.sidequest.domain.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpServiceTest {

    private final XpService service = new XpService(List.of(new DifficultyXpStrategy(), new StreakBonusXpStrategy()));

    @ParameterizedTest(name = "{0}, streak {1} -> {2} XP")
    @CsvSource({
            "EASY,   1, 10",
            "MEDIUM, 1, 20",
            "HARD,   1, 35",
            "EASY,   2, 12",   // +2 pentru a doua zi
            "MEDIUM, 4, 26",   // +6
            "HARD,   6, 45",   // +10
            "EASY,   11, 30",  // +20 = plafonul
            "EASY,   12, 30",  // peste plafon nu mai crește
            "HARD,   100, 55"
    })
    void totalCombinesDifficultyAndStreakBonus(Difficulty difficulty, int streak, int expected) {
        assertEquals(expected, service.calculate(new XpContext(difficulty, streak)).total());
    }

    @Test
    void firstDayHasNoStreakBonusLineInTheBreakdown() {
        XpAward award = service.calculate(new XpContext(Difficulty.MEDIUM, 1));
        assertEquals(List.of(new XpAward.Line("Dificultate", 20)), award.breakdown());
    }

    @Test
    void breakdownListsEveryContributingStrategyInOrder() {
        XpAward award = service.calculate(new XpContext(Difficulty.HARD, 3));
        assertEquals(
                List.of(new XpAward.Line("Dificultate", 35), new XpAward.Line("Bonus streak", 4)),
                award.breakdown());
        assertEquals(39, award.total());
    }

    @Test
    void streakBonusIsNeverNegativeEvenForZeroOrNegativeStreak() {
        assertEquals(0, new StreakBonusXpStrategy().xpFor(new XpContext(Difficulty.EASY, 0)));
        assertEquals(0, new StreakBonusXpStrategy().xpFor(new XpContext(Difficulty.EASY, -5)));
    }

    @Test
    void newRulesPlugInWithoutChangingTheService() {
        XpStrategy weekend = new XpStrategy() {
            @Override
            public String name() {
                return "Weekend";
            }

            @Override
            public int xpFor(XpContext context) {
                return 5;
            }
        };
        XpService extended = new XpService(List.of(new DifficultyXpStrategy(), weekend));

        XpAward award = extended.calculate(new XpContext(Difficulty.EASY, 1));
        assertEquals(15, award.total());
        assertTrue(award.breakdown().contains(new XpAward.Line("Weekend", 5)));
    }

    @Test
    void noStrategiesMeansNoXp() {
        assertEquals(0, new XpService(List.of()).calculate(new XpContext(Difficulty.HARD, 5)).total());
    }
}
