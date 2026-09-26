package dev.sidequest.service;

import dev.sidequest.domain.Proof;
import dev.sidequest.domain.Quest;
import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.domain.User;
import dev.sidequest.repository.QuestAssignmentRepository;
import dev.sidequest.repository.QuestRepository;
import dev.sidequest.repository.UserRepository;
import dev.sidequest.streak.StreakCalculator;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelInfo;
import dev.sidequest.xp.LevelService;
import dev.sidequest.xp.XpAward;
import dev.sidequest.xp.XpContext;
import dev.sidequest.xp.XpService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

@Service
public class QuestService {

    private final UserRepository users;
    private final QuestRepository quests;
    private final QuestAssignmentRepository assignments;
    private final StreakCalculator streaks;
    private final XpService xpService;
    private final LevelService levelService;
    private final Clock clock;
    private final RandomGenerator random;

    public QuestService(UserRepository users,
                        QuestRepository quests,
                        QuestAssignmentRepository assignments,
                        StreakCalculator streaks,
                        XpService xpService,
                        LevelService levelService,
                        Clock clock,
                        RandomGenerator random) {
        this.users = users;
        this.quests = quests;
        this.assignments = assignments;
        this.streaks = streaks;
        this.xpService = xpService;
        this.levelService = levelService;
        this.clock = clock;
        this.random = random;
    }

    // Intenționat fără @Transactional: dacă două cereri creează simultan quest-ul zilei, a doua pică
    // pe UNIQUE(user_id, local_date) și trebuie să poată reciti rândul câștigătorului. Într-o singură
    // tranzacție, excepția ar marca tranzacția rollback-only.
    public TodayQuest getToday(Long userId) {
        User user = findUser(userId);
        LocalDate today = streaks.today(user.getZoneId());
        QuestAssignment assignment = findOrCreateAssignment(user, today);

        StreakResult streak = streakOf(user, today, false);
        StreakResult afterCompletion = streakOf(user, today, true);
        XpAward reward = xpService.calculate(new XpContext(assignment.getQuest().getDifficulty(), afterCompletion.current()));
        return new TodayQuest(assignment, streak, reward.total());
    }

    /**
     * Bifează quest-ul de azi, opțional cu o dovadă. {@code proof.imagePath()} trebuie să fie
     * un fișier deja salvat de {@code ProofImageStorage}; textul e curățat și validat aici.
     */
    @Transactional
    public CompletionResult complete(Long userId, Proof proof) {
        User user = findUser(userId);
        LocalDate today = streaks.today(user.getZoneId());

        QuestAssignment assignment = assignments.findByUserIdAndLocalDate(userId, today)
                .orElseThrow(() -> new NotFoundException("Nu ai un quest pentru azi. Cere-l mai întâi cu GET /users/" + userId + "/quest/today"));
        if (assignment.isCompleted()) {
            throw new ConflictException("Quest-ul de azi e deja completat. Revino mâine pentru altul!");
        }

        StreakResult streak = streakOf(user, today, true);
        XpAward award = xpService.calculate(new XpContext(assignment.getQuest().getDifficulty(), streak.current()));

        LevelInfo before = levelService.forXp(user.getTotalXp());
        assignment.complete(clock.instant(), award.total(), new Proof(cleanProofText(proof.text()), proof.imagePath()));
        user.addXp(award.total());
        LevelInfo after = levelService.forXp(user.getTotalXp());

        return new CompletionResult(assignment, award, streak, after, after.level() > before.level());
    }

    /** Toate quest-urile completate de utilizator, de la cel mai recent la cel mai vechi. */
    @Transactional(readOnly = true)
    public List<QuestAssignment> history(Long userId) {
        findUser(userId);
        return assignments.findCompletedHistory(userId);
    }

    private static String cleanProofText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.strip();
        if (trimmed.length() > Proof.MAX_TEXT_LENGTH) {
            throw new InvalidInputException("Descrierea dovezii poate avea cel mult " + Proof.MAX_TEXT_LENGTH + " de caractere.");
        }
        return trimmed;
    }

    /** Streak-ul din zilele deja completate; cu {@code includeToday} simulează completarea quest-ului de azi. */
    private StreakResult streakOf(User user, LocalDate today, boolean includeToday) {
        Set<LocalDate> completed = new HashSet<>(assignments.findCompletedDates(user.getId()));
        if (includeToday) {
            completed.add(today);
        }
        return streaks.calculate(completed, today);
    }

    private QuestAssignment findOrCreateAssignment(User user, LocalDate today) {
        return assignments.findByUserIdAndLocalDate(user.getId(), today)
                .orElseGet(() -> createAssignment(user, today));
    }

    private QuestAssignment createAssignment(User user, LocalDate today) {
        Quest quest = pickQuestFor(user);
        try {
            return assignments.saveAndFlush(new QuestAssignment(user, quest, today));
        } catch (DataIntegrityViolationException raced) {
            return assignments.findByUserIdAndLocalDate(user.getId(), today).orElseThrow(() -> raced);
        }
    }

    /** Preferă quest-uri pe care utilizatorul nu le-a mai primit; când le-a primit pe toate, reia de la capăt. */
    private Quest pickQuestFor(User user) {
        List<Quest> all = quests.findAll().stream()
                .sorted(Comparator.comparing(Quest::getId))
                .toList();
        if (all.isEmpty()) {
            throw new IllegalStateException("Nu există quest-uri în baza de date");
        }

        Set<Long> seen = new HashSet<>(assignments.findAssignedQuestIds(user.getId()));
        List<Quest> unseen = all.stream().filter(q -> !seen.contains(q.getId())).toList();
        List<Quest> pool = unseen.isEmpty() ? all : unseen;
        return pool.get(random.nextInt(pool.size()));
    }

    private User findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilizatorul " + userId + " nu există"));
    }
}
