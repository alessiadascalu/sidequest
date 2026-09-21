package dev.sidequest.streak;

/**
 * @param current        zile consecutive active. Rămâne "în viață" și dacă azi nu e încă completat,
 *                       atât timp cât ieri a fost (mai ai timp până la miezul nopții local).
 * @param longest        cea mai lungă serie consecutivă din tot istoricul
 * @param completedToday dacă ziua locală curentă are deja un quest completat
 */
public record StreakResult(int current, int longest, boolean completedToday) {
}
