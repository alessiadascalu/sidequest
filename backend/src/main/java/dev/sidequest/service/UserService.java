package dev.sidequest.service;

import dev.sidequest.domain.User;
import dev.sidequest.repository.QuestAssignmentRepository;
import dev.sidequest.repository.UserRepository;
import dev.sidequest.streak.StreakCalculator;
import dev.sidequest.xp.LevelService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class UserService {

    private final UserRepository users;
    private final QuestAssignmentRepository assignments;
    private final StreakCalculator streaks;
    private final LevelService levelService;

    public UserService(UserRepository users,
                       QuestAssignmentRepository assignments,
                       StreakCalculator streaks,
                       LevelService levelService) {
        this.users = users;
        this.assignments = assignments;
        this.streaks = streaks;
        this.levelService = levelService;
    }

    public User create(String username, ZoneId zoneId) {
        String name = username.trim();
        if (users.existsByUsernameIgnoreCase(name)) {
            throw new ConflictException("Username-ul „" + name + "” e deja luat");
        }
        try {
            return users.saveAndFlush(new User(name, zoneId));
        } catch (DataIntegrityViolationException raced) {
            throw new ConflictException("Username-ul „" + name + "” e deja luat");
        }
    }

    public Profile profile(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilizatorul " + userId + " nu există"));
        return new Profile(
                user,
                levelService.forXp(user.getTotalXp()),
                streaks.calculate(assignments.findCompletedDates(userId), user.getZoneId()),
                assignments.countByUserIdAndCompletedAtIsNotNull(userId));
    }
}
