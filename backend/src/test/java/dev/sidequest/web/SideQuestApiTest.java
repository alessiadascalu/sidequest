package dev.sidequest.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sidequest.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste cap-coadă (HTTP -> servicii -> H2 în memorie), cu timpul controlat de un Clock mutabil. */
@SpringBootTest
@AutoConfigureMockMvc
class SideQuestApiTest {

    static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    static final AtomicInteger USERS = new AtomicInteger();

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-06-10T09:00:00Z"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(Instant.parse("2026-06-10T09:00:00Z"));
    }

    // ---------- helpers ----------

    private long createUser(ZoneId zone) throws Exception {
        String body = """
                {"username": "player%d", "zoneId": "%s"}""".formatted(USERS.incrementAndGet(), zone.getId());
        MvcResult result = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode today(long userId) throws Exception {
        return read(mvc.perform(get("/users/{id}/quest/today", userId)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode complete(long userId) throws Exception {
        return read(mvc.perform(post("/users/{id}/quest/today/complete", userId)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode profile(long userId) throws Exception {
        return read(mvc.perform(get("/users/{id}/profile", userId)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    /** Mută ceasul la momentul local dat, cere quest-ul zilei și îl completează. */
    private JsonNode completeAt(long userId, String localDateTime, ZoneId zone) throws Exception {
        clock.setLocal(localDateTime, zone);
        today(userId);
        return complete(userId);
    }

    // ---------- POST /users ----------

    @Test
    void createUserReturns201WithLocationAndZeroXp() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"maria_ro\",\"zoneId\":\"Europe/Bucharest\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/profile")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("maria_ro"))
                .andExpect(jsonPath("$.zoneId").value("Europe/Bucharest"))
                .andExpect(jsonPath("$.totalXp").value(0));
    }

    @Test
    void createUserRejectsInvalidInputWithFieldErrors() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"zoneId\":\"Mars/Olympus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.zoneId").exists());

        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        // offset-urile nu sunt fusuri IANA; ne trebuie un ZoneId real cu reguli DST
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"valid_name\",\"zoneId\":\"+02:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.zoneId").exists());
    }

    @Test
    void createUserRejectsMalformedJson() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{nu e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameIsRejectedCaseInsensitively() throws Exception {
        String body = "{\"username\":\"DuplicatUnic\",\"zoneId\":\"Europe/Bucharest\"}";
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"duplicatunic\",\"zoneId\":\"Asia/Tokyo\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- GET quest/today ----------

    @Test
    void todayQuestIsCreatedOnceAndStableWithinTheSameDay() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T08:00", BUCHAREST);
        JsonNode morning = today(id);
        clock.setLocal("2026-06-10T23:59", BUCHAREST);
        JsonNode night = today(id);

        assertEquals(morning.get("assignmentId"), night.get("assignmentId"));
        assertEquals(morning.get("quest").get("id"), night.get("quest").get("id"));
        assertEquals("2026-06-10", morning.get("date").asText());
        assertFalse(morning.get("completed").asBoolean());
        assertTrue(morning.get("xpReward").asInt() >= 10);
        assertFalse(morning.get("quest").get("text").asText().isBlank());
    }

    @Test
    void unknownUserGets404OnEveryEndpoint() throws Exception {
        mvc.perform(get("/users/999999/quest/today")).andExpect(status().isNotFound());
        mvc.perform(post("/users/999999/quest/today/complete")).andExpect(status().isNotFound());
        mvc.perform(get("/users/999999/profile")).andExpect(status().isNotFound());
    }

    @Test
    void nonNumericUserIdIs400() throws Exception {
        mvc.perform(get("/users/abc/profile")).andExpect(status().isBadRequest());
    }

    // ---------- POST quest/today/complete ----------

    @Test
    void completingAwardsXpAndUpdatesProfile() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T10:00", BUCHAREST);
        int expectedReward = today(id).get("xpReward").asInt();

        JsonNode done = complete(id);
        assertEquals(expectedReward, done.get("xpAwarded").asInt());
        assertEquals(1, done.get("streak").get("current").asInt());
        assertTrue(done.get("streak").get("completedToday").asBoolean());
        assertFalse(done.get("xpBreakdown").isEmpty());

        JsonNode p = profile(id);
        assertEquals(expectedReward, p.get("totalXp").asInt());
        assertEquals(1, p.get("completedQuests").asInt());
        assertEquals(1, p.get("streak").get("current").asInt());
        assertEquals(1, p.get("level").get("level").asInt());
        assertEquals("Cartof de Canapea", p.get("level").get("title").asText());

        JsonNode after = today(id);
        assertTrue(after.get("completed").asBoolean());
        assertFalse(after.get("completedAt").isNull());
    }

    @Test
    void completingTwiceOnTheSameDayIs409AndAwardsNothingExtra() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T10:00", BUCHAREST);
        today(id);
        int xp = complete(id).get("xpAwarded").asInt();

        mvc.perform(post("/users/{id}/quest/today/complete", id)).andExpect(status().isConflict());
        assertEquals(xp, profile(id).get("totalXp").asInt());
    }

    @Test
    void completingWithoutHavingFetchedTheQuestIs404() throws Exception {
        long id = createUser(BUCHAREST);
        mvc.perform(post("/users/{id}/quest/today/complete", id)).andExpect(status().isNotFound());
    }

    @Test
    void twoSimultaneousCompletionsAwardXpExactlyOnce() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T10:00", BUCHAREST);
        today(id);

        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> attempt = () -> {
                go.await();
                return mvc.perform(post("/users/{id}/quest/today/complete", id)).andReturn().getResponse().getStatus();
            };
            Future<Integer> a = pool.submit(attempt);
            Future<Integer> b = pool.submit(attempt);
            go.countDown();

            List<Integer> statuses = List.of(a.get(), b.get());
            assertEquals(1, statuses.stream().filter(s -> s == 200).count(), "statusuri: " + statuses);
            assertEquals(1, statuses.stream().filter(s -> s == 409).count(), "statusuri: " + statuses);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, profile(id).get("completedQuests").asInt());
    }

    // ---------- Streak prin API ----------

    @Test
    void completingAt2359AndAt0001MakesTwoConsecutiveDays() throws Exception {
        long id = createUser(BUCHAREST);

        JsonNode first = completeAt(id, "2026-06-10T23:59", BUCHAREST);
        assertEquals(1, first.get("streak").get("current").asInt());

        // 2 minute mai târziu e altă zi: quest nou, necompletat, dar streak-ul de ieri e intact.
        clock.setLocal("2026-06-11T00:01", BUCHAREST);
        JsonNode newDay = today(id);
        assertEquals("2026-06-11", newDay.get("date").asText());
        assertFalse(newDay.get("completed").asBoolean());
        assertNotEquals(first.get("assignmentId"), newDay.get("assignmentId"));
        assertEquals(1, newDay.get("streak").get("current").asInt());
        assertFalse(newDay.get("streak").get("completedToday").asBoolean());

        JsonNode second = complete(id);
        assertEquals(2, second.get("streak").get("current").asInt());
        // a doua zi de streak: bonusul de +2 apare în detaliu
        assertTrue(second.get("xpBreakdown").toString().contains("Bonus streak"));
    }

    @Test
    void aMissedDayResetsTheStreakButKeepsTheRecord() throws Exception {
        long id = createUser(BUCHAREST);
        completeAt(id, "2026-06-10T12:00", BUCHAREST);
        assertEquals(2, completeAt(id, "2026-06-11T12:00", BUCHAREST).get("streak").get("current").asInt());

        // 12 iunie: ratat complet. 13 iunie dimineața:
        clock.setLocal("2026-06-13T08:00", BUCHAREST);
        JsonNode p = profile(id);
        assertEquals(0, p.get("streak").get("current").asInt());
        assertEquals(2, p.get("streak").get("longest").asInt());

        JsonNode restart = completeAt(id, "2026-06-13T09:00", BUCHAREST);
        assertEquals(1, restart.get("streak").get("current").asInt());
        assertFalse(restart.get("xpBreakdown").toString().contains("Bonus streak"));
    }

    @Test
    void sameInstantGivesDifferentLocalDaysToUsersInDifferentZones() throws Exception {
        long tokyo = createUser(TOKYO);
        long la = createUser(LOS_ANGELES);

        clock.set(Instant.parse("2026-06-10T20:00:00Z"));
        assertEquals("2026-06-11", today(tokyo).get("date").asText());
        assertEquals("2026-06-10", today(la).get("date").asText());
    }

    @Test
    void streakSurvivesTheSpringForwardDstTransition() throws Exception {
        long id = createUser(BUCHAREST);
        assertEquals(1, completeAt(id, "2026-03-27T12:00", BUCHAREST).get("streak").get("current").asInt());
        assertEquals(2, completeAt(id, "2026-03-28T23:59", BUCHAREST).get("streak").get("current").asInt());
        // 29 mar: ziua de 23h (ceasul sare 03:00 -> 04:00)
        assertEquals(3, completeAt(id, "2026-03-29T00:01", BUCHAREST).get("streak").get("current").asInt());
        assertEquals(4, completeAt(id, "2026-03-30T00:30", BUCHAREST).get("streak").get("current").asInt());
    }

    @Test
    void streakSurvivesTheFallBackDstTransition() throws Exception {
        long id = createUser(BUCHAREST);
        assertEquals(1, completeAt(id, "2026-10-24T12:00", BUCHAREST).get("streak").get("current").asInt());
        // 25 oct 00:30 și 26 oct 00:30 sunt la 25h distanță (ziua lungă), dar sunt zile consecutive
        assertEquals(2, completeAt(id, "2026-10-25T00:30", BUCHAREST).get("streak").get("current").asInt());
        assertEquals(3, completeAt(id, "2026-10-26T00:30", BUCHAREST).get("streak").get("current").asInt());
    }

    // ---------- XP și niveluri ----------

    @Test
    void sevenDaysInARowGuaranteesLevelingUpExactlyOnce() throws Exception {
        long id = createUser(BUCHAREST);
        int sum = 0;
        int levelUps = 0;
        for (int day = 1; day <= 7; day++) {
            JsonNode done = completeAt(id, "2026-07-%02dT12:00".formatted(day), BUCHAREST);
            sum += done.get("xpAwarded").asInt();
            if (done.get("leveledUp").asBoolean()) {
                levelUps++;
            }
            assertEquals(day, done.get("streak").get("current").asInt());
        }
        // minim 10 XP/zi + bonus de streak (0,2,4,...,12) => cel puțin 112 XP după 7 zile => nivel 2
        JsonNode p = profile(id);
        assertEquals(sum, p.get("totalXp").asInt());
        assertTrue(sum >= 112, "XP total: " + sum);
        assertEquals(1, levelUps);
        assertEquals(2, p.get("level").get("level").asInt());
        assertEquals("Ucenic Curios", p.get("level").get("title").asText());
        assertEquals(7, p.get("streak").get("longest").asInt());
    }

    // ---------- Rotația quest-urilor ----------

    @Test
    void aUserGetsAllThirtyQuestsBeforeAnyRepeats() throws Exception {
        long id = createUser(BUCHAREST);
        Set<Long> seen = new HashSet<>();
        List<Long> order = new ArrayList<>();
        for (int day = 1; day <= 30; day++) {
            clock.set(Instant.parse("2026-08-01T09:00:00Z").plusSeconds(86_400L * (day - 1)));
            long questId = today(id).get("quest").get("id").asLong();
            order.add(questId);
            seen.add(questId);
        }
        assertEquals(30, seen.size(), "quest-uri primite în 30 de zile: " + order);

        // ziua 31: toate au fost văzute, rotația reîncepe fără erori
        clock.set(Instant.parse("2026-08-01T09:00:00Z").plusSeconds(86_400L * 30));
        assertTrue(seen.contains(today(id).get("quest").get("id").asLong()));
    }
}
