package dev.sidequest.streak;

import dev.sidequest.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreakCalculatorTest {

    static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-06-10T09:00:00Z"));
    private final StreakCalculator calculator = new StreakCalculator(clock);

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    private static List<LocalDate> dates(String... iso) {
        return java.util.Arrays.stream(iso).map(LocalDate::parse).toList();
    }

    private StreakResult at(String localDateTime, ZoneId zone, List<LocalDate> completed) {
        clock.setLocal(localDateTime, zone);
        return calculator.calculate(completed, zone);
    }

    private static void assertStreak(int current, int longest, boolean completedToday, StreakResult actual) {
        assertEquals(new StreakResult(current, longest, completedToday), actual);
    }

    @Nested
    @DisplayName("Reguli de bază")
    class Basics {

        @Test
        void noHistoryMeansNoStreak() {
            assertStreak(0, 0, false, at("2026-06-10T12:00", BUCHAREST, List.of()));
        }

        @Test
        void completingOnlyTodayGivesStreakOfOne() {
            assertStreak(1, 1, true, at("2026-06-10T12:00", BUCHAREST, dates("2026-06-10")));
        }

        @Test
        void consecutiveDaysEndingTodayCountAll() {
            assertStreak(4, 4, true,
                    at("2026-06-10T12:00", BUCHAREST, dates("2026-06-07", "2026-06-08", "2026-06-09", "2026-06-10")));
        }

        @Test
        void streakStaysAliveWhenTodayIsNotDoneYetButYesterdayWas() {
            // Mai ai timp până la miezul nopții: nu se pierde streak-ul înainte să se termine ziua.
            assertStreak(2, 2, false,
                    at("2026-06-10T08:00", BUCHAREST, dates("2026-06-08", "2026-06-09")));
        }

        @Test
        void inputOrderAndDuplicatesDoNotMatter() {
            assertStreak(3, 3, true,
                    at("2026-06-10T12:00", BUCHAREST,
                            dates("2026-06-10", "2026-06-08", "2026-06-09", "2026-06-09", "2026-06-10")));
        }

        @Test
        void datesInTheFutureAreIgnored() {
            // ex. utilizatorul și-a mutat ceasul înapoi sau a schimbat fusul orar
            assertStreak(1, 1, true,
                    at("2026-06-10T12:00", BUCHAREST, dates("2026-06-10", "2026-06-11", "2026-06-12")));
            assertStreak(0, 0, false,
                    at("2026-06-10T12:00", BUCHAREST, dates("2026-06-11", "2026-06-12")));
        }
    }

    @Nested
    @DisplayName("Zi ratată")
    class MissedDay {

        @Test
        void aFullyMissedDayResetsCurrentStreak() {
            // A completat 7, 8; a ratat 9; azi e 10 și încă n-a completat.
            assertStreak(0, 2, false,
                    at("2026-06-10T12:00", BUCHAREST, dates("2026-06-07", "2026-06-08")));
        }

        @Test
        void completingAfterAMissedDayStartsOverFromOne() {
            // 7, 8 completate; 9 ratat; 10 completat -> streak nou de 1, recordul rămâne 2.
            assertStreak(1, 2, true,
                    at("2026-06-10T12:00", BUCHAREST, dates("2026-06-07", "2026-06-08", "2026-06-10")));
        }

        @Test
        void aSingleGapSplitsHistoryIntoSeparateRuns() {
            List<LocalDate> history = dates(
                    "2026-05-01", "2026-05-02", "2026-05-03", "2026-05-04", "2026-05-05", // 5 zile
                    "2026-06-09", "2026-06-10");                                          // 2 zile
            assertStreak(2, 5, true, at("2026-06-10T18:00", BUCHAREST, history));
        }

        @Test
        void longestStreakSurvivesEvenWhenCurrentIsZero() {
            assertStreak(0, 3, false,
                    at("2026-06-20T10:00", BUCHAREST, dates("2026-06-01", "2026-06-02", "2026-06-03")));
        }

        @Test
        void twoDaysAgoIsAlreadyTooLate() {
            assertStreak(0, 1, false, at("2026-06-10T12:00", BUCHAREST, dates("2026-06-08")));
        }
    }

    @Nested
    @DisplayName("Completare la 23:59 vs 00:01")
    class MidnightBoundary {

        @Test
        void oneMinuteBeforeMidnightIsStillTheSameDay() {
            // Ieri completat; azi e 10, ora 23:59 -> streak-ul încă în viață, ziua nu s-a încheiat.
            StreakResult r = at("2026-06-10T23:59", BUCHAREST, dates("2026-06-09"));
            assertStreak(1, 1, false, r);
            assertEquals(d("2026-06-10"), calculator.today(BUCHAREST));
        }

        @Test
        void completingAt2359CountsForThatDay() {
            StreakResult r = at("2026-06-10T23:59", BUCHAREST, dates("2026-06-09", "2026-06-10"));
            assertStreak(2, 2, true, r);
        }

        @Test
        void twoMinutesLaterItIsANewDayAndTodayIsNotCompleted() {
            List<LocalDate> completed = dates("2026-06-09", "2026-06-10"); // a completat la 23:59
            StreakResult r = at("2026-06-11T00:01", BUCHAREST, completed);
            assertStreak(2, 2, false, r); // streak-ul rămâne, dar azi e o zi nouă, necompletată
            assertEquals(d("2026-06-11"), calculator.today(BUCHAREST));
        }

        @Test
        void completingAt0001OnTheNextDayExtendsTheStreak() {
            List<LocalDate> completed = dates("2026-06-09", "2026-06-10", "2026-06-11");
            assertStreak(3, 3, true, at("2026-06-11T00:01", BUCHAREST, completed));
        }

        @Test
        void missingTheWholeNextDayBreaksItExactlyAtMidnight() {
            List<LocalDate> completed = dates("2026-06-09");
            // 10 iunie 23:59 -> încă poate salva streak-ul
            assertStreak(1, 1, false, at("2026-06-10T23:59", BUCHAREST, completed));
            // 11 iunie 00:01 -> ziua 10 a trecut fără completare
            assertStreak(0, 1, false, at("2026-06-11T00:01", BUCHAREST, completed));
        }

        @Test
        void theBoundaryIsExactlyLocalMidnight() {
            clock.setLocal("2026-06-10T23:59:59.999999999", BUCHAREST);
            assertEquals(d("2026-06-10"), calculator.today(BUCHAREST));
            clock.advance(Duration.ofNanos(1));
            assertEquals(d("2026-06-11"), calculator.today(BUCHAREST));
        }

        @Test
        void completionsMinutesApartAcrossMidnightAreTwoDifferentDays() {
            // 23:59 și 00:01 sunt la doar 2 minute distanță, dar în zile diferite -> streak 2, nu 1.
            assertStreak(2, 2, true,
                    at("2026-06-11T00:01", BUCHAREST, dates("2026-06-10", "2026-06-11")));
        }
    }

    @Nested
    @DisplayName("Fusuri orare diferite")
    class TimeZones {

        @Test
        void sameInstantIsADifferentLocalDateInDifferentZones() {
            clock.set(Instant.parse("2026-06-10T20:00:00Z"));
            assertEquals(d("2026-06-10"), calculator.today(LOS_ANGELES)); // 13:00
            assertEquals(d("2026-06-10"), calculator.today(ZoneId.of("UTC")));
            assertEquals(d("2026-06-11"), calculator.today(TOKYO));       // 05:00 a doua zi
        }

        @Test
        void sameInstantAndHistoryGiveDifferentStreaksPerZone() {
            List<LocalDate> completed = dates("2026-06-09");
            clock.set(Instant.parse("2026-06-10T20:00:00Z"));

            // Los Angeles: azi = 10, ieri = 9 completat -> streak-ul e în viață
            assertStreak(1, 1, false, calculator.calculate(completed, LOS_ANGELES));
            // Tokyo: azi = 11, ieri = 10 necompletat -> streak-ul s-a pierdut
            assertStreak(0, 1, false, calculator.calculate(completed, TOKYO));
        }

        @Test
        void utcDateWouldGiveTheWrongAnswerForAUserWestOfGreenwich() {
            // 11 iunie 02:30 UTC = 10 iunie 19:30 la Los Angeles. Data UTC ar zice "11", dar pentru
            // utilizator e încă 10 iunie și a completat deja azi.
            clock.set(Instant.parse("2026-06-11T02:30:00Z"));
            assertEquals(d("2026-06-10"), calculator.today(LOS_ANGELES));
            assertStreak(2, 2, true,
                    calculator.calculate(dates("2026-06-09", "2026-06-10"), LOS_ANGELES));
        }

        @Test
        void utcDateWouldGiveTheWrongAnswerForAUserEastOfGreenwich() {
            // 10 iunie 22:30 UTC = 11 iunie 07:30 la Tokyo; ziua 10 a trecut deja pentru el.
            clock.set(Instant.parse("2026-06-10T22:30:00Z"));
            assertEquals(d("2026-06-11"), calculator.today(TOKYO));
            assertStreak(0, 1, false, calculator.calculate(dates("2026-06-09"), TOKYO));
        }

        @ParameterizedTest(name = "{0} la {1}Z -> azi = {2}")
        @CsvSource({
                "Europe/Bucharest,     2026-06-10T20:59:59Z, 2026-06-10",  // 23:59:59 (UTC+3)
                "Europe/Bucharest,     2026-06-10T21:00:00Z, 2026-06-11",  // 00:00:00
                "Asia/Kolkata,         2026-06-10T18:29:59Z, 2026-06-10",  // UTC+5:30, 23:59:59
                "Asia/Kolkata,         2026-06-10T18:30:00Z, 2026-06-11",
                "Pacific/Kiritimati,   2026-06-10T09:59:59Z, 2026-06-10",  // UTC+14, cel mai "devreme": 23:59:59
                "Pacific/Kiritimati,   2026-06-10T10:00:00Z, 2026-06-11",
                "Pacific/Pago_Pago,    2026-06-10T10:59:59Z, 2026-06-09",  // UTC-11, cel mai "târziu"
                "Pacific/Pago_Pago,    2026-06-10T11:00:00Z, 2026-06-10",
                "America/Los_Angeles,  2026-06-10T06:59:59Z, 2026-06-09",  // UTC-7
                "America/Los_Angeles,  2026-06-10T07:00:00Z, 2026-06-10"
        })
        void todayFollowsTheLocalMidnightOfEachZone(String zone, String instant, String expectedToday) {
            clock.set(Instant.parse(instant));
            assertEquals(LocalDate.parse(expectedToday), calculator.today(ZoneId.of(zone)));
        }
    }

    @Nested
    @DisplayName("DST")
    class DaylightSaving {

        @Test
        void premiseSpringForwardDayIs23HoursAndFallBackDayIs25() {
            assertEquals(Duration.ofHours(23), dayLength(BUCHAREST, d("2026-03-29")));
            assertEquals(Duration.ofHours(25), dayLength(BUCHAREST, d("2026-10-25")));
            assertEquals(Duration.ofHours(23), dayLength(NEW_YORK, d("2026-03-08")));
            assertEquals(Duration.ofHours(25), dayLength(NEW_YORK, d("2026-11-01")));
        }

        private Duration dayLength(ZoneId zone, LocalDate day) {
            return Duration.between(day.atStartOfDay(zone), day.plusDays(1).atStartOfDay(zone));
        }

        @Test
        void streakIsNotBrokenByTheSpringForwardShortDay() {
            List<LocalDate> completed = dates("2026-03-27", "2026-03-28", "2026-03-29", "2026-03-30");
            assertStreak(4, 4, true, at("2026-03-30T09:00", BUCHAREST, completed));
        }

        @Test
        void streakIsNotBrokenByTheFallBackLongDay() {
            List<LocalDate> completed = dates("2026-10-24", "2026-10-25", "2026-10-26");
            assertStreak(3, 3, true, at("2026-10-26T09:00", BUCHAREST, completed));
        }

        @Test
        void twoCompletionsMoreThan24hApartCanStillBeConsecutiveDays() {
            // 25 oct 00:30 (EEST) și 26 oct 00:30 (EET) sunt la 25h distanță — dar sunt zile consecutive.
            // O logică naivă de tip "au trecut mai mult de 24h => streak rupt" ar greși aici.
            ZonedDateTime first = ZonedDateTime.of(2026, 10, 25, 0, 30, 0, 0, BUCHAREST);
            ZonedDateTime second = ZonedDateTime.of(2026, 10, 26, 0, 30, 0, 0, BUCHAREST);
            assertEquals(Duration.ofHours(25), Duration.between(first, second));

            assertStreak(2, 2, true,
                    at("2026-10-26T00:30", BUCHAREST, dates("2026-10-25", "2026-10-26")));
        }

        @Test
        void twoCompletionsLessThan24hApartCanBeConsecutiveDaysToo() {
            // 29 mar 00:30 (EET) și 30 mar 00:30 (EEST) sunt la doar 23h distanță, dar zile consecutive.
            // O logică naivă de tip "trebuie să treacă 24h pentru ziua următoare" ar greși aici.
            ZonedDateTime first = ZonedDateTime.of(2026, 3, 29, 0, 30, 0, 0, BUCHAREST);
            ZonedDateTime second = ZonedDateTime.of(2026, 3, 30, 0, 30, 0, 0, BUCHAREST);
            assertEquals(Duration.ofHours(23), Duration.between(first, second));

            assertStreak(2, 2, true,
                    at("2026-03-30T00:30", BUCHAREST, dates("2026-03-29", "2026-03-30")));
        }

        @Test
        void theSpringForwardJumpStaysWithinTheSameCalendarDay() {
            // 29 mar 2026 în București ceasul sare de la 03:00 la 04:00 (ora 03:xx nu există).
            clock.set(Instant.parse("2026-03-29T00:59:59Z")); // 02:59:59 EET, ultima secundă dinainte de salt
            assertEquals(d("2026-03-29"), calculator.today(BUCHAREST));
            clock.set(Instant.parse("2026-03-29T01:00:00Z")); // 04:00:00 EEST, prima secundă după salt
            assertEquals(d("2026-03-29"), calculator.today(BUCHAREST));
        }

        @Test
        void repeatedHourOnFallBackIsTheSameCalendarDay() {
            // 25 oct 2026: 04:00 EEST -> 03:00 EET, deci 03:30 apare de două ori.
            clock.set(Instant.parse("2026-10-25T00:30:00Z")); // 03:30 EEST
            LocalDate first = calculator.today(BUCHAREST);
            clock.set(Instant.parse("2026-10-25T01:30:00Z")); // 03:30 EET
            LocalDate second = calculator.today(BUCHAREST);
            assertEquals(d("2026-10-25"), first);
            assertEquals(first, second);
        }

        @Test
        void midnightBoundaryStillHoldsOnTheDstDays() {
            // Ziua lungă (25 oct): ultimul moment e 23:59:59 EET = 21:59:59Z
            clock.set(Instant.parse("2026-10-25T21:59:59Z"));
            assertEquals(d("2026-10-25"), calculator.today(BUCHAREST));
            clock.set(Instant.parse("2026-10-25T22:00:00Z"));
            assertEquals(d("2026-10-26"), calculator.today(BUCHAREST));

            // Ziua scurtă (29 mar): ultimul moment e 23:59:59 EEST = 20:59:59Z
            clock.set(Instant.parse("2026-03-29T20:59:59Z"));
            assertEquals(d("2026-03-29"), calculator.today(BUCHAREST));
            clock.set(Instant.parse("2026-03-29T21:00:00Z"));
            assertEquals(d("2026-03-30"), calculator.today(BUCHAREST));
        }

        @Test
        void missingADayAroundDstStillBreaksTheStreak() {
            // DST nu trebuie să "ierte" o zi ratată: 28 mar completat, 29 ratat, 30 mar -> streak 0.
            assertStreak(0, 1, false, at("2026-03-30T09:00", BUCHAREST, dates("2026-03-28")));
        }

        @Test
        void usDstTransitionsBehaveTheSame() {
            assertStreak(3, 3, true,
                    at("2026-03-09T09:00", NEW_YORK, dates("2026-03-07", "2026-03-08", "2026-03-09")));
            assertStreak(3, 3, true,
                    at("2026-11-02T09:00", NEW_YORK, dates("2026-10-31", "2026-11-01", "2026-11-02")));
        }

        @Test
        void sameInstantOnTheDstDayIsADifferentDateInBucharestAndNewYork() {
            clock.set(Instant.parse("2026-03-29T01:30:00Z"));
            assertEquals(d("2026-03-29"), calculator.today(BUCHAREST)); // 04:30 EEST
            assertEquals(d("2026-03-28"), calculator.today(NEW_YORK));  // 21:30 EDT
        }
    }

    @Nested
    @DisplayName("Longest streak și granițe de calendar")
    class Calendar {

        @Test
        void streakCrossesMonthAndYearBoundaries() {
            List<LocalDate> completed = dates("2025-12-30", "2025-12-31", "2026-01-01", "2026-01-02");
            assertStreak(4, 4, true, at("2026-01-02T10:00", BUCHAREST, completed));
        }

        @Test
        void streakCountsLeapDay() {
            List<LocalDate> completed = dates("2028-02-28", "2028-02-29", "2028-03-01");
            assertStreak(3, 3, true, at("2028-03-01T10:00", BUCHAREST, completed));
        }

        @Test
        void longestStreakPicksTheBestRunNotTheMostRecent() {
            List<LocalDate> completed = dates(
                    "2026-01-01", "2026-01-02", "2026-01-03", "2026-01-04",  // 4
                    "2026-02-10", "2026-02-11",                              // 2
                    "2026-06-10");                                           // 1 (azi)
            assertStreak(1, 4, true, at("2026-06-10T10:00", BUCHAREST, completed));
        }

        @Test
        void longestStreakCanBeTheCurrentOne() {
            List<LocalDate> completed = dates(
                    "2026-05-01", "2026-05-02",
                    "2026-06-05", "2026-06-06", "2026-06-07", "2026-06-08", "2026-06-09");
            assertStreak(5, 5, false, at("2026-06-10T07:00", BUCHAREST, completed));
        }
    }

    @Test
    void calculateWithExplicitTodayDoesNotTouchTheClock() {
        clock.set(Instant.parse("2000-01-01T00:00:00Z"));
        StreakResult r = calculator.calculate(dates("2026-06-09", "2026-06-10"), d("2026-06-10"));
        assertTrue(r.completedToday());
        assertEquals(2, r.current());
        assertFalse(calculator.calculate(dates("2026-06-09"), d("2026-06-11")).completedToday());
    }
}
