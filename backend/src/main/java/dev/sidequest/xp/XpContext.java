package dev.sidequest.xp;

import dev.sidequest.domain.Difficulty;

/**
 * @param streakDays lungimea streak-ului INCLUZÂND quest-ul care tocmai se completează (minim 1)
 */
public record XpContext(Difficulty difficulty, int streakDays) {
}
