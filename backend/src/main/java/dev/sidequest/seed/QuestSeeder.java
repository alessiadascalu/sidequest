package dev.sidequest.seed;

import dev.sidequest.domain.Quest;
import dev.sidequest.repository.QuestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static dev.sidequest.domain.Category.CREATIV;
import static dev.sidequest.domain.Category.FITNESS_EXPLORE;
import static dev.sidequest.domain.Category.FOCUS;
import static dev.sidequest.domain.Category.SOCIAL;
import static dev.sidequest.domain.Difficulty.EASY;
import static dev.sidequest.domain.Difficulty.HARD;
import static dev.sidequest.domain.Difficulty.MEDIUM;

/** La primul start (tabel gol) încarcă cele 30 de quest-uri. Rulează o singură dată per bază de date. */
@Component
public class QuestSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuestSeeder.class);

    // Metodă, nu constantă: entitățile JPA primesc ID la salvare, deci instanțele nu se refolosesc între rulări.
    static List<Quest> seedQuests() {
        return List.of(
            // ---- Social (8) ----
            new Quest("Fă-i un compliment sincer unui coleg. Bonus: fără să sune a mesaj de la bancă.", SOCIAL, EASY),
            new Quest("Sună un prieten pe care îl vezi doar la nunți și la înmormântări. Nu, mesajul vocal nu se pune.", SOCIAL, MEDIUM),
            new Quest("Spune „bună ziua” vecinului de la lift și nu te holba în telefon. Da, se poate.", SOCIAL, EASY),
            new Quest("Trimite-i unui prieten un meme atât de specific, încât să știe clar că te-ai gândit la el.", SOCIAL, EASY),
            new Quest("Invită pe cineva la o cafea. „Ne auzim noi” nu e o invitație, e o amenințare politicoasă.", SOCIAL, MEDIUM),
            new Quest("Întreabă un om pe care abia îl cunoști ce muzică ascultă. Fii pregătit pentru povestea vieții lui.", SOCIAL, HARD),
            new Quest("Mulțumește-i cuiva care nu se așteaptă: curier, casieră, tanti de la poartă. Cu ochi în ochi.", SOCIAL, EASY),
            new Quest("Scrie-i un „mă gândeam la tine” cuiva cu care nu ai vorbit de peste un an. Curaj, nu mușcă mesajul.", SOCIAL, HARD),

            // ---- Fitness / Explore (8) ----
            new Quest("Fă 20 de genuflexiuni cât fierbe apa. Apa nu așteaptă, mușchii tăi nici.", FITNESS_EXPLORE, EASY),
            new Quest("Ia-o pe un drum nou spre casă, fără GPS. Dacă te pierzi, se numește „explorare”.", FITNESS_EXPLORE, MEDIUM),
            new Quest("Urcă pe scări în loc de lift. Liftul se descurcă și fără tine azi.", FITNESS_EXPLORE, EASY),
            new Quest("Plimbă-te 30 de minute fără căști. Orașul are coloană sonoră proprie și nu are reclame.", FITNESS_EXPLORE, MEDIUM),
            new Quest("Găsește un parc sau o stradă în care n-ai fost niciodată și fă o poză drept dovadă.", FITNESS_EXPLORE, MEDIUM),
            new Quest("Fă 5 minute de stretching ca o pisică după un somn de 14 ore. Căscatul e inclus.", FITNESS_EXPLORE, EASY),
            new Quest("Strânge 8.000 de pași azi. Traseul până la frigider se numără, dar doar dacă te și întorci.", FITNESS_EXPLORE, HARD),
            new Quest("Fă o plimbare la apus și nu posta nimic. Da, se poate trăi și așa.", FITNESS_EXPLORE, MEDIUM),

            // ---- Focus (7) ----
            new Quest("Lucrează 25 de minute cu telefonul în altă cameră. Va supraviețui, promitem.", FOCUS, MEDIUM),
            new Quest("Alege UN singur lucru amânat de o lună și fă-l acum. Da, exact ăla la care te-ai gândit.", FOCUS, HARD),
            new Quest("Închide toate tab-urile inutile. „Le citesc mai târziu” nu s-a întâmplat niciodată.", FOCUS, EASY),
            new Quest("Scrie pe hârtie cele 3 priorități ale zilei și taie-le pe rând. Satisfacție garantată sau banii înapoi.", FOCUS, EASY),
            new Quest("Fă 10 minute de respirație conștientă. Gândurile vor veni; tu doar nu le oferi cafea.", FOCUS, MEDIUM),
            new Quest("Stai 45 de minute pe o singură sarcină, fără notificări. Multitasking-ul e un mit, ca „mai am 5 minute”.", FOCUS, HARD),
            new Quest("Fă ordine pe birou sau pe desktop. Un mediu curat, o minte ceva mai puțin panicată.", FOCUS, EASY),

            // ---- Creativ (7) ----
            new Quest("Desenează-ți ziua în 5 minute, în stil „copil de 6 ani cu mult talent”. Ai voie să iasă urât.", CREATIV, EASY),
            new Quest("Scrie o poezie de 4 versuri despre ceva plictisitor (ex.: agrafa). Rima e obligatorie.", CREATIV, MEDIUM),
            new Quest("Gătește ceva fără rețetă, doar din ce găsești prin frigider. Instinct și puțină speranță.", CREATIV, HARD),
            new Quest("Fă o poză artistică unui obiect banal și dă-i un nume pompos, gen „Melancolia lingurii”.", CREATIV, EASY),
            new Quest("Inventează un cocktail (sau mocktail) și dă-i un nume ridicol. Se acceptă și ceaiul.", CREATIV, MEDIUM),
            new Quest("Scrie o poveste în exact 50 de cuvinte, cu final neașteptat. Numărătoarea o faci singur, fără AI.", CREATIV, MEDIUM),
            new Quest("Redecorează un colț din casă cu ce ai la îndemână. Bonus: o plantă care încă trăiește.", CREATIV, HARD));
    }

    private final QuestRepository quests;

    public QuestSeeder(QuestRepository quests) {
        this.quests = quests;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (quests.count() > 0) {
            return;
        }
        List<Quest> seed = seedQuests();
        quests.saveAll(seed);
        log.info("Am încărcat {} quest-uri", seed.size());
    }
}
