package dev.sidequest.streak;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * Calculează streak-ul din zilele LOCALE (LocalDate) în care utilizatorul a completat quest-ul.
 *
 * <p>Lucrăm doar cu date calendaristice, nu cu diferențe de 24h între Instant-uri: o zi cu DST are
 * 23 sau 25 de ore, dar "ziua următoare" rămâne mereu {@code date.plusDays(1)}.
 * "Azi" vine din {@link Clock}-ul injectat, văzut în fusul orar al utilizatorului, deci testele
 * pot fixa timpul exact (23:59 vs 00:01, DST etc.).
 */
@Component
public class StreakCalculator {

    private final Clock clock;

    public StreakCalculator(Clock clock) {
        this.clock = clock;
    }

    /** Data calendaristică de acum în fusul dat (nu data UTC). */
    public LocalDate today(ZoneId zone) {
        return LocalDate.now(clock.withZone(zone));
    }

    public StreakResult calculate(Collection<LocalDate> completedDates, ZoneId zone) {
        return calculate(completedDates, today(zone));
    }

    public StreakResult calculate(Collection<LocalDate> completedDates, LocalDate today) {
        // Datele din viitor (ceas dat înapoi, fus schimbat) nu pot face parte dintr-un streak curent.
        NavigableSet<LocalDate> days = new TreeSet<>();
        for (LocalDate date : completedDates) {
            if (!date.isAfter(today)) {
                days.add(date);
            }
        }

        boolean completedToday = days.contains(today);
        return new StreakResult(currentStreak(days, today), longestStreak(days), completedToday);
    }

    private int currentStreak(NavigableSet<LocalDate> days, LocalDate today) {
        LocalDate cursor;
        if (days.contains(today)) {
            cursor = today;
        } else if (days.contains(today.minusDays(1))) {
            cursor = today.minusDays(1);
        } else {
            return 0; // cel puțin o zi întreagă ratată
        }

        int streak = 0;
        while (days.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int longestStreak(NavigableSet<LocalDate> days) {
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            run = (previous != null && previous.plusDays(1).equals(day)) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = day;
        }
        return longest;
    }
}
