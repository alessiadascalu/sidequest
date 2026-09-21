package dev.sidequest.service;

import dev.sidequest.domain.User;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;

public record Profile(User user, LevelInfo level, StreakResult streak, long completedQuests) {
}
