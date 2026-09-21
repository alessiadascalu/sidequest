# SideQuest 🧭

O mini-misiune pe zi. Primești un quest scurt și amuzant (în română), îl bifezi cu **Completed**, îți crești **streak-ul** și câștigi **XP** ca să urci de nivel: de la „Cartof de Canapea” până la „Boss Final”.

Proiect de 2 zile, fără autentificare reală: îți alegi un username, iar aplicația ține minte id-ul în `localStorage`.

## Structura

```
SideQuestApp/
├── backend/    Java 21 · Spring Boot 3.5 · Spring Data JPA · H2 (fișier) · JUnit 5
└── frontend/   React 19 · Vite · canvas-confetti
```

## Cum rulezi proiectul

**Cerințe:** JDK 21 și Node 20.19+. Maven nu trebuie instalat, e inclus Maven Wrapper.

### 1. Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

La primul start se creează baza de date `backend/data/sidequest.mv.db` și se încarcă cele 30 de quest-uri. Datele rămân între restarturi. Pentru un start de la zero, șterge folderul `backend/data/`.

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Deschide <http://localhost:5173>. Vite trimite cererile `/api/*` către `http://localhost:8080` (prefixul `/api` e scos de proxy), deci nu e nevoie de CORS.

### Teste

```bash
cd backend
./mvnw test
```

## API

| Metodă | Endpoint | Ce face |
|---|---|---|
| `POST` | `/users` | Creează un utilizator. Body: `{"username": "maria", "zoneId": "Europe/Bucharest"}` → `201` |
| `GET` | `/users/{id}/quest/today` | Quest-ul zilei locale a utilizatorului (îl creează la prima cerere). Include `xpReward` și streak-ul |
| `POST` | `/users/{id}/quest/today/complete` | Bifează quest-ul: acordă XP, actualizează streak-ul și nivelul. A doua oară în aceeași zi → `409` |
| `GET` | `/users/{id}/profile` | XP total, nivel + titlu, progres către nivelul următor, streak curent și record |

Erorile vin în format `ProblemDetail` (RFC 9457): `404` utilizator inexistent sau quest neatribuit, `400` validare (cu `errors` pe câmpuri), `409` conflict (username luat / quest deja completat).

## Cum funcționează

- **Quest-ul zilei:** un rând `QuestAssignment` per utilizator și zi, garantat de `UNIQUE(user_id, local_date)`. Se preferă quest-uri pe care utilizatorul nu le-a mai primit; după ce le-a primit pe toate 30, rotația reîncepe.
- **Streak:** zile locale consecutive cu quest completat. Dacă azi nu ai apucat să completezi, dar ieri da, streak-ul e încă în viață (mai ai până la miezul nopții). O zi întreagă ratată îl resetează la 0; recordul (`longest`) se păstrează.
- **XP:** `XpService` însumează mai multe `XpStrategy` (Strategy pattern): `DifficultyXpStrategy` (Ușor 10 / Mediu 20 / Greu 35) și `StreakBonusXpStrategy` (+2 XP pe fiecare zi de streak după prima, maximum +20). O regulă nouă înseamnă doar un `@Component` nou, fără să modifici `XpService`.
- **Niveluri:** nivelul *n* începe la `50·n·(n−1)` XP și are lățimea `100·n` (nivel 2 la 100 XP, nivel 3 la 300, nivel 4 la 600…). Titlurile: Cartof de Canapea → Ucenic Curios → Explorator de Cartier → Vânător de Misiuni → Cavaler al Rutinei → Maestru al Side-Quest-urilor → Legendă Locală → Boss Final.
- **Concurență:** `@Version` pe `QuestAssignment` face ca două „Completed” simultane să acorde XP o singură dată (al doilea primește `409`). Dacă două cereri creează simultan quest-ul zilei, pierzătorul prinde violarea `UNIQUE` și îl citește pe al câștigătorului.

## Decizie: `LocalDate` + `ZoneId`, nu UTC

Un streak e o poveste despre **zile calendaristice ale omului**, nu despre intervale de 24 de ore. Dacă am folosi data UTC, „ziua” unui utilizator ar începe și s-ar termina la ore care nu au legătură cu viața lui:

- Un utilizator din București (UTC+3 vara) care completează la 01:11 dimineața, ora locală, are în UTC data de *ieri* și și-ar rupe streak-ul, deși a făcut quest-ul „azi”.
- Un utilizator din Los Angeles care completează la 19:30 pe 10 iunie e deja pe 11 iunie în UTC.

Ce facem în schimb:

1. Fiecare utilizator are un `ZoneId` IANA (ex. `Europe/Bucharest`).
2. „Azi” = `LocalDate.now(clock.withZone(zoneId))`. `Clock` e injectat, deci testele pot fixa exact 23:59 sau 00:01.
3. La atribuire, `QuestAssignment.localDate` se salvează o dată și nu se mai schimbă. Istoricul rămâne stabil chiar dacă utilizatorul își schimbă fusul orar ulterior.
4. `completedAt` e un `Instant` (momentul real, fără ambiguitate), dar **nu** el decide streak-ul, ci `localDate`.
5. `StreakCalculator` lucrează doar cu `LocalDate`: „ziua următoare” e mereu `date.plusDays(1)`. Nu scădem `Instant`-uri și nu comparăm cu 24h, pentru că zilele cu DST au 23 sau 25 de ore. De exemplu, 25 oct 00:30 și 26 oct 00:30 (Europa/București) sunt la 25h distanță, deși sunt zile consecutive.

**Limitări asumate** (scope de 2 zile): dacă un utilizator își schimbă fusul orar, zilele deja atribuite nu se recalculează. Și, fără auth, oricine cunoaște un `id` poate cere datele acelui utilizator.

## Teste

`./mvnw test` rulează 106 teste:

- **`StreakCalculatorTest`** (48): zi ratată, completare la **23:59 vs 00:01**, fusuri orare diferite (același instant → zile diferite; Kiritimati UTC+14, Pago Pago UTC−11, Kolkata UTC+5:30), **DST** primăvară și toamnă în București și New York (ziua de 23h/25h, ora „inexistentă” și ora repetată), granițe de an/lună/an bisect, date din viitor, duplicate. Un test de mutație (înlocuirea fusului utilizatorului cu UTC) pică 18 teste.
- **`XpServiceTest`, `LevelServiceTest`**: recompense, plafon de bonus, granițele nivelurilor, titluri.
- **`SideQuestApiTest`**: fluxul HTTP complet cu H2 în memorie și `Clock` mutabil (streak peste miezul nopții și peste DST, zi ratată, două cereri concurente, rotația celor 30 de quest-uri).
- **`QuestSeederTest`, `QuestAssignmentConstraintTest`**: seed-ul (30 de quest-uri, 4 categorii, diacritice intacte) și constrângerile `UNIQUE`.

CI: `.github/workflows/ci.yml` rulează `mvn test` la fiecare push și pull request.
