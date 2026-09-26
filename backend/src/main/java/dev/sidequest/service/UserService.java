package dev.sidequest.service;

import dev.sidequest.domain.User;
import dev.sidequest.repository.QuestAssignmentRepository;
import dev.sidequest.repository.UserRepository;
import dev.sidequest.streak.StreakCalculator;
import dev.sidequest.xp.LevelService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Optional;

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

    /**
     * "Login" fără parolă: dacă username-ul există (indiferent de majuscule), întoarce acel
     * utilizator; altfel îl creează cu fusul orar dat. Pentru un utilizator existent fusul
     * trimis e ignorat: zilele deja atribuite depind de fusul salvat.
     */
    public LoginResult loginOrCreate(String username, ZoneId zoneId) {
        String name = username.trim();
        Optional<User> existing = users.findByUsernameIgnoreCase(name);
        if (existing.isPresent()) {
            return new LoginResult(existing.get(), false);
        }
        try {
            return new LoginResult(users.saveAndFlush(new User(name, zoneId)), true);
        } catch (DataIntegrityViolationException raced) {
            // Două cereri simultane cu același username nou: câștigătorul l-a creat, noi doar ne logăm.
            return users.findByUsernameIgnoreCase(name)
                    .map(user -> new LoginResult(user, false))
                    .orElseThrow(() -> raced);
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
