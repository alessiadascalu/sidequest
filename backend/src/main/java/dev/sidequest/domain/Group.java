package dev.sidequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Un grup de prieteni cu leaderboard comun. Se intră în el doar cu {@code inviteCode}.
 *
 * <p>Numele entității e "QuestGroup", iar tabela "quest_groups": GROUP e cuvânt rezervat
 * atât în JPQL (GROUP BY), cât și în SQL/H2.
 */
@Entity(name = "QuestGroup")
@Table(
        name = "quest_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_invite_code", columnNames = "invite_code"))
public class Group {

    public static final int MAX_NAME_LENGTH = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "invite_code", nullable = false, length = 12)
    private String inviteCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    // Aceeași coloană, doar pentru citire: id-ul creatorului fără să încărcăm (lazy) utilizatorul.
    @Column(name = "creator_user_id", insertable = false, updatable = false)
    private Long creatorUserId;

    protected Group() {
    }

    public Group(String name, String inviteCode, Instant createdAt, User creator) {
        this.name = name;
        this.inviteCode = inviteCode;
        this.createdAt = createdAt;
        this.creator = creator;
        this.creatorUserId = creator.getId();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public User getCreator() {
        return creator;
    }

    public Long getCreatorUserId() {
        return creatorUserId;
    }
}
