package dev.sidequest.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sidequest.support.MutableClock;
import dev.sidequest.support.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Teste cap-coadă (HTTP -> servicii -> H2 în memorie), cu timpul controlat de un Clock mutabil. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class SideQuestApiTest {

    static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    static final AtomicInteger USERS = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(TestClockConfig.START);
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

    private JsonNode login(String username, String zoneId, int expectedStatus) throws Exception {
        return read(mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"zoneId\":\"%s\"}".formatted(username, zoneId)))
                .andExpect(status().is(expectedStatus))
                .andReturn());
    }

    private JsonNode history(long userId) throws Exception {
        return read(mvc.perform(get("/users/{id}/history", userId)).andExpect(status().isOk()).andReturn());
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
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.history").isEmpty());
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

    // ---------- POST /users ca login ----------

    @Test
    void existingUsernameLogsInInsteadOfFailing() throws Exception {
        long id = createUser(BUCHAREST);
        String username = profile(id).get("username").asText();

        // alt case, alt fus orar: tot același cont, cu fusul original păstrat
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"zoneId\":\"Asia/Tokyo\"}".formatted(username.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.zoneId").value("Europe/Bucharest"))
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void loginReturnsXpStreakLevelAndHistoryOfTheExistingUser() throws Exception {
        long id = createUser(BUCHAREST);
        String username = profile(id).get("username").asText();
        int xp = completeAt(id, "2026-06-10T12:00", BUCHAREST).get("xpAwarded").asInt();
        xp += completeAt(id, "2026-06-11T12:00", BUCHAREST).get("xpAwarded").asInt();

        clock.setLocal("2026-06-12T08:00", BUCHAREST);
        JsonNode login = login(username, "Europe/Bucharest", 200);

        assertFalse(login.get("created").asBoolean());
        assertEquals(id, login.get("id").asLong());
        assertEquals(xp, login.get("totalXp").asInt());
        assertEquals(xp, login.get("level").get("totalXp").asInt());
        assertEquals(2, login.get("completedQuests").asInt());
        // azi (12) încă necompletat, dar streak-ul de ieri e viu
        assertEquals(2, login.get("streak").get("current").asInt());
        assertFalse(login.get("streak").get("completedToday").asBoolean());
        assertEquals(2, login.get("history").size());
        assertEquals("2026-06-11", login.get("history").get(0).get("date").asText());
    }

    @Test
    void newUsernameIsCreatedWithEmptyHistory() throws Exception {
        JsonNode created = login("nou_nout", "Europe/Bucharest", 201);
        assertTrue(created.get("created").asBoolean());
        assertEquals(0, created.get("totalXp").asInt());
        assertEquals(0, created.get("streak").get("current").asInt());
        assertEquals(1, created.get("level").get("level").asInt());
        assertTrue(created.get("history").isEmpty());

        // a doua oară: login, nu cont nou
        assertEquals(created.get("id"), login("nou_nout", "Europe/Bucharest", 200).get("id"));
    }

    @Test
    void twoSimultaneousSignupsWithTheSameNewUsernameEndUpOnTheSameAccount() throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> attempt = () -> {
                go.await();
                return mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cursa_simultana\",\"zoneId\":\"Europe/Bucharest\"}")).andReturn();
            };
            Future<MvcResult> a = pool.submit(attempt);
            Future<MvcResult> b = pool.submit(attempt);
            go.countDown();

            List<MvcResult> results = List.of(a.get(), b.get());
            List<Integer> statuses = results.stream().map(r -> r.getResponse().getStatus()).sorted().toList();
            assertEquals(List.of(200, 201), statuses);
            assertEquals(read(results.get(0)).get("id"), read(results.get(1)).get("id"));
        } finally {
            pool.shutdownNow();
        }
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
        mvc.perform(get("/users/999999/history")).andExpect(status().isNotFound());
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

    // ---------- Istoric ----------

    @Test
    void historyListsOnlyCompletedQuestsNewestFirstWithTheXpActuallyAwarded() throws Exception {
        long id = createUser(BUCHAREST);
        JsonNode d1 = completeAt(id, "2026-06-10T12:00", BUCHAREST);
        JsonNode d2 = completeAt(id, "2026-06-11T12:00", BUCHAREST);
        clock.setLocal("2026-06-12T12:00", BUCHAREST);
        today(id); // atribuit, dar necompletat: nu apare în istoric

        JsonNode h = history(id);
        assertEquals(2, h.size());

        JsonNode newest = h.get(0);
        assertEquals("2026-06-11", newest.get("date").asText());
        assertEquals(d2.get("assignmentId"), newest.get("assignmentId"));
        assertEquals(d2.get("xpAwarded").asInt(), newest.get("xpAwarded").asInt());
        assertEquals(d2.get("completedAt"), newest.get("completedAt"));
        assertFalse(newest.get("quest").get("text").asText().isBlank());
        assertFalse(newest.get("quest").get("category").asText().isBlank());
        assertFalse(newest.get("quest").get("categoryLabel").asText().isBlank());
        assertFalse(newest.get("quest").get("difficulty").asText().isBlank());
        assertTrue(newest.get("proof").isNull());

        assertEquals("2026-06-10", h.get(1).get("date").asText());
        assertEquals(d1.get("xpAwarded").asInt(), h.get(1).get("xpAwarded").asInt());
    }

    @Test
    void historyIsEmptyForANewUser() throws Exception {
        assertTrue(history(createUser(BUCHAREST)).isEmpty());
    }

    // ---------- Dovadă la completare ----------

    // Semnătura PNG + câțiva octeți: suficient, validăm formatul după "magic bytes"
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};

    @Value("${sidequest.uploads-dir}") String uploadsDir;

    private long uploadedFiles() throws Exception {
        Path dir = Path.of(uploadsDir);
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        }
    }

    private MockMultipartHttpServletRequestBuilder completeWithProof(long userId) {
        return multipart("/users/{id}/quest/today/complete", userId);
    }

    @Test
    void completingWithPhotoAndTextStoresBothAndServesThePhoto() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T12:00", BUCHAREST);
        today(id);

        JsonNode done = read(mvc.perform(completeWithProof(id)
                        .file(new MockMultipartFile("photo", "../../etc/passwd.png", "image/png", PNG))
                        .param("proofText", "  Am urcat 12 etaje pe scări!  "))
                .andExpect(status().isOk())
                .andReturn());

        String imageUrl = done.get("proof").get("imageUrl").asText();
        assertEquals("Am urcat 12 etaje pe scări!", done.get("proof").get("text").asText());
        // numele clientului e ignorat: salvăm sub un UUID generat de noi
        assertTrue(imageUrl.matches("/uploads/[0-9a-f-]{36}\\.png"), imageUrl);

        byte[] served = mvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(PNG, served);

        JsonNode entry = history(id).get(0);
        assertEquals(imageUrl, entry.get("proof").get("imageUrl").asText());
        assertEquals("Am urcat 12 etaje pe scări!", entry.get("proof").get("text").asText());
        assertEquals(imageUrl, today(id).get("proof").get("imageUrl").asText());
    }

    @Test
    void proofIsOptionalTextOnlyAndPhotoOnlyBothWork() throws Exception {
        long textOnly = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T12:00", BUCHAREST);
        today(textOnly);
        JsonNode t = read(mvc.perform(completeWithProof(textOnly).param("proofText", "Făcut!"))
                .andExpect(status().isOk()).andReturn());
        assertEquals("Făcut!", t.get("proof").get("text").asText());
        assertTrue(t.get("proof").get("imageUrl").isNull());

        long photoOnly = createUser(BUCHAREST);
        today(photoOnly);
        JsonNode p = read(mvc.perform(completeWithProof(photoOnly)
                        .file(new MockMultipartFile("photo", "x.jpg", "image/jpeg", JPEG))
                        .param("proofText", "   "))
                .andExpect(status().isOk()).andReturn());
        assertTrue(p.get("proof").get("text").isNull());
        assertTrue(p.get("proof").get("imageUrl").asText().endsWith(".jpg"));

        // multipart gol = bifare simplă, fără dovadă
        long none = createUser(BUCHAREST);
        today(none);
        JsonNode n = read(mvc.perform(completeWithProof(none)).andExpect(status().isOk()).andReturn());
        assertTrue(n.get("proof").isNull());
    }

    @Test
    void aFileThatIsNotReallyAnImageIsRejectedAndTheQuestStaysOpen() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T12:00", BUCHAREST);
        today(id);
        long before = uploadedFiles();

        byte[] html = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        mvc.perform(completeWithProof(id).file(new MockMultipartFile("photo", "poza.png", "image/png", html)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        assertEquals(before, uploadedFiles());
        assertFalse(today(id).get("completed").asBoolean());
        assertEquals(0, profile(id).get("totalXp").asInt());
    }

    @Test
    void tooLongProofTextIs400AndLeavesNoOrphanPhoto() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T12:00", BUCHAREST);
        today(id);
        long before = uploadedFiles();

        mvc.perform(completeWithProof(id)
                        .file(new MockMultipartFile("photo", "p.png", "image/png", PNG))
                        .param("proofText", "a".repeat(501)))
                .andExpect(status().isBadRequest());

        assertEquals(before, uploadedFiles());
        assertFalse(today(id).get("completed").asBoolean());
    }

    @Test
    void aSecondCompletionWithPhotoIs409AndDoesNotKeepTheNewPhoto() throws Exception {
        long id = createUser(BUCHAREST);
        clock.setLocal("2026-06-10T12:00", BUCHAREST);
        today(id);
        complete(id);
        long before = uploadedFiles();

        mvc.perform(completeWithProof(id).file(new MockMultipartFile("photo", "p.png", "image/png", PNG)))
                .andExpect(status().isConflict());
        assertEquals(before, uploadedFiles());
    }

    @Test
    void uploadsEndpointOnlyServesStoredImageNames() throws Exception {
        mvc.perform(get("/uploads/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/application.properties")).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/..%2F..%2Fpom.xml")).andExpect(status().is4xxClientError());
    }
}
