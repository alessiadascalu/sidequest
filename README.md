# SideQuest 🧭

O mini-misiune pe zi. Primești un quest scurt și amuzant (în română), îl bifezi cu **Completed** (opțional cu o poză sau câteva cuvinte drept dovadă), îți crești **streak-ul** și câștigi **XP** ca să urci de nivel: de la „Cartof de Canapea” până la „Boss Final”. Tot ce ai completat rămâne în tab-ul **Istoric**.

Fără parolă: **username-ul e contul**. Dacă scrii un username care există deja, intri direct în contul lui, cu tot cu XP, streak și istoric. Dacă nu există, se creează. Aplicația ține minte id-ul în `localStorage`.

## Ce e nou în Faza 2: grupuri

- **Grupuri cu prietenii:** creezi un grup și primești un **cod de invitație** de 6 caractere, pe care îl trimiți prietenilor. Ei intră în grup cu codul. Un utilizator poate fi în oricâte grupuri.
- **Leaderboard pe grup:** membrii sortați după XP total, cu nivel, titlu și streak-ul curent. Locul 1 primește coroană, trofeu și card auriu, iar tu ești evidențiat cu violet. Dacă ești pe locul 1 într-un grup cu concurență, te întâmpină o ploaie de confetti aurii.
- **Tab nou „Grupuri”:** fără grupuri vezi cele două opțiuni („Creează un grup” / „Alătură-te unui grup”). Cu grupuri, le alegi dintr-o listă și vezi leaderboard-ul. După creare, codul apare mare, cu buton de copiere (și de „Trimite”, pe telefoanele care suportă share).

## Ce e nou în Faza 1

- **Fix login:** `POST /users` nu mai răspunde cu `409 username deja luat`. Username existent (indiferent de majuscule) → `200` cu datele utilizatorului; username nou → `201` cu contul creat.
- **Istoric:** `GET /users/{id}/history` și un tab nou „Istoric”, cu timeline grupat pe zile, iconițe pe categorie, XP câștigat și dovada.
- **Dovadă la completare:** poză (salvată local în `backend/uploads/`) și/sau text scurt, ambele opționale. Pozele se servesc prin `GET /uploads/{fișier}`.
- **Frontend rescris:** dark mode cu accente neon, Framer Motion peste tot (tranziții între ecrane și tab-uri, bară de XP care se umple animat, confetti la completare, ecran dedicat de level-up), mascota **Questy**, bottom sheet pentru dovadă, lightbox pentru poze.

## Structura

```
SideQuestApp/
├── backend/      Java 21 · Spring Boot 3.5 · Spring Data JPA · H2 local / PostgreSQL în prod · JUnit 5
│   └── Dockerfile     imaginea pentru Render
├── frontend/     React 19 · Vite · Framer Motion · canvas-confetti
│   └── .env.example   VITE_API_URL
└── render.yaml   Blueprint Render: backend (Docker) + PostgreSQL
```

