package dev.sidequest.repository;

import dev.sidequest.domain.QuestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestAssignmentRepository extends JpaRepository<QuestAssignment, Long> {

    Optional<QuestAssignment> findByUserIdAndLocalDate(Long userId, LocalDate localDate);

    @Query("select a.localDate from QuestAssignment a where a.user.id = :userId and a.completedAt is not null")
    List<LocalDate> findCompletedDates(@Param("userId") Long userId);

    /** Zilele completate ale mai multor utilizatori dintr-un singur query (pentru leaderboard). */
    @Query("""
            select new dev.sidequest.repository.CompletedDay(a.user.id, a.localDate)
            from QuestAssignment a
            where a.user.id in :userIds and a.completedAt is not null""")
    List<CompletedDay> findCompletedDaysOfUsers(@Param("userIds") Collection<Long> userIds);

    @Query("select a.quest.id from QuestAssignment a where a.user.id = :userId")
    List<Long> findAssignedQuestIds(@Param("userId") Long userId);

    long countByUserIdAndCompletedAtIsNotNull(Long userId);

    /** Quest-urile completate, cele mai noi primele; quest-ul vine în același query (fără N+1). */
    @Query("""
            select a from QuestAssignment a join fetch a.quest join fetch a.user
            where a.user.id = :userId and a.completedAt is not null
            order by a.localDate desc, a.completedAt desc""")
    List<QuestAssignment> findCompletedHistory(@Param("userId") Long userId);
}
