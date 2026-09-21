package dev.sidequest.seed;

import dev.sidequest.domain.Category;
import dev.sidequest.domain.Difficulty;
import dev.sidequest.domain.Quest;
import dev.sidequest.repository.QuestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class QuestSeederTest {

    @Autowired QuestRepository quests;
    @Autowired QuestSeeder seeder;

    @Test
    void seedsExactlyThirtyQuests() {
        assertEquals(30, quests.count());
    }

    @Test
    void coversAllFourCategoriesWithSeveralQuestsEach() {
        Map<Category, Long> perCategory = quests.findAll().stream()
                .collect(Collectors.groupingBy(Quest::getCategory, Collectors.counting()));
        assertEquals(EnumSet.allOf(Category.class), perCategory.keySet());
        perCategory.forEach((category, count) -> assertTrue(count >= 7, category + " are doar " + count));
    }

    @Test
    void usesAllDifficultiesAndNoQuestIsDuplicatedOrBlank() {
        List<Quest> all = quests.findAll();
        Set<Difficulty> difficulties = all.stream().map(Quest::getDifficulty).collect(Collectors.toSet());
        assertEquals(EnumSet.allOf(Difficulty.class), difficulties);

        Set<String> texts = new HashSet<>();
        for (Quest q : all) {
            assertFalse(q.getText().isBlank());
            assertTrue(texts.add(q.getText()), "duplicat: " + q.getText());
        }
    }

    @Test
    void questsAreWrittenInRomanianWithDiacriticsIntact() {
        // Dacă encodarea sursei/DB-ului ar strica UTF-8, am vedea „Äƒ” în loc de „ă”.
        Set<Character> chars = quests.findAll().stream()
                .flatMapToInt(q -> q.getText().chars())
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
        assertTrue(chars.contains('ă'));
        assertTrue(chars.contains('ț') || chars.contains('ș'));
        assertFalse(chars.contains('Ã'));
        assertFalse(chars.contains('�'));
    }

    @Test
    void runningTheSeederAgainDoesNotDuplicateQuests() {
        seeder.run(null);
        seeder.run(null);
        assertEquals(30, quests.count());
    }
}