Deploy gratuit: **backend pe Render** (Web Service + PostgreSQL), **frontend pe Vercel**. Vezi [Deploy](#deploy).

## Cum rulezi proiectul

**Cerințe:** JDK 21 și Node 20.19+. Maven nu trebuie instalat, e inclus Maven Wrapper.

### 1. Backend (port 8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

La primul start se creează baza de date `backend/data/sidequest.mv.db` și se încarcă cele 30 de quest-uri. Datele rămân între restarturi.

Tot la pornire se creează automat folderul **`backend/uploads/`**, unde se salvează pozele-dovadă (nu trebuie creat de mână; e în `.gitignore`). Locația se poate schimba cu proprietatea `sidequest.uploads-dir`, de exemplu `./mvnw spring-boot:run -Dspring-boot.run.arguments=--sidequest.uploads-dir=/cale/alta`. Limita pentru o poză e de 10 MB.

**Ai deja o bază de date dintr-o versiune anterioară?** Nu trebuie ștearsă: Hibernate (`ddl-auto=update`) adaugă singur coloanele noi din Faza 1 (`xp_awarded`, `proof_text`, `proof_image_path`) și tabelele noi din Faza 2 (`quest_groups`, `group_memberships`). Quest-urile completate înainte de Faza 1 apar în istoric fără XP (nu era salvat per quest) și fără dovadă.

Pentru un start de la zero, șterge folderele `backend/data/` și `backend/uploads/`.

### 2. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Deschide <http://localhost:5173>. Fără fișier `.env`, Vite trimite cererile `/api/*` către `http://localhost:8080` (prefixul `/api` e scos de proxy), deci nu e nevoie de CORS. Tot prin proxy vin și pozele (`/api/uploads/...`).

Varianta cu adresa backend-ului explicită, ca în producție: `cp .env.example .env` (conține `VITE_API_URL=http://localhost:8080`). Frontend-ul cheamă atunci backend-ul direct, iar backend-ul permite implicit originea `http://localhost:5173` (CORS). Dacă Vite pornește pe alt port, pornește backend-ul cu `FRONTEND_URL=http://localhost:<port>`.

**Eroare 502 în browser?** Înseamnă că Vite rulează, dar backend-ul nu. Pornește-l (pasul 1) și așteaptă linia `Started SideQuestApplication` înainte să deschizi aplicația.

Dacă aveai deja proiectul instalat, rulează din nou `npm install`: s-a adăugat dependența `framer-motion`. Alte comenzi utile: `npm run lint` (oxlint) și `npm run build`.

### Teste

```bash
cd backend
./mvnw test
```

## Deploy

Ordinea contează: întâi backend-ul (ca să ai URL-ul API-ului), apoi frontend-ul, apoi înapoi pe Render ca să-i spui backend-ului adresa frontend-ului (CORS).

### 1. Backend + baza de date pe Render

Render nu are runtime nativ pentru Java, așa că backend-ul rulează ca imagine **Docker**. `backend/Dockerfile` face build-ul cu Maven Wrapper (`./mvnw package`) și pornește jar-ul cu profilul `prod`. Nu trebuie completate comenzi de build/start în dashboard.

**Varianta recomandată: Blueprint (`render.yaml`)**

1. Urcă repo-ul pe GitHub.
2. În Render: **New → Blueprint**, alegi repo-ul. Render citește `render.yaml` și creează:
   - `sidequest-db`: PostgreSQL, plan gratuit;
   - `sidequest-api`: Web Service Docker, plan gratuit, cu variabilele bazei de date legate automat.
3. Când îți cere `FRONTEND_URL`, poți lăsa gol deocamdată (îl completezi la pasul 3).
4. Primul build durează câteva minute. La final, `https://<nume>.onrender.com/actuator/health` trebuie să răspundă `{"status":"UP"}`. Schema bazei de date și cele 30 de quest-uri se creează singure la prima pornire (`ddl-auto=update`).

**Varianta manuală (fără Blueprint)**

1. **New → PostgreSQL** (plan Free). Din pagina bazei copiezi **Internal Database URL** (`postgresql://user:parolă@host/db`).
2. **New → Web Service** → repo-ul → Language/Runtime **Docker**, **Root Directory** `backend`, Dockerfile path `./Dockerfile`, **Health Check Path** `/actuator/health`.
3. Variabile de mediu: `DATABASE_URL` = URL-ul copiat la pasul 1 și `FRONTEND_URL` (vezi mai jos). Backend-ul desface singur `DATABASE_URL` în setările JDBC.

**Variabile de mediu pe Render (backend)**

| Variabilă | Obligatorie | Ce e |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | da (deja setată) | `prod`. Setată în `Dockerfile` și în `render.yaml`. Activează PostgreSQL |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | da, **sau** `DATABASE_URL` | Conexiunea la PostgreSQL. Cu Blueprint-ul sunt legate automat de `sidequest-db` |
| `DATABASE_URL` | alternativă la `DB_*` | `postgresql://user:parolă@host:port/db` (formatul din dashboard-ul Render). Dacă e setat și `DB_HOST`, câștigă `DB_*` |
| `FRONTEND_URL` | da, pentru frontend-ul deployat | Originile care pot apela API-ul (CORS), separate prin virgulă, cu `*` permis. Ex.: `https://sidequest.vercel.app,https://sidequest-*.vercel.app` (al doilea acoperă preview-urile Vercel). Fără `/` la final. Implicit: `http://localhost:5173` |
| `PORT` | **nu o seta** | Render o setează singur; serverul ascultă pe ea (local: 8080) |
| `UPLOADS_DIR` | nu | Unde se salvează pozele (implicit `uploads/` în container) |

### 2. Frontend pe Vercel

1. În Vercel: **Add New → Project**, imporți repo-ul.
2. **Root Directory**: `frontend`. Framework-ul (Vite), build-ul (`npm run build`) și output-ul (`dist`) sunt detectate automat.
3. **Environment Variables**: `VITE_API_URL` = URL-ul backend-ului de pe Render, **fără `/` la final**, ex. `https://sidequest-api.onrender.com`. Bifează Production și Preview.
4. **Deploy.**

`VITE_API_URL` e citită **la build** și inclusă în JavaScript-ul generat: dacă o schimbi, fă **Redeploy**. Un build pe Vercel fără ea pică intenționat, cu un mesaj clar, în loc să publice un site care nu merge.

| Variabilă (Vercel) | Exemplu |
|---|---|
| `VITE_API_URL` | `https://sidequest-api.onrender.com` |

### 3. Leagă-le între ele

Copiază URL-ul Vercel (ex. `https://sidequest.vercel.app`) în `FRONTEND_URL` pe Render (**Environment → Save, rebuild and deploy**). Fără pasul ăsta browserul blochează cererile (eroare CORS în consolă).

### Limitări ale planurilor gratuite (asumate)

- **Pozele-dovadă nu sunt persistente.** Discul unui serviciu Render e efemer: fișierele din `uploads/` se pierd **la fiecare redeploy și la fiecare restart**. Pe planul gratuit serviciul adoarme după ~15 minute fără trafic, iar la trezire pornește un container nou, deci pozele se vor pierde **destul de des**. Textul dovezii, XP-ul, streak-ul, istoricul și grupurile sunt în PostgreSQL și rămân. În aplicație, o poză pierdută apare ca „📷 poză pierdută”, nu ca imagine stricată. E un compromis acceptat pentru acum; soluția pe termen lung e un storage extern (ex. S3/Cloudflare R2) sau un Persistent Disk Render (doar pe planurile plătite).
- **Pornire lentă după inactivitate.** Prima cerere după ce serviciul a adormit poate dura ~1 minut. Frontend-ul afișează atunci un mesaj că serverul se trezește.
- **Baza PostgreSQL gratuită de pe Render expiră** după o perioadă limitată (la momentul scrierii, 30 de zile de la creare; verifică politica actuală Render). Înainte de expirare, fă upgrade sau exportă datele.
- H2 rămâne în jar (e baza implicită locală), dar în profilul `prod` nu e folosit.

## API

| Metodă | Endpoint | Ce face |
|---|---|---|
| `POST` | `/users` | **Login sau creare.** Body: `{"username": "maria", "zoneId": "Europe/Bucharest"}`. Username existent (case-insensitive) → `200`; nou → `201` + `Location`. Răspunsul conține profilul complet (XP, nivel, streak), `created` și `history` |
| `GET` | `/users/{id}/quest/today` | Quest-ul zilei locale a utilizatorului (îl creează la prima cerere). Include `xpReward`, streak-ul și `proof` dacă e deja completat |
| `POST` | `/users/{id}/quest/today/complete` | Bifează quest-ul: acordă XP, actualizează streak-ul și nivelul. Fără body sau `multipart/form-data` cu câmpurile opționale `photo` (fișier) și `proofText` (max. 500 caractere). A doua oară în aceeași zi → `409` |
| `GET` | `/users/{id}/profile` | XP total, nivel + titlu, progres către nivelul următor, streak curent și record |
| `GET` | `/users/{id}/history` | Quest-urile completate, cele mai noi primele: dată, `completedAt`, quest (text, categorie, dificultate), `xpAwarded`, `proof: {text, imageUrl}` sau `null` |
| `GET` | `/uploads/{fișier}` | Poza-dovadă salvată (URL-ul vine în `proof.imageUrl`) |
| `POST` | `/groups` | Creează un grup. Body: `{"name": "Gașca", "creatorUserId": 1}` → `201` cu `id`, `name`, `inviteCode`, `createdAt`, `creatorUserId`, `memberCount`, `joinedAt`. Creatorul devine automat membru |
| `POST` | `/groups/join` | Intră într-un grup. Body: `{"userId": 2, "inviteCode": "K7QX2M"}` → `200` cu grupul. Codul e acceptat și cu litere mici, spații sau cratimă. Cod inexistent → `404`; deja membru → `409` |
| `GET` | `/users/{id}/groups` | Grupurile utilizatorului, în ordinea în care a intrat în ele (cu `inviteCode` și `memberCount`) |
| `GET` | `/groups/{id}/leaderboard` | Membrii grupului, sortați descrescător după `totalXp`: `rank`, `userId`, `username`, `totalXp`, `level`, `title`, `streak` (curent), `completedToday` |

La login pentru un utilizator existent, `zoneId` din body e ignorat: zilele deja atribuite depind de fusul salvat la creare.

Exemplu de completare cu dovadă:

```bash
curl -X POST http://localhost:8080/users/1/quest/today/complete \
     -F "photo=@poza.jpg" -F "proofText=Am urcat 12 etaje pe scări"
```

Erorile vin în format `ProblemDetail` (RFC 9457): `404` utilizator/poză inexistentă sau quest neatribuit, `400` validare (cu `errors` pe câmpuri; și fișier care nu e poză, text de dovadă prea lung), `409` quest deja completat, `413` poză mai mare de 10 MB.

## Cum funcționează

- **Quest-ul zilei:** un rând `QuestAssignment` per utilizator și zi, garantat de `UNIQUE(user_id, local_date)`. Se preferă quest-uri pe care utilizatorul nu le-a mai primit; după ce le-a primit pe toate 30, rotația reîncepe.
- **Streak:** zile locale consecutive cu quest completat. Dacă azi nu ai apucat să completezi, dar ieri da, streak-ul e încă în viață (mai ai până la miezul nopții). O zi întreagă ratată îl resetează la 0; recordul (`longest`) se păstrează.
- **XP:** `XpService` însumează mai multe `XpStrategy` (Strategy pattern): `DifficultyXpStrategy` (Ușor 10 / Mediu 20 / Greu 35) și `StreakBonusXpStrategy` (+2 XP pe fiecare zi de streak după prima, maximum +20). O regulă nouă înseamnă doar un `@Component` nou, fără să modifici `XpService`.
- **Niveluri:** nivelul *n* începe la `50·n·(n−1)` XP și are lățimea `100·n` (nivel 2 la 100 XP, nivel 3 la 300, nivel 4 la 600…). Titlurile: Cartof de Canapea → Ucenic Curios → Explorator de Cartier → Vânător de Misiuni → Cavaler al Rutinei → Maestru al Side-Quest-urilor → Legendă Locală → Boss Final.
- **Login fără parolă:** căutarea după username e case-insensitive (`Maria` și `maria` sunt același cont). Dacă două cereri creează simultan același username nou, cea care pierde cursa pe `UNIQUE(username)` citește contul câștigătorului și se loghează în el, deci ambele primesc același utilizator.
- **Dovezi:** pozele se salvează în `uploads/` sub un nume generat (`<uuid>.<ext>`), nu sub numele trimis de client. Formatul (JPG, PNG, GIF, WebP) se verifică după primii octeți ai fișierului, nu după `Content-Type`, deci un HTML deghizat în `.png` e respins. Endpoint-ul de servire acceptă doar nume de forma celor generate (fără `../`) și trimite `X-Content-Type-Options: nosniff`. Dacă completarea eșuează după ce poza a fost scrisă (`409`, text prea lung), fișierul e șters. În baza de date, `QuestAssignment` are `proof_text`, `proof_image_path` (numele fișierului) și `xp_awarded` (XP-ul acordat efectiv, cu bonusul de streak de atunci, afișat în istoric).
- **Grupuri:** entitățile `Group` (tabela `quest_groups`, pentru că `GROUP` e cuvânt rezervat în SQL și în JPQL) și `GroupMembership` (`UNIQUE(user_id, group_id)`: nu poți fi de două ori în același grup). Codurile de invitație au 6 caractere dintr-un alfabet fără caractere care se confundă (`0/O`, `1/I/L`), deci ~887 milioane de combinații. Sunt generate cu `SecureRandom`, verificate să fie libere și protejate de `UNIQUE(invite_code)`.
- **Leaderboard:** sortare după XP total, apoi după streak-ul curent, apoi după username (ca ordinea să fie stabilă). Membrii cu același XP împart locul (1, 1, 3). Streak-ul fiecărui membru e calculat cu `StreakCalculator`, în fusul orar al acelui membru. Zilele completate ale tuturor membrilor vin dintr-un singur query.
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

**Limitări asumate:** dacă un utilizator își schimbă fusul orar, zilele deja atribuite nu se recalculează. Și, fără parolă, oricine scrie un username existent intră în contul respectiv, iar oricine cunoaște un `id` poate cere datele acelui utilizator. Pozele au nume UUID greu de ghicit, dar nu sunt protejate de autentificare.

## Teste

`./mvnw test` rulează 146 de teste:

- **`StreakCalculatorTest`** (48): zi ratată, completare la **23:59 vs 00:01**, fusuri orare diferite (același instant → zile diferite; Kiritimati UTC+14, Pago Pago UTC−11, Kolkata UTC+5:30), **DST** primăvară și toamnă în București și New York (ziua de 23h/25h, ora „inexistentă” și ora repetată), granițe de an/lună/an bisect, date din viitor, duplicate. Un test de mutație (înlocuirea fusului utilizatorului cu UTC) pică 18 teste.
- **`XpServiceTest`, `LevelServiceTest`**: recompense, plafon de bonus, granițele nivelurilor, titluri.
- **`SideQuestApiTest`**: fluxul HTTP complet cu H2 în memorie și `Clock` mutabil (streak peste miezul nopții și peste DST, zi ratată, două cereri concurente, rotația celor 30 de quest-uri). Nou în Faza 1:
  - **login:** username existent → `200` cu același id, fus orar păstrat, alt case acceptat; login-ul returnează XP-ul, streak-ul (încă viu dacă azi nu e completat, dar ieri da) și istoricul; username nou → `201` cu istoric gol; două înregistrări simultane cu același username nou ajung în același cont (`200` + `201`);
  - **istoric:** doar quest-urile completate, descrescător, cu XP-ul acordat efectiv; `404` pentru utilizator inexistent;
  - **dovezi:** poză + text (numele trimis de client e ignorat, poza e servită identic, apare în istoric și în quest-ul zilei), doar text, doar poză, fără nimic; fișier care nu e poză → `400` și quest-ul rămâne deschis; text > 500 → `400` fără poză orfană; a doua completare cu poză → `409` fără poză orfană; `/uploads` refuză nume arbitrare și `../`.
- **`GroupApiTest`** (Faza 2): creare grup (cod de 6 caractere, creatorul e membru, validare nume/creator), coduri unice și fără caractere ambigue, join cu cod valid (și scris cu litere mici, spații, cratimă), **join cu cod invalid** → `404` cu mesaj clar, **join când ești deja membru** (inclusiv creatorul) → `409`, un user în mai multe grupuri, **ordinea din leaderboard** (XP descrescător, rang, streak curent, nivel, titlu; un membru care a ratat o zi are streak 0) și locuri împărțite la XP egal.
- **`InviteCodeGeneratorTest`**: lungime, alfabet, normalizarea codului scris de utilizator.
- **Deploy**: `DatabaseUrlEnvironmentPostProcessorTest` (URL-ul intern Render fără port, URL extern cu port, `sslmode` și parolă codată, URL-uri invalide, prioritatea `DB_*` față de `DATABASE_URL`), `CorsConfigTest` și `CorsTest` (origine exactă și pattern de preview Vercel permise, alte origini respinse cu `403`, `/actuator/health` expus, restul actuator-ului nu).

Profilul `prod` a fost verificat și manual, cap-coadă, pe un PostgreSQL 18 real: pornire cu `DATABASE_URL` și `PORT`, crearea schemei, login, completare cu poză, istoric, grupuri, leaderboard, CORS.
- **`QuestSeederTest`, `QuestAssignmentConstraintTest`**: seed-ul (30 de quest-uri, 4 categorii, diacritice intacte) și constrângerile `UNIQUE` (inclusiv membru dublu în grup și cod de invitație duplicat).

Testele scriu pozele în `backend/target/test-uploads/`, nu în `uploads/`.

CI: `.github/workflows/ci.yml` rulează `mvn test` la fiecare push și pull request.
