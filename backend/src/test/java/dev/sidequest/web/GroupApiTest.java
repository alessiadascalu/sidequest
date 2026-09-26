package dev.sidequest.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sidequest.support.MutableClock;
import dev.sidequest.support.TestClockConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Grupuri, coduri de invitație și leaderboard, prin HTTP (același context ca SideQuestApiTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
class GroupApiTest {

    static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");
    static final AtomicInteger USERS = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(TestClockConfig.START);
    }

    // ---------- helpers ----------

    private record TestUser(long id, String username) {
    }

    private TestUser user() throws Exception {
        String name = "grp_user" + USERS.incrementAndGet();
        MvcResult result = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"zoneId\":\"Europe/Bucharest\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return new TestUser(read(result).get("id").asLong(), name);
    }

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode createGroup(long creatorId, String name) throws Exception {
        return read(postJson("/groups", "{\"name\":\"%s\",\"creatorUserId\":%d}".formatted(name, creatorId))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions join(long userId, String code) throws Exception {
        return postJson("/groups/join", "{\"userId\":%d,\"inviteCode\":\"%s\"}".formatted(userId, code));
    }

    private JsonNode groupsOf(long userId) throws Exception {
        return read(mvc.perform(get("/users/{id}/groups", userId)).andExpect(status().isOk()).andReturn());
    }

    private JsonNode leaderboard(long groupId) throws Exception {
        return read(mvc.perform(get("/groups/{id}/leaderboard", groupId)).andExpect(status().isOk()).andReturn());
    }

    private void completeOn(long userId, String localDateTime) throws Exception {
        clock.setLocal(localDateTime, BUCHAREST);
        mvc.perform(get("/users/{id}/quest/today", userId)).andExpect(status().isOk());
        mvc.perform(post("/users/{id}/quest/today/complete", userId)).andExpect(status().isOk());
    }

    private JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    // ---------- POST /groups ----------

    @Test
    void creatingAGroupReturnsAnInviteCodeAndMakesTheCreatorAMember() throws Exception {
        TestUser maria = user();
        JsonNode group = createGroup(maria.id(), "  Echipa de Joi  ");

        assertTrue(group.get("id").isNumber());
        assertEquals("Echipa de Joi", group.get("name").asText());
        assertTrue(group.get("inviteCode").asText().matches("[A-Z0-9]{6}"), group.get("inviteCode").asText());
        assertEquals(maria.id(), group.get("creatorUserId").asLong());
        assertEquals(1, group.get("memberCount").asInt());
        assertFalse(group.get("createdAt").isNull());

        JsonNode mine = groupsOf(maria.id());
        assertEquals(1, mine.size());
        assertEquals(group.get("id"), mine.get(0).get("id"));
        assertEquals(group.get("inviteCode"), mine.get(0).get("inviteCode"));

        JsonNode board = leaderboard(group.get("id").asLong());
        assertEquals(1, board.size());
        assertEquals(maria.username(), board.get(0).get("username").asText());
    }

    @Test
    void inviteCodesAreUniqueAndAvoidLookalikeCharacters() throws Exception {
        TestUser owner = user();
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            String code = createGroup(owner.id(), "Grup " + i).get("inviteCode").asText();
            codes.add(code);
            assertFalse(code.matches(".*[01OIL].*"), "cod cu caractere ambigue: " + code);
        }
        assertEquals(25, codes.size());
        assertEquals(25, groupsOf(owner.id()).size());
    }

    @Test
    void creatingAGroupValidatesTheNameAndTheCreator() throws Exception {
        TestUser u = user();
        postJson("/groups", "{\"name\":\"   \",\"creatorUserId\":%d}".formatted(u.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        postJson("/groups", "{\"name\":\"%s\",\"creatorUserId\":%d}".formatted("x".repeat(41), u.id()))
                .andExpect(status().isBadRequest());
        postJson("/groups", "{\"name\":\"Fără creator\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.creatorUserId").exists());
        postJson("/groups", "{\"name\":\"Fantomă\",\"creatorUserId\":999999}")
                .andExpect(status().isNotFound());
    }

    // ---------- POST /groups/join ----------

    @Test
    void joiningWithAValidCodeAddsTheUserToTheGroup() throws Exception {
        TestUser owner = user();
        TestUser friend = user();
        JsonNode group = createGroup(owner.id(), "Prietenii");
        String code = group.get("inviteCode").asText();

        join(friend.id(), code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(group.get("id").asLong()))
                .andExpect(jsonPath("$.name").value("Prietenii"))
                .andExpect(jsonPath("$.memberCount").value(2));

        assertEquals(group.get("id"), groupsOf(friend.id()).get(0).get("id"));
        assertEquals(2, groupsOf(owner.id()).get(0).get("memberCount").asInt());
        assertEquals(2, leaderboard(group.get("id").asLong()).size());
    }

    @Test
    void theCodeIsCaseInsensitiveAndToleratesSpacesAndDashes() throws Exception {
        TestUser owner = user();
        TestUser friend = user();
        String code = createGroup(owner.id(), "Tolerant").get("inviteCode").asText();
        String typed = " " + code.substring(0, 3).toLowerCase() + "-" + code.substring(3) + " ";

        join(friend.id(), typed).andExpect(status().isOk());
    }

    @Test
    void joiningWithAnInvalidCodeIs404WithAClearMessage() throws Exception {
        TestUser u = user();
        join(u.id(), "ZZZZZ9")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("ZZZZZ9")));
        assertTrue(groupsOf(u.id()).isEmpty());

        join(u.id(), "   ").andExpect(status().isBadRequest());
    }

    @Test
    void joiningAGroupYouAreAlreadyInIs409() throws Exception {
        TestUser owner = user();
        TestUser friend = user();
        JsonNode group = createGroup(owner.id(), "Deja aici");
        String code = group.get("inviteCode").asText();

        join(friend.id(), code).andExpect(status().isOk());
        join(friend.id(), code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Deja aici")));
        // creatorul e membru din start
        join(owner.id(), code).andExpect(status().isConflict());

        assertEquals(2, leaderboard(group.get("id").asLong()).size());
        assertEquals(1, groupsOf(friend.id()).size());
    }

    @Test
    void aUserCanBeInSeveralGroups() throws Exception {
        TestUser a = user();
        TestUser b = user();
        TestUser me = user();
        String first = createGroup(a.id(), "Unu").get("inviteCode").asText();
        String second = createGroup(b.id(), "Doi").get("inviteCode").asText();
        createGroup(me.id(), "Trei");

        join(me.id(), first).andExpect(status().isOk());
        join(me.id(), second).andExpect(status().isOk());

        List<String> names = new ArrayList<>();
        groupsOf(me.id()).forEach(g -> names.add(g.get("name").asText()));
        assertEquals(List.of("Trei", "Unu", "Doi"), names); // în ordinea intrării
    }

    @Test
    void unknownUserOrGroupIs404() throws Exception {
        join(999999, "ABCDEF").andExpect(status().isNotFound());
        mvc.perform(get("/users/999999/groups")).andExpect(status().isNotFound());
        mvc.perform(get("/groups/999999/leaderboard")).andExpect(status().isNotFound());
    }

    // ---------- GET /groups/{id}/leaderboard ----------

    @Test
    void leaderboardIsSortedByTotalXpWithStreakLevelAndTitle() throws Exception {
        TestUser rookie = user();   // 0 quest-uri
        TestUser casual = user();   // 1 quest: maximum 35 XP
        TestUser grinder = user();  // 3 zile la rând: minimum 10+12+14 = 36 XP

        JsonNode group = createGroup(rookie.id(), "Clasament");
        String code = group.get("inviteCode").asText();
        join(casual.id(), code).andExpect(status().isOk());
        join(grinder.id(), code).andExpect(status().isOk());

        completeOn(casual.id(), "2026-06-10T12:00");
        completeOn(grinder.id(), "2026-06-10T12:00");
        completeOn(grinder.id(), "2026-06-11T12:00");
        completeOn(grinder.id(), "2026-06-12T12:00");

        clock.setLocal("2026-06-12T20:00", BUCHAREST);
        JsonNode board = leaderboard(group.get("id").asLong());

        assertEquals(3, board.size());
        assertEquals(List.of(grinder.username(), casual.username(), rookie.username()),
                List.of(board.get(0).get("username").asText(), board.get(1).get("username").asText(),
                        board.get(2).get("username").asText()));
        assertEquals(List.of(1, 2, 3), List.of(board.get(0).get("rank").asInt(), board.get(1).get("rank").asInt(),
                board.get(2).get("rank").asInt()));

        JsonNode first = board.get(0);
        assertEquals(grinder.id(), first.get("userId").asLong());
        assertTrue(first.get("totalXp").asInt() >= 36);
        assertTrue(first.get("totalXp").asInt() > board.get(1).get("totalXp").asInt());
        assertEquals(3, first.get("streak").asInt());
        assertTrue(first.get("completedToday").asBoolean());
        assertTrue(first.get("level").asInt() >= 1);
        assertFalse(first.get("title").asText().isBlank());

        // casual a completat pe 10, iar pe 11 nu: pe 12 streak-ul lui e 0 (calculat cu StreakCalculator)
        assertEquals(0, board.get(1).get("streak").asInt());
        assertEquals(0, board.get(2).get("totalXp").asInt());
        assertEquals(1, board.get(2).get("level").asInt());
        assertEquals("Cartof de Canapea", board.get(2).get("title").asText());
    }

    @Test
    void membersWithEqualXpShareTheRank() throws Exception {
        TestUser a = user();
        TestUser b = user();
        TestUser c = user();
        JsonNode group = createGroup(a.id(), "Egalitate");
        join(b.id(), group.get("inviteCode").asText()).andExpect(status().isOk());
        join(c.id(), group.get("inviteCode").asText()).andExpect(status().isOk());
        completeOn(c.id(), "2026-06-10T12:00");

        JsonNode board = leaderboard(group.get("id").asLong());
        assertEquals(c.username(), board.get(0).get("username").asText());
        assertEquals(1, board.get(0).get("rank").asInt());
        // a și b au 0 XP: amândoi pe locul 2, ordonați după username
        assertEquals(2, board.get(1).get("rank").asInt());
        assertEquals(2, board.get(2).get("rank").asInt());
        assertEquals(a.username(), board.get(1).get("username").asText());
    }
}
