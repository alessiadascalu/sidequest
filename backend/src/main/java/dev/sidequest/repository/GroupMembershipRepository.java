package dev.sidequest.repository;

import dev.sidequest.domain.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    long countByGroupId(Long groupId);

    /** Grupurile utilizatorului, în ordinea în care a intrat în ele. */
    @Query("""
            select m from GroupMembership m join fetch m.group
            where m.user.id = :userId
            order by m.joinedAt, m.id""")
    List<GroupMembership> findByUserWithGroup(@Param("userId") Long userId);

    @Query("select m from GroupMembership m join fetch m.user where m.group.id = :groupId")
    List<GroupMembership> findByGroupWithUser(@Param("groupId") Long groupId);

    @Query("""
            select new dev.sidequest.repository.GroupSize(m.group.id, count(m))
            from GroupMembership m
            where m.group.id in :groupIds
            group by m.group.id""")
    List<GroupSize> countMembers(@Param("groupIds") Collection<Long> groupIds);
}
