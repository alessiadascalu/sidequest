package dev.sidequest.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clock de test pe care îl poți "muta" în timp. {@link #withZone} întoarce o vedere care
 * împarte același moment, ca mutările ulterioare să se vadă și prin ea (exact ce face
 * StreakCalculator când cere ceasul în fusul utilizatorului).
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public MutableClock(Instant start) {
        this(new AtomicReference<>(start), ZoneOffset.UTC);
    }

    private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public void set(Instant instant) {
        now.set(instant);
    }

    /** Mută ceasul la ora locală dată, în fusul dat. */
    public void setLocal(String isoLocalDateTime, ZoneId zone) {
        set(LocalDateTime.parse(isoLocalDateTime).atZone(zone).toInstant());
    }

    public void advance(Duration duration) {
        now.updateAndGet(i -> i.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, zone);
    }

    @Override
    public Instant instant() {
        return now.get();
    }
}
