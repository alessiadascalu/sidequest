package dev.sidequest.xp;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DifficultyXpStrategy implements XpStrategy {

    @Override
    public String name() {
        return "Dificultate";
    }

    @Override
    public int xpFor(XpContext context) {
        return switch (context.difficulty()) {
            case EASY -> 10;
            case MEDIUM -> 20;
            case HARD -> 35;
        };
    }
}
