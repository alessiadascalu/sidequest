package dev.sidequest.web;

import dev.sidequest.domain.Group;
import dev.sidequest.domain.Proof;
import dev.sidequest.domain.Quest;
import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.domain.User;
import dev.sidequest.group.GroupSummary;
import dev.sidequest.group.LeaderboardEntry;
import dev.sidequest.service.CompletionResult;
import dev.sidequest.service.LoginResult;
import dev.sidequest.service.Profile;
import dev.sidequest.service.TodayQuest;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;
import dev.sidequest.xp.XpAward;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Contractul JSON al API-ului (separat de entități, ca modelul de date să poată evolua liber). */
public final class Dtos {

    private Dtos() {
    }

    /** Body-ul pentru POST /users: login dacă username-ul există, creare altfel. */
    public record CreateUserRequest(
            @NotBlank
            @Size(min = 3, max = 30)
            @Pattern(regexp = "[\\p{L}\\p{N}_.-]*", message = "poate conține doar litere, cifre, '_', '.' și '-'")
            String username,

            @NotBlank
            @ValidZoneId
            String zoneId) {
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

    /** Null dacă quest-ul nu are dovadă. {@code imageUrl} e relativ la API: GET /uploads/{fișier}. */
    public record ProofDto(String text, String imageUrl) {
        static ProofDto from(Proof proof) {
            if (proof.isEmpty()) {
                return null;
            }
            String url = proof.imagePath() == null ? null : "/uploads/" + proof.imagePath();
            return new ProofDto(proof.text(), url);
        }
    }

    public record TodayQuestResponse(
            Long assignmentId,
            LocalDate date,
            boolean completed,
            Instant completedAt,
            int xpReward,
            QuestDto quest,
            StreakDto streak,
            ProofDto proof) {
        static TodayQuestResponse from(TodayQuest today) {
            QuestAssignment a = today.assignment();
            return new TodayQuestResponse(a.getId(), a.getLocalDate(), a.isCompleted(), a.getCompletedAt(),
                    today.xpReward(), QuestDto.from(a.getQuest()), StreakDto.from(today.streak()),
                    ProofDto.from(a.getProof()));
        }
    }

    /** @param xpAwarded null pentru quest-uri completate înainte ca XP-ul să fie salvat per quest */
    public record HistoryEntryDto(
            Long assignmentId,
            LocalDate date,
            Instant completedAt,
            Integer xpAwarded,
            QuestDto quest,
            ProofDto proof) {
        static HistoryEntryDto from(QuestAssignment a) {
            return new HistoryEntryDto(a.getId(), a.getLocalDate(), a.getCompletedAt(), a.getXpAwarded(),
                    QuestDto.from(a.getQuest()), ProofDto.from(a.getProof()));
        }

        static List<HistoryEntryDto> from(List<QuestAssignment> assignments) {
            return assignments.stream().map(HistoryEntryDto::from).toList();
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
            StreakDto streak,
            ProofDto proof) {
        static CompleteQuestResponse from(CompletionResult result) {
            QuestAssignment a = result.assignment();
            XpAward award = result.award();
            return new CompleteQuestResponse(a.getId(), a.getLocalDate(), a.getCompletedAt(), award.total(),
                    award.breakdown().stream().map(l -> new XpLineDto(l.source(), l.xp())).toList(),
                    result.leveledUp(), LevelDto.from(result.level()), StreakDto.from(result.streak()),
                    ProofDto.from(a.getProof()));
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

    // ---------- Grupuri ----------

    public record CreateGroupRequest(
            @NotBlank(message = "grupul are nevoie de un nume")
            @Size(max = Group.MAX_NAME_LENGTH, message = "numele grupului poate avea cel mult 40 de caractere")
            String name,

            @NotNull
            Long creatorUserId) {
    }

    public record JoinGroupRequest(
            @NotNull
            Long userId,

            @NotBlank(message = "scrie codul de invitație")
            @Size(max = 20)
            String inviteCode) {
    }

    /** @param joinedAt când a intrat în grup utilizatorul pentru care s-a făcut cererea */
    public record GroupDto(
            Long id,
            String name,
            String inviteCode,
            Instant createdAt,
            Long creatorUserId,
            long memberCount,
            Instant joinedAt) {
        static GroupDto from(GroupSummary summary) {
            Group g = summary.group();
            return new GroupDto(g.getId(), g.getName(), g.getInviteCode(), g.getCreatedAt(),
                    g.getCreatorUserId(), summary.memberCount(), summary.joinedAt());
        }

        static List<GroupDto> from(List<GroupSummary> summaries) {
            return summaries.stream().map(GroupDto::from).toList();
        }
    }

    /** @param streak streak-ul curent, calculat în fusul orar al membrului */
    public record LeaderboardEntryDto(
            int rank,
            Long userId,
            String username,
            int totalXp,
            int level,
            String title,
            int streak,
            boolean completedToday) {
        static LeaderboardEntryDto from(LeaderboardEntry e) {
            return new LeaderboardEntryDto(e.rank(), e.user().getId(), e.user().getUsername(), e.user().getTotalXp(),
                    e.level().level(), e.level().title(), e.streak().current(), e.streak().completedToday());
        }

        static List<LeaderboardEntryDto> from(List<LeaderboardEntry> board) {
            return board.stream().map(LeaderboardEntryDto::from).toList();
        }
    }

    /**
     * Răspunsul la POST /users: tot ce îi trebuie aplicației ca să arate direct ecranul principal.
     *
     * @param created true = cont nou (201), false = utilizator existent, logat (200)
     */
    public record LoginResponse(
            Long id,
            String username,
            String zoneId,
            int totalXp,
            long completedQuests,
            LevelDto level,
            StreakDto streak,
            boolean created,
            List<HistoryEntryDto> history) {
        static LoginResponse from(LoginResult login, Profile profile, List<QuestAssignment> history) {
            ProfileResponse p = ProfileResponse.from(profile);
            return new LoginResponse(p.id(), p.username(), p.zoneId(), p.totalXp(), p.completedQuests(),
                    p.level(), p.streak(), login.created(), HistoryEntryDto.from(history));
        }
    }
}
