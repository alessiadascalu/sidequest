package dev.sidequest.xp;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class XpService {

    private final List<XpStrategy> strategies;

    // Spring injectează toate implementările XpStrategy, în ordinea @Order.
    public XpService(List<XpStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public XpAward calculate(XpContext context) {
        List<XpAward.Line> lines = new ArrayList<>();
        int total = 0;
        for (XpStrategy strategy : strategies) {
            int xp = strategy.xpFor(context);
            if (xp > 0) {
                lines.add(new XpAward.Line(strategy.name(), xp));
                total += xp;
            }
        }
        return new XpAward(total, List.copyOf(lines));
    }
}
