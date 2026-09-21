package dev.sidequest.xp;

/**
 * @param xpIntoLevel     XP acumulat în nivelul curent
 * @param xpForNextLevel  XP total necesar pentru a trece de nivelul curent (lățimea nivelului)
 * @param progress        xpIntoLevel / xpForNextLevel, în [0, 1)
 */
public record LevelInfo(int level, String title, int totalXp, int xpIntoLevel, int xpForNextLevel, double progress) {
}
