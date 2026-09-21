package dev.sidequest.service;

import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;
import dev.sidequest.xp.XpAward;

public record CompletionResult(
        QuestAssignment assignment,
        XpAward award,
        StreakResult streak,
        LevelInfo level,
        boolean leveledUp) {
}
