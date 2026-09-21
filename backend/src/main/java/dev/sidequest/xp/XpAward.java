package dev.sidequest.xp;

import java.util.List;

public record XpAward(int total, List<Line> breakdown) {

    public record Line(String source, int xp) {
    }
}
