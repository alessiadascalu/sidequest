package dev.sidequest.xp;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** +2 XP pentru fiecare zi de streak după prima, plafonat la +20 (streak de 11 zile). */
@Component
@Order(2)
public class StreakBonusXpStrategy implements XpStrategy {

    static final int XP_PER_STREAK_DAY = 2;
    static final int MAX_BONUS = 20;

    @Override
    public String name() {
        return "Bonus streak";
    }

    @Override
    public int xpFor(XpContext context) {
        int extraDays = Math.max(context.streakDays() - 1, 0);
        return Math.min(extraDays * XP_PER_STREAK_DAY, MAX_BONUS);
    }
}
