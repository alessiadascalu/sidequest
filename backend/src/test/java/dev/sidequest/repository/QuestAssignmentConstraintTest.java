package dev.sidequest.repository;

import dev.sidequest.domain.Category;
import dev.sidequest.domain.Difficulty;
import dev.sidequest.domain.Group;
import dev.sidequest.domain.GroupMembership;
import dev.sidequest.domain.Quest;
import dev.sidequest.domain.QuestAssignment;
import dev.sidequest.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class QuestAssignmentConstraintTest {

    @Autowired UserRepository users;
    @Autowired QuestRepository quests;
    @Autowired QuestAssignmentRepository assignments;
    @Autowired GroupRepository groups;
    @Autowired GroupMembershipRepository memberships;

    @Test
    void databaseRejectsASecondAssignmentForTheSameUserAndDate() {
        User user = users.save(new User("unic1", ZoneId.of("Europe/Bucharest")));
        Quest a = quests.save(new Quest("A", Category.FOCUS, Difficulty.EASY));
        Quest b = quests.save(new Quest("B", Category.FOCUS, Difficulty.EASY));
        LocalDate day = LocalDate.of(2026, 6, 10);

        assignments.saveAndFlush(new QuestAssignment(user, a, day));

        assertThrows(DataIntegrityViolationException.class,
                () -> assignments.saveAndFlush(new QuestAssignment(user, b, day)));
    }

    @Test
    void sameDateIsFineForDifferentUsersAndDifferentDatesAreFineForTheSameUser() {
        User u1 = users.save(new User("unic2", ZoneId.of("Europe/Bucharest")));
        User u2 = users.save(new User("unic3", ZoneId.of("Asia/Tokyo")));
        Quest q = quests.save(new Quest("Q", Category.SOCIAL, Difficulty.MEDIUM));
        LocalDate day = LocalDate.of(2026, 6, 10);

        assertDoesNotThrow(() -> {
            assignments.saveAndFlush(new QuestAssignment(u1, q, day));
            assignments.saveAndFlush(new QuestAssignment(u2, q, day));
            assignments.saveAndFlush(new QuestAssignment(u1, q, day.plusDays(1)));
        });
    }

    @Test
    void aUserCannotBeInTheSameGroupTwice() {
        User owner = users.save(new User("grup_owner", ZoneId.of("Europe/Bucharest")));
        Instant now = Instant.parse("2026-06-10T09:00:00Z");
        Group group = groups.saveAndFlush(new Group("G", "ABCDEF", now, owner));
        memberships.saveAndFlush(new GroupMembership(owner, group, now));

        assertThrows(DataIntegrityViolationException.class,
                () -> memberships.saveAndFlush(new GroupMembership(owner, group, now)));
    }

    @Test
    void inviteCodesAreUniqueInTheDatabase() {
        User owner = users.save(new User("grup_owner2", ZoneId.of("Europe/Bucharest")));
        Instant now = Instant.parse("2026-06-10T09:00:00Z");
        groups.saveAndFlush(new Group("G1", "QWERTY", now, owner));
        assertThrows(DataIntegrityViolationException.class,
                () -> groups.saveAndFlush(new Group("G2", "QWERTY", now, owner)));
    }

    @Test
    void usernamesAreUnique() {
        users.saveAndFlush(new User("acelasi", ZoneId.of("Europe/Bucharest")));
        assertThrows(DataIntegrityViolationException.class,
                () -> users.saveAndFlush(new User("acelasi", ZoneId.of("Asia/Tokyo"))));
    }
}
