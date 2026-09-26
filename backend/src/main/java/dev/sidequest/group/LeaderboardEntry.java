package dev.sidequest.group;

import dev.sidequest.domain.User;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;

/** @param rank locul în grup; membrii cu același XP împart locul (1, 1, 3) */
public record LeaderboardEntry(int rank, User user, LevelInfo level, StreakResult streak) {
}
