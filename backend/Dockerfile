# Render nu are runtime nativ pentru Java, deci backend-ul rulează ca imagine Docker.
# Build-ul folosește Maven Wrapper, exact ca local.

# ---------- build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Întâi doar wrapper-ul și pom.xml: stratul cu dependențe se refolosește cât timp pom.xml nu se schimbă.
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Pe Windows mvnw poate fi comis fără bitul de execuție.
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Testele rulează în CI (GitHub Actions); aici doar împachetăm jar-ul.
RUN ./mvnw -B -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Rulăm ca utilizator neprivilegiat; uploads/ trebuie să fie scriibil de el.
RUN useradd --system --create-home --uid 10001 sidequest && mkdir uploads && chown sidequest uploads
COPY --from=build /src/target/sidequest.jar app.jar
USER sidequest

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"

# Render trimite portul în PORT (server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
