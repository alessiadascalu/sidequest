package dev.sidequest.service;

import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.streak.StreakResult;

/**
 * @param xpReward XP-ul pe care îl aduce (sau l-a adus) completarea, cu tot cu bonusul de streak
 */
public record TodayQuest(QuestAssignment assignment, StreakResult streak, int xpReward) {
}
