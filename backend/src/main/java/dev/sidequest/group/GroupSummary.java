package dev.sidequest.group;

import dev.sidequest.domain.Group;

import java.time.Instant;

/** Un grup văzut de un membru: câți membri are și când a intrat el în grup. */
public record GroupSummary(Group group, long memberCount, Instant joinedAt) {
}
