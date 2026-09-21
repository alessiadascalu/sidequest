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
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Quest-ul unui utilizator pentru o zi calendaristică LOCALĂ.
 * {@code localDate} se calculează o singură dată, în fusul orar al utilizatorului la momentul
 * atribuirii, și nu se mai schimbă: istoricul (deci și streak-ul) rămâne stabil chiar dacă
 * utilizatorul își schimbă fusul orar mai târziu.
 */
@Entity
@Table(
        name = "quest_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_assignment_user_date",
                columnNames = {"user_id", "local_date"}))
public class QuestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    // Momentul real al completării (Instant = fără ambiguitate de fus orar). Null = necompletat.
    @Column(name = "completed_at")
    private Instant completedAt;

    // Blochează dublul "Completed" concurent (al doilea commit pică cu optimistic lock).
    @Version
    private long version;

    protected QuestAssignment() {
    }

    public QuestAssignment(User user, Quest quest, LocalDate localDate) {
        this.user = user;
        this.quest = quest;
        this.localDate = localDate;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Quest getQuest() {
        return quest;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void complete(Instant when) {
        if (isCompleted()) {
            throw new IllegalStateException("Quest-ul este deja completat");
        }
        this.completedAt = when;
    }
}
