FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
RUN ./mvnw -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build --chown=app:app /app/target/business-api-0.1.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=remote
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -Djava.awt.headless=true"
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
