package dev.sidequest.xp;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nivelul n începe la 50·n·(n-1) XP și are lățimea 100·n:
 * nivel 1 = 0–99, nivel 2 = 100–299, nivel 3 = 300–599, nivel 4 = 600–999 ...
 */
@Service
public class LevelService {

    private static final List<String> TITLES = List.of(
            "Cartof de Canapea",
            "Ucenic Curios",
            "Explorator de Cartier",
            "Vânător de Misiuni",
            "Cavaler al Rutinei",
            "Maestru al Side-Quest-urilor",
            "Legendă Locală",
            "Boss Final");

    public LevelInfo forXp(int totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("XP negativ: " + totalXp);
        }
        int level = 1;
        while (totalXp >= xpRequiredForLevel(level + 1)) {
            level++;
        }
        int start = xpRequiredForLevel(level);
        int width = xpRequiredForLevel(level + 1) - start;
        int into = totalXp - start;
        return new LevelInfo(level, titleFor(level), totalXp, into, width, (double) into / width);
    }

    /** XP total la care începe nivelul dat. */
    public int xpRequiredForLevel(int level) {
        return 50 * level * (level - 1);
    }

    public String titleFor(int level) {
        return TITLES.get(Math.min(level, TITLES.size()) - 1);
    }
}
