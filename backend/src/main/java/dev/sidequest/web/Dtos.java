package dev.sidequest.web;

import dev.sidequest.domain.Quest;
import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.domain.User;
import dev.sidequest.service.CompletionResult;
import dev.sidequest.service.Profile;
import dev.sidequest.service.TodayQuest;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;
import dev.sidequest.xp.XpAward;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contractul JSON al API-ului (separat de entități, ca modelul de date să poată evolua liber). */
public final class Dtos {

    private Dtos() {
    }

    public record CreateUserRequest(
            @NotBlank
            @Size(min = 3, max = 30)
            @Pattern(regexp = "[\\p{L}\\p{N}_.-]*", message = "poate conține doar litere, cifre, '_', '.' și '-'")
            String username,

            @NotBlank
            @ValidZoneId
            String zoneId) {
    }

    public record UserResponse(Long id, String username, String zoneId, int totalXp) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getZoneId().getId(), user.getTotalXp());
        }
    }

    public record QuestDto(Long id, String text, String category, String categoryLabel, String difficulty) {
        static QuestDto from(Quest quest) {
            return new QuestDto(quest.getId(), quest.getText(), quest.getCategory().name(),
                    quest.getCategory().label(), quest.getDifficulty().name());
        }
    }

    public record StreakDto(int current, int longest, boolean completedToday) {
        static StreakDto from(StreakResult streak) {
            return new StreakDto(streak.current(), streak.longest(), streak.completedToday());
        }
    }

    public record LevelDto(int level, String title, int totalXp, int xpIntoLevel, int xpForNextLevel, double progress) {
        static LevelDto from(LevelInfo info) {
            return new LevelDto(info.level(), info.title(), info.totalXp(), info.xpIntoLevel(),
                    info.xpForNextLevel(), info.progress());
        }
    }

    public record TodayQuestResponse(
            Long assignmentId,
            LocalDate date,
            boolean completed,
            Instant completedAt,
            int xpReward,
            QuestDto quest,
            StreakDto streak) {
        static TodayQuestResponse from(TodayQuest today) {
            QuestAssignment a = today.assignment();
            return new TodayQuestResponse(a.getId(), a.getLocalDate(), a.isCompleted(), a.getCompletedAt(),
                    today.xpReward(), QuestDto.from(a.getQuest()), StreakDto.from(today.streak()));
        }
    }

    public record XpLineDto(String source, int xp) {
    }

    public record CompleteQuestResponse(
            Long assignmentId,
            LocalDate date,
            Instant completedAt,
            int xpAwarded,
            List<XpLineDto> xpBreakdown,
            boolean leveledUp,
            LevelDto level,
            StreakDto streak) {
        static CompleteQuestResponse from(CompletionResult result) {
            QuestAssignment a = result.assignment();
            XpAward award = result.award();
            return new CompleteQuestResponse(a.getId(), a.getLocalDate(), a.getCompletedAt(), award.total(),
                    award.breakdown().stream().map(l -> new XpLineDto(l.source(), l.xp())).toList(),
                    result.leveledUp(), LevelDto.from(result.level()), StreakDto.from(result.streak()));
        }
    }

    public record ProfileResponse(
            Long id,
            String username,
            String zoneId,
            int totalXp,
            long completedQuests,
            LevelDto level,
            StreakDto streak) {
        static ProfileResponse from(Profile profile) {
            User u = profile.user();
            return new ProfileResponse(u.getId(), u.getUsername(), u.getZoneId().getId(), u.getTotalXp(),
                    profile.completedQuests(), LevelDto.from(profile.level()), StreakDto.from(profile.streak()));
        }
    }
}
