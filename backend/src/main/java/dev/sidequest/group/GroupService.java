package dev.sidequest.group;

import dev.sidequest.domain.Group;
import dev.sidequest.domain.GroupMembership;
import dev.sidequest.domain.User;
import dev.sidequest.repository.CompletedDay;
import dev.sidequest.repository.GroupMembershipRepository;
import dev.sidequest.repository.GroupRepository;
import dev.sidequest.repository.GroupSize;
import dev.sidequest.repository.QuestAssignmentRepository;
import dev.sidequest.repository.UserRepository;
import dev.sidequest.service.ConflictException;
import dev.sidequest.service.NotFoundException;
import dev.sidequest.streak.StreakCalculator;
import dev.sidequest.streak.StreakResult;
import dev.sidequest.xp.LevelService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {

    // Cu ~887 milioane de coduri posibile, o coliziune e rară; 10 încercări sunt o plasă de siguranță.
    static final int MAX_CODE_ATTEMPTS = 10;

    private final GroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final UserRepository users;
    private final QuestAssignmentRepository assignments;
    private final StreakCalculator streaks;
    private final LevelService levelService;
    private final InviteCodeGenerator codes;
    private final Clock clock;

    public GroupService(GroupRepository groups,
                        GroupMembershipRepository memberships,
                        UserRepository users,
                        QuestAssignmentRepository assignments,
                        StreakCalculator streaks,
                        LevelService levelService,
                        InviteCodeGenerator codes,
                        Clock clock) {
        this.groups = groups;
        this.memberships = memberships;
        this.users = users;
        this.assignments = assignments;
        this.streaks = streaks;
        this.levelService = levelService;
        this.codes = codes;
        this.clock = clock;
    }

    /** Creează grupul cu un cod de invitație nou; creatorul devine primul membru. */
    @Transactional
    public GroupSummary create(String name, Long creatorUserId) {
        User creator = findUser(creatorUserId);
        Instant now = clock.instant();
        Group group = groups.save(new Group(name.strip(), uniqueInviteCode(), now, creator));
        memberships.save(new GroupMembership(creator, group, now));
        return new GroupSummary(group, 1, now);
    }

    // Fără @Transactional, ca la crearea quest-ului zilei: dacă două cereri de join identice se
    // întrec, a doua pică pe UNIQUE(user_id, group_id) și trebuie să poată răspunde cu 409.
    public GroupSummary join(Long userId, String inviteCode) {
        User user = findUser(userId);
        String code = InviteCodeGenerator.normalize(inviteCode);
        Group group = groups.findByInviteCode(code)
                .orElseThrow(() -> new NotFoundException(
                        "Nu există niciun grup cu codul „" + code + "”. Verifică literele și încearcă din nou."));

        if (memberships.existsByUserIdAndGroupId(userId, group.getId())) {
            throw alreadyMember(group);
        }
        GroupMembership membership;
        try {
            membership = memberships.saveAndFlush(new GroupMembership(user, group, clock.instant()));
        } catch (DataIntegrityViolationException raced) {
            throw alreadyMember(group);
        }
        return new GroupSummary(group, memberships.countByGroupId(group.getId()), membership.getJoinedAt());
    }

    @Transactional(readOnly = true)
    public List<GroupSummary> groupsOf(Long userId) {
        findUser(userId);
        List<GroupMembership> mine = memberships.findByUserWithGroup(userId);
        if (mine.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> sizes = memberships.countMembers(mine.stream().map(m -> m.getGroup().getId()).toList())
                .stream()
                .collect(Collectors.toMap(GroupSize::groupId, GroupSize::members));
        return mine.stream()
                .map(m -> new GroupSummary(m.getGroup(), sizes.getOrDefault(m.getGroup().getId(), 1L), m.getJoinedAt()))
                .toList();
    }

    /**
     * Membrii grupului, de la cel mai mult XP la cel mai puțin. La XP egal decide streak-ul
     * curent, apoi username-ul (ca ordinea să fie stabilă); membrii cu XP egal împart locul.
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(Long groupId) {
        if (!groups.existsById(groupId)) {
            throw new NotFoundException("Grupul " + groupId + " nu există");
        }
        List<User> members = memberships.findByGroupWithUser(groupId).stream().map(GroupMembership::getUser).toList();
        Map<Long, Set<LocalDate>> completed = assignments
                .findCompletedDaysOfUsers(members.stream().map(User::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(CompletedDay::userId,
                        Collectors.mapping(CompletedDay::date, Collectors.toSet())));

        record Row(User user, StreakResult streak) {
        }
        List<Row> rows = members.stream()
                .map(u -> new Row(u, streaks.calculate(completed.getOrDefault(u.getId(), Set.of()), u.getZoneId())))
                .sorted(Comparator.comparingInt((Row r) -> r.user().getTotalXp()).reversed()
                        .thenComparing(Comparator.comparingInt((Row r) -> r.streak().current()).reversed())
                        .thenComparing(r -> r.user().getUsername(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LeaderboardEntry> board = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean tiedWithPrevious = i > 0 && rows.get(i - 1).user().getTotalXp() == row.user().getTotalXp();
            int rank = tiedWithPrevious ? board.get(i - 1).rank() : i + 1;
            board.add(new LeaderboardEntry(rank, row.user(), levelService.forXp(row.user().getTotalXp()), row.streak()));
        }
        return board;
    }

    private String uniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codes.next();
            if (!groups.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Nu am găsit un cod de invitație liber după " + MAX_CODE_ATTEMPTS + " încercări");
    }

    private static ConflictException alreadyMember(Group group) {
        return new ConflictException("Ești deja membru în grupul „" + group.getName() + "”.");
    }

    private User findUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilizatorul " + userId + " nu există"));
    }
}
