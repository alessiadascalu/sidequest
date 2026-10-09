# SideQuest 🧭

**O mini-misiune pe zi.** SideQuest îți dă în fiecare zi un quest scurt și amuzant (de exemplu „urcă pe scări în loc de lift” sau „scrie-i unui prieten pe care nu l-ai mai văzut de mult”). Îl bifezi, opțional cu o poză sau câteva cuvinte drept dovadă, îți crești **streak-ul**, câștigi **XP** și urci de nivel, de la „Cartof de Canapea” până la „Boss Final”. Cu prietenii te poți întrece în grupuri, pe un leaderboard comun.

🔗 **Live:** <https://sidequest-eta-murex.vercel.app>

> ⏳ Backend-ul rulează pe planul gratuit Render, care **adoarme după 15 minute de inactivitate**. Prima cerere după o pauză poate dura **până la ~50 de secunde**, cât timp serverul se trezește (aplicația afișează un mesaj). După aceea, totul merge normal.

---

## Cuprins

- [Funcționalități](#funcționalități)
- [Stack tehnic](#stack-tehnic)
- [Decizie tehnică: Streak Engine pe `LocalDate` + `ZoneId`, nu UTC](#decizie-tehnică-streak-engine-pe-localdate--zoneid-nu-utc)
- [Rulare locală](#rulare-locală)
- [Deploy în producție](#deploy-în-producție)
- [Structura proiectului](#structura-proiectului)
- [API](#api)
- [Cum funcționează](#cum-funcționează)
- [Teste](#teste)

---

## Funcționalități

- **Quest zilnic.** Un quest pe zi, ales din 30 de misiuni în 4 categorii. Se preferă quest-uri pe care nu le-ai mai primit; după ce le-ai primit pe toate, rotația reîncepe.
- **Streak.** Zile consecutive cu quest completat, socotite în **fusul tău orar**. Dacă azi nu l-ai făcut încă, dar ieri da, streak-ul e încă viu până la miezul nopții. Recordul personal se păstrează.
- **XP și niveluri.** XP în funcție de dificultate (Ușor 10 / Mediu 20 / Greu 35), plus un bonus de streak (+2 XP pe fiecare zi de streak după prima, maximum +20). Opt niveluri cu titluri: Cartof de Canapea → Ucenic Curios → Explorator de Cartier → Vânător de Misiuni → Cavaler al Rutinei → Maestru al Side-Quest-urilor → Legendă Locală → Boss Final.
- **Dovezi la completare.** Opțional, o poză (JPG, PNG, GIF, WebP, max. 10 MB) și/sau un text scurt (max. 500 de caractere).
- **Istoric.** Timeline cu toate quest-urile completate, grupate pe zile: categorie, XP câștigat și dovada.
- **Grupuri cu leaderboard.** Creezi un grup și primești un **cod de invitație** de 6 caractere pe care îl trimiți prietenilor. Leaderboard-ul sortează membrii după XP total, cu nivel, titlu și streak curent. Locul 1 primește coroană și card auriu (și confetti, dacă ești tu). Poți fi în oricâte grupuri.
- **Login fără parolă.** Username-ul e contul (fără diferență între majuscule și minuscule): un username existent te loghează, unul nou creează contul.
- **UI.** Dark mode cu accente neon, animații Framer Motion (tranziții, bară de XP animată, ecran de level-up), confetti la completare și mascota **Questy**.

## Stack tehnic

| | |
|---|---|
| **Backend** | Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) · Hibernate |
| **Bază de date** | PostgreSQL în producție · H2 (fișier) local, implicit |
| **Frontend** | React 19 · Vite · Framer Motion · canvas-confetti |
| **Teste** | JUnit 5 · Spring Boot Test / MockMvc · H2 în memorie (146 de teste) |
| **Infrastructură** | Render (backend Docker + PostgreSQL) · Vercel (frontend) · GitHub Actions (CI) |

## Decizie tehnică: Streak Engine pe `LocalDate` + `ZoneId`, nu UTC

Un streak e despre **zilele calendaristice ale utilizatorului**, nu despre intervale de 24 de ore. Cu data UTC, „ziua” unui utilizator ar începe și s-ar termina la ore fără legătură cu viața lui:

- Un utilizator din București (UTC+3 vara) care completează la 01:11 noaptea, ora locală, e încă în ziua *de ieri* în UTC. Streak-ul i s-ar rupe, deși a făcut quest-ul „azi”.
- Un utilizator din Los Angeles care completează pe 10 iunie la 19:30 e deja pe 11 iunie în UTC.

Soluția:

1. Fiecare utilizator are un `ZoneId` IANA (ex. `Europe/Bucharest`), trimis de browser la crearea contului.
2. „Azi” = `LocalDate.now(clock.withZone(zoneId))`. `Clock` e injectat, deci testele pot fixa exact ora 23:59 sau 00:01.
3. La atribuire, `QuestAssignment.localDate` se salvează o singură dată și nu se mai schimbă, deci istoricul rămâne stabil.
4. `completedAt` e un `Instant` (momentul real, fără ambiguitate), dar streak-ul nu se calculează după el, ci după `localDate`.
5. `StreakCalculator` lucrează doar cu `LocalDate`: „ziua următoare” e mereu `date.plusDays(1)`. Nu se scad `Instant`-uri și nu se compară cu 24h, pentru că zilele cu schimbare de oră (DST) au 23 sau 25 de ore. De exemplu, 25 oct 00:30 și 26 oct 00:30 (Europe/Bucharest) sunt la 25h distanță, dar sunt zile consecutive.

Un test de mutație (înlocuirea fusului utilizatorului cu UTC) face să pice 18 teste, deci decizia e acoperită de teste.

**Limitări asumate:** dacă un utilizator își schimbă fusul orar, zilele deja atribuite nu se recalculează. Și, fiind fără parolă, oricine scrie un username existent intră în contul respectiv. E un proiect demonstrativ, nu o aplicație cu autentificare.

## Rulare locală

**Cerințe:** JDK 21 și Node 20.19+. Maven nu trebuie instalat (proiectul include Maven Wrapper).

### 1. Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Fără niciun profil activ, backend-ul folosește **H2 în fișier** (`backend/data/sidequest.mv.db`), deci nu ai nevoie de PostgreSQL local. La prima pornire se creează schema și se încarcă cele 30 de quest-uri. Datele rămân între restarturi. Așteaptă linia `Started SideQuestApplication`.

Pozele-dovadă se salvează în `backend/uploads/` (folderul se creează automat). Pentru un start de la zero, șterge `backend/data/` și `backend/uploads/`.

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Deschide <http://localhost:5173>. Fără fișier `.env`, Vite trimite cererile `/api/*` prin proxy la `http://localhost:8080`, deci nu e nevoie de configurare CORS.

Opțional, ca în producție: `cp .env.example .env` (setează `VITE_API_URL=http://localhost:8080`). Frontend-ul cheamă atunci backend-ul direct, iar backend-ul permite implicit originea `http://localhost:5173`.

> **Eroare 502 în browser?** Vite rulează, dar backend-ul nu. Pornește-l (pasul 1).

Alte comenzi: `npm run lint` (oxlint), `npm run build`, `npm run preview`.

### 3. Teste

```bash
cd backend
./mvnw test
```

## Deploy în producție

```
Browser ──► Vercel (frontend static, React)
               │  VITE_API_URL
               ▼
            Render Web Service (Spring Boot, Docker, profil prod)
               │  DB_* (legate automat)
               ▼
            Render PostgreSQL
```

Ordinea: întâi backend-ul (ca să ai URL-ul API-ului), apoi frontend-ul, apoi `FRONTEND_URL` pe backend (CORS).

### 1. Backend + PostgreSQL pe Render

Render nu are runtime nativ pentru Java, așa că backend-ul rulează ca imagine **Docker**: `backend/Dockerfile` face build-ul cu Maven Wrapper și pornește jar-ul cu profilul `prod`.

1. În Render: **New → Blueprint** și alegi repo-ul. Render citește **`render.yaml`** din rădăcină și creează:
   - `sidequest-db`: PostgreSQL, plan gratuit;
   - `sidequest-api`: Web Service Docker, plan gratuit, cu variabilele bazei de date legate automat și health check pe `/actuator/health`.
2. `FRONTEND_URL` poate rămâne gol deocamdată (se completează la pasul 3).
3. După build, `https://<serviciu>.onrender.com/actuator/health` trebuie să răspundă `{"status":"UP"}`. Schema și quest-urile se creează singure la prima pornire.

Fără Blueprint: creezi manual un PostgreSQL și un Web Service Docker (Root Directory `backend`), apoi setezi `DATABASE_URL` cu *Internal Database URL*; backend-ul îl transformă singur în setări JDBC.

**Variabile de mediu (backend, Render):**

| Variabilă | Ce e |
|---|---|
| `FRONTEND_URL` | **Necesară.** Originile care pot apela API-ul (CORS), separate prin virgulă, cu `*` permis. Ex.: `https://sidequest-eta-murex.vercel.app,https://sidequest-*.vercel.app` (al doilea acoperă preview-urile Vercel). Fără `/` la final. Implicit: `http://localhost:5173` |
| `SPRING_PROFILES_ACTIVE` | `prod` (setată deja în `Dockerfile` și `render.yaml`). Activează PostgreSQL |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexiunea la PostgreSQL, legată automat de Blueprint. Alternativ: `DATABASE_URL` (dacă sunt setate ambele, câștigă `DB_*`) |
| `PORT` | Setată de Render; **nu o seta manual** (local: 8080) |
| `UPLOADS_DIR` | Opțională. Unde se salvează pozele (implicit `uploads/`) |

### 2. Frontend pe Vercel

1. **Add New → Project** și imporți repo-ul.
2. **Root Directory**: `frontend`. Vercel detectează singur Vite, build-ul (`npm run build`) și output-ul (`dist`).
3. **Environment Variables:** `VITE_API_URL` = URL-ul backend-ului de pe Render, fără `/` la final (ex. `https://sidequest-api.onrender.com`). Bifează Production și Preview.
4. **Deploy.**

`VITE_API_URL` e citită **la build**: dacă o schimbi, fă **Redeploy**. Un build fără ea pică intenționat, cu un mesaj clar, în loc să publice un site care nu merge.

### 3. Legătura între ele

Pune URL-ul Vercel în `FRONTEND_URL` pe Render (**Environment → Save, rebuild and deploy**). Fără pasul ăsta, browserul blochează cererile (eroare CORS în consolă).

### Limitări ale planurilor gratuite

- **Pornire lentă:** serviciul Render adoarme după **15 minute** fără trafic; prima cerere după aceea poate dura **până la ~50 de secunde**.
- **Pozele-dovadă nu sunt persistente.** Discul Render e efemer: fișierele din `uploads/` se pierd la fiecare redeploy și la fiecare repornire (inclusiv la trezirea din somn). Textul dovezii, XP-ul, streak-ul, istoricul și grupurile sunt în PostgreSQL și rămân. O poză pierdută apare în aplicație ca „📷 poză pierdută”. Soluția pe termen lung: storage extern (S3 / Cloudflare R2).
- **Baza PostgreSQL gratuită de pe Render expiră** după o perioadă limitată (verifică politica actuală Render).

## Structura proiectului

```
SideQuestApp/
├── backend/                     Spring Boot (Java 21)
│   ├── Dockerfile               imaginea pentru Render (build Maven + runtime JRE)
│   ├── pom.xml, mvnw            Maven Wrapper
│   └── src/
│       ├── main/java/dev/sidequest/
│       │   ├── config/          Clock, CORS, conversia DATABASE_URL → JDBC
│       │   ├── domain/          entități JPA: User, Quest, QuestAssignment, Group, GroupMembership
│       │   ├── repository/      Spring Data JPA
│       │   ├── service/         QuestService, UserService (logica de business)
│       │   ├── streak/          StreakCalculator (Streak Engine)
│       │   ├── xp/              XpService + XpStrategy, LevelService
│       │   ├── group/           GroupService, leaderboard, coduri de invitație
│       │   ├── storage/         salvarea pozelor-dovadă
│       │   ├── seed/            cele 30 de quest-uri
│       │   └── web/             controllere REST, DTO-uri, tratarea erorilor
│       ├── main/resources/      application.properties (H2), application-prod.properties (PostgreSQL)
│       └── test/                JUnit 5
├── frontend/                    React 19 + Vite
│   ├── src/
│   │   ├── App.jsx              ecrane și tab-uri (Azi, Istoric, Grupuri)
│   │   ├── components/          componente UI
│   │   └── api.js               clientul HTTP (VITE_API_URL sau proxy /api)
│   ├── vite.config.js           proxy-ul de dev și verificarea VITE_API_URL la build
│   └── .env.example
├── render.yaml                  Blueprint Render: backend (Docker) + PostgreSQL
└── .github/workflows/ci.yml     rulează testele la fiecare push și pull request
```

## API

| Metodă | Endpoint | Ce face |
|---|---|---|
| `POST` | `/users` | **Login sau creare.** Body: `{"username": "maria", "zoneId": "Europe/Bucharest"}`. Username existent → `200`; nou → `201` + `Location`. Răspunsul conține profilul (XP, nivel, streak), `created` și `history` |
| `GET` | `/users/{id}/quest/today` | Quest-ul zilei locale a utilizatorului (creat la prima cerere), cu `xpReward`, streak și `proof` dacă e deja completat |
| `POST` | `/users/{id}/quest/today/complete` | Completează quest-ul: acordă XP, actualizează streak-ul și nivelul. Fără body sau `multipart/form-data` cu `photo` și `proofText` (opționale). A doua oară în aceeași zi → `409` |
| `GET` | `/users/{id}/profile` | XP total, nivel + titlu, progres către nivelul următor, streak curent și record |
| `GET` | `/users/{id}/history` | Quest-urile completate, cele mai noi primele, cu `xpAwarded` și `proof: {text, imageUrl}` |
| `GET` | `/uploads/{fișier}` | Poza-dovadă (URL-ul vine în `proof.imageUrl`) |
| `POST` | `/groups` | Creează un grup. Body: `{"name": "Gașca", "creatorUserId": 1}` → `201` cu `inviteCode`. Creatorul devine automat membru |
| `POST` | `/groups/join` | Intră într-un grup. Body: `{"userId": 2, "inviteCode": "K7QX2M"}`. Cod inexistent → `404`; deja membru → `409` |
| `GET` | `/users/{id}/groups` | Grupurile utilizatorului |
| `GET` | `/groups/{id}/leaderboard` | Membrii sortați după XP: `rank`, `username`, `totalXp`, `level`, `title`, `streak`, `completedToday` |
| `GET` | `/actuator/health` | Health check (folosit de Render) |

Exemplu de completare cu dovadă:

```bash
curl -X POST http://localhost:8080/users/1/quest/today/complete \
     -F "photo=@poza.jpg" -F "proofText=Am urcat 12 etaje pe scări"
```

Erorile vin ca `ProblemDetail` (RFC 9457): `404` resursă inexistentă, `400` validare (cu `errors` pe câmpuri), `409` conflict, `413` poză peste 10 MB.

## Cum funcționează

- **Quest-ul zilei:** un rând `QuestAssignment` per utilizator și zi, garantat de `UNIQUE(user_id, local_date)`.
- **XP (Strategy pattern):** `XpService` însumează toate bean-urile `XpStrategy` (`DifficultyXpStrategy`, `StreakBonusXpStrategy`). O regulă nouă înseamnă doar un `@Component` nou, fără modificări în `XpService`.
- **Niveluri:** nivelul *n* începe la `50·n·(n−1)` XP (nivel 2 la 100 XP, nivel 3 la 300, nivel 4 la 600…).
- **Concurență:** `@Version` pe `QuestAssignment` face ca două completări simultane să acorde XP o singură dată (a doua primește `409`). Dacă două cereri creează simultan același quest al zilei sau același username, cererea care pierde prinde violarea `UNIQUE` și citește rândul creat de cealaltă.
- **Dovezi:** pozele se salvează sub un nume generat (`<uuid>.<ext>`). Formatul se verifică după primii octeți ai fișierului, nu după `Content-Type`, deci un HTML deghizat în `.png` e respins. Endpoint-ul de servire acceptă doar nume generate (fără `../`) și trimite `X-Content-Type-Options: nosniff`. Dacă completarea eșuează după ce poza a fost scrisă, fișierul e șters.
- **Grupuri:** codurile de invitație au 6 caractere dintr-un alfabet fără caractere care se confundă (`0/O`, `1/I/L`), sunt generate cu `SecureRandom` și protejate de `UNIQUE(invite_code)`. Tabela se numește `quest_groups`, pentru că `GROUP` e cuvânt rezervat în SQL.
- **Leaderboard:** sortare după XP, apoi după streak, apoi după username; la XP egal se împarte locul (1, 1, 3). Streak-ul fiecărui membru e calculat în fusul lui orar, iar zilele completate ale tuturor membrilor vin dintr-un singur query.

## Teste

`./mvnw test` rulează **146 de teste**, iar GitHub Actions le rulează la fiecare push și pull request:

- **`StreakCalculatorTest`** (48): zi ratată, completare la 23:59 vs 00:01, fusuri orare extreme (Kiritimati UTC+14, Pago Pago UTC−11, Kolkata UTC+5:30), DST primăvara și toamna în București și New York, granițe de lună/an/an bisect.
- **`XpServiceTest`, `LevelServiceTest`:** recompense, plafonul bonusului, granițele nivelurilor, titluri.
- **`SideQuestApiTest`:** fluxul HTTP complet cu H2 în memorie și `Clock` controlabil: streak peste miezul nopții și peste DST, login, istoric, dovezi (poză, text, fișiere invalide, fără poze orfane), cereri concurente, rotația quest-urilor.
- **`GroupApiTest`:** creare grup, join cu cod valid/invalid, membru dublu, ordinea și locurile împărțite din leaderboard.
- **Deploy:** conversia `DATABASE_URL` → JDBC, CORS (origine exactă și pattern de preview Vercel permise, alte origini respinse), doar `/actuator/health` expus.
- **`QuestSeederTest`, `QuestAssignmentConstraintTest`, `InviteCodeGeneratorTest`:** seed-ul, constrângerile `UNIQUE`, formatul codurilor.
