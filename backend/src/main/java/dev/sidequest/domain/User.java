package dev.sidequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZoneId;

// "users" (nu "user"): USER e cuvânt rezervat în H2 și în SQL standard.
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    // ID IANA (ex. "Europe/Bucharest"); îl păstrăm ca String și îl parsăm la nevoie.
    @Column(name = "zone_id", nullable = false, length = 64)
    private String zoneId;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    protected User() {
    }

    public User(String username, ZoneId zoneId) {
        this.username = username;
        this.zoneId = zoneId.getId();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public ZoneId getZoneId() {
        return ZoneId.of(zoneId);
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void addXp(int xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP-ul adăugat nu poate fi negativ: " + xp);
        }
        this.totalXp += xp;
    }
}
